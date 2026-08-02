package io.hsar.mapgenerator.commandgui.operation

import io.hsar.mapgenerator.commandgui.MapCoordinate
import io.hsar.mapgenerator.commandgui.MapCoordinateBounds
import io.hsar.mapgenerator.commandgui.MapCoordinateDelta
import io.hsar.mapgenerator.commandgui.MapSelectionCorner

data class OperationTransformModifiers(
    val altHeld: Boolean = false,
    val shiftHeld: Boolean = false,
)

data class OperationTransformResult(
    val content: OperationContent,
    val changeSet: OperationChangeSet,
)

sealed interface OperationTransformGesture {
    val original: OperationContent
    val selectedIds: Set<OperationObjectId>

    fun preview(pointer: MapCoordinate, modifiers: OperationTransformModifiers = OperationTransformModifiers()): OperationContent

    fun complete(pointer: MapCoordinate, modifiers: OperationTransformModifiers = OperationTransformModifiers()) =
        preview(pointer, modifiers).let { content -> OperationTransformResult(content, content.changesFrom(original)) }

    data class Move(
        override val original: OperationContent,
        override val selectedIds: Set<OperationObjectId>,
        val start: MapCoordinate,
        val backgroundBounds: MapCoordinateBounds,
        val aspectRatio: Double,
        val constrainUnit: (OperationUnit, Boolean) -> OperationUnit = { unit, _ -> unit },
    ) : OperationTransformGesture {
        override fun preview(pointer: MapCoordinate, modifiers: OperationTransformModifiers) =
            pointer.clampedTo(backgroundBounds).let { current ->
                original.moveUnbounded(
                    selectedIds,
                    MapCoordinateDelta(current.x - start.x, current.y - start.y),
                    constrainUnit,
                    modifiers.shiftHeld,
                )
            }
    }

    data class Resize(
        override val original: OperationContent,
        override val selectedIds: Set<OperationObjectId>,
        val grabbedCorner: MapSelectionCorner,
        val aspectRatio: Double,
        val originalFixedCorner: MapCoordinate? = null,
    ) : OperationTransformGesture {
        override fun preview(pointer: MapCoordinate, modifiers: OperationTransformModifiers) = original.resize(
            ids = selectedIds,
            grabbedCorner = grabbedCorner,
            draggedCorner = pointer,
            aspectRatio = aspectRatio,
            fromCentre = modifiers.altHeld,
            preserveAspect = modifiers.shiftHeld,
            originalFixedCorner = originalFixedCorner,
        )
    }

    data class Rotate(
        override val original: OperationContent,
        override val selectedIds: Set<OperationObjectId>,
        val pivot: MapCoordinate,
        val start: MapCoordinate,
        val aspectRatio: Double,
    ) : OperationTransformGesture {
        override fun preview(pointer: MapCoordinate, modifiers: OperationTransformModifiers) = original.rotate(
            selectedIds,
            rotationDelta(pivot, start, pointer, aspectRatio),
            aspectRatio,
            modifiers.shiftHeld,
        )
    }
}

private fun MapCoordinate.clampedTo(bounds: MapCoordinateBounds) = MapCoordinate(
    x.coerceIn(bounds.left, bounds.right),
    y.coerceIn(bounds.top, bounds.bottom),
)
