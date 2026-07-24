package io.hsar.mapgenerator.commandgui.korge.interaction

import io.hsar.mapgenerator.commandgui.korge.drawing.FreeformDrawing
import io.hsar.mapgenerator.commandgui.korge.drawing.LineDrawing
import io.hsar.mapgenerator.commandgui.korge.drawing.MapDrawing
import io.hsar.mapgenerator.commandgui.korge.drawing.MapDrawingGeometry
import io.hsar.mapgenerator.commandgui.korge.drawing.MapDrawingPoint
import io.hsar.mapgenerator.commandgui.korge.drawing.OvalDrawing
import io.hsar.mapgenerator.commandgui.korge.drawing.RectangleDrawing
import kotlin.math.abs

internal object MapSelectionResize {
    fun anchorFor(
        bounds: MapSelectionBounds,
        grabbedCorner: ScreenSelectionCorner,
        selectionSize: Int,
        altHeld: Boolean,
    ): MapCoordinate =
        if (useCenterAnchor(selectionSize, altHeld)) {
            bounds.center()
        } else {
            bounds.coordinate(grabbedCorner.opposite())
        }

    fun anchorFor(
        drawing: MapDrawing,
        grabbedCorner: ScreenSelectionCorner,
        altHeld: Boolean,
        aspectRatio: Double,
    ): MapCoordinate =
        when (drawing) {
            is RectangleDrawing -> {
                val bounds = MapSelectionBounds.fromPoints(drawing.start.toCoordinate(), drawing.end.toCoordinate())
                if (altHeld) bounds.center() else bounds.coordinate(grabbedCorner.opposite())
            }

            is OvalDrawing -> {
                val bounds = MapSelectionBounds(
                    left = drawing.center.x - drawing.xRadius,
                    top = drawing.center.y - drawing.yRadius,
                    right = drawing.center.x + drawing.xRadius,
                    bottom = drawing.center.y + drawing.yRadius,
                )
                if (altHeld) bounds.center() else bounds.coordinate(grabbedCorner.opposite())
            }

            is FreeformDrawing -> anchorFor(
                bounds = drawing.selectionBounds(aspectRatio),
                grabbedCorner = grabbedCorner,
                selectionSize = 1,
                altHeld = altHeld,
            )

            is LineDrawing ->
                if (altHeld) {
                    MapCoordinate(
                        x = (drawing.start.x + drawing.end.x) * 0.5,
                        y = (drawing.start.y + drawing.end.y) * 0.5,
                    )
                } else {
                    drawing.endpointFor(grabbedCorner.opposite())
                }
        }

    fun resizeSnapshots(
        originalSnapshots: List<MapPlacedObjectSnapshot>,
        bounds: MapSelectionBounds,
        grabbedCorner: ScreenSelectionCorner,
        anchor: MapCoordinate,
        draggedCorner: MapCoordinate,
        originalFixedCorner: MapCoordinate? = null,
        selectionSize: Int = originalSnapshots.size,
        aspectRatio: Double = 1.0,
    ): List<MapPlacedObjectSnapshot> {
        val originalCorner = bounds.coordinate(grabbedCorner)
        val scaleX = scale(draggedCorner.x, anchor.x, originalCorner.x)
        val scaleY = scale(draggedCorner.y, anchor.y, originalCorner.y)

        return originalSnapshots.map { snapshot ->
            when (snapshot) {
                is UnitSnapshot -> snapshot
                is GraphicSnapshot -> snapshot.copy(
                    mapCoordinate = MapCoordinate(
                        anchor.x + (snapshot.mapCoordinate.x - anchor.x) * graphicScale(scaleX, scaleY),
                        anchor.y + (snapshot.mapCoordinate.y - anchor.y) * graphicScale(scaleX, scaleY),
                    ),
                    normalizedWidth = (snapshot.normalizedWidth * abs(graphicScale(scaleX, scaleY))).coerceAtLeast(MIN_LOCAL_RADIUS),
                    normalizedHeight = (snapshot.normalizedHeight * abs(graphicScale(scaleX, scaleY))).coerceAtLeast(MIN_LOCAL_RADIUS),
                )
                is DrawingSnapshot -> snapshot.copy(
                    drawing = snapshot.drawing.resize(
                        bounds = bounds,
                        grabbedCorner = grabbedCorner,
                        anchor = anchor,
                        draggedCorner = draggedCorner,
                        originalFixedCorner = originalFixedCorner,
                        scaleX = scaleX,
                        scaleY = scaleY,
                        selectionSize = selectionSize,
                        aspectRatio = aspectRatio,
                    ),
                )
            }
        }
    }

