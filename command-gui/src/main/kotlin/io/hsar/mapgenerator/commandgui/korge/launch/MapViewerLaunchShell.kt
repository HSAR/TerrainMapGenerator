package io.hsar.mapgenerator.commandgui.korge.launch

import io.hsar.mapgenerator.commandgui.korge.ui.ScreenBounds
import korlibs.image.color.Colors
import korlibs.image.color.RGBA
import korlibs.korge.view.Container
import korlibs.korge.view.addUpdater
import korlibs.korge.view.container
import korlibs.korge.view.onStageResized
import korlibs.korge.view.solidRect

internal class MapViewerLaunchShell(
    root: Container,
    viewportWidth: Int,
    viewportHeight: Int,
    background: LaunchTopographyBackground,
) {
    private val layer = root.container()
    private val backgroundBaseLayer = layer.container()
    private val backgroundRasterLayer = layer.container()
    private val backgroundWashLayer = layer.container()
    private val headerLayer = layer.container()
    private var latestBackground = background
    private var displayedBackground = background
    private var displayedRaster: Container? = null
    private var transition: BackgroundTransition? = null
    private val pendingBackgrounds = mutableListOf<LaunchTopographyBackground>()
    private var viewportWidth = viewportWidth
    private var viewportHeight = viewportHeight

    init {
        layer.zIndex = Z_INDEX
        layer.onStageResized { width, height ->
            render(width, height)
        }
        layer.addUpdater {
            updateTransition()
        }
        render(viewportWidth, viewportHeight)
    }

    fun close() {
        layer.removeFromParent()
    }

    fun updateBackground(background: LaunchTopographyBackground) {
        if (background.quality <= latestBackground.quality) return
        latestBackground = background
        pendingBackgrounds += background
        startNextTransition()
    }

    private fun render(viewportWidth: Int, viewportHeight: Int) {
        this.viewportWidth = viewportWidth
        this.viewportHeight = viewportHeight
        snapToLatestBackground()
        renderBackdrop()
        headerLayer.removeChildren()
        MapViewerLaunchpadTemplate.renderHeader(headerLayer, MapViewerLaunchShellLayoutBuilder.build(viewportWidth).headerBounds)
    }

    private fun renderBackdrop() {
        backgroundBaseLayer.removeChildren()
        backgroundBaseLayer.solidRect(viewportWidth.toDouble(), viewportHeight.toDouble(), Colors.BLACK)
        backgroundRasterLayer.removeChildren()
        displayedRaster = backgroundRasterLayer.container().also { raster ->
            displayedBackground.renderRaster(raster, viewportWidth, viewportHeight)
        }
        backgroundWashLayer.removeChildren()
        backgroundWashLayer.solidRect(viewportWidth.toDouble(), viewportHeight.toDouble(), RGBA(0, 0, 0, 72))
    }

    private fun snapToLatestBackground() {
        pendingBackgrounds.clear()
        transition = null
        displayedBackground = latestBackground
    }

    private fun startNextTransition() {
        if (transition != null) return
        val next = pendingBackgrounds.removeFirstOrNull() ?: return
        val incoming = backgroundRasterLayer.container().also { raster ->
            raster.alpha = 0.0
            next.renderRaster(raster, viewportWidth, viewportHeight)
        }
        transition = BackgroundTransition(
            background = next,
            outgoing = displayedRaster,
            incoming = incoming,
            startedAtNanos = System.nanoTime(),
        )
    }

    private fun updateTransition(nowNanos: Long = System.nanoTime()) {
        transition?.let { active ->
            val elapsedMilliseconds = (nowNanos - active.startedAtNanos) / 1_000_000.0
            active.incoming.alpha = LaunchBackgroundCrossfade.alpha(elapsedMilliseconds)
            if (elapsedMilliseconds >= LaunchBackgroundCrossfade.DURATION_MILLISECONDS) {
                active.incoming.alpha = 1.0
                active.outgoing?.removeFromParent()
                displayedBackground = active.background
                displayedRaster = active.incoming
                transition = null
                startNextTransition()
            }
        }
    }

    private data class BackgroundTransition(
        val background: LaunchTopographyBackground,
        val outgoing: Container?,
        val incoming: Container,
        val startedAtNanos: Long,
    )

    private companion object {
        const val Z_INDEX = 59_000.0
    }
}

internal data class MapViewerLaunchShellLayout(
    val headerBounds: ScreenBounds,
)

internal object MapViewerLaunchShellLayoutBuilder {
    fun build(viewportWidth: Int): MapViewerLaunchShellLayout {
        val headerWidth = 540.0
        return MapViewerLaunchShellLayout(
            headerBounds = ScreenBounds(
                left = (viewportWidth - headerWidth) * 0.5,
                top = 20.0,
                width = headerWidth,
                height = 78.0,
            ),
        )
    }
}
