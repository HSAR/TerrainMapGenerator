package io.hsar.mapgenerator.commandgui.operation

enum class OperationDestroyedUnitDisplayMode(val label: String) {
    Full("Full"),
    Partial("Partial"),
    Hide("Hide");

    companion object {
        fun fromSettingsValue(value: String?): OperationDestroyedUnitDisplayMode =
            entries.firstOrNull { it.label.equals(value, true) || it.name.equals(value, true) } ?: Full
    }
}

object OperationDestroyedUnitDisplayPolicy {
    fun visible(unit: OperationUnit, mode: OperationDestroyedUnitDisplayMode) =
        !unit.destroyed || mode != OperationDestroyedUnitDisplayMode.Hide

    fun iconAlpha(unit: OperationUnit, mode: OperationDestroyedUnitDisplayMode) =
        if (unit.destroyed && mode == OperationDestroyedUnitDisplayMode.Partial) 0.5 else 1.0

    fun movable(unit: OperationUnit, mode: OperationDestroyedUnitDisplayMode) =
        !unit.destroyed || mode == OperationDestroyedUnitDisplayMode.Full

    fun movable(objects: List<OperationObject>, selectedIds: Set<OperationObjectId>, mode: OperationDestroyedUnitDisplayMode) =
        objects.filterIsInstance<OperationUnit>().none { it.id in selectedIds && !movable(it, mode) }

    fun visibleSelection(objects: List<OperationObject>, selectedIds: Set<OperationObjectId>, mode: OperationDestroyedUnitDisplayMode) =
        selectedIds.intersect(objects.filter { it !is OperationUnit || visible(it, mode) }.mapTo(hashSetOf(), OperationObject::id))
}
