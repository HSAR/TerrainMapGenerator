package io.hsar.mapgenerator.commandgui.korge.ui

import io.hsar.mapgenerator.commandgui.korge.interaction.TacticalFireResolution
import io.hsar.mapgenerator.commandgui.korge.interaction.operation
import io.hsar.mapgenerator.commandgui.operation.OperationGunrepPresentationService
import io.hsar.mapgenerator.image.Palette
import korlibs.korge.view.Container
import korlibs.korge.view.container
import korlibs.korge.view.xy
import kotlin.math.ceil

internal class MapViewerGunrepDialog(
    private val root: Container,
    private val dialogManager: MapViewerDialogManager,
    private val dialogKey: Any,
    private val resolution: TacticalFireResolution,
    private val onDismiss: () -> Unit = {},
) {
    private val presentation = OperationGunrepPresentationService.present(resolution.operation())
    private var window: MapViewerDialogWindow? = null

    fun open(): Boolean {
        if (dialogManager.hasTarget(dialogKey)) return false
        val stage = root.stage ?: return false
        window = MapViewerDialogWindow(
            root = root,
            title = presentation.title,
            preferredWidth = 520.0,
            preferredHeight = 400.0,
            contentHeight = { contentHeight() },
            actions = listOf(MapViewerDialogAction("Dismiss") { close() }),
            onClose = ::close,
            renderContent = { parent, width, _ ->
                MapViewerTextViews.leftBox(parent, presentation.attackerLabel, width, 34.0)
                MapViewerTextViews.leftBox(parent, presentation.weaponsLabel, width, 34.0) { y = 40.0 }
                MapViewerTextViews.leftBox(parent, "Rolls:", width, 34.0) { y = 80.0 }
                resolution.dieResults.forEachIndexed { index, result ->
                    val row = index / ROLLS_PER_ROW
                    val column = index % ROLLS_PER_ROW
                    val die = parent.container().xy(column * DIE_WIDTH, 120.0 + row * DIE_HEIGHT)
                    MapViewerTextViews.leftBox(
                        die,
                        result.label,
                        DIE_WIDTH - DIE_GAP,
                        DIE_HEIGHT - DIE_GAP,
                        color = if (result.damage > 0) Palette.Colours.GREEN else Palette.Colours.WHITE,
                    )
                }
                val summaryTop = 120.0 + rollRows() * DIE_HEIGHT + 8.0
                MapViewerTextViews.leftBox(parent, presentation.damageLabel, width, 34.0) { y = summaryTop }
                presentation.ecmLabel?.let { MapViewerTextViews.leftBox(parent, it, width, 34.0) { y = summaryTop + 40.0 } }
                val outcomeOffset = if (resolution.ecmActivations.isNotEmpty() || resolution.effectiveEcmLevel > 0) 40.0 else 0.0
                presentation.fightersDestroyedLabel?.let { MapViewerTextViews.leftBox(parent, it, width, 34.0) { y = summaryTop + 40.0 + outcomeOffset } }
                presentation.targetDestroyedLabel?.let { MapViewerTextViews.leftBox(parent, it, width, 34.0) { y = summaryTop + 80.0 + outcomeOffset } }
            },
            persistence = MapViewerDialogPersistence(type = "gunrep"),
            mode = MapViewerDialogMode.FLOATING,
            dialogManager = dialogManager,
        )
        dialogManager.register(
            owner = this,
            targetKey = dialogKey,
            blocksScreenPoint = { point -> window?.blocksScreenPoint(point) == true },
            close = ::close,
        )
        window?.show(stage.views.actualVirtualWidth, stage.views.actualVirtualHeight)
        return true
    }

    private fun close() {
        if (window == null) return
        dialogManager.unregister(this)
        window?.close()
        window = null
        onDismiss()
    }

    private fun rollRows(): Int = ceil(resolution.dieResults.size / ROLLS_PER_ROW.toDouble()).toInt().coerceAtLeast(1)

    private fun contentHeight(): Double = 120.0 + rollRows() * DIE_HEIGHT + 128.0 +
            if (resolution.ecmActivations.isNotEmpty() || resolution.effectiveEcmLevel > 0) 40.0 else 0.0

    private companion object {
        const val ROLLS_PER_ROW = 8
        const val DIE_WIDTH = 56.0
        const val DIE_HEIGHT = 36.0
        const val DIE_GAP = 4.0
    }
}
