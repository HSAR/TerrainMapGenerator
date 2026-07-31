package io.hsar.mapgenerator.commandgui.compose

import io.hsar.mapgenerator.commandgui.MapCoordinate
import io.hsar.mapgenerator.commandgui.MapViewerLedgerReplay
import io.hsar.mapgenerator.commandgui.MapViewport
import io.hsar.mapgenerator.commandgui.ViewerPoint
import io.hsar.mapgenerator.commandgui.toWorldPoint
import java.util.*

/**
 * The interaction state owned by the Compose workspace rather than individual controls.
 *
 * Persisted operation content continues to be supplied by the shared ledger/save model; this
 * state only records the user's current workspace intent.
 */
data class ComposeOperationState(
    val activeTool: ComposeMapTool = ComposeMapTool.SELECT,
    val selectedObjectId: UUID? = null,
    val ledgerCursor: Int? = null,
    val noteDraft: String = "",
) {
    fun selectTool(tool: ComposeMapTool) =
        copy(activeTool = tool)

    fun selectObject(id: UUID?) =
        copy(selectedObjectId = id)

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
    PAN("Pan"),
    DRAW("Draw"),
    MEASURE("Measure"),
}
