package io.hsar.mapgenerator.grid

import io.hsar.mapgenerator.graph.Point
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Baseline measurements captured from the original 2560x1440 map assets.
 * The grid UI uses these proportions so gutters and labels stay visually
 * consistent as the map size changes.
 */
object GridLayoutScale {
    val BASELINE_MAP_SIZE = Point(BASELINE_MAP_WIDTH, BASELINE_MAP_HEIGHT)

    val MAP_ID_LEFT_GUTTER_PERCENT_OF_WIDTH = BASE_MAP_ID_GUTTER_X / BASELINE_MAP_WIDTH
    val MAP_ID_TOP_GUTTER_PERCENT_OF_HEIGHT = BASE_MAP_ID_GUTTER_Y / BASELINE_MAP_HEIGHT
    val GRID_LINE_LEFT_GUTTER_PERCENT_OF_WIDTH = BASE_GRID_GUTTER_X / BASELINE_MAP_WIDTH
    val GRID_LINE_TOP_GUTTER_PERCENT_OF_HEIGHT = BASE_GRID_GUTTER_Y / BASELINE_MAP_HEIGHT

    val MAP_ID_FONT_SIZE = BASE_MAP_ID_FONT_SIZE
    val GRID_LINE_FONT_SIZE = BASE_GRID_LINE_ID_FONT_SIZE

    fun mapIdPadding(mapWidth: Int, mapHeight: Int): Point =
        Point(
            MAP_ID_LEFT_GUTTER_PERCENT_OF_WIDTH * mapWidth,
            MAP_ID_TOP_GUTTER_PERCENT_OF_HEIGHT * mapHeight,
        )

    fun gridLinePadding(mapWidth: Int, mapHeight: Int): Point =
        Point(
            GRID_LINE_LEFT_GUTTER_PERCENT_OF_WIDTH * mapWidth,
            GRID_LINE_TOP_GUTTER_PERCENT_OF_HEIGHT * mapHeight,
        )

    fun labelScale(mapWidth: Int, mapHeight: Int): Double =
        sqrt(
            min(mapWidth / BASELINE_MAP_WIDTH, mapHeight / BASELINE_MAP_HEIGHT)
                .coerceAtLeast(0.0),
        )

    private const val BASELINE_MAP_WIDTH = 2560.0
    private const val BASELINE_MAP_HEIGHT = 1440.0
    private const val BASE_MAP_ID_GUTTER_X = 60.0
    private const val BASE_MAP_ID_GUTTER_Y = 40.0
    private const val BASE_GRID_GUTTER_X = BASE_MAP_ID_GUTTER_X
    private const val BASE_GRID_GUTTER_Y = BASE_MAP_ID_GUTTER_Y
    private const val BASE_MAP_ID_FONT_SIZE = 30.0
    private const val BASE_GRID_LINE_ID_FONT_SIZE = 24.0
}
