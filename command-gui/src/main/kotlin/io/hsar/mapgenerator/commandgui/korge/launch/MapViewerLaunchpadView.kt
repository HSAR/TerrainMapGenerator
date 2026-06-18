package io.hsar.mapgenerator.commandgui.korge.launch

import io.hsar.mapgenerator.commandgui.korge.ui.ScreenBounds
import korlibs.image.color.Colors
import korlibs.korge.view.Container
import korlibs.korge.view.container
import korlibs.korge.view.onStageResized
import korlibs.korge.view.solidRect
import korlibs.korge.view.xy

internal class MapViewerLaunchpadView(
    private val root: Container,
    viewportWidth: Int,
    viewportHeight: Int,
    private val onCreateNewOperation: () -> Unit,
    private val onLaunchExistingOperation: () -> Unit,
) {
    private val layer = root.container()
    private var layout = MapViewerLaunchpadLayoutBuilder.build(viewportWidth, viewportHeight)

    init {
        layer.zIndex = Z_INDEX
        layer.onStageResized { width, height ->
            render(width, height)
        }
        render(viewportWidth, viewportHeight)
    }

    fun close() {
        layer.removeFromParent()
    }

    private fun render(viewportWidth: Int, viewportHeight: Int) {
        layout = MapViewerLaunchpadLayoutBuilder.build(viewportWidth, viewportHeight)
        layer.removeChildren()
        layer.solidRect(viewportWidth.toDouble(), viewportHeight.toDouble(), Colors.BLACK)

        val panel = MapViewerLaunchUi.panel(layer, layout.panelBounds)
        val title = MapViewerLaunchUi.label(panel, "Operational Command Tool", strong = true)
        title.xy((layout.panelBounds.width - title.width) * 0.5, 44.0)
        val welcome = MapViewerLaunchUi.label(panel, "Welcome, Commander.")
        welcome.xy((layout.panelBounds.width - welcome.width) * 0.5, 86.0)

        val buttonLeft = (layout.panelBounds.width - BUTTON_WIDTH) * 0.5
        MapViewerLaunchUi.button(panel, BUTTON_WIDTH, BUTTON_HEIGHT, "Create New Operation", onClick = onCreateNewOperation)
            .xy(buttonLeft, 150.0)
        MapViewerLaunchUi.button(
            parent = panel,
            width = BUTTON_WIDTH,
            height = BUTTON_HEIGHT,
            label = "Launch Existing Operation",
            disabled = true,
            onClick = onLaunchExistingOperation,
        ).xy(buttonLeft, 212.0)
    }

    private companion object {
        const val Z_INDEX = 60_000.0
        const val BUTTON_WIDTH = 300.0
        const val BUTTON_HEIGHT = 44.0
    }
}

internal data class MapViewerLaunchpadLayout(
    val panelBounds: ScreenBounds,
)

internal object MapViewerLaunchpadLayoutBuilder {
    fun build(viewportWidth: Int, viewportHeight: Int): MapViewerLaunchpadLayout {
        val panelWidth = 420.0
        val panelHeight = 320.0
        return MapViewerLaunchpadLayout(
            panelBounds = ScreenBounds(
                left = (viewportWidth - panelWidth) * 0.5,
                top = (viewportHeight - panelHeight) * 0.5,
                width = panelWidth,
                height = panelHeight,
            ),
        )
    }
}
