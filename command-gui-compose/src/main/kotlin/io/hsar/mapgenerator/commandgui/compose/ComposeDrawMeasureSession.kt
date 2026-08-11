package io.hsar.mapgenerator.commandgui.compose

import io.hsar.mapgenerator.commandgui.MapCoordinate
import io.hsar.mapgenerator.commandgui.MapCoordinateBounds
import io.hsar.mapgenerator.commandgui.MapViewerDrawingGesture
import io.hsar.mapgenerator.commandgui.MapViewerDrawingMode
import io.hsar.mapgenerator.commandgui.MapViewerMeasurement
import io.hsar.mapgenerator.commandgui.measureMapDrag
import io.hsar.mapgenerator.commandgui.operation.OperationArgb
import io.hsar.mapgenerator.commandgui.operation.OperationDrawing
import io.hsar.mapgenerator.commandgui.startedAt
import io.hsar.mapgenerator.grid.GridLayer

internal data class ComposeDrawMeasureSession(
    val drawing: MapViewerDrawingGesture? = null,
    val measurementOrigin: MapCoordinate? = null,
    val measurement: MapViewerMeasurement? = null,
) {
    val active get() = drawing != null || measurementOrigin != null

    fun update(
        point: MapCoordinate,
        shiftHeld: Boolean,
        aspectRatio: Double,
        minimumFreeformDistance: Double,
        gridLayer: GridLayer?,
        mapWidth: Int,
        mapHeight: Int,
    ) = when {
        drawing != null -> copy(drawing = drawing.update(point, shiftHeld, aspectRatio, minimumFreeformDistance))
        measurementOrigin != null && gridLayer != null -> MapCoordinateBounds(0.0, 0.0, 1.0, 1.0).clamp(point).let { destination ->
            copy(measurement = measureMapDrag(gridLayer, measurementOrigin, destination, mapWidth, mapHeight))
        }
        else -> this
    }

    fun finishDrawing(): OperationDrawing? = drawing?.committedOrNull()

    fun release(): ComposeDrawMeasureOutcome = finishDrawing()
        ?.let(ComposeDrawMeasureOutcome::DrawingCommitted)
        ?: if (active) ComposeDrawMeasureOutcome.Cleared else ComposeDrawMeasureOutcome.NoOp

    fun cancel(): ComposeDrawMeasureOutcome =
        if (active) ComposeDrawMeasureOutcome.Cleared else ComposeDrawMeasureOutcome.NoOp

    companion object {
        fun startDrawing(
            mode: MapViewerDrawingMode,
            colour: OperationArgb,
            point: MapCoordinate,
            bounds: MapCoordinateBounds,
            blocked: Boolean = false,
        ) = mode.startedAt(point, colour, bounds)
            ?.takeUnless { blocked }
            ?.let { ComposeDrawMeasureSession(drawing = it) }

        fun startMeasurement(point: MapCoordinate, gridLayer: GridLayer?, blocked: Boolean = false) =
            point.takeIf { !blocked && gridLayer != null && it.x in 0.0..1.0 && it.y in 0.0..1.0 }
                ?.let { ComposeDrawMeasureSession(measurementOrigin = it) }
    }
}

internal sealed interface ComposeDrawMeasureOutcome {
    data class DrawingCommitted(val drawing: OperationDrawing) : ComposeDrawMeasureOutcome
    data object Cleared : ComposeDrawMeasureOutcome
    data object NoOp : ComposeDrawMeasureOutcome
}
