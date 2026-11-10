# Hamlet project status and work log

**Status reviewed:** 2026-11-10

This is the living record of Hamlet's implemented foundation, important project
decisions, verification, and remaining work. Update it when a milestone changes
the actual behavior of the app; do not mark a feature complete merely because it
has been planned or has a supporting data structure.

## Product direction

Hamlet is an online-first Android medieval village simulation. AI should help
drive villagers' schedules, goals, and context-sensitive decisions. The
simulation remains authoritative: a provider proposes an action, and the game
validates and applies it. Local deterministic behavior is a clearly surfaced
resilience mode when AI is unavailable, not a substitute for the intended
online-first experience.

The player primarily observes and influences a persistent village rather than
directly controlling every villager. The long-term vision includes autonomous
characters with needs, personalities, ambitions, memories, relationships, and
social histories. The current app is a tested foundation and visual prototype,
not yet that complete simulation.

## Current implementation

| Area | Status | What is present |
|---|---|---|
| Android app | Implemented foundation | Single-module Kotlin app with a Jetpack Compose interface. |
| Starter world | Implemented foundation | Deterministic initialization of 12 villagers, 10 locations, and directed initial relationships. |
| AI decision boundary | Implemented foundation | Typed provider interface, per-villager context, proposal validation, stale-revision rejection, and deterministic fallback behavior. |
| Live AI service | Not implemented | The app reports AI as disconnected; there is no Kilo adapter or live provider configuration. |
| Simulation time | UI prototype | The interface has clock, pause, and speed controls. A persisted core world clock and calendar are not implemented. |
| Outdoor map | Implemented | Fixed, authored 64 x 40 Tiled TMX map with an original pixel-art tileset. It is not procedurally generated at runtime. |
| Map interaction | Implemented | Finger-drag panning, pinch zoom, building selection, villager selection, and a visible selected-villager highlight. |
| Premade interiors | Implemented | Nine distinct, editable rooms, sized from 9 x 7 to 12 x 9 tiles, with purpose-specific floors and furniture. Tapping a building opens its linked room. |
| Tile navigation groundwork | Implemented, not connected to NPCs | Room entry coordinates, blocked-tile grids, a four-direction pathfinder, and tests for doorway-to-floor reachability. |
| Autonomous villager movement | Not implemented | Villagers do not yet walk along outdoor or indoor paths, enter buildings, or animate as part of simulation behavior. |
| Persistence | Not implemented | The world is initialized in memory; state is not saved and restored between app sessions. |
| Memories feature | Not implemented | A villager memory timeline/tab was requested but has not been built. |
| Long-term simulation | Not implemented | Needs, active schedules, autonomous goals, dialogue, memory, dynamic social behavior, economy, and production remain future work. |

## Work completed

### Project direction and testable foundation

- Reviewed the original project aim and revised the master prompt to make
  online-first AI a core gameplay requirement, with deterministic local
  behavior treated as a transparent fallback.
- Created the Kotlin simulation model and deterministic starter village:
  12 named villagers, 10 locations, and seeded initial relationships.
- Added a typed AI decision-provider boundary and tests for valid decisions,
  invalid or stale proposals, and unavailable-provider fallback behavior.
- Established a native Android/Compose build and test workflow in the
  Codespace.

### Pixel-art village and navigation controls

- Researched the Android rendering/tooling options and chose Tiled-compatible
  TMX maps and original pixel-art assets with the existing Kotlin/Compose app,
  rather than replacing the app with a Godot project.
- Built a fixed 64 x 40 village map arranged around a shared green, with
  small-scale cottages and workshops, paths, gardens, farm, woodland, stream,
  and bridge. Map and tileset generator scripts reproduce the authored assets.
- Connected the map renderer to the Tiled map and tileset.
- Added drag-to-pan and pinch-to-zoom controls, villager selection and
  highlighting, clock controls, and a clear AI disconnected state.
- Added building-to-interior links and controls for entering a room and
  returning outdoors.

### Distinct interiors and tile walkability

- Replaced the oversized, repetitive rooms with nine compact, fixed layouts:
  tavern, cottage, smithy, bakery, chapel, farmstead, carpenter workshop,
  weaver cottage, and market store.
- Expanded the original interior tileset with varied floor materials and
  building-specific furniture. The rooms are authored maps, not procedurally
  generated spaces.
- Used an English hall-house reference as broad historical inspiration for
  some domestic layouts; this is not intended to represent every region,
  period, or household.
