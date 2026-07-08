package io.hsar.mapgenerator.commandgui.korge.ui

import io.hsar.mapgenerator.commandgui.korge.interaction.CameraViewport
import io.hsar.mapgenerator.commandgui.korge.interaction.MapCoordinateSpace
import io.hsar.mapgenerator.commandgui.korge.interaction.MapPlacedObjectId
import io.hsar.mapgenerator.commandgui.korge.interaction.MapPlacedObjectSnapshot
import io.hsar.mapgenerator.commandgui.korge.interaction.ScreenSelectionFrame
import io.hsar.mapgenerator.commandgui.korge.interaction.selectionFrame

internal fun MapPlacedObjectSnapshot.focusTargetBounds(
    coordinateSpace: MapCoordinateSpace,
    viewport: CameraViewport,
): ScreenBounds =
    selectionFrame(coordinateSpace, viewport).bounds()

internal fun Collection<MapPlacedObjectSnapshot>.focusTargetBounds(
    coordinateSpace: MapCoordinateSpace,
    viewport: CameraViewport,
): List<ScreenBounds> =
    map { it.focusTargetBounds(coordinateSpace, viewport) }

internal fun referencedObjectFocusTargetBounds(
    referencedObjectIds: Set<MapPlacedObjectId>,
    objectSnapshot: (MapPlacedObjectId) -> MapPlacedObjectSnapshot?,
    coordinateSpace: MapCoordinateSpace,
    viewport: CameraViewport,
): List<ScreenBounds> =
    referencedObjectIds
        .mapNotNull(objectSnapshot)
        .focusTargetBounds(coordinateSpace, viewport)

private fun ScreenSelectionFrame.bounds(): ScreenBounds =
    ScreenBounds(
        left = corners.minOf { it.x },
        top = corners.minOf { it.y },
        width = corners.maxOf { it.x } - corners.minOf { it.x },
        height = corners.maxOf { it.y } - corners.minOf { it.y },
    )
