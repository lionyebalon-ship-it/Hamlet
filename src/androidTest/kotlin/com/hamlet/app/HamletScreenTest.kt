package com.hamlet.app

import android.graphics.Bitmap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.click
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.assertNotEquals
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HamletScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun villageScreenShowsWorldAndAiConnectionState() {
        composeRule.onNodeWithText("HAMLET  //  SETTLEMENT SIM").assertIsDisplayed()
        composeRule.onNodeWithText("AI DISCONNECTED").assertIsDisplayed()
        composeRule.onNodeWithText("THE FIRST HAMLET").assertIsDisplayed()
        composeRule.onNodeWithText("12 SOULS").assertIsDisplayed()
        composeRule.onNodeWithText("SELECTED").assertIsDisplayed()
        composeRule.onNodeWithText("DRAG TO PAN  ·  PINCH TO ZOOM").assertIsDisplayed()
        composeRule.onNodeWithText("ZOOM 1.0x").assertIsDisplayed()
    }

    @Test
    fun tappingMapVillagerSelectsTheirProfile() {
        val map = composeRule.onNodeWithTag("village-map-canvas")
        val mapSize = map.fetchSemanticsNode().size
        map.performTouchInput {
            click(Offset(mapSize.width * 0.5f, mapSize.height * 0.52f))
        }

        composeRule.onNodeWithText("ELIZA  ·  34")
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun tappingBuildingOpensItsPremadeInteriorAndReturnsOutside() {
        val map = composeRule.onNodeWithTag("village-map-canvas")
        val mapSize = map.fetchSemanticsNode().size
        map.performTouchInput {
            click(Offset(mapSize.width * 0.45f, mapSize.height * 0.4f))
        }

        composeRule.onNodeWithText("THE GOLDEN STAG - COMMON ROOM").assertIsDisplayed()
        composeRule.onNodeWithTag("leave-interior").performClick()
        composeRule.onNodeWithText("THE FIRST HAMLET").assertIsDisplayed()
    }

    @Test
    fun capturesEveryDistinctPremadeInterior() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val targetContext = instrumentation.targetContext
        val villageMap = targetContext.assets.open("maps/hamlet.tmx")
            .use(TiledMapParser::parse)
        val outputDirectory = File(targetContext.getExternalFilesDir(null), "interior-captures")
        check(outputDirectory.mkdirs() || outputDirectory.isDirectory)
        val interiors = listOf(
            "The Golden Stag Tavern" to "THE GOLDEN STAG - COMMON ROOM",
            "North Cottage" to "A HAMLET COTTAGE",
            "Village Blacksmith" to "THE VILLAGE SMITHY",
            "Village Bakery" to "THE VILLAGE BAKERY",
            "Village Chapel" to "THE VILLAGE CHAPEL",
            "Carpenter Workshop" to "THE CARPENTER'S WORKSHOP",
            "Farmstead" to "THE FARMSTEAD",
            "Weaver Cottage" to "THE WEAVER'S COTTAGE",
            "Market Stalls" to "THE MARKET STORE",
        )

        interiors.forEach { (buildingName, roomTitle) ->
            val map = composeRule.onNodeWithTag("village-map-canvas")
            val mapSize = map.fetchSemanticsNode().size
            val scale = max(
                mapSize.width / villageMap.pixelWidth.toFloat(),
                mapSize.height / villageMap.pixelHeight.toFloat(),
            )
            val building = villageMap.objectLayers
                .flatMap { it.objects }
                .single { it.name == buildingName }
            val tap = Offset(
                x = (mapSize.width - villageMap.pixelWidth * scale) / 2f +
                    (building.x + building.width / 2f) * scale,
                y = (mapSize.height - villageMap.pixelHeight * scale) / 2f +
                    (building.y + building.height / 2f) * scale,
            )
            map.performTouchInput { click(tap) }
            composeRule.onNodeWithText(roomTitle).assertIsDisplayed()
            composeRule.waitForIdle()
            Thread.sleep(500)

            val bitmap = checkNotNull(instrumentation.uiAutomation.takeScreenshot())
            val roomName = requireNotNull(building.interiorMap)
                .substringAfterLast('/')
                .removeSuffix(".tmx")
            val file = File(outputDirectory, "$roomName.png")
            val saved = FileOutputStream(file).use {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
            }
            bitmap.recycle()
            check(saved) { "Failed to write screenshot for $roomName" }

            composeRule.onNodeWithTag("leave-interior").performClick()
            composeRule.onNodeWithText("THE FIRST HAMLET").assertIsDisplayed()
        }
    }

    @Test
    fun pinchingZoomsAndDraggingPansTheVillageMap() {
        val map = composeRule.onNodeWithTag("village-map-canvas")
        val mapSize = map.fetchSemanticsNode().size
        val center = Offset(mapSize.width / 2f, mapSize.height / 2f)
        val pinchDelta = mapSize.width * 0.08f
        map.performTouchInput {
            down(0, center - Offset(pinchDelta, 0f))
            down(1, center + Offset(pinchDelta, 0f))
            moveTo(0, center - Offset(pinchDelta * 1.8f, 0f), delayMillis = 100)
            moveTo(1, center + Offset(pinchDelta * 1.8f, 0f), delayMillis = 100)
            up(0)
            up(1)
        }

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("ZOOM 1.0x").fetchSemanticsNodes().isEmpty()
        }
        val zoomedMap = composeRule.onNodeWithTag("village-map-canvas")
        val zoomDescription = zoomedMap.fetchSemanticsNode().config[SemanticsProperties.StateDescription]
        zoomedMap.performTouchInput {
            down(0, center)
            moveTo(0, center + Offset(mapSize.width * 0.15f, 0f), delayMillis = 350)
            up(0)
        }
        val pannedDescription = zoomedMap.fetchSemanticsNode().config[SemanticsProperties.StateDescription]
        assertNotEquals(zoomDescription, pannedDescription)
        composeRule.onNodeWithText("DRAG TO PAN  ·  PINCH TO ZOOM").assertIsDisplayed()
    }

    @Test
    fun askingForAPlanUsesAnExplicitFallbackWhileProviderIsDisconnected() {
        val askButton = composeRule.onNodeWithText("✦   ASK FOR A PLAN")
        askButton.performScrollTo()
        askButton.performClick()

        composeRule.onNodeWithText("AI is disconnected. Local fallback:", substring = true)
            .performScrollTo()
            .assertIsDisplayed()
    }
}
