package com.hamlet.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.max
import kotlin.math.min
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hamlet.core.AiDecisionProvider
import com.hamlet.core.DecisionOutcome
import com.hamlet.core.DecisionSource
import com.hamlet.core.HamletWorld
import com.hamlet.core.NpcActivity
import com.hamlet.core.NpcDecisionService
import com.hamlet.core.ProviderFailure
import com.hamlet.core.ProviderResult
import com.hamlet.core.Villager
import com.hamlet.core.WorldInitializer
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val Night = Color(0xFF11191B)
private val Panel = Color(0xFF1C292A)
private val PanelRaised = Color(0xFF29383A)
private val Border = Color(0xFF52604A)
private val Paper = Color(0xFFF1E2BA)
private val Muted = Color(0xFFB4B798)
private val Gold = Color(0xFFF0C75E)
private val Leaf = Color(0xFFA8C47A)
private val Warning = Color(0xFFE5A45C)
private const val MIN_MAP_ZOOM = 1f
private const val MAX_MAP_ZOOM = 3.5f

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(
                background = Night,
                surface = Panel,
                primary = Gold,
                onPrimary = Night,
                onBackground = Paper,
                onSurface = Paper,
            )) {
                HamletScreen()
            }
        }
    }
}

@Composable
private fun HamletScreen() {
    val context = LocalContext.current
    val mapArtwork = remember(context) { loadHamletMap(context.assets) }
    var interiorArtwork by remember { mutableStateOf<HamletMapArtwork?>(null) }
    val initialWorld = remember { WorldInitializer.createFirstHamlet() }
    var world by remember { mutableStateOf(initialWorld) }
    var selectedVillagerId by remember {
        mutableStateOf(initialWorld.villagers.values.first().id)
    }
    var clockMinutes by remember { mutableIntStateOf(8 * 60 + 40) }
    var speedIndex by remember { mutableIntStateOf(1) }
    var isPaused by remember { mutableStateOf(false) }
    var isPlanning by remember { mutableStateOf(false) }
    var latestMessage by remember { mutableStateOf<String?>(null) }
    var latestSource by remember { mutableStateOf<DecisionSource?>(null) }
    val selectedVillager = world.villagers.getValue(selectedVillagerId)
    val scroll = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    val decisionService = remember {
        NpcDecisionService(
            AiDecisionProvider {
                ProviderResult.Failure(ProviderFailure.UNAVAILABLE)
            },
        )
    }
    val speedMultipliers = listOf(1, 3, 6)
    val speedLabels = listOf("1×", "3×", "6×")

    LaunchedEffect(isPaused, speedIndex) {
        val multiplier = speedMultipliers[speedIndex]
        if (!isPaused) {
            while (true) {
                delay((12_000L / multiplier).coerceAtLeast(250L))
                clockMinutes = (clockMinutes + 1) % (24 * 60)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Night)
            .verticalScroll(scroll)
            .padding(
                top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 10.dp,
                bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 18.dp,
            )
            .padding(horizontal = 20.dp),
    ) {
        Header()
        Spacer(Modifier.height(18.dp))
        ClockPanel(
            clockMinutes = clockMinutes,
            speedLabel = speedLabels[speedIndex],
            isPaused = isPaused,
            onTogglePause = { isPaused = !isPaused },
            onCycleSpeed = { speedIndex = (speedIndex + 1) % speedLabels.size },
        )
        Spacer(Modifier.height(18.dp))
        SectionHeading(
            title = "THE VILLAGE",
            trailing = "12 SOULS",
        )
        Spacer(Modifier.height(9.dp))
        VillageMap(
            world = world,
            artwork = interiorArtwork ?: mapArtwork,
            selectedVillagerId = selectedVillagerId,
            onSelectVillager = { selectedVillagerId = it },
            onEnterInterior = { assetPath ->
                interiorArtwork = loadHamletMap(context.assets, assetPath)
            },
            onExitInterior = { interiorArtwork = null },
        )
        Spacer(Modifier.height(20.dp))
        SectionHeading(title = "YOUR VILLAGERS", trailing = "VIEW ALL  →")
        Spacer(Modifier.height(10.dp))
        VillagerRoster(
            villagers = world.villagers.values.toList(),
            selectedVillagerId = selectedVillagerId,
            onSelect = { selectedVillagerId = it },
        )
        Spacer(Modifier.height(15.dp))
        ProfileCard(
            villager = selectedVillager,
            isPlanning = isPlanning,
            latestMessage = latestMessage,
            latestSource = latestSource,
            onRequestPlan = {
                if (!isPlanning) {
                    isPlanning = true
                    latestMessage = null
                    coroutineScope.launch {
                        when (val outcome = decisionService.decide(world, selectedVillagerId)) {
                            is DecisionOutcome.Applied -> {
                                world = outcome.world
                                latestSource = outcome.source
                                val action = when (outcome.action.type) {
                                    com.hamlet.core.ProposedActionType.TRAVEL_TO -> {
                                        val destination = outcome.action.targetLocationId
                                            ?.let(world.locations::get)?.name ?: "the village"
                                        "Plans to travel to $destination."
                                    }
                                    com.hamlet.core.ProposedActionType.WORK -> "Settles in for a day's work."
                                    com.hamlet.core.ProposedActionType.REST -> "Finds a quiet place to rest."
                                    com.hamlet.core.ProposedActionType.OBSERVE -> "Takes a moment to observe the village."
                                    com.hamlet.core.ProposedActionType.END_ACTIVITY -> "Ends the current activity."
                                }
                                latestMessage = if (outcome.source == DecisionSource.AI) {
                                    action
                                } else {
                                    "AI is disconnected. Local fallback: $action"
                                }
                            }
                            is DecisionOutcome.Rejected -> {
                                latestSource = null
                                latestMessage = "Decision rejected: ${outcome.reason}"
                            }
                        }
                        isPlanning = false
                    }
                }
            },
        )
        Spacer(Modifier.height(18.dp))
        AiStatusFooter()
    }
}

@Composable
private fun Header() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Text(
                "HAMLET  //  SETTLEMENT SIM",
                color = Gold,
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp,
            )
            Text(
                "A living village",
                color = Paper,
                fontSize = 22.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
            )
        }
        Row(
            modifier = Modifier
                .clip(CutCornerShape(5.dp))
                .background(Color(0xFF3B2E20))
                .border(2.dp, Color(0xFF8A6237), CutCornerShape(5.dp))
                .padding(horizontal = 8.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(7.dp).background(Warning))
            Spacer(Modifier.width(6.dp))
            Text(
                "AI DISCONNECTED",
                color = Color(0xFFE3BD8C),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 0.6.sp,
            )
        }
    }
}

