package io.hsar.mapgenerator.image

import io.hsar.mapgenerator.grid.GridLayerBuilder
import io.hsar.mapgenerator.grid.GridLayerPainter
import java.util.*

class GridRenderer(
    private val imageBuilder: io.hsar.mapgenerator.image.ImageBuilder,
    private val random: Random = Random(),
) {

    fun draw(
        GZDand100kmGSID: String = "",
        gridScale: Int,
        metresPerPixel: Double
    ) {
        val gridLayer = GridLayerBuilder.build(
            width = imageBuilder.width,
            height = imageBuilder.height,
            gridScale = gridScale,
            unitsPerPixel = metresPerPixel,
            random = random,
            mapId = GZDand100kmGSID.ifBlank { null }
        )
        GridLayerPainter.draw(imageBuilder, gridLayer)
    }

    companion object {
        const val ONE_KM_GRID_SQUARES = 1_000
        const val HUNDRED_M_GRID_SQUARES = 100
    }
}
