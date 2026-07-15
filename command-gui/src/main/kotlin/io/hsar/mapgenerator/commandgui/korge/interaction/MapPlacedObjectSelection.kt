package io.hsar.mapgenerator.commandgui.korge.interaction

internal enum class MapPlacedObjectSelectionKind {
    UNIT,
    GRAPHIC,
    DRAWING,
}

internal data class MapPlacedObjectSelection(
    val kind: MapPlacedObjectSelectionKind,
    val id: MapPlacedObjectId,
    val distanceToPointer: Double,
)

internal object MapPlacedObjectSelectionResolver {
    fun choose(vararg candidates: MapPlacedObjectSelection?): MapPlacedObjectSelection? =
        candidates
            .filterNotNull()
            .minWithOrNull(
                compareBy<MapPlacedObjectSelection> { it.kind.priority }
                    .thenBy { it.distanceToPointer },
            )

    private val MapPlacedObjectSelectionKind.priority: Int
        get() = when (this) {
            MapPlacedObjectSelectionKind.UNIT -> 0
            MapPlacedObjectSelectionKind.GRAPHIC -> 1
            MapPlacedObjectSelectionKind.DRAWING -> 2
        }
}