@Composable
private fun ClockPanel(
    clockMinutes: Int,
    speedLabel: String,
    isPaused: Boolean,
    onTogglePause: () -> Unit,
    onCycleSpeed: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CutCornerShape(8.dp))
            .background(Panel)
            .border(2.dp, Border, CutCornerShape(8.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Text("DAY 01  ·  SPRING", color = Muted, fontSize = 10.sp, fontFamily = FontFamily.Monospace, letterSpacing = 0.7.sp)
            Spacer(Modifier.height(3.dp))
            val hour = clockMinutes / 60
            val minute = clockMinutes % 60
            val displayHour = when {
                hour % 12 == 0 -> 12
                else -> hour % 12
            }
            Text(
                "%d:%02d %s".format(displayHour, minute, if (hour < 12) "AM" else "PM"),
                color = Paper,
                fontSize = 23.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            SmallControl(label = if (isPaused) "▶" else "Ⅱ", onClick = onTogglePause)
            Spacer(Modifier.width(8.dp))
            SmallControl(label = speedLabel, onClick = onCycleSpeed, wide = true)
        }
    }
}

@Composable
private fun SmallControl(label: String, onClick: () -> Unit, wide: Boolean = false) {
    Box(
        modifier = Modifier
            .clip(CutCornerShape(4.dp))
            .background(PanelRaised)
            .clickable(onClick = onClick)
            .padding(horizontal = if (wide) 12.dp else 13.dp, vertical = 11.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = Gold, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
    }
}

@Composable
private fun SectionHeading(title: String, trailing: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, color = Paper, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, letterSpacing = 0.6.sp)
        Text(trailing, color = Gold, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, letterSpacing = 0.2.sp)
    }
}

