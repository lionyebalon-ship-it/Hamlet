package com.hamlet.app

data class TilePosition(val x: Int, val y: Int)

object TilePathfinder {
    fun findPath(
        map: TiledMap,
        start: TilePosition,
        destination: TilePosition,
    ): List<TilePosition>? {
        val walkable = map.walkableTiles()
        val startIndex = map.indexOf(start) ?: return null
        val destinationIndex = map.indexOf(destination) ?: return null
        if (!walkable[startIndex] || !walkable[destinationIndex]) return null

        val previous = IntArray(walkable.size) { -1 }
        val visited = BooleanArray(walkable.size)
        val pending = ArrayDeque<Int>()
        pending.addLast(startIndex)
        visited[startIndex] = true
        val offsets = intArrayOf(-map.width, -1, 1, map.width)

        while (pending.isNotEmpty()) {
            val current = pending.removeFirst()
            if (current == destinationIndex) {
                return buildPath(previous, startIndex, destinationIndex, map.width)
            }
            val x = current % map.width
            for (offset in offsets) {
                val next = current + offset
                if (next !in walkable.indices || visited[next]) continue
                if ((offset == -1 && x == 0) || (offset == 1 && x == map.width - 1)) continue
                if (!walkable[next]) continue
                visited[next] = true
                previous[next] = current
                pending.addLast(next)
            }
        }
        return null
    }

    private fun buildPath(
        previous: IntArray,
        start: Int,
        destination: Int,
        mapWidth: Int,
    ): List<TilePosition> {
        val path = mutableListOf<TilePosition>()
        var current = destination
        while (current != start) {
            path += TilePosition(current % mapWidth, current / mapWidth)
            current = previous[current]
            check(current >= 0) { "Path predecessor chain ended before its start" }
        }
        path += TilePosition(start % mapWidth, start / mapWidth)
        return path.asReversed()
    }

    private fun TiledMap.indexOf(position: TilePosition): Int? =
        if (position.x in 0 until width && position.y in 0 until height) {
            position.y * width + position.x
        } else {
            null
        }
}
