package io.hsar.mapgenerator.commandgui.korge.render

import io.hsar.mapgenerator.commandgui.MapViewerRenderData
import io.hsar.mapgenerator.commandgui.ViewerGeometry
import io.hsar.mapgenerator.commandgui.geometryFor
import java.awt.image.BufferedImage
import korlibs.image.awt.toBMP32
import korlibs.image.bitmap.Bitmap32

internal data class RenderedMap(
    val bitmap: Bitmap32,
    val geometry: ViewerGeometry,
)

internal class MapViewerRenderer {

    fun render(renderData: MapViewerRenderData): RenderedMap {
        val image = BufferedImage(
            renderData.baseImage.width,
            renderData.baseImage.height,
            BufferedImage.TYPE_INT_ARGB,
        ).also { baseImage ->
            val graphics = baseImage.createGraphics()
            try {
                graphics.drawImage(renderData.baseImage, 0, 0, null)
            } finally {
                graphics.dispose()
            }
        }

        return RenderedMap(
            bitmap = image.toBMP32().premultipliedIfRequired(),
            geometry = geometryFor(image),
        )
    }
}