@Composable
private fun VillageMap(
    world: HamletWorld,
    artwork: HamletMapArtwork,
    selectedVillagerId: String,
    onSelectVillager: (String) -> Unit,
    onEnterInterior: (String) -> Unit,
    onExitInterior: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp)
            .clip(CutCornerShape(8.dp))
            .border(2.dp, Color(0xFF89956A), CutCornerShape(8.dp)),
    ) {
        VillageMapCanvas(
            world = world,
            artwork = artwork,
            selectedVillagerId = selectedVillagerId,
            onSelectVillager = onSelectVillager,
            onEnterInterior = onEnterInterior,
        )
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(11.dp)
                .clip(CutCornerShape(4.dp))
                .background(Color(0xF0172420))
                .border(1.dp, Color(0xFF88976A), CutCornerShape(4.dp))
                .padding(horizontal = 9.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("◆", color = Gold, fontSize = 10.sp)
            Spacer(Modifier.width(6.dp))
            Text(
                (artwork.map.title ?: world.name).uppercase(),
                color = Paper,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 0.1.sp,
            )
        }
        if (artwork.map.title != null) {
            Text(
                "← OUTSIDE",
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 11.dp, top = 48.dp)
                    .clip(CutCornerShape(4.dp))
                    .background(Color(0xF0172420))
                    .border(1.dp, Color(0xFF88976A), CutCornerShape(4.dp))
                    .clickable(onClick = onExitInterior)
                    .padding(horizontal = 8.dp, vertical = 6.dp)
                    .testTag("leave-interior"),
                color = Gold,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
            )
        }
        if (artwork.map.title == null) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(11.dp)
                    .clip(CutCornerShape(4.dp))
                    .background(Color(0xF0172420))
                    .border(1.dp, Color(0xFF88976A), CutCornerShape(4.dp))
                    .padding(horizontal = 9.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(6.dp).background(Leaf))
                Spacer(Modifier.width(5.dp))
                Text("${world.locations.size} LOCATIONS", color = Paper, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            }
        }
        Text(
            "DRAG TO PAN  ·  PINCH TO ZOOM",
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 11.dp, bottom = 11.dp)
                .clip(CutCornerShape(4.dp))
                .background(Color(0xF0172420))
                .border(1.dp, Color(0xFF88976A), CutCornerShape(4.dp))
                .padding(horizontal = 8.dp, vertical = 6.dp)
                .testTag("map-gesture-hint"),
            color = Paper,
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
        )
    }
}

private fun mapBaseScale(viewportWidth: Float, viewportHeight: Float, map: TiledMap): Float {
    val horizontalScale = viewportWidth / map.pixelWidth
    val verticalScale = viewportHeight / map.pixelHeight
    return if (map.title == null) {
        max(horizontalScale, verticalScale)
    } else {
        min(horizontalScale, verticalScale)
    }
}

