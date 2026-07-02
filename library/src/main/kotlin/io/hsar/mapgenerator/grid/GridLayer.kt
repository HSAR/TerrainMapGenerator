package io.hsar.mapgenerator.grid

import java.awt.Color

data class GridLayer(
    val mapId: String,
    val referenceSystem: ReferenceSystem = ReferenceSystem.MGRS,
    val gridScale: Int,
    val metresPerPixel: Double,
    val detailLevel: Int,
    val pixelsPerLine: Double,
    val verticalLineNumberStart: MgrsCoord,
    val horizontalLineNumberStart: MgrsCoord,
    val verticalLineCount: Int,
    val horizontalLineCount: Int,
    val colour: Color = GridPalettes.DEFAULT.lineColour,
)
