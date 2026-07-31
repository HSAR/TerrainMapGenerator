package io.hsar.mapgenerator.commandgui

/** A position expressed as a fraction of the map image dimensions. */
data class MapCoordinate(
    val x: Double,
    val y: Double,
)

data class MapCoordinateDelta(
    val x: Double,
    val y: Double,
)

data class MapCoordinateBounds(
    val left: Double,
    val top: Double,
    val right: Double,
    val bottom: Double,
) {
    fun contains(point: MapCoordinate) =
        point.x in left..right && point.y in top..bottom

    fun clamp(point: MapCoordinate) = MapCoordinate(
        x = point.x.coerceIn(left, right),
        y = point.y.coerceIn(top, bottom),
    )
}

fun MapCoordinate.toMapPixels(mapWidth: Int, mapHeight: Int) = ViewerPoint(
    x = x * mapWidth,
    y = y * mapHeight,
)

fun MapCoordinate.toWorldPoint(
    mapOffsetX: Double,
    mapOffsetY: Double,
    mapWidth: Int,
    mapHeight: Int,
) = toMapPixels(mapWidth, mapHeight).let { mapPixels ->
    ViewerPoint(mapOffsetX + mapPixels.x, mapOffsetY + mapPixels.y)
}