@Composable
private fun VillageMapCanvas(
    world: HamletWorld,
    artwork: HamletMapArtwork,
    selectedVillagerId: String,
    onSelectVillager: (String) -> Unit,
    onEnterInterior: (String) -> Unit,
) {
    val map = artwork.map
    val objects = map.objectLayers.flatMap { it.objects }
    val selectedVillagerName = world.villagers[selectedVillagerId]?.name
    var cameraZoom by remember(map) { mutableFloatStateOf(1f) }
    var cameraPan by remember(map) { mutableStateOf(Offset.Zero) }
    Box(Modifier.fillMaxSize()) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    if (map.title == null) Color(0xFF638052) else Color(0xFF3B3128),
                )
                .testTag("village-map-canvas")
                .semantics {
                    stateDescription =
                        "Map zoom ${cameraZoom}x, pan ${cameraPan.x.toInt()}, ${cameraPan.y.toInt()} pixels"
                }
                .pointerInput(map) {
                    detectTransformGestures { centroid, pan, zoomChange, _ ->
                        val oldZoom = cameraZoom
                        val newZoom = (oldZoom * zoomChange).coerceIn(MIN_MAP_ZOOM, MAX_MAP_ZOOM)
                        val zoomRatio = newZoom / oldZoom
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val centeredCentroid = centroid - center
                        val newPan = Offset(
                            x = centeredCentroid.x - (centeredCentroid.x - cameraPan.x) * zoomRatio + pan.x,
                            y = centeredCentroid.y - (centeredCentroid.y - cameraPan.y) * zoomRatio + pan.y,
                        )
                        val baseScale = mapBaseScale(
                            size.width.toFloat(),
                            size.height.toFloat(),
                            map,
                        )
                        val maxPanX = ((map.pixelWidth * baseScale * newZoom) - size.width).coerceAtLeast(0f) / 2f
                        val maxPanY = ((map.pixelHeight * baseScale * newZoom) - size.height).coerceAtLeast(0f) / 2f
                        cameraZoom = newZoom
                        cameraPan = Offset(
                            x = newPan.x.coerceIn(-maxPanX, maxPanX),
                            y = newPan.y.coerceIn(-maxPanY, maxPanY),
                        )
                    }
                }
                .pointerInput(map, world, cameraZoom, cameraPan) {
                    detectTapGestures { tap ->
                        val baseScale = mapBaseScale(
                            size.width.toFloat(),
                            size.height.toFloat(),
                            map,
                        )
                        val renderedScale = baseScale * cameraZoom
                            val mapOrigin = Offset(
                                x = (size.width - map.pixelWidth * renderedScale) / 2f + cameraPan.x,
                                y = (size.height - map.pixelHeight * renderedScale) / 2f + cameraPan.y,
                            )
                            val mapX = (tap.x - mapOrigin.x) / renderedScale
                            val mapY = (tap.y - mapOrigin.y) / renderedScale
                            objects.lastOrNull { objectOnMap ->
                                objectOnMap.type in setOf("villager", "building") &&
                                    mapX in objectOnMap.x..(objectOnMap.x + objectOnMap.width) &&
                                    mapY in objectOnMap.y..(objectOnMap.y + objectOnMap.height)
                            }?.let { tappedObject ->
                                if (tappedObject.type == "building") {
                                    tappedObject.interiorMap?.let(onEnterInterior)
                                } else {
                                    world.villagers.values
                                        .firstOrNull { it.name == tappedObject.name }
                                        ?.let { onSelectVillager(it.id) }
                                }
                            }
                    }
                },
        ) {
            val baseScale = mapBaseScale(size.width, size.height, map)
            val renderedScale = baseScale * cameraZoom
            val origin = Offset(
                x = (size.width - map.pixelWidth * renderedScale) / 2f + cameraPan.x,
                y = (size.height - map.pixelHeight * renderedScale) / 2f + cameraPan.y,
            )
            withTransform({
                translate(origin.x, origin.y)
                scale(renderedScale, renderedScale, Offset.Zero)
            }) {
                drawImage(
                    image = artwork.terrainImage,
                    dstOffset = IntOffset.Zero,
                    dstSize = IntSize(map.pixelWidth, map.pixelHeight),
                    filterQuality = FilterQuality.None,
                )
                objects.sortedBy { it.y + it.height }.forEachIndexed { index, mapObject ->
                    when (mapObject.type) {
                        "building" -> drawBuilding(
                            mapObject.x,
                            mapObject.y,
                            mapObject.width,
                            mapObject.height,
                            mapObject.name.uppercase(),
                            buildingRoofColor(mapObject.name),
                        )
                        "tree" -> drawTree(mapObject.x, mapObject.y, mapObject.width, mapObject.height)
                        "villager" -> drawVillager(
                            mapObject.x,
                            mapObject.y,
                            mapObject.width,
                            mapObject.height,
                            index,
                            mapObject.name == selectedVillagerName,
                        )
                        "bridge", "dock" -> drawDock(mapObject.x, mapObject.y, mapObject.width, mapObject.height)
                    }
                }
            }
        }
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(9.dp)
                .clip(CutCornerShape(4.dp))
                .background(Color(0xF0172420))
                .border(1.dp, Color(0xFF88976A), CutCornerShape(4.dp)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "ZOOM ${cameraZoom}x",
                modifier = Modifier
                    .padding(start = 8.dp, top = 6.dp, bottom = 6.dp)
                    .testTag("map-zoom-level"),
                color = Gold,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
            )
            Text(
                "RESET",
                modifier = Modifier
                    .clickable {
                        cameraZoom = 1f
                        cameraPan = Offset.Zero
                    }
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                color = Paper,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
            )
        }
    }
}

