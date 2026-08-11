package io.hsar.mapgenerator.commandgui.operation

const val OPERATION_DISPLAY_NAME_PROPERTY = "displayName"

val OperationDrawingObject.displayName: String
    get() = customProperties[OPERATION_DISPLAY_NAME_PROPERTY]?.trim()?.takeIf(String::isNotEmpty)
        ?: drawing.displayName

val OperationDrawingObject.optionalDisplayName: String?
    get() = customProperties[OPERATION_DISPLAY_NAME_PROPERTY]?.trim()?.takeIf(String::isNotEmpty)

object OperationDrawingAuthoring {
    fun rename(
        content: OperationContent,
        drawingId: OperationObjectId,
        displayName: String,
    ): OperationChangeSet {
        val before = content.objectById(drawingId) as? OperationDrawingObject ?: return OperationChangeSet()
        val normalized = displayName.trim().takeIf(String::isNotEmpty)
        val properties = normalized
            ?.let { before.customProperties + (OPERATION_DISPLAY_NAME_PROPERTY to it) }
            ?: before.customProperties - OPERATION_DISPLAY_NAME_PROPERTY
        val after = before.copy(customProperties = properties)
        return if (after == before) OperationChangeSet()
        else OperationChangeSet(objects = listOf(OperationValueChange(before, after)))
    }
}
