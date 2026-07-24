package io.hsar.mapgenerator.commandgui.korge.launch

import io.hsar.mapgenerator.commandgui.korge.ui.MapViewerDialogAction
import io.hsar.mapgenerator.commandgui.korge.ui.MapViewerDialogLayoutBuilder
import io.hsar.mapgenerator.commandgui.korge.ui.MapViewerDialogMode
import io.hsar.mapgenerator.commandgui.korge.ui.MapViewerDialogPersistence
import io.hsar.mapgenerator.commandgui.korge.ui.MapViewerDialogWindow
import io.hsar.mapgenerator.commandgui.korge.ui.MapViewerTextViews
import io.hsar.mapgenerator.commandgui.korge.ui.MapViewerUiBoxes
import io.hsar.mapgenerator.commandgui.korge.ui.ScreenBounds
import korlibs.image.bitmap.Bitmap32
import korlibs.image.color.Colors
import korlibs.image.color.RGBA
import korlibs.korge.annotations.KorgeExperimental
import korlibs.korge.input.mouse
import korlibs.korge.view.Container
import korlibs.korge.view.Image
import korlibs.korge.view.SolidRect
import korlibs.korge.view.addUpdater
import korlibs.korge.view.clipContainer
import korlibs.korge.view.container
import korlibs.korge.view.image
import korlibs.korge.view.onStageResized
import korlibs.korge.view.solidRect
import korlibs.korge.view.xy
import korlibs.math.geom.Size2D