private fun DrawScope.drawTree(x: Float, y: Float, width: Float, height: Float) {
    drawRect(Color(0x5532291D), Offset(x + width * 0.1f, y + height * 0.78f), Size(width * 0.8f, height * 0.18f))
    drawRect(Color(0xFF51432F), Offset(x + width * 0.42f, y + height * 0.48f), Size(width * 0.2f, height * 0.46f))
    drawRect(Color(0xFF36523C), Offset(x + width * 0.08f, y + height * 0.32f), Size(width * 0.76f, height * 0.3f))
    drawRect(Color(0xFF466843), Offset(x + width * 0.18f, y + height * 0.12f), Size(width * 0.72f, height * 0.32f))
    drawRect(Color(0xFF7C9B5B), Offset(x + width * 0.35f, y), Size(width * 0.36f, height * 0.25f))
    drawRect(Color(0xFF52774A), Offset(x + width * 0.18f, y + height * 0.58f), Size(width * 0.62f, height * 0.2f))
    drawRect(Color(0xFF89A960), Offset(x + width * 0.25f, y + height * 0.2f), Size(width * 0.2f, height * 0.12f))
}

private fun DrawScope.drawBuilding(
    x: Float,
    y: Float,
    width: Float,
    height: Float,
    label: String,
    roofColor: Color,
) {
    drawRect(Color(0xFF514631), Offset(x - 4, y + height - 2), Size(width + 8, 8f))
    val roofTile = 6f
    for (row in 0 until 3) {
        val inset = (2 - row) * roofTile
        drawRect(
            roofColor,
            Offset(x - 2 + inset, y + row * roofTile),
            Size(width + 4 - inset * 2, roofTile),
        )
        drawRect(
            roofColor.copy(red = (roofColor.red * 0.78f).coerceIn(0f, 1f)),
            Offset(x - 2 + inset, y + row * roofTile + roofTile - 2f),
            Size(width + 4 - inset * 2, 2f),
        )
    }
    drawRect(Color(0xFFDECA96), Offset(x + 5, y + 17), Size(width - 10, height - 19))
    drawRect(Color(0xFFEEE0B4), Offset(x + 8, y + 19), Size(width - 16, height - 21))
    drawRect(Color(0xFF523D2D), Offset(x + width / 2 - 4, y + height - 15), Size(8f, 13f))
    drawRect(Color(0xFF352C25), Offset(x + width / 2 - 2, y + height - 8), Size(2f, 2f))
    drawRect(Color(0xFF6A583E), Offset(x + 10, y + 22), Size(6f, 6f))
    drawRect(Color(0xFF8EA06A), Offset(x + 11, y + 23), Size(4f, 4f))
    drawRect(Color(0xFF6A583E), Offset(x + width - 16, y + 22), Size(6f, 6f))
    drawRect(Color(0xFF8EA06A), Offset(x + width - 15, y + 23), Size(4f, 4f))
    drawIntoCanvas { canvas ->
        val paint = android.graphics.Paint().apply {
            color = android.graphics.Color.rgb(255, 246, 211)
            textSize = 8f
            isAntiAlias = true
            textAlign = android.graphics.Paint.Align.CENTER
            typeface = android.graphics.Typeface.create("monospace", android.graphics.Typeface.BOLD)
            setShadowLayer(2f, 0f, 1f, android.graphics.Color.rgb(20, 32, 23))
        }
        canvas.nativeCanvas.drawText(label, x + width / 2, y + height + 10, paint)
    }
}

