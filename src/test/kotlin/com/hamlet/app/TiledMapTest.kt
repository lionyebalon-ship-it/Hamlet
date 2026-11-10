package com.hamlet.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class TiledMapTest {
    @Test
    fun `fixed hamlet map has complete layers and modest village buildings`() {
        val map = File("src/main/assets/maps/hamlet.tmx").inputStream().use(TiledMapParser::parse)

        assertEquals(64, map.width)
        assertEquals(40, map.height)
        assertEquals(16, map.tileWidth)
        assertEquals(16, map.tileHeight)
        assertEquals(3, map.layers.size)
        assertTrue(map.layers.all { it.tiles.size == map.width * map.height })
        val objects = map.objectLayers.single().objects
        val buildings = objects.filter { it.type == "building" }
        assertEquals(12, buildings.size)
        assertEquals(20, objects.count { it.type == "tree" })
        assertEquals(12, objects.count { it.type == "villager" })
        assertTrue(buildings.all { it.width <= 56f && it.height <= 48f })
        assertTrue(objects.all {
            it.x >= 0f && it.y >= 0f &&
                it.x + it.width <= map.pixelWidth &&
                it.y + it.height <= map.pixelHeight
        })
        assertEquals(
            setOf("The Golden Stag Tavern", "Village Blacksmith", "Village Bakery"),
            buildings.take(3).map { it.name }.toSet(),
        )
        assertTrue(buildings.all { building ->
            building.interiorMap?.startsWith("maps/interiors/") == true &&
                File("src/main/assets/${building.interiorMap}").isFile
        })
    }

    @Test
    fun `interiors have distinct sizes materials furnishings and connected walkable floor`() {
        val roomDimensions = mapOf(
            "tavern" to (12 to 9),
            "cottage" to (9 to 8),
            "smithy" to (10 to 8),
            "bakery" to (9 to 8),
            "chapel" to (10 to 9),
            "farmstead" to (10 to 8),
            "workshop" to (11 to 8),
            "weaver" to (9 to 8),
            "market" to (9 to 7),
        )
        val floorMaterials = mapOf(
            "tavern" to 1..4,
            "cottage" to 49..52,
            "smithy" to 53..56,
            "bakery" to 57..60,
            "chapel" to 53..56,
            "farmstead" to 61..64,
            "workshop" to 1..4,
            "weaver" to 1..4,
            "market" to 53..56,
        )

        roomDimensions.forEach { (roomName, dimensions) ->
            val roomMap = File("src/main/assets/maps/interiors/$roomName.tmx")
                .inputStream()
                .use(TiledMapParser::parse)

            assertEquals(dimensions.first, roomMap.width)
            assertEquals(dimensions.second, roomMap.height)
            assertEquals(96, roomMap.tileset.tileCount)
            assertEquals("../../tilesets/hamlet-interiors.png", roomMap.tileset.imageSource)
            assertTrue(roomMap.title.orEmpty().isNotBlank())
            assertTrue(roomMap.entryX != null && roomMap.entryY != null)
            assertEquals(3, roomMap.layers.size)
            assertTrue(roomMap.layers.all { it.tiles.size == roomMap.width * roomMap.height })
            assertTrue(roomMap.layers.first().tiles.all { it in floorMaterials.getValue(roomName) })

            val walkable = roomMap.walkableTiles()
            val entry = TilePosition(roomMap.entryX!!, roomMap.entryY!!)
            assertTrue("The doorway entry must be walkable in $roomName", walkable[entry.y * roomMap.width + entry.x])
            roomMap.layers.last().tiles.forEachIndexed { index, gid ->
                if (gid != 0) assertTrue("Furniture must block walking in $roomName", !walkable[index])
            }
            walkable.indices.filter { walkable[it] }.forEach { index ->
                val destination = TilePosition(index % roomMap.width, index / roomMap.width)
                assertTrue(
                    "No walkable route from the door to $destination in $roomName",
                    TilePathfinder.findPath(roomMap, entry, destination) != null,
                )
            }
        }
    }
}
