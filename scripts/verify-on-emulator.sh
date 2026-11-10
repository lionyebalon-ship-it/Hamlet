#!/usr/bin/env bash
set -euo pipefail

root_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
sdk_root="${ANDROID_HOME:-$HOME/Android/Sdk}"
avd_name="${HAMLET_AVD_NAME:-HamletPixel8}"
emulator_pid=""
emulator_log="$(mktemp)"
instrumentation_output=""

cleanup() {
    if [[ -n "$emulator_pid" ]]; then
        kill "$emulator_pid" 2>/dev/null || true
        wait "$emulator_pid" 2>/dev/null || true
    fi
    rm -f "$emulator_log"
}
trap cleanup EXIT

export HOME="${HOME:-/home/codespace}"
export ANDROID_HOME="$sdk_root"
export ANDROID_SDK_ROOT="$sdk_root"
export ANDROID_AVD_HOME="${ANDROID_AVD_HOME:-$HOME/.android/avd}"
export PATH="$sdk_root/platform-tools:$PATH"

cd "$root_dir"
./gradlew assembleDebug assembleDebugAndroidTest --no-daemon

kvm_group="$(stat -c '%g' /dev/kvm)"
sudo setpriv \
    --reuid="$(id -u)" \
    --regid="$(id -g)" \
    --groups="$kvm_group" \
    -- env \
        HOME="$HOME" \
        ANDROID_HOME="$ANDROID_HOME" \
        ANDROID_SDK_ROOT="$ANDROID_SDK_ROOT" \
        ANDROID_AVD_HOME="$ANDROID_AVD_HOME" \
        "$sdk_root/emulator/emulator" \
            -avd "$avd_name" \
            -no-window -no-audio -gpu swiftshader \
            -no-snapshot -no-boot-anim -memory 4096 -cores 4 \
        >"$emulator_log" 2>&1 &
emulator_pid=$!

adb start-server
for _ in $(seq 1 180); do
    if adb devices | grep -qE 'emulator-[0-9]+[[:space:]]+device$'; then
        break
    fi
    sleep 1
done

if ! adb devices | grep -qE 'emulator-[0-9]+[[:space:]]+device$'; then
    cat "$emulator_log"
    echo "Emulator did not connect to adb." >&2
    exit 1
fi

for _ in $(seq 1 300); do
    if [[ "$(adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" == "1" ]]; then
        break
    fi
    sleep 1
done

if [[ "$(adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" != "1" ]]; then
    cat "$emulator_log"
    echo "Android did not finish booting." >&2
    exit 1
fi

adb devices -l
adb install -r build/outputs/apk/debug/Hamlet-debug.apk
adb install -r build/outputs/apk/androidTest/debug/Hamlet-debug-androidTest.apk

if ! instrumentation_output="$(adb shell am instrument -w -r com.hamlet.app.test/androidx.test.runner.AndroidJUnitRunner 2>&1)"; then
    printf '%s\n' "$instrumentation_output"
    cat "$emulator_log"
    adb devices -l
    exit 1
fi
printf '%s\n' "$instrumentation_output"
if ! grep -qE '^OK \([0-9]+ tests?\)$' <<<"$instrumentation_output"; then
    echo "Compose instrumentation tests did not report success." >&2
    exit 1
fi

adb shell am force-stop com.hamlet.app
launch_output="$(adb shell am start -W -n com.hamlet.app/.MainActivity 2>&1)"
printf '%s\n' "$launch_output"
if ! grep -q '^Status: ok$' <<<"$launch_output" ||
    ! grep -q '^Activity: com.hamlet.app/.MainActivity$' <<<"$launch_output"; then
    echo "Hamlet did not launch successfully." >&2
    exit 1
fi
sleep 15
adb exec-out screencap -p > /tmp/hamlet-current-screen.png
echo "Captured /tmp/hamlet-current-screen.png"
