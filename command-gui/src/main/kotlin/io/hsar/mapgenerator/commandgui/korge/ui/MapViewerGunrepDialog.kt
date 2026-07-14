package io.hsar.mapgenerator.commandgui.korge.ui

import io.hsar.mapgenerator.commandgui.korge.interaction.TacticalFireResolution
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
    private var window: MapViewerDialogWindow? = null

    fun open(): Boolean {
        if (dialogManager.hasTarget(dialogKey)) return false
        val stage = root.stage ?: return false
        window = MapViewerDialogWindow(
            root = root,
            title = "GUNREP — ${resolution.targetName}",
            preferredWidth = 520.0,
            preferredHeight = 400.0,
            contentHeight = { contentHeight() },
            actions = listOf(MapViewerDialogAction("Dismiss") { close() }),
            onClose = ::close,
            renderContent = { parent, width ->
                MapViewerTextViews.leftBox(parent, "Attacker: ${resolution.attackerName}", width, 34.0)
                MapViewerTextViews.leftBox(parent, "Weapons: ${weaponSummary()}", width, 34.0) { y = 40.0 }
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
                MapViewerTextViews.leftBox(parent, "Damage: ${resolution.damage}", width, 34.0) { y = summaryTop }
                resolution.fightersDestroyed.takeIf { it > 0 }?.let { fighters ->
                    MapViewerTextViews.leftBox(parent, "Fighters destroyed: $fighters", width, 34.0) { y = summaryTop + 40.0 }
                }
                resolution.targetDestroyed.takeIf { it }?.let {
                    MapViewerTextViews.leftBox(parent, "Target destroyed", width, 34.0) { y = summaryTop + 80.0 }
                }
            },
            persistence = MapViewerDialogPersistence(type = "gunrep"),
            mode = MapViewerDialogMode.FLOATING,
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

    private fun weaponSummary(): String = resolution.weaponNames
        .distinct()
        .joinToString { name ->
            val count = resolution.weaponNames.count { it == name }
            if (count == 1) name else "$name x$count"
        }

    private fun rollRows(): Int = ceil(resolution.dieResults.size / ROLLS_PER_ROW.toDouble()).toInt().coerceAtLeast(1)

    private fun contentHeight(): Double = 120.0 + rollRows() * DIE_HEIGHT + 128.0

    private companion object {
        const val ROLLS_PER_ROW = 8
        const val DIE_WIDTH = 56.0
        const val DIE_HEIGHT = 36.0
        const val DIE_GAP = 4.0
    }
}