    private fun useCenterAnchor(selectionSize: Int, altHeld: Boolean): Boolean =
        if (selectionSize == 1) altHeld else !altHeld

    private fun scale(current: Double, anchor: Double, original: Double): Double {
        val originalDelta = original - anchor
        if (abs(originalDelta) <= MIN_ORIGINAL_DELTA) {
            return 1.0
        }
        return ((current - anchor) / originalDelta).coerceAtLeast(MIN_SCALE)
    }

    private fun graphicScale(scaleX: Double, scaleY: Double): Double =
        if (abs(scaleX - 1.0) >= abs(scaleY - 1.0)) scaleX else scaleY

    private fun MapDrawing.resize(
        bounds: MapSelectionBounds,
        grabbedCorner: ScreenSelectionCorner,
        anchor: MapCoordinate,
        draggedCorner: MapCoordinate,
        originalFixedCorner: MapCoordinate?,
        scaleX: Double,
        scaleY: Double,
        selectionSize: Int,
        aspectRatio: Double,
    ): MapDrawing =
        when (this) {
            is FreeformDrawing -> copy(points = points.map { it.resize(anchor, scaleX, scaleY) })
            is LineDrawing -> resizeFromEndpoint(
                grabbedCorner = grabbedCorner,
                anchor = anchor,
                draggedCorner = draggedCorner,
                altHeld = selectionSize == 1 && anchor.isCloseTo(MapDrawingGeometry.center(start, end)),
            )

            is RectangleDrawing ->
                if (selectionSize == 1 && rotationDegrees != 0.0 && originalFixedCorner != null) {
                    if (anchor.isCloseTo(MapDrawingGeometry.center(start, end))) {
                        resizeRotatedRectangleFromCenter(draggedCorner, aspectRatio)
                    } else {
                        resizeRotatedRectangleFromFixedCorner(originalFixedCorner, draggedCorner, aspectRatio)
                    }
                } else {
                    copy(
                        start = start.resize(anchor, scaleX, scaleY),
                        end = end.resize(anchor, scaleX, scaleY),
                    )
                }

            is OvalDrawing ->
                if (selectionSize == 1 && rotationDegrees != 0.0 && originalFixedCorner != null) {
                    if (anchor.isCloseTo(center)) {
                        resizeRotatedOvalFromCenter(draggedCorner, aspectRatio)
                    } else {
                        resizeRotatedOvalFromFixedCorner(originalFixedCorner, draggedCorner, aspectRatio)
                    }
                } else {
                    copy(
                        center = center.resize(anchor, scaleX, scaleY),
                        xRadius = xRadius * abs(scaleX),
                        yRadius = yRadius * abs(scaleY),
                    )
                }
        }

    private fun RectangleDrawing.resizeRotatedRectangleFromFixedCorner(
        originalFixedCorner: MapCoordinate,
        draggedCorner: MapCoordinate,
        aspectRatio: Double,
    ): RectangleDrawing {
        val centerPoint = originalFixedCorner.midpoint(draggedCorner).toDrawingPoint()
        val fixedLocal = originalFixedCorner.rotateAround(centerPoint, -rotationDegrees, aspectRatio)
        val draggedLocal = draggedCorner.rotateAround(centerPoint, -rotationDegrees, aspectRatio)
        val localBounds = MapSelectionBounds.fromPoints(fixedLocal.toCoordinate(), draggedLocal.toCoordinate())
        return copy(
            start = MapDrawingPoint(localBounds.left, localBounds.top),
            end = MapDrawingPoint(localBounds.right, localBounds.bottom),
        )
    }

