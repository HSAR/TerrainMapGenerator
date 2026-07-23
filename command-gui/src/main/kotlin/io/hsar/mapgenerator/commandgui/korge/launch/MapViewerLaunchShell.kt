package io.hsar.mapgenerator.commandgui.korge.launch

import io.hsar.mapgenerator.commandgui.korge.ui.ScreenBounds
import korlibs.image.bitmap.Bitmap32
import korlibs.image.color.Colors
import korlibs.image.color.RGBA
import korlibs.korge.view.Container
import korlibs.korge.view.Image
import korlibs.korge.view.addUpdater
import korlibs.korge.view.clipContainer
import korlibs.korge.view.container
import korlibs.korge.view.image
import korlibs.korge.view.onStageResized
import korlibs.korge.view.solidRect
import korlibs.korge.view.xy
import korlibs.math.geom.Size2D

internal class MapViewerLaunchShell(
    root: Container,
    viewportWidth: Int,
    viewportHeight: Int,
    private val background: LaunchTopographyBackground,
) {
    private val layer = root.container()
    private val backgroundBaseLayer = layer.container()
    private val backgroundRasterLayer = layer.container()
    private val backgroundWashLayer = layer.container()
    private val headerLayer = layer.container()
    private val textureCanvas = backgroundRasterLayer.clipContainer(
        Size2D(background.textureWidth.toDouble(), background.textureHeight.toDouble()),
    )
    private val renderedTiles = mutableMapOf<LaunchBackgroundTileCoordinate, RenderedTile>()
    private val tileReveals = mutableListOf<TileReveal>()
    private var viewportWidth = viewportWidth
    private var viewportHeight = viewportHeight

    init {
        layer.zIndex = Z_INDEX
        layer.onStageResized { width, height ->
            render(width, height)
        }
        layer.addUpdater {
            updateTileReveals()
        }
        background.tiles().forEach(::renderSnapshot)
        render(viewportWidth, viewportHeight)
    }

    fun close() {
        layer.removeFromParent()
    }

    fun updateTile(update: LaunchBackgroundTileUpdate) = when (update.quality) {
        LaunchBackgroundTileQuality.PREVIEW -> renderPreview(update.coordinate, update.bitmap, animate = true)
        LaunchBackgroundTileQuality.REFINEMENT -> renderRefinement(update.coordinate, update.bitmap, animate = true)
    }

    private fun render(viewportWidth: Int, viewportHeight: Int) {
        this.viewportWidth = viewportWidth
        this.viewportHeight = viewportHeight
        backgroundBaseLayer.removeChildren()
        backgroundBaseLayer.solidRect(viewportWidth.toDouble(), viewportHeight.toDouble(), Colors.BLACK)
        LaunchBackgroundLayout.cover(
            background.textureWidth,
            background.textureHeight,
            viewportWidth,
            viewportHeight,
        ).also { placement ->
            textureCanvas.scaleX = placement.scale
            textureCanvas.scaleY = placement.scale
            textureCanvas.xy(placement.left, placement.top)
        }
        backgroundWashLayer.removeChildren()
        backgroundWashLayer.solidRect(viewportWidth.toDouble(), viewportHeight.toDouble(), RGBA(0, 0, 0, 72))
        headerLayer.removeChildren()
        MapViewerLaunchpadTemplate.renderHeader(headerLayer, MapViewerLaunchShellLayoutBuilder.build(viewportWidth).headerBounds)
    }

    private fun renderSnapshot(snapshot: LaunchBackgroundTileSnapshot) {
        snapshot.refinement?.let { bitmap -> renderRefinement(snapshot.coordinate, bitmap, animate = false) }
            ?: snapshot.preview?.let { bitmap -> renderPreview(snapshot.coordinate, bitmap, animate = false) }
    }

    private fun renderPreview(coordinate: LaunchBackgroundTileCoordinate, bitmap: Bitmap32, animate: Boolean) {
        val rendered = renderedTile(coordinate)
        if (rendered.preview != null || rendered.refinement != null) return
        rendered.preview = rendered.clip.image(bitmap) {
            smoothing = false
            scaleX = LaunchBackgroundTileCoordinate.PREVIEW_DIVISOR.toDouble()
            scaleY = LaunchBackgroundTileCoordinate.PREVIEW_DIVISOR.toDouble()
            xy(
                -LaunchBackgroundTileCoordinate.TILE_BLEED.toDouble(),
                -LaunchBackgroundTileCoordinate.TILE_BLEED.toDouble(),
            )
            alpha = if (animate) 0.0 else 1.0
        }
        if (animate) {
            tileReveals += TileReveal.Preview(rendered.preview!!, System.nanoTime())
        }
    }

    private fun renderRefinement(coordinate: LaunchBackgroundTileCoordinate, bitmap: Bitmap32, animate: Boolean) {
        val rendered = renderedTile(coordinate)
        if (rendered.refinement != null) return
        rendered.refinement = rendered.clip.image(bitmap) {
            smoothing = true
            xy(
                -LaunchBackgroundTileCoordinate.TILE_BLEED.toDouble(),
                -LaunchBackgroundTileCoordinate.TILE_BLEED.toDouble(),
            )
            alpha = if (animate) 0.0 else 1.0
        }
        if (animate) {
            tileReveals.removeAll { reveal ->
                reveal is TileReveal.Preview && reveal.preview === rendered.preview
            }
            tileReveals += TileReveal.Refinement(
                preview = rendered.preview,
                refinement = rendered.refinement!!,
                startingPreviewAlpha = rendered.preview?.alpha ?: 0.0,
                startedAtNanos = System.nanoTime(),
                rendered = rendered,
            )
        } else {
            rendered.preview?.removeFromParent()
            rendered.preview = null
        }
    }

    private fun renderedTile(coordinate: LaunchBackgroundTileCoordinate): RenderedTile =
        renderedTiles.getOrPut(coordinate) {
            RenderedTile(
                clip = textureCanvas.clipContainer(
                    Size2D(
                        LaunchBackgroundTileCoordinate.TILE_SIZE.toDouble(),
                        LaunchBackgroundTileCoordinate.TILE_SIZE.toDouble(),
                    ),
                ) {
                    xy(coordinate.left.toDouble(), coordinate.top.toDouble())
                },
            )
        }

    private fun updateTileReveals(nowNanos: Long = System.nanoTime()) {
        tileReveals.forEach { reveal ->
            val elapsedMilliseconds = (nowNanos - reveal.startedAtNanos) / 1_000_000.0
            when (reveal) {
                is TileReveal.Preview ->
                    reveal.preview.alpha = LaunchBackgroundTileReveal.previewAlpha(elapsedMilliseconds)

                is TileReveal.Refinement -> {
                    val refinementAlpha = LaunchBackgroundTileReveal.refinementAlpha(elapsedMilliseconds)
                    reveal.refinement.alpha = refinementAlpha
                    reveal.preview?.alpha = reveal.startingPreviewAlpha * (1.0 - refinementAlpha)
                }
            }
        }
        tileReveals.filter { reveal ->
            val elapsedMilliseconds = (nowNanos - reveal.startedAtNanos) / 1_000_000.0
            when (reveal) {
                is TileReveal.Preview -> elapsedMilliseconds >= LaunchBackgroundTileReveal.PREVIEW_FADE_MILLISECONDS
                is TileReveal.Refinement -> elapsedMilliseconds >= LaunchBackgroundTileReveal.REFINEMENT_FADE_MILLISECONDS
            }
        }.also { completed ->
            completed.forEach { reveal ->
                when (reveal) {
                    is TileReveal.Preview -> reveal.preview.alpha = 1.0
                    is TileReveal.Refinement -> {
                        reveal.preview?.removeFromParent()
                        reveal.refinement.alpha = 1.0
                        reveal.rendered.preview = null
                    }
                }
            }
            tileReveals.removeAll(completed)
        }
    }

    private data class RenderedTile(
        val clip: Container,
        var preview: Image? = null,
        var refinement: Image? = null,
    )

    private sealed interface TileReveal {
        val startedAtNanos: Long

        data class Preview(
            val preview: Image,
            override val startedAtNanos: Long,
        ) : TileReveal

        data class Refinement(
            val preview: Image?,
            val refinement: Image,
            val startingPreviewAlpha: Double,
            override val startedAtNanos: Long,
            val rendered: RenderedTile,
        ) : TileReveal
    }

    private companion object {
        const val Z_INDEX = 59_000.0
    }
}

internal data class MapViewerLaunchShellLayout(
    val headerBounds: ScreenBounds,
)

internal data class MapViewerViewport(
    val width: Int,
    val height: Int,
)

internal object MapViewerViewportResolver {
    fun resolve(actualWidth: Int, actualHeight: Int, fallbackWidth: Int, fallbackHeight: Int): MapViewerViewport =
        if (actualWidth > 0 && actualHeight > 0) {
            MapViewerViewport(actualWidth, actualHeight)
        } else {
            MapViewerViewport(fallbackWidth, fallbackHeight)
        }
}

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
