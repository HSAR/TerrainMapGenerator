package io.hsar.mapgenerator.commandgui.korge.ui

import io.hsar.mapgenerator.commandgui.korge.interaction.MapPlacedObjectId

internal object MapViewerNoteReferenceDropTarget {
    fun canCreateReference(
        readOnly: Boolean,
        draggedObjectIds: Set<MapPlacedObjectId>,
        referencedObjectIds: Collection<MapPlacedObjectId>,
        pointerInDropTarget: Boolean,
    ): Boolean =
        !readOnly &&
                pointerInDropTarget &&
                draggedObjectIds.any { it !in referencedObjectIds }
}
