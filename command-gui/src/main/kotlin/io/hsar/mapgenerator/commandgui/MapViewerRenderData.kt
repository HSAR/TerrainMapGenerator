package io.hsar.mapgenerator.commandgui

import io.hsar.mapgenerator.grid.GridLayer
import java.awt.image.BufferedImage

data class MapViewerRenderData(
    val baseImage: BufferedImage,
    val gridLayer: GridLayer? = null,
)
