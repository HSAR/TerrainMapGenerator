package io.hsar.mapgenerator.grid

import io.hsar.mapgenerator.graph.Point
import kotlin.math.min

/**
 * Baseline measurements captured from the original 2560x1440 map assets.
 * The grid UI uses these proportions so gutters and labels stay visually
 * consistent as the map size changes.
 */
object GridLayoutScale {
    val BASELINE_MAP_SIZE = Point(BASELINE_MAP_WIDTH, BASELINE_MAP_HEIGHT)

    val MAP_ID_LEFT_GUTTER_RATIO = BASE_GRID_PADDING_X / BASELINE_MAP_WIDTH
    val MAP_ID_TOP_GUTTER_RATIO = BASE_GRID_PADDING_Y / BASELINE_MAP_HEIGHT
    val GRID_LINE_LEFT_GUTTER_RATIO = BASE_GRID_PADDING_X / BASELINE_MAP_WIDTH
    val GRID_LINE_TOP_GUTTER_RATIO = BASE_GRID_PADDING_Y / BASELINE_MAP_HEIGHT

    val MAP_ID_FONT_SCALE = BASE_MAP_ID_FONT_SIZE
    val GRID_LINE_FONT_SCALE = BASE_GRID_LINE_ID_FONT_SIZE

    fun mapIdPadding(mapWidth: Int, mapHeight: Int): Point =
        Point(
            MAP_ID_LEFT_GUTTER_RATIO * mapWidth,
            MAP_ID_TOP_GUTTER_RATIO * mapHeight,
        )

    fun gridLinePadding(mapWidth: Int, mapHeight: Int): Point =
        Point(
            GRID_LINE_LEFT_GUTTER_RATIO * mapWidth,
            GRID_LINE_TOP_GUTTER_RATIO * mapHeight,
        )

    fun labelScale(mapWidth: Int, mapHeight: Int): Double =
        min(mapWidth / BASELINE_MAP_WIDTH, mapHeight / BASELINE_MAP_HEIGHT)

    private const val BASELINE_MAP_WIDTH = 2560.0
    private const val BASELINE_MAP_HEIGHT = 1440.0
    private const val BASE_GRID_PADDING_X = 60.0
    private const val BASE_GRID_PADDING_Y = 42.0
    private const val BASE_MAP_ID_FONT_SIZE = 36.0
    private const val BASE_GRID_LINE_ID_FONT_SIZE = 28.0
}