private fun DrawScope.drawVillager(
    x: Float,
    y: Float,
    width: Float,
    height: Float,
    paletteIndex: Int,
    selected: Boolean,
) {
    val tunic = listOf(
        Color(0xFFB96B4E), Color(0xFF849C69), Color(0xFFC4A15C),
        Color(0xFF987DAC), Color(0xFF5E9B99), Color(0xFFC17A4A),
    )[paletteIndex % 6]
    if (selected) {
        drawCircle(
            color = Color(0xAA171D13),
            radius = width * 0.72f,
            center = Offset(x + width / 2f, y + height * 0.9f),
        )
        drawCircle(
            color = Gold,
            radius = width * 0.68f,
            center = Offset(x + width / 2f, y + height * 0.9f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f),
        )
        drawRect(Gold, Offset(x + width * 0.32f, y - 5f), Size(width * 0.36f, 3f))
    }
    drawRect(Color(0xFF65462F), Offset(x + width * 0.25f, y + height * 0.08f), Size(width * 0.5f, height * 0.28f))
    drawRect(Color(0xFFD7AE78), Offset(x + width * 0.3f, y + height * 0.2f), Size(width * 0.4f, height * 0.25f))
    drawRect(Color(0xFF49362A), Offset(x + width * 0.25f, y + height * 0.08f), Size(width * 0.5f, height * 0.14f))
    drawRect(tunic, Offset(x + width * 0.12f, y + height * 0.44f), Size(width * 0.76f, height * 0.32f))
    drawRect(tunic.copy(red = (tunic.red * 0.74f).coerceIn(0f, 1f)), Offset(x + width * 0.12f, y + height * 0.68f), Size(width * 0.76f, height * 0.13f))
    drawRect(Color(0xFF51402F), Offset(x + width * 0.25f, y + height * 0.8f), Size(width * 0.2f, height * 0.17f))
    drawRect(Color(0xFF51402F), Offset(x + width * 0.56f, y + height * 0.8f), Size(width * 0.2f, height * 0.17f))
    drawRect(Color(0xFFE7CB8C), Offset(x + width * 0.62f, y + height * 0.27f), Size(width * 0.12f, height * 0.08f))
}

private fun DrawScope.drawDock(x: Float, y: Float, width: Float, height: Float) {
    drawRect(Color(0xFF58402D), Offset(x, y + height * 0.12f), Size(width, height * 0.76f))
    val plankHeight = height / 4f
    repeat(4) { row ->
        val plankY = y + row * plankHeight
        drawRect(
            if (row % 2 == 0) Color(0xFFAC8050) else Color(0xFF966D45),
            Offset(x + 2f, plankY),
            Size(width - 4f, plankHeight - 1f),
        )
    }
}

private fun buildingRoofColor(name: String): Color = when {
    name.contains("tavern", ignoreCase = true) -> Color(0xFF8D4C39)
    name.contains("smith", ignoreCase = true) -> Color(0xFF625251)
    name.contains("bakery", ignoreCase = true) -> Color(0xFFB56F48)
    name.contains("chapel", ignoreCase = true) -> Color(0xFF95866C)
    name.contains("farm", ignoreCase = true) -> Color(0xFF9B7041)
    name.contains("market", ignoreCase = true) -> Color(0xFF926746)
    name.contains("workshop", ignoreCase = true) -> Color(0xFF81563D)
    else -> Color(0xFF956349)
}

