package io.hsar.mapgenerator.image

import io.hsar.mapgenerator.graph.Line

class PathRenderer(
    private val metresPerPixel: Double,
    private val imageBuilder: io.hsar.mapgenerator.image.ImageBuilder
) {
    fun drawPaths(siteJoins: List<Line>) {
        val scaledPathWidth = STANDARD_ROAD_WIDTH / metresPerPixel
        imageBuilder.drawDiagonalPaths(siteJoins, scaledPathWidth.toFloat(), Palette.Colours.DARK)
    }

    companion object {
        const val STANDARD_ROAD_WIDTH = 10
    }

}