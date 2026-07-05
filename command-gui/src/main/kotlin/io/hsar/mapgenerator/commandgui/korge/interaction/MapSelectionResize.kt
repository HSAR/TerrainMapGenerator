package io.hsar.mapgenerator.commandgui.korge.interaction

import io.hsar.mapgenerator.commandgui.korge.drawing.FreeformDrawing
import io.hsar.mapgenerator.commandgui.korge.drawing.LineDrawing
import io.hsar.mapgenerator.commandgui.korge.drawing.MapDrawing
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

    fun resizeSnapshots(
        originalSnapshots: List<MapPlacedObjectSnapshot>,
        bounds: MapSelectionBounds,
        grabbedCorner: ScreenSelectionCorner,
        anchor: MapCoordinate,
        draggedCorner: MapCoordinate,
    ): List<MapPlacedObjectSnapshot> {
        val originalCorner = bounds.coordinate(grabbedCorner)
        val scaleX = scale(draggedCorner.x, anchor.x, originalCorner.x)
        val scaleY = scale(draggedCorner.y, anchor.y, originalCorner.y)
        return originalSnapshots.map { snapshot ->
            when (snapshot) {
                is UnitSnapshot -> snapshot
                is DrawingSnapshot -> snapshot.copy(
                    drawing = snapshot.drawing.resize(
                        anchor = anchor,
                        scaleX = scaleX,
                        scaleY = scaleY,
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

    private fun MapDrawing.resize(anchor: MapCoordinate, scaleX: Double, scaleY: Double): MapDrawing =
        when (this) {
            is FreeformDrawing -> copy(points = points.map { it.resize(anchor, scaleX, scaleY) })
            is LineDrawing -> copy(
                start = start.resize(anchor, scaleX, scaleY),
                end = end.resize(anchor, scaleX, scaleY),
            )

            is RectangleDrawing -> copy(
                start = start.resize(anchor, scaleX, scaleY),
                end = end.resize(anchor, scaleX, scaleY),
            )

            is OvalDrawing -> copy(
                center = center.resize(anchor, scaleX, scaleY),
                xRadius = xRadius * abs(scaleX),
                yRadius = yRadius * abs(scaleY),
            )
        }

    private fun MapDrawingPoint.resize(anchor: MapCoordinate, scaleX: Double, scaleY: Double): MapDrawingPoint =
        MapDrawingPoint(
            x = anchor.x + (x - anchor.x) * scaleX,
            y = anchor.y + (y - anchor.y) * scaleY,
        )

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

    private fun ScreenSelectionCorner.opposite(): ScreenSelectionCorner =
        when (this) {
            ScreenSelectionCorner.TOP_LEFT -> ScreenSelectionCorner.BOTTOM_RIGHT
            ScreenSelectionCorner.TOP_RIGHT -> ScreenSelectionCorner.BOTTOM_LEFT
            ScreenSelectionCorner.BOTTOM_RIGHT -> ScreenSelectionCorner.TOP_LEFT
            ScreenSelectionCorner.BOTTOM_LEFT -> ScreenSelectionCorner.TOP_RIGHT
        }

    private const val MIN_ORIGINAL_DELTA = 0.0000001
    private const val MIN_SCALE = 0.01
}