@Composable
private fun VillagerRoster(
    villagers: List<Villager>,
    selectedVillagerId: String,
    onSelect: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        villagers.forEachIndexed { index, villager ->
            val selected = villager.id == selectedVillagerId
            Column(
                modifier = Modifier
                    .width(76.dp)
                    .clip(CutCornerShape(5.dp))
                    .background(if (selected) Color(0xFF554324) else Panel)
                    .border(
                        if (selected) 3.dp else 2.dp,
                        if (selected) Gold else Border,
                        CutCornerShape(5.dp),
                    )
                    .clickable { onSelect(villager.id) }
                    .padding(vertical = 8.dp, horizontal = 5.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                VillagerToken(index = index, selected = selected)
                Spacer(Modifier.height(5.dp))
                Text(
                    villager.name.substringBefore(' '),
                    color = if (selected) Paper else Muted,
                    fontSize = 10.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    if (selected) "SELECTED" else " ",
                    color = Gold,
                    fontSize = 7.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                )
                Text(
                    villager.occupation.uppercase(),
                    color = if (selected) Gold else Color(0xFF818677),
                    fontSize = 7.sp,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 0.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun VillagerToken(index: Int, selected: Boolean) {
    val palette = listOf(
        Color(0xFFBA7254), Color(0xFF78916D), Color(0xFFBE9D5E), Color(0xFF9180A8),
        Color(0xFF9E5E4E), Color(0xFF649A96), Color(0xFFC77C4A), Color(0xFF788BB0),
        Color(0xFF9A875D), Color(0xFFB87886), Color(0xFF6C916D), Color(0xFFB29A73),
    )
    val tunic = palette[index % palette.size]
    val skin = Color(0xFFE0B787)
    val hair = Color(0xFF49382A)
    Box(
        modifier = Modifier
            .size(38.dp)
            .background(if (selected) Color(0xFF66512D) else Color(0xFF33413A))
            .border(2.dp, if (selected) Gold else Border, CutCornerShape(2.dp))
            .padding(3.dp),
    ) {
        androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
            val pixel = size.width / 8f
            fun pixel(color: Color, x: Int, y: Int, width: Int = 1, height: Int = 1) {
                drawRect(color, Offset(x * pixel, y * pixel), Size(width * pixel, height * pixel))
            }
            pixel(Color(0xFF64734D), 0, 0, 8, 8)
            pixel(Color(0xFF7E8A59), 1, 0, 6, 2)
            pixel(Color(0xFF526344), 0, 6, 8, 2)
            pixel(hair, 2, 1, 4, 2)
            pixel(skin, 2, 2, 4, 3)
            pixel(hair, 2, 2, 1, 1)
            pixel(hair, 5, 2, 1, 1)
            pixel(Color(0xFF292A23), 3, 3)
            pixel(Color(0xFF292A23), 5, 3)
            pixel(tunic, 1, 5, 6, 2)
            pixel(tunic.copy(red = (tunic.red * 0.72f).coerceIn(0f, 1f)), 2, 7, 4, 1)
            pixel(Color(0xFFE4C776), 6, 5, 1, 2)
        }
    }
}

@Composable
private fun ProfileCard(
    villager: Villager,
    isPlanning: Boolean,
    latestMessage: String?,
    latestSource: DecisionSource?,
    onRequestPlan: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CutCornerShape(8.dp))
            .background(Panel)
            .border(2.dp, Border, CutCornerShape(8.dp))
            .padding(15.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "${villager.name.uppercase()}  ·  ${villager.age}",
                    color = Paper,
                    fontSize = 16.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    "${villager.occupation}  ·  ${activityLabel(villager.activity)}",
                    color = Leaf,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 0.3.sp,
                )
            }
            VillagerToken(index = villager.name.length % 12, selected = true)
        }
        Spacer(Modifier.height(12.dp))
        Text("AMBITIONS", color = Muted, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.1.sp)
        Spacer(Modifier.height(3.dp))
        Text(villager.ambition, color = Paper, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            villager.personalityTraits.take(3).forEach { trait ->
                Text(
                    trait.uppercase(),
                    modifier = Modifier
                        .clip(CutCornerShape(3.dp))
                        .background(PanelRaised)
                        .padding(horizontal = 7.dp, vertical = 5.dp),
                    color = Muted,
                    fontSize = 8.sp,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 0.45.sp,
                )
            }
        }
        Spacer(Modifier.height(13.dp))
        Button(
            onClick = onRequestPlan,
            enabled = !isPlanning,
            modifier = Modifier.fillMaxWidth(),
            shape = CutCornerShape(5.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF786039),
                contentColor = Color(0xFFFFF2D5),
                disabledContainerColor = Color(0xFF4F4838),
                disabledContentColor = Muted,
            ),
        ) {
            Text(
                if (isPlanning) "CONSULTING THE VILLAGER…" else "✦   ASK FOR A PLAN",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 0.8.sp,
            )
        }
        if (latestMessage != null) {
            Spacer(Modifier.height(9.dp))
            Text(
                latestMessage,
                color = if (latestSource == DecisionSource.FALLBACK) Warning else Leaf,
                fontSize = 11.sp,
                lineHeight = 16.sp,
            )
        }
    }
}

private fun activityLabel(activity: NpcActivity): String = when (activity) {
    NpcActivity.IDLE -> "At home"
    NpcActivity.TRAVELING -> "Planning to travel"
    NpcActivity.WORKING -> "Working"
    NpcActivity.RESTING -> "Resting"
    NpcActivity.OBSERVING -> "Observing"
}

@Composable
private fun AiStatusFooter() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = CutCornerShape(5.dp),
        color = Color(0xFF29251F),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF4A3F2D)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("AI", color = Gold, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace, letterSpacing = 0.6.sp)
            Spacer(Modifier.width(8.dp))
            Text(
                "AI decisions shape this village. Connect a provider to begin.",
                color = Muted,
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                lineHeight = 13.sp,
            )
        }
    }
}
