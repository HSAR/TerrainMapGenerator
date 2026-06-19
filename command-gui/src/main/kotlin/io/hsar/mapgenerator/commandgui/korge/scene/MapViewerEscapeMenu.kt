package io.hsar.mapgenerator.commandgui.korge.scene

import io.hsar.mapgenerator.commandgui.korge.launch.MapViewerLaunchUi
import korlibs.korge.view.Container
import korlibs.korge.view.container
import korlibs.korge.view.onStageResized
import korlibs.korge.view.solidRect
import korlibs.korge.view.xy
import korlibs.math.geom.Vector2D

internal class MapViewerEscapeMenu(
    private val root: Container,
    private val viewportWidth: () -> Int,
    private val viewportHeight: () -> Int,
    private val onReturnToLaunchpad: () -> Unit,
) {
    private var layer: Container? = null
    private var layout = MapViewerEscapeMenuLayoutBuilder.build(viewportWidth(), viewportHeight())

    val isOpen: Boolean
        get() = layer != null

    fun toggle() {
        if (isOpen) close() else open()
    }

    fun close() {
        layer?.removeFromParent()
        layer = null
    }

    fun blocks(point: Vector2D): Boolean =
        isOpen && layout.panelBounds.contains(point)

    private fun open() {
        layer = root.container().also { overlay ->
            overlay.onStageResized { width, height ->
                render(overlay, width, height)
            }
            render(overlay, viewportWidth(), viewportHeight())
        }
    }

    private fun render(overlay: Container, width: Int, height: Int) {
        layout = MapViewerEscapeMenuLayoutBuilder.build(width, height)
        overlay.removeChildren()
        overlay.solidRect(width.toDouble(), height.toDouble(), korlibs.image.color.RGBA(0, 0, 0, 140))
        val panel = MapViewerLaunchUi.panel(overlay, layout.panelBounds)
        val title = MapViewerLaunchUi.label(panel, "Menu", strong = true)
        title.xy(layout.titleX - (title.width * 0.5), layout.titleY)
        val subtitle = MapViewerLaunchUi.label(panel, "Return to the launchpad after saving.")
        subtitle.xy(layout.subtitleX - (subtitle.width * 0.5), layout.subtitleY)
        MapViewerLaunchUi.button(panel, layout.returnButtonBounds.width, layout.returnButtonBounds.height, "Return To Launchpad") {
            onReturnToLaunchpad()
        }.xy(layout.returnButtonBounds.left, layout.returnButtonBounds.top)
        MapViewerLaunchUi.button(panel, layout.closeButtonBounds.width, layout.closeButtonBounds.height, "Close") {
            close()
        }.xy(layout.closeButtonBounds.left, layout.closeButtonBounds.top)
    }
}
