package io.hsar.mapgenerator.commandgui.korge.interaction

import io.hsar.mapgenerator.commandgui.ViewerGeometry
import io.hsar.mapgenerator.commandgui.korge.drawing.CircleDrawing
import io.hsar.mapgenerator.commandgui.korge.drawing.FreeformDrawing
import io.hsar.mapgenerator.commandgui.korge.drawing.MapDrawing
import io.hsar.mapgenerator.commandgui.korge.drawing.MapDrawingMode
import io.hsar.mapgenerator.commandgui.korge.drawing.MapDrawingPoint
import io.hsar.mapgenerator.commandgui.korge.drawing.MapDrawingState
import io.hsar.mapgenerator.commandgui.korge.drawing.MapDrawingStore
import io.hsar.mapgenerator.commandgui.korge.drawing.RectangleDrawing
import io.hsar.mapgenerator.commandgui.korge.input.CameraPanner
import io.hsar.mapgenerator.commandgui.korge.input.PanState
import io.hsar.mapgenerator.commandgui.korge.overlay.GridCursorReference
import io.hsar.mapgenerator.commandgui.korge.overlay.GridMeasurement
import io.hsar.mapgenerator.commandgui.korge.overlay.GridOverlayState
import io.hsar.mapgenerator.graph.Point
import io.hsar.mapgenerator.graph.Rectangle
import io.hsar.mapgenerator.grid.GridLayer
import io.hsar.mapgenerator.image.Palette
import korlibs.event.Key
import korlibs.event.MouseButton
import korlibs.korge.annotations.KorgeExperimental
import korlibs.korge.input.cursor
import korlibs.korge.input.mouse
import korlibs.korge.view.Camera
import korlibs.korge.view.View
import korlibs.korge.view.addUpdater
import korlibs.math.geom.Vector2D
import kotlin.math.hypot
import kotlin.math.pow

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
    private val currentTool: () -> MapViewerTool = { MapViewerTool.SELECT },
    private val currentDrawingMode: () -> MapDrawingMode = { MapDrawingMode.FREEFORM },
    private val drawingStore: MapDrawingStore = MapDrawingStore(),
    private val isScreenOverlayBlocking: (Vector2D) -> Boolean = { false },
    private val isMapPlacementDragging: () -> Boolean = { false },
    private val onViewportChanged: (CameraViewport) -> Unit = {},
    private val onTitleChanged: (String) -> Unit = {},
    private val onOverlayStateChanged: (GridOverlayState) -> Unit,
    private val onDrawingStateChanged: (MapDrawingState) -> Unit = {},
) {
    private var overlayDirty = true
    private var lastOverlayState: GridOverlayState? = null
    private var activeReference: GridCursorReference? = null
    private var activeMeasurement: GridMeasurement? = null
    private var measurementOrigin: MapPointerReference? = null
    private var activeDrawing: ActiveDrawing? = null
    private var previousLeftHeld = false
    private var previousTool = MapViewerTool.SELECT
    private var previousDrawingMode = MapDrawingMode.FREEFORM
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
            val placementDragging = isMapPlacementDragging()
            val tool = currentTool()
            val drawingMode = currentDrawingMode()
            val intent = MapPointerIntentResolver.resolve(
                pointerBlocked = isScreenOverlayBlocking(pointer),
                dragging = panState.isDragging,
                leftHeld = input.mouseButtonPressed(MouseButton.LEFT),
                rightHeld = input.mouseButtonPressed(MouseButton.RIGHT),
                spaceHeld = input.keys[Key.SPACE],
            )

            if (panState.update(pointer, intent.rightPanHeld, intent.spacePanHeld)) {
                markOverlayDirty()
            }
            if (tool != previousTool && clearMeasurement()) {
                markOverlayDirty()
            }
            if ((tool != previousTool || drawingMode != previousDrawingMode) && clearActiveDrawingPreview()) {
                notifyDrawingChanged()
            }
            if (updateMeasureState(pointer, intent.leftActive, tool)) {
                markOverlayDirty()
            }
            if (updateDrawingState(pointer, intent.leftActive, tool, drawingMode)) {
                notifyDrawingChanged()
            }
            if (updateActiveReference(
                    pointer = pointer,
                    leftHeld = if (placementDragging) input.mouseButtonPressed(MouseButton.LEFT) else intent.leftActive,
                    tool = tool,
                )
            ) {
                markOverlayDirty()
            }
            previousLeftHeld = intent.leftActive
            previousTool = tool
            previousDrawingMode = drawingMode

            panState.refreshCursor()
            flushOverlayIfDirty()
        }

        root.mouse {
            onScrollAnywhere {
                if (MapPointerIntentResolver.resolve(
                        pointerBlocked = isScreenOverlayBlocking(input.mousePos),
                        dragging = panState.isDragging,
                        leftHeld = input.mouseButtonPressed(MouseButton.LEFT),
                        rightHeld = input.mouseButtonPressed(MouseButton.RIGHT),
                        spaceHeld = input.keys[Key.SPACE],
                    ).allowZoom &&
                    cameraPanner.zoomAt(input.mousePos, zoomFactor(scrollDeltaYLines.toDouble()))
                ) {
                    markOverlayDirty()
                }
            }
        }

        markOverlayDirty()
    }

    private fun applyViewport(viewport: CameraViewport) {
        camera.setTo(viewport.toCameraRectangle())
        onViewportChanged(viewport)
        onTitleChanged(viewportTitle(viewport))
        markOverlayDirty()
    }

    private fun viewportTitle(viewport: CameraViewport): String =
        "Terrain Map Viewer - ${viewport.viewportWidth}x${viewport.viewportHeight}px - ${formatZoom(viewport.zoom)}"

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
            mapWorldX = geometry.mapOffsetX.toDouble(),
            mapWorldY = geometry.mapOffsetY.toDouble(),
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
            measurement = activeMeasurement,
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

    private fun updateActiveReference(pointer: Vector2D, leftHeld: Boolean, tool: MapViewerTool): Boolean {
        val nextReference = if (leftHeld && tool != MapViewerTool.DRAW) {
            createActiveReference(pointer, clampToMap = tool == MapViewerTool.MEASURE && measurementOrigin != null)
        } else {
            null
        }
        if (nextReference == activeReference) {
            return false
        }

        activeReference = nextReference
        return true
    }

    private fun updateDrawingState(
        pointer: Vector2D,
        leftHeld: Boolean,
        tool: MapViewerTool,
        mode: MapDrawingMode,
    ): Boolean {
        if (tool != MapViewerTool.DRAW) {
            return clearActiveDrawingPreview()
        }

        if (!leftHeld) {
            return commitActiveDrawing()
        }

        val point = if (activeDrawing == null) {
            if (previousLeftHeld) {
                return false
            }
            createMapPointerReference(pointer, clampToMap = false)?.toDrawingPoint() ?: return false
        } else {
            createMapPointerReference(pointer, clampToMap = true)?.toDrawingPoint() ?: return false
        }
        val nextDrawing = activeDrawing?.updated(point) ?: mode.startedAt(point)
        if (nextDrawing == activeDrawing) {
            return false
        }

        activeDrawing = nextDrawing
        drawingStore.preview(nextDrawing.preview)
        return true
    }

    private fun commitActiveDrawing(): Boolean {
        val drawing = activeDrawing?.committedOrNull() ?: return clearActiveDrawingPreview()
        activeDrawing = null
        drawingStore.commit(drawing)
        return true
    }

    private fun clearActiveDrawingPreview(): Boolean {
        if (activeDrawing == null && drawingStore.state.preview == null) {
            return false
        }

        activeDrawing = null
        drawingStore.clearPreview()
        return true
    }

    private fun notifyDrawingChanged() {
        onDrawingStateChanged(drawingStore.state)
    }

    private fun updateMeasureState(pointer: Vector2D, leftHeld: Boolean, tool: MapViewerTool): Boolean {
        if (tool != MapViewerTool.MEASURE || gridLayer == null) {
            return clearMeasurement()
        }

        if (!leftHeld) {
            return clearMeasurement()
        }

        val origin = measurementOrigin
            ?: if (previousLeftHeld) {
                return false
            } else {
                createMapPointerReference(pointer, clampToMap = false)?.also { measurementOrigin = it } ?: return false
            }
        val destination = createMapPointerReference(pointer, clampToMap = true) ?: return false
        val nextMeasurement = GridMeasurementCalculator.calculate(
            layer = gridLayer,
            originMapX = origin.mapX,
            originMapY = origin.mapY,
            destinationMapX = destination.mapX,
            destinationMapY = destination.mapY,
        )
        if (nextMeasurement == activeMeasurement) {
            return false
        }

        activeMeasurement = nextMeasurement
        return true
    }

    private fun clearMeasurement(): Boolean {
        if (measurementOrigin == null && activeMeasurement == null) {
            return false
        }

        measurementOrigin = null
        activeMeasurement = null
        return true
    }

    private fun createActiveReference(pointer: Vector2D, clampToMap: Boolean): GridCursorReference? =
        createMapPointerReference(pointer, clampToMap)
            ?.let { reference ->
                GridCursorReference(
                    screenX = pointer.x.coerceIn(
                        cameraPanner.viewport.screenX(geometry.mapOffsetX.toDouble()),
                        cameraPanner.viewport.screenX(geometry.mapOffsetX.toDouble() + mapWidth),
                    ),
                    screenY = pointer.y.coerceIn(
                        cameraPanner.viewport.screenY(geometry.mapOffsetY.toDouble()),
                        cameraPanner.viewport.screenY(geometry.mapOffsetY.toDouble() + mapHeight),
                    ),
                    text = GridReferenceFormatter.format(layer = gridLayer ?: return null, mapX = reference.mapX, mapY = reference.mapY),
                )
            }

    private fun createMapPointerReference(pointer: Vector2D, clampToMap: Boolean): MapPointerReference? {
        val viewport = cameraPanner.viewport
        val mapLeft = viewport.screenX(geometry.mapOffsetX.toDouble())
        val mapTop = viewport.screenY(geometry.mapOffsetY.toDouble())
        val mapRight = mapLeft + mapWidth * viewport.zoom
        val mapBottom = mapTop + mapHeight * viewport.zoom

        if (!clampToMap && (pointer.x !in mapLeft..mapRight || pointer.y !in mapTop..mapBottom)) {
            return null
        }

        val screenX = pointer.x.coerceIn(mapLeft, mapRight)
        val screenY = pointer.y.coerceIn(mapTop, mapBottom)
        val mapX = ((screenX - mapLeft) / viewport.zoom).coerceIn(0.0, mapWidth.toDouble())
        val mapY = ((screenY - mapTop) / viewport.zoom).coerceIn(0.0, mapHeight.toDouble())

        return MapPointerReference(
            mapX = mapX,
            mapY = mapY,
        )
    }

    private fun MapPointerReference.toDrawingPoint() = MapDrawingPoint(mapX, mapY)

    private fun MapDrawingMode.startedAt(point: MapDrawingPoint): ActiveDrawing =
        when (this) {
            MapDrawingMode.FREEFORM -> ActiveDrawing.Freeform(listOf(point))
            MapDrawingMode.RECTANGLE -> ActiveDrawing.Rectangle(start = point, end = point)
            MapDrawingMode.CIRCLE -> ActiveDrawing.Circle(center = point, edge = point)
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
        private const val ZOOM_STEP = 1.4
        private const val MIN_FREEFORM_POINT_DISTANCE = 0.5

        private fun zoomFactor(scrollDeltaYLines: Double): Double =
            ZOOM_STEP.pow(-scrollDeltaYLines)

        private fun formatZoom(value: Double) = "${(value * 100).toInt()}%"
    }

    private data class MapPointerReference(
        val mapX: Double,
        val mapY: Double,
    )

    private sealed interface ActiveDrawing {
        val preview: MapDrawing

        fun updated(point: MapDrawingPoint): ActiveDrawing

        fun committedOrNull(): MapDrawing?

        data class Freeform(
            val points: List<MapDrawingPoint>,
        ) : ActiveDrawing {
            override val preview: MapDrawing
                get() = FreeformDrawing(points.toList(), Palette.Colours.RED)

            override fun updated(point: MapDrawingPoint): ActiveDrawing {
                val lastPoint = points.last()
                return if (hypot(point.x - lastPoint.x, point.y - lastPoint.y) >= MIN_FREEFORM_POINT_DISTANCE) {
                    copy(points = points + point)
                } else {
                    this
                }
            }

            override fun committedOrNull(): MapDrawing? =
                points.takeIf { it.size >= 2 }?.let { FreeformDrawing(it.toList(), Palette.Colours.RED) }
        }

        data class Rectangle(
            val start: MapDrawingPoint,
            val end: MapDrawingPoint,
        ) : ActiveDrawing {
            override val preview: MapDrawing
                get() = RectangleDrawing(start, end, Palette.Colours.RED)

            override fun updated(point: MapDrawingPoint): ActiveDrawing =
                copy(end = point)

            override fun committedOrNull(): MapDrawing? =
                preview.takeIf { start != end }
        }

        data class Circle(
            val center: MapDrawingPoint,
            val edge: MapDrawingPoint,
        ) : ActiveDrawing {
            override val preview: MapDrawing
                get() = CircleDrawing(center, edge, Palette.Colours.RED)

            override fun updated(point: MapDrawingPoint): ActiveDrawing =
                copy(edge = point)

            override fun committedOrNull(): MapDrawing? =
                preview.takeIf { center != edge }
        }
    }
}