@OptIn(KorgeExperimental::class)
internal class MapViewerLaunchShell(
    root: Container,
    viewportWidth: Int,
    viewportHeight: Int,
    private var background: LaunchTopographyBackground,
    private val onRegenerate: () -> Unit = {},
) {
    private val layer = root.container()
    private val backgroundBaseLayer = layer.container()
    private val backgroundRasterLayer = layer.container()
    private val backgroundWashLayer = layer.container()
    private val headerLayer = layer.container()
    private val backgroundInfoToggle = layer.container()
    private var backgroundInfoDialog: MapViewerDialogWindow? = null
    private var activeRaster = RasterCanvas(background, createTextureCanvas(background))
    private var outgoingRaster: RasterCanvas? = null
    private var renderedTiles = mutableMapOf<LaunchBackgroundTileCoordinate, RenderedTile>()
    private val tileReveals = mutableListOf<TileReveal>()
    private val tileBlackouts = mutableListOf<TileBlackout>()
    private var viewportWidth = viewportWidth
    private var viewportHeight = viewportHeight

    init {
        layer.zIndex = Z_INDEX
        layer.onStageResized { width, height ->
            currentViewport(width, height).let { viewport ->
                render(viewport.width, viewport.height)
            }
        }
        layer.addUpdater {
            updateTileReveals()
            updateTileBlackouts()
        }
        backgroundInfoToggle.mouse {
            onClick {
                if (backgroundInfoDialog == null) openBackgroundInfo() else closeBackgroundInfo()
            }
        }
        background.tiles().forEach(::renderSnapshot)
        render(viewportWidth, viewportHeight)
    }

    fun close() {
        closeBackgroundInfo()
        layer.removeFromParent()
    }

    fun regenerationViewport(): MapViewerViewport = currentViewport(viewportWidth, viewportHeight)

    fun replaceBackground(newBackground: LaunchTopographyBackground, onFadedToBlack: () -> Unit = {}) {
        val startedAtNanos = System.nanoTime()
        outgoingRaster?.canvas?.removeFromParent()
        tileBlackouts.clear()
        tileReveals.clear()
        val outgoingTiles = renderedTiles
        val outgoingBackground = background
        outgoingRaster = activeRaster
        background = newBackground
        activeRaster = RasterCanvas(newBackground, createTextureCanvas(newBackground))
        renderedTiles = mutableMapOf()
        LaunchBackgroundTilePlan.priorityOrder(outgoingBackground).forEachIndexed { index, coordinate ->
            outgoingTiles[coordinate]?.let { rendered ->
                rendered.blackout = rendered.clip.solidRect(
                    LaunchBackgroundTileCoordinate.TILE_CLIP_SIZE.toDouble(),
                    LaunchBackgroundTileCoordinate.TILE_CLIP_SIZE.toDouble(),
                    Colors.BLACK,
                ) { alpha = 0.0 }
                tileBlackouts += TileBlackout(
                    rendered,
                    startedAtNanos + index * BLACKOUT_STAGGER_MILLISECONDS * 1_000_000L,
                )
            }
        }
        if (tileBlackouts.isEmpty()) {
            outgoingRaster?.canvas?.removeFromParent()
            outgoingRaster = null
        }
        render(viewportWidth, viewportHeight)
        backgroundInfoDialog?.refresh()
        onFadedToBlack()
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
        outgoingRaster?.let { raster -> layoutRaster(raster, viewportWidth, viewportHeight) }
        layoutRaster(activeRaster, viewportWidth, viewportHeight)
        backgroundWashLayer.removeChildren()
        backgroundWashLayer.solidRect(viewportWidth.toDouble(), viewportHeight.toDouble(), RGBA(0, 0, 0, 72))
        headerLayer.removeChildren()
        MapViewerLaunchpadTemplate.renderHeader(headerLayer, MapViewerLaunchShellLayoutBuilder.build(viewportWidth).headerBounds)
        MapViewerUiBoxes.button(backgroundInfoToggle, BACKGROUND_BUTTON_WIDTH, BACKGROUND_BUTTON_HEIGHT)
        MapViewerTextViews.box(
            parent = backgroundInfoToggle,
            text = background.biome,
            width = BACKGROUND_BUTTON_WIDTH,
            height = BACKGROUND_BUTTON_HEIGHT,
            color = io.hsar.mapgenerator.image.Palette.Colours.WHITE,
        )
        backgroundInfoToggle.xy(BACKGROUND_BUTTON_LEFT, BACKGROUND_BUTTON_TOP)
    }

    private fun currentViewport(fallbackWidth: Int, fallbackHeight: Int): MapViewerViewport =
        layer.stage?.views?.let { views ->
            MapViewerViewportResolver.resolve(
                actualWidth = views.actualVirtualWidth,
                actualHeight = views.actualVirtualHeight,
                fallbackWidth = views.gameWindow.width,
                fallbackHeight = views.gameWindow.height,
            )
        } ?: MapViewerViewport(fallbackWidth, fallbackHeight)

    private fun createTextureCanvas(background: LaunchTopographyBackground): Container =
        backgroundRasterLayer.clipContainer(
            Size2D(background.textureWidth.toDouble(), background.textureHeight.toDouble()),
        )

    private fun layoutRaster(raster: RasterCanvas, viewportWidth: Int, viewportHeight: Int) {
        LaunchBackgroundLayout.cover(
            raster.background.textureWidth,
            raster.background.textureHeight,
            viewportWidth,
            viewportHeight,
        ).also { placement ->
            raster.canvas.scaleX = placement.scale
            raster.canvas.scaleY = placement.scale
            raster.canvas.xy(placement.left, placement.top)
        }
    }

    private fun openBackgroundInfo() {
        val stage = layer.stage ?: return
        backgroundInfoDialog = MapViewerDialogWindow(
            root = layer,
            title = "Background",
            titleProvider = { "Background: ${background.biome}" },
            initialPosition = MapViewerDialogLayoutBuilder::alignLeft,
            persistence = MapViewerDialogPersistence(type = "launch-background"),
            preferredWidth = 360.0,
            preferredHeight = 330.0,
            contentHeight = { 150.0 },
            actions = listOf(MapViewerDialogAction("Regenerate") { onRegenerate() }),
            onClose = ::closeBackgroundInfo,
            renderContent = ::renderBackgroundInfo,
            mode = MapViewerDialogMode.FLOATING,
        ).also { dialog -> dialog.show(stage.views.actualVirtualWidth, stage.views.actualVirtualHeight) }
    }

    private fun closeBackgroundInfo() {
        backgroundInfoDialog?.close()
        backgroundInfoDialog = null
    }

    private fun renderBackgroundInfo(parent: Container, itemWidth: Double, contentViewportHeight: Double) {
        background.information.forEachIndexed { index, (label, value) ->
            MapViewerTextViews.box(parent, "$label: $value", itemWidth, 28.0).xy(0.0, index * 30.0)
        }
    }

    private fun renderSnapshot(snapshot: LaunchBackgroundTileSnapshot) {
        snapshot.refinement?.let { bitmap -> renderRefinement(snapshot.coordinate, bitmap, animate = false) }
            ?: snapshot.preview?.let { bitmap -> renderPreview(snapshot.coordinate, bitmap, animate = false) }
    }

    private fun renderPreview(coordinate: LaunchBackgroundTileCoordinate, bitmap: Bitmap32, animate: Boolean) {
        val rendered = renderedTile(coordinate)
        if (rendered.preview != null || rendered.refinement != null) return
        val image = rendered.clip.image(bitmap) {
            smoothing = false
            scaleX = LaunchBackgroundTileCoordinate.PREVIEW_DIVISOR.toDouble()
            scaleY = LaunchBackgroundTileCoordinate.PREVIEW_DIVISOR.toDouble()
            xy(
                -LaunchBackgroundTileCoordinate.TILE_BLEED.toDouble(),
                -LaunchBackgroundTileCoordinate.TILE_BLEED.toDouble(),
            )
            alpha = if (animate) 0.0 else 1.0
        }
        rendered.preview = image
        if (animate) {
            tileReveals += TileReveal.Preview(image, System.nanoTime())
        }
    }

    private fun renderRefinement(coordinate: LaunchBackgroundTileCoordinate, bitmap: Bitmap32, animate: Boolean) {
        val rendered = renderedTile(coordinate)
        if (rendered.refinement != null) return
        val image = rendered.clip.image(bitmap) {
            smoothing = true
            xy(
                -LaunchBackgroundTileCoordinate.TILE_BLEED.toDouble(),
                -LaunchBackgroundTileCoordinate.TILE_BLEED.toDouble(),
            )
            alpha = if (animate) 0.0 else 1.0
        }
        rendered.refinement = image
        if (animate) {
            tileReveals.removeAll { reveal ->
                reveal is TileReveal.Preview && reveal.preview === rendered.preview
            }
            tileReveals += TileReveal.Refinement(
                preview = rendered.preview,
                refinement = image,
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
                clip = activeRaster.canvas.clipContainer(
                    Size2D(
                        LaunchBackgroundTileCoordinate.TILE_CLIP_SIZE.toDouble(),
                        LaunchBackgroundTileCoordinate.TILE_CLIP_SIZE.toDouble(),
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

    private fun updateTileBlackouts(nowNanos: Long = System.nanoTime()) {
        tileBlackouts.forEach { blackout ->
            val progress = ((nowNanos - blackout.startedAtNanos).toDouble() / BLACKOUT_FADE_MILLISECONDS).coerceIn(0.0, 1.0)
            blackout.rendered.blackout?.alpha = progress
        }
        if (tileBlackouts.isNotEmpty() && tileBlackouts.all { blackout ->
                (nowNanos - blackout.startedAtNanos) / 1_000_000.0 >= BLACKOUT_FADE_MILLISECONDS
            }
        ) {
            outgoingRaster?.canvas?.removeFromParent()
            outgoingRaster = null
            tileBlackouts.clear()
        }
    }

    private data class RenderedTile(
        val clip: Container,
        var preview: Image? = null,
        var refinement: Image? = null,
        var blackout: SolidRect? = null,
    )

    private data class RasterCanvas(
        val background: LaunchTopographyBackground,
        val canvas: Container,
    )

    private data class TileBlackout(
        val rendered: RenderedTile,
        val startedAtNanos: Long,
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
        const val BACKGROUND_BUTTON_LEFT = 20.0
        const val BACKGROUND_BUTTON_TOP = 20.0
        const val BACKGROUND_BUTTON_WIDTH = 220.0
        const val BACKGROUND_BUTTON_HEIGHT = 42.0
        const val BLACKOUT_FADE_MILLISECONDS = 280.0
        const val BLACKOUT_STAGGER_MILLISECONDS = 45L
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
