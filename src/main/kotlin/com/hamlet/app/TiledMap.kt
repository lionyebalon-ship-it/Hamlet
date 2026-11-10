package com.hamlet.app

import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import org.w3c.dom.Element
import org.xml.sax.InputSource
import java.io.File
import java.io.InputStream
import java.io.StringReader
import javax.xml.parsers.DocumentBuilderFactory

data class TiledMap(
    val width: Int,
    val height: Int,
    val tileWidth: Int,
    val tileHeight: Int,
    val title: String?,
    val entryX: Int?,
    val entryY: Int?,
    val tileset: TiledTileset,
    val layers: List<TiledTileLayer>,
    val objectLayers: List<TiledObjectLayer>,
) {
    val pixelWidth: Int get() = width * tileWidth
    val pixelHeight: Int get() = height * tileHeight

    fun walkableTiles(): BooleanArray {
        val floor = layers.firstOrNull()?.tiles
            ?: error("A walkable map needs a floor layer")
        val walls = layers.getOrNull(1)?.tiles
        val furnishings = layers.getOrNull(2)?.tiles
        return BooleanArray(width * height) { index ->
            floor[index] != 0 &&
                (walls == null || walls[index] == 0 || walls[index] == 8) &&
                (furnishings == null || furnishings[index] == 0)
        }
    }
}

data class TiledTileset(
    val firstGid: Int,
    val tileCount: Int,
    val columns: Int,
    val imageSource: String,
)

data class TiledTileLayer(
    val name: String,
    val tiles: IntArray,
)

data class TiledObjectLayer(
    val name: String,
    val objects: List<TiledMapObject>,
)

data class TiledMapObject(
    val id: Int,
    val name: String,
    val type: String,
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val interiorMap: String?,
)

data class HamletMapArtwork(
    val map: TiledMap,
    val terrainImage: ImageBitmap,
)

object TiledMapParser {
    fun parse(input: InputStream): TiledMap {
        val xml = input.bufferedReader(Charsets.UTF_8).use { it.readText() }
        require(!Regex("<!\\s*(DOCTYPE|ENTITY)\\b", RegexOption.IGNORE_CASE).containsMatchIn(xml)) {
            "Tiled maps must not declare document types or entities"
        }
        val factory = DocumentBuilderFactory.newInstance().apply {
            isExpandEntityReferences = false
        }
        val root = factory.newDocumentBuilder().parse(InputSource(StringReader(xml))).documentElement
        require(root.tagName == "map") { "Expected a Tiled <map> document" }
        require(root.getAttribute("orientation") == "orthogonal") {
            "Only orthogonal Tiled maps are supported"
        }
        require(root.getAttribute("infinite") !in setOf("1", "true")) {
            "Infinite Tiled maps are not supported"
        }

        val width = root.requiredInt("width")
        val height = root.requiredInt("height")
        val tileWidth = root.requiredInt("tilewidth")
        val tileHeight = root.requiredInt("tileheight")
        val mapProperties = root.children("properties").singleOrNull()
            ?.children("property")
            ?.associate { it.requiredAttribute("name") to it.getAttribute("value") }
            .orEmpty()
        require(width > 0 && height > 0 && tileWidth > 0 && tileHeight > 0) {
            "Tiled map dimensions must be positive"
        }
        val entryX = mapProperties["entry-x"]?.toIntOrNull()
        val entryY = mapProperties["entry-y"]?.toIntOrNull()
        require((entryX == null) == (entryY == null)) {
            "Tiled map entry coordinates must define both entry-x and entry-y"
        }
        require(entryX == null || (entryX in 0 until width && entryY in 0 until height)) {
            "Tiled map entry coordinates are outside the map dimensions"
        }

        val tilesetElement = root.children("tileset").singleOrNull()
            ?: error("Expected exactly one embedded tileset")
        require(!tilesetElement.hasAttribute("source")) {
            "External Tiled tilesets are not supported"
        }
        val image = tilesetElement.children("image").singleOrNull()
            ?: error("Tileset must contain exactly one image")
        val tileset = TiledTileset(
            firstGid = tilesetElement.requiredInt("firstgid"),
            tileCount = tilesetElement.requiredInt("tilecount"),
            columns = tilesetElement.requiredInt("columns"),
            imageSource = image.requiredAttribute("source"),
        )
        require(tileset.firstGid > 0 && tileset.tileCount > 0 && tileset.columns > 0) {
            "Tileset dimensions and first GID must be positive"
        }

        val layers = root.children("layer").map { layer ->
            require(layer.requiredInt("width") == width && layer.requiredInt("height") == height) {
                "Tile layer '${layer.getAttribute("name")}' must match the map dimensions"
            }
            val data = layer.children("data").singleOrNull()
                ?: error("Tile layer '${layer.getAttribute("name")}' must contain data")
            require(data.getAttribute("encoding") == "csv") {
                "Only CSV-encoded Tiled layers are supported"
            }
            val tiles = data.textContent.trim().split(Regex("[,\\s]+")).map { token ->
                token.toIntOrNull()
                    ?: error("Invalid tile GID '$token' in layer '${layer.getAttribute("name")}'")
            }.toIntArray()
            require(tiles.size == width * height) {
                "Tile layer '${layer.getAttribute("name")}' has ${tiles.size} entries; expected ${width * height}"
            }
            tiles.forEach { gid ->
                require(gid == 0 || gid in tileset.firstGid until tileset.firstGid + tileset.tileCount) {
                    "Tile GID $gid is outside the configured tileset"
                }
            }
            TiledTileLayer(layer.getAttribute("name"), tiles)
        }

        val objectLayers = root.children("objectgroup").map { group ->
            val objects = group.children("object").map { element ->
                TiledMapObject(
                    id = element.requiredInt("id"),
                    name = element.getAttribute("name"),
                    type = element.getAttribute("class").ifEmpty { element.getAttribute("type") },
                    x = element.requiredFloat("x"),
                    y = element.requiredFloat("y"),
                    width = element.requiredFloat("width"),
                    height = element.requiredFloat("height"),
                    interiorMap = element.children("properties").singleOrNull()
                        ?.children("property")
                        ?.firstOrNull { it.getAttribute("name") == "interior-map" }
                        ?.getAttribute("value")
                        ?.takeIf(String::isNotBlank),
                )
            }
            TiledObjectLayer(group.getAttribute("name"), objects)
        }

        return TiledMap(
            width = width,
            height = height,
            tileWidth = tileWidth,
            tileHeight = tileHeight,
            title = mapProperties["room-title"],
            entryX = entryX,
            entryY = entryY,
            tileset = tileset,
            layers = layers,
            objectLayers = objectLayers,
        )
    }