    private fun LineDrawing.resizeFromEndpoint(
        grabbedCorner: ScreenSelectionCorner,
        anchor: MapCoordinate,
        draggedCorner: MapCoordinate,
        altHeld: Boolean,
    ): LineDrawing =
        if (altHeld) {
            val oppositeEndpoint = MapDrawingPoint(
                x = anchor.x * 2.0 - draggedCorner.x,
                y = anchor.y * 2.0 - draggedCorner.y,
            )
            if (grabbedCorner.selectsStartEndpoint()) {
                copy(start = draggedCorner.toDrawingPoint(), end = oppositeEndpoint)
            } else {
                copy(start = oppositeEndpoint, end = draggedCorner.toDrawingPoint())
            }
        } else if (grabbedCorner.selectsStartEndpoint()) {
            copy(start = draggedCorner.toDrawingPoint(), end = anchor.toDrawingPoint())
        } else {
            copy(start = anchor.toDrawingPoint(), end = draggedCorner.toDrawingPoint())
        }

    private fun RectangleDrawing.resizeRotatedRectangleFromCenter(
        draggedCorner: MapCoordinate,
        aspectRatio: Double,
    ): RectangleDrawing {
        val centerPoint = MapDrawingGeometry.center(start, end)
        val draggedLocal = draggedCorner.rotateAround(centerPoint, -rotationDegrees, aspectRatio)
        val xRadius = abs(draggedLocal.x - centerPoint.x).coerceAtLeast(MIN_LOCAL_RADIUS)
        val yRadius = abs(draggedLocal.y - centerPoint.y).coerceAtLeast(MIN_LOCAL_RADIUS)
        return copy(
            start = MapDrawingPoint(centerPoint.x - xRadius, centerPoint.y - yRadius),
            end = MapDrawingPoint(centerPoint.x + xRadius, centerPoint.y + yRadius),
        )
    }

    private fun OvalDrawing.resizeRotatedOvalFromFixedCorner(
        originalFixedCorner: MapCoordinate,
        draggedCorner: MapCoordinate,
        aspectRatio: Double,
    ): OvalDrawing {
        val centerPoint = originalFixedCorner.midpoint(draggedCorner).toDrawingPoint()
        val fixedLocal = originalFixedCorner.rotateAround(centerPoint, -rotationDegrees, aspectRatio)
        val draggedLocal = draggedCorner.rotateAround(centerPoint, -rotationDegrees, aspectRatio)
        val localBounds = MapSelectionBounds.fromPoints(fixedLocal.toCoordinate(), draggedLocal.toCoordinate())
        return copy(
            center = MapDrawingPoint(
                x = (localBounds.left + localBounds.right) * 0.5,
                y = (localBounds.top + localBounds.bottom) * 0.5,
            ),
            xRadius = (localBounds.right - localBounds.left) * 0.5,
            yRadius = (localBounds.bottom - localBounds.top) * 0.5,
        )
    }

    private fun OvalDrawing.resizeRotatedOvalFromCenter(
        draggedCorner: MapCoordinate,
        aspectRatio: Double,
    ): OvalDrawing {
        val draggedLocal = draggedCorner.rotateAround(center, -rotationDegrees, aspectRatio)
        return copy(
            xRadius = abs(draggedLocal.x - center.x).coerceAtLeast(MIN_LOCAL_RADIUS),
            yRadius = abs(draggedLocal.y - center.y).coerceAtLeast(MIN_LOCAL_RADIUS),
        )
    }

    private fun MapDrawingPoint.resize(anchor: MapCoordinate, scaleX: Double, scaleY: Double): MapDrawingPoint =
        MapDrawingPoint(
            x = anchor.x + (x - anchor.x) * scaleX,
            y = anchor.y + (y - anchor.y) * scaleY,
        )

    private fun MapDrawingPoint.rotateAround(
        pivot: MapDrawingPoint,
        rotationDegrees: Double,
        aspectRatio: Double,
    ): MapDrawingPoint =
        run {
            val radians = Math.toRadians(rotationDegrees)
            val dx = (x - pivot.x) * aspectRatio
            val dy = y - pivot.y
            val cos = kotlin.math.cos(radians)
            val sin = kotlin.math.sin(radians)
            MapDrawingPoint(
                x = pivot.x + (dx * cos - dy * sin) / aspectRatio,
                y = pivot.y + dx * sin + dy * cos,
            )
        }

