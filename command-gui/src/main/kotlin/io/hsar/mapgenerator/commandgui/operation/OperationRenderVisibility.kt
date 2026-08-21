package io.hsar.mapgenerator.commandgui.operation

import io.hsar.mapgenerator.commandgui.MapViewerScreenBounds

/** Ordered paint candidates. Interaction must continue to use the complete operation content. */
data class OperationRenderVisibility(
    val drawings: List<OperationDrawingObject>,
    val graphics: List<OperationGraphic>,
    val units: List<OperationUnit>,
) {
    val objects: List<OperationObject> = drawings + graphics + units

    companion object {
        fun resolve(
            content: OperationContent,
            projection: OperationSelectionProjection,
            viewport: MapViewerScreenBounds,
            screenPadding: Double = 4.0,
        ): OperationRenderVisibility {
            fun OperationObject.visible() = viewport.expanded(screenPadding).overlaps(selectionScreenBounds(projection))
            return OperationRenderVisibility(
                drawings = content.objects.filterIsInstance<OperationDrawingObject>().filter(OperationObject::visible),
                graphics = content.objects.filterIsInstance<OperationGraphic>().filter(OperationObject::visible),
                units = content.objects.filterIsInstance<OperationUnit>().filter(OperationObject::visible),
            )
        }
    }
}