    private fun Element.children(tagName: String): List<Element> =
        (0 until childNodes.length).mapNotNull { index ->
            (childNodes.item(index) as? Element)?.takeIf { it.tagName == tagName }
        }

    private fun Element.requiredAttribute(name: String): String =
        getAttribute(name).takeIf(String::isNotBlank)
            ?: error("Missing '$name' attribute on <$tagName>")

    private fun Element.requiredInt(name: String): Int =
        requiredAttribute(name).toIntOrNull()
            ?: error("Invalid integer '$name' attribute on <$tagName>")

    private fun Element.requiredFloat(name: String): Float =
        requiredAttribute(name).toFloatOrNull()
            ?: error("Invalid number '$name' attribute on <$tagName>")
}

fun loadHamletMap(
    assets: AssetManager,
    mapAssetPath: String = "maps/hamlet.tmx",
): HamletMapArtwork {
    val map = assets.open(mapAssetPath).use(TiledMapParser::parse)
    val mapDirectory = File(mapAssetPath).parentFile
        ?: error("Invalid map asset path: $mapAssetPath")
    val imagePath = mapDirectory.resolve(map.tileset.imageSource).normalize()
    require(!imagePath.isAbsolute && !imagePath.path.startsWith("..")) {
        "Tileset image path escapes the Android assets directory"
    }
    val bitmap = assets.open(imagePath.path).use { stream ->
        BitmapFactory.decodeStream(stream)
            ?: error("Could not decode tileset image '${imagePath.path}'")
    }
    val requiredColumns = map.tileset.columns * map.tileWidth
    val requiredRows = (map.tileset.tileCount + map.tileset.columns - 1) / map.tileset.columns
    require(bitmap.width >= requiredColumns && bitmap.height >= requiredRows * map.tileHeight) {
        "Tileset image '${imagePath.path}' is smaller than its declared tile dimensions"
    }
    val terrain = Bitmap.createBitmap(map.pixelWidth, map.pixelHeight, Bitmap.Config.ARGB_8888)
    val paint = Paint().apply {
        isAntiAlias = false
        isFilterBitmap = false
    }
    val source = Rect()
    val destination = RectF()
    val canvas = AndroidCanvas(terrain)
    map.layers.forEach { layer ->
        layer.tiles.forEachIndexed { index, gid ->
            if (gid == 0) return@forEachIndexed
            val localId = gid - map.tileset.firstGid
            val sourceX = localId % map.tileset.columns * map.tileWidth
            val sourceY = localId / map.tileset.columns * map.tileHeight
            source.set(sourceX, sourceY, sourceX + map.tileWidth, sourceY + map.tileHeight)
            val destinationX = index % map.width * map.tileWidth
            val destinationY = index / map.width * map.tileHeight
            destination.set(
                destinationX.toFloat(),
                destinationY.toFloat(),
                (destinationX + map.tileWidth).toFloat(),
                (destinationY + map.tileHeight).toFloat(),
            )
            canvas.drawBitmap(bitmap, source, destination, paint)
        }
    }
    return HamletMapArtwork(map, terrain.asImageBitmap())
}
