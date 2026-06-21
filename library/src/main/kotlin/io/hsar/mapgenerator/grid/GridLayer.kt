package io.hsar.mapgenerator.grid

data class GridLayer(
    val mapId: String,
    val gridScale: Int,
    val metresPerPixel: Double,
    val detailLevel: Int,
    val pixelsPerLine: Double,
    val verticalLineNumberStart: Double,
    val horizontalLineNumberStart: Double,
    val verticalLineCount: Int,
    val horizontalLineCount: Int,
)
