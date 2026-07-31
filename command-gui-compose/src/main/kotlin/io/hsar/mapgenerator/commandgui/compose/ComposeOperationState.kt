package io.hsar.mapgenerator.commandgui.compose

import io.hsar.mapgenerator.commandgui.MapCoordinate
import io.hsar.mapgenerator.commandgui.MapViewerLedgerReplay
import io.hsar.mapgenerator.commandgui.MapViewport
import io.hsar.mapgenerator.commandgui.ViewerPoint
import io.hsar.mapgenerator.commandgui.toWorldPoint
import io.hsar.mapgenerator.commandgui.withBoxSelection
import io.hsar.mapgenerator.commandgui.withSelection
import java.util.*
import java.util.Collections.emptySet

/**
 * The interaction state owned by the Compose workspace rather than individual controls.
 *
 * Persisted operation content continues to be supplied by the shared ledger/save model; this
 * state only records the user's current workspace intent.
 */
data class ComposeOperationState(
    val activeTool: ComposeMapTool = ComposeMapTool.SELECT,
    val selectedObjectIds: Set<UUID> = emptySet(),
    val ledgerCursor: Int? = null,
    val noteDraft: String = "",
) {
    fun selectTool(tool: ComposeMapTool) =
        copy(activeTool = tool)

    fun selectObject(id: UUID?, additive: Boolean = false) =
        copy(selectedObjectIds = selectedObjectIds.withSelection(id, additive))

    fun selectBox(ids: Set<UUID>, additive: Boolean) =
        copy(selectedObjectIds = selectedObjectIds.withBoxSelection(ids, additive))

    fun moveLedgerCursor(index: Int?) =
        copy(ledgerCursor = index)

    fun updateNoteDraft(note: String) =
        copy(noteDraft = note)
}

internal fun MapViewerLedgerReplay.objectAtScreen(
    screenPoint: ViewerPoint,
    viewport: MapViewport,
    mapOffsetX: Double,
    mapOffsetY: Double,
    mapWidth: Int,
    mapHeight: Int,
    hitRadius: Double = 12.0,
) = objects.asReversed().firstOrNull { mapObject ->
    mapObject.x?.let { mapX ->
        mapObject.y?.let { mapY ->
            MapCoordinate(mapX, mapY).toWorldPoint(mapOffsetX, mapOffsetY, mapWidth, mapHeight).let { worldPoint ->
                (viewport.screenX(worldPoint.x) - screenPoint.x).let { x ->
                    (viewport.screenY(worldPoint.y) - screenPoint.y).let { y ->
                        x * x + y * y <= hitRadius * hitRadius
                    }
                }
            }
        }
    } == true
}

enum class ComposeMapTool(
    val label: String,
) {
    SELECT("Select"),
    DRAW("Draw"),
    MEASURE("Measure"),
}