- Added entry-tile metadata, walkability derived from floor/wall/furniture
  layers, and a four-direction tile pathfinder. Tests verify that each room's
  doorway is walkable and every walkable floor tile is reachable from it.
- Captured and checked nine screenshots from the running Android app. The
  gallery is in [`../screenshots/interiors/`](../screenshots/interiors/).

## Remaining work, in priority order

These are implementation priorities, not claims that work is already in
progress. Keep them ordered by the dependencies needed for a useful playable
vertical slice.

### 1. Complete a persistent, testable simulation loop

- Move simulation time and calendar into the platform-independent world model;
  make pause and speed controls drive that model rather than only UI state.
- Add a reliable save/restore path for the world and verify that villagers,
  relationships, clock, and world revision survive app restarts.
- Define deterministic world-tick and action-execution rules, with focused JVM
  tests for time advancement and state transitions.

### 2. Connect online AI to real villager decisions

- Implement the Kilo provider behind the existing provider interface. Keep
  credentials out of source control and make provider/model configuration
  explicit.
- Add structured response parsing and validation, request timeouts, bounded
  retries/concurrency, rate-limit handling, and usage/error reporting.
- Replace the always-disconnected app wiring with real connection state and
  clear user-visible outcomes. Preserve and test fallback behavior for
  offline, authentication, rate-limit, and malformed-response cases.
- Schedule AI requests at appropriate events rather than making unbounded
  requests every simulation tick. The simulation must validate every proposed
  action before execution.
- Test orchestration with fake providers without requiring live credentials;
  verify live connectivity separately when safe credentials and network access
  are available.

### 3. Make villagers navigate and act in the world

- Implement path planning for the outdoor map and transitions between outdoor
  locations and authored interiors.
- Connect NPC decisions and schedules to movement, collision, building entry,
  and activity state changes.
- Add visible movement and activity animations, then cover navigation and
  transitions with deterministic tests and emulator checks.
- Keep tile collision consistent across map rendering, route planning, and
  NPC movement.

### 4. Implement requested character-facing features

- Add the requested selected-villager memories entry and a separate,
  chronological memory view. First define what is stored, how it is retrieved,
  and how private memories are kept private.
- Add needs, flexible daily routines, and goal/plan state so villagers have
  meaningful reasons to choose actions.
- Make relationship changes and social memories follow validated events and
  interactions, not arbitrary updates.

### 5. Grow the simulation after the vertical slice is reliable

- Add perception, autonomous goal selection, planning, and interruption-aware
  schedules.
- Add dialogue and social events, then economy, production, trade, and
  community projects.
- Improve save migration, accessibility, performance, error handling, and
  release packaging as those features become concrete.

## Verification record

The following checks have passed during this work:

- JVM tests for deterministic world initialization and AI decision validation
  and fallback behavior.
- JVM tests for Tiled parsing, interior dimensions/materials, doorway access,
  furniture blocking, and room reachability.
- Debug app and Android instrumentation APK builds.
- Tiled CLI validation of the outdoor map and authored interior maps.
- Compose instrumentation tests for the map UI, selection/navigation,
  building entry/exit, and AI-disconnected fallback.
- The nine-room screenshot capture test; its captured images were pulled from
  the emulator and checked into the screenshot gallery.

Useful local checks:

```sh
./gradlew testDebugUnitTest assembleDebug
./gradlew assembleDebugAndroidTest
./gradlew connectedDebugAndroidTest
git diff --check
```

Emulator-backed tests require the configured `HamletPixel8` Android virtual
device. A passing map/pathfinding test proves that the route data is valid; it
does not prove that NPCs currently move in the app.

## Project decisions and accuracy rules

- Keep the simulation core independent of Android UI and lifecycle APIs.
- AI proposes; the simulation validates and executes. Never let provider
  output mutate authoritative world state directly.
- Keep AI as the intended primary experience and make degraded mode explicit.
- Keep the starter village and rooms hand-authored and fixed. Do not introduce
  runtime procedural map generation unless the product direction changes.
- Continue using the native Kotlin/Compose app and Tiled-compatible TMX workflow
  unless a concrete requirement justifies a migration.
- Use original art assets. Treat user-provided screenshots as inspiration, not
  assets to copy into the game.
- Report implemented behavior, test evidence, and remaining limitations
  separately. In particular, do not describe the pathfinder as working NPC
  movement or the disconnected provider boundary as live AI integration.
