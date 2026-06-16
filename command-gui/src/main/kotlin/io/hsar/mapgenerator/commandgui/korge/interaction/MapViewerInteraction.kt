package io.hsar.mapgenerator.commandgui.korge.interaction

import io.hsar.mapgenerator.commandgui.ViewerGeometry
import io.hsar.mapgenerator.grid.GridLayer
import io.hsar.mapgenerator.commandgui.korge.input.CameraPanner
import io.hsar.mapgenerator.commandgui.korge.input.PanState
import io.hsar.mapgenerator.graph.Point
import io.hsar.mapgenerator.graph.Rectangle
import korlibs.event.Key
import korlibs.event.MouseButton
import korlibs.korge.annotations.KorgeExperimental
import korlibs.korge.input.keys
import korlibs.korge.input.mouse
import korlibs.korge.input.cursor
import korlibs.korge.view.Camera
import korlibs.korge.view.View
import korlibs.korge.view.addUpdater
import korlibs.math.geom.Vector2D

@OptIn(KorgeExperimental::class)
internal class MapViewerInteraction(
    private val root: View,
    private val camera: Camera,
    private val geometry: ViewerGeometry,
    private val viewportWidth: Int,
    private val viewportHeight: Int,
    private val mapWidth: Int,
    private val mapHeight: Int,
    private val gridLayer: GridLayer?,
    private val onOverlayStateChanged: (GridOverlayState) -> Unit,
) {
    private var overlayDirty = true
    private var lastOverlayState: GridOverlayState? = null
    private var activeReference: GridCursorReference? = null
    private val cameraPanner = CameraPanner(
        initialViewport = CameraViewport.centeredOnMap(
            geometry = geometry,
            mapWidth = mapWidth,
            mapHeight = mapHeight,
            viewportWidth = viewportWidth,
            viewportHeight = viewportHeight,
        ),
        updateViewport = ::applyViewport,
    )
    private val panState = PanState(
        panner = cameraPanner,
        updateCursor = { root.cursor = it },
    )

    fun install() {
        val stage = root.stage ?: error("MapViewerInteraction requires the root to be attached to a stage")
        val input = stage.views.input
        applyViewport(cameraPanner.viewport)

        root.addUpdater {
            val pointer = input.mousePos
            val leftHeld = input.mouseButtonPressed(MouseButton.LEFT)
            val rightHeld = input.mouseButtonPressed(MouseButton.RIGHT)
            val spaceHeld = input.keys[Key.SPACE]

            if (panState.update(pointer, rightHeld, spaceHeld)) {
                markOverlayDirty()
            }
            if (updateActiveReference(pointer, leftHeld)) {
                markOverlayDirty()
            }

            panState.refreshCursor()
            flushOverlayIfDirty()
        }

        root.mouse {
            onScrollAnywhere {
                if (cameraPanner.zoomAt(input.mousePos, if (scrollDeltaYLines < 0) ZOOM_STEP_IN else ZOOM_STEP_OUT)) {
                    markOverlayDirty()
                }
            }
        }

        markOverlayDirty()
    }

    private fun applyViewport(viewport: CameraViewport) {
        camera.setTo(viewport.toCameraRectangle())
        markOverlayDirty()
    }

    private fun flushOverlayIfDirty() {
        val layer = gridLayer ?: return
        if (!overlayDirty) {
            return
        }

        val viewport = cameraPanner.viewport
        val mapLeft = viewport.screenX(geometry.mapOffsetX.toDouble())
        val mapTop = viewport.screenY(geometry.mapOffsetY.toDouble())
        val state = GridOverlayState(
            viewport = currentViewport(viewport),
            mapOffsetX = mapLeft,
            mapOffsetY = mapTop,
            scale = viewport.zoom,
            pixelWidth = mapWidth,
            pixelHeight = mapHeight,
            backgroundWidth = geometry.backgroundWidth,
            backgroundHeight = geometry.backgroundHeight,
            visibleCanvasLeft = 0.0,
            visibleCanvasTop = 0.0,
            visibleCanvasRight = viewportWidth.toDouble(),
            visibleCanvasBottom = viewportHeight.toDouble(),
            activeReference = activeReference,
            gridLayer = layer,
        )

        if (state != lastOverlayState) {
            onOverlayStateChanged(state)
            lastOverlayState = state
        }

        overlayDirty = false
    }

    private fun markOverlayDirty() {
        overlayDirty = true
    }

    private fun updateActiveReference(pointer: Vector2D, leftHeld: Boolean): Boolean {
        val nextReference = if (leftHeld) {
            createActiveReference(pointer)
        } else {
            null
        }
        if (nextReference == activeReference) {
            return false
        }

        activeReference = nextReference
        return true
    }

    private fun createActiveReference(pointer: Vector2D): GridCursorReference? {
        val viewport = cameraPanner.viewport
        val mapLeft = viewport.screenX(geometry.mapOffsetX.toDouble())
        val mapTop = viewport.screenY(geometry.mapOffsetY.toDouble())
        val mapRight = mapLeft + mapWidth * viewport.zoom
        val mapBottom = mapTop + mapHeight * viewport.zoom

        if (pointer.x !in mapLeft..mapRight || pointer.y !in mapTop..mapBottom) {
            return null
        }

        val mapX = ((pointer.x - mapLeft) / viewport.zoom).coerceIn(0.0, mapWidth.toDouble())
        val mapY = ((pointer.y - mapTop) / viewport.zoom).coerceIn(0.0, mapHeight.toDouble())

        return GridCursorReference(
            screenX = pointer.x,
            screenY = pointer.y,
            text = GridReferenceFormatter.format(layer = gridLayer ?: return null, mapX = mapX, mapY = mapY),
        )
    }

    private fun currentViewport(viewport: CameraViewport): Rectangle {
        val visibleLeft = maxOf(0.0, viewport.left - geometry.mapOffsetX)
        val visibleTop = maxOf(0.0, viewport.top - geometry.mapOffsetY)
        val visibleRight = minOf(mapWidth.toDouble(), viewport.right - geometry.mapOffsetX)
        val visibleBottom = minOf(mapHeight.toDouble(), viewport.bottom - geometry.mapOffsetY)

        return Rectangle(
            Point(visibleLeft, visibleTop),
            Point(visibleRight, visibleBottom),
        )
    }

    private companion object {
        private const val ZOOM_STEP_IN = 1.1
        private const val ZOOM_STEP_OUT = 1 / 1.1
    }
}

internal data class GridOverlayState(
    val viewport: Rectangle,
    val mapOffsetX: Double,
    val mapOffsetY: Double,
    val scale: Double,
    val pixelWidth: Int,
    val pixelHeight: Int,
    val backgroundWidth: Int,
    val backgroundHeight: Int,
    val visibleCanvasLeft: Double,
    val visibleCanvasTop: Double,
    val visibleCanvasRight: Double,
    val visibleCanvasBottom: Double,
    val activeReference: GridCursorReference?,
    val gridLayer: GridLayer,
)

internal data class GridCursorReference(
    val screenX: Double,
    val screenY: Double,
    val text: String,
)
