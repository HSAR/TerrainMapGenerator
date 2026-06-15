package io.hsar.mapgenerator.grid

data class GridLayer(
    val mapId: String,
    val gridScale: Int,
    val metresPerPixel: Double,
    val detailLevel: Int,
    val pixelsPerLine: Double,
    val verticalLineNumberStart: Int,
    val horizontalLineNumberStart: Int,
    val verticalLineCount: Int,
    val horizontalLineCount: Int,
)