    private fun MapCoordinate.rotateAround(
        pivot: MapDrawingPoint,
        rotationDegrees: Double,
        aspectRatio: Double,
    ): MapDrawingPoint =
        MapDrawingPoint(x, y).rotateAround(pivot, rotationDegrees, aspectRatio)

    private fun MapCoordinate.midpoint(other: MapCoordinate): MapCoordinate =
        MapCoordinate(
            x = (x + other.x) * 0.5,
            y = (y + other.y) * 0.5,
        )

    private fun MapCoordinate.toDrawingPoint(): MapDrawingPoint =
        MapDrawingPoint(x, y)

    private fun MapDrawingPoint.toCoordinate(): MapCoordinate =
        MapCoordinate(x, y)

    private fun MapSelectionBounds.coordinate(corner: ScreenSelectionCorner): MapCoordinate =
        when (corner) {
            ScreenSelectionCorner.TOP_LEFT -> MapCoordinate(left, top)
            ScreenSelectionCorner.TOP_RIGHT -> MapCoordinate(right, top)
            ScreenSelectionCorner.BOTTOM_RIGHT -> MapCoordinate(right, bottom)
            ScreenSelectionCorner.BOTTOM_LEFT -> MapCoordinate(left, bottom)
        }

    private fun MapSelectionBounds.center(): MapCoordinate =
        MapCoordinate(
            x = (left + right) * 0.5,
            y = (top + bottom) * 0.5,
        )

    private fun MapCoordinate.isCloseTo(other: MapDrawingPoint): Boolean =
        kotlin.math.abs(x - other.x) <= 0.0000001 && kotlin.math.abs(y - other.y) <= 0.0000001

    private fun ScreenSelectionCorner.opposite(): ScreenSelectionCorner =
        when (this) {
            ScreenSelectionCorner.TOP_LEFT -> ScreenSelectionCorner.BOTTOM_RIGHT
            ScreenSelectionCorner.TOP_RIGHT -> ScreenSelectionCorner.BOTTOM_LEFT
            ScreenSelectionCorner.BOTTOM_RIGHT -> ScreenSelectionCorner.TOP_LEFT
            ScreenSelectionCorner.BOTTOM_LEFT -> ScreenSelectionCorner.TOP_RIGHT
        }

    private fun ScreenSelectionCorner.selectsStartEndpoint(): Boolean =
        this == ScreenSelectionCorner.TOP_LEFT || this == ScreenSelectionCorner.BOTTOM_LEFT

    private fun LineDrawing.endpointFor(corner: ScreenSelectionCorner): MapCoordinate =
        if (corner.selectsStartEndpoint()) start.toCoordinate() else end.toCoordinate()

    private fun MapDrawing.selectionBounds(aspectRatio: Double): MapSelectionBounds =
        when (this) {
            is FreeformDrawing -> points.bounds()
            is LineDrawing -> listOf(start, end).bounds()
            is RectangleDrawing -> MapSelectionBounds.fromPoints(start.toCoordinate(), end.toCoordinate())
            is OvalDrawing -> MapSelectionBounds(
                left = center.x - xRadius,
                top = center.y - yRadius,
                right = center.x + xRadius,
                bottom = center.y + yRadius,
            )
        }

    private fun List<MapDrawingPoint>.bounds(): MapSelectionBounds =
        if (isEmpty()) {
            MapSelectionBounds(0.0, 0.0, 0.0, 0.0)
        } else {
            MapSelectionBounds(
                left = minOf { it.x },
                top = minOf { it.y },
                right = maxOf { it.x },
                bottom = maxOf { it.y },
            )
        }

    private const val MIN_ORIGINAL_DELTA = 0.0000001
    private const val MIN_LOCAL_RADIUS = 0.0000001
    private const val MIN_SCALE = 0.01
}
