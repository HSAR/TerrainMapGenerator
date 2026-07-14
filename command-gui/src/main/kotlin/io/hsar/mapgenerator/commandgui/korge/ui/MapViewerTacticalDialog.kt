package io.hsar.mapgenerator.commandgui.korge.ui

import io.hsar.mapgenerator.commandgui.korge.interaction.MapPlacedObjectId
import io.hsar.mapgenerator.commandgui.korge.interaction.TacticalFireResolution
import io.hsar.mapgenerator.commandgui.korge.interaction.TacticalTargetingReport
import io.hsar.mapgenerator.commandgui.korge.interaction.TacticalWeaponStatus
import io.hsar.mapgenerator.image.Palette
import korlibs.korge.view.Container
import korlibs.korge.view.container
import korlibs.korge.view.xy
import kotlin.math.roundToInt

internal class MapViewerTacticalDialog(
    private val root: Container,
    private val dialogManager: MapViewerDialogManager,
    private val dialogKey: Any,
    private val unitName: String,
    private val reportProvider: () -> TacticalTargetingReport?,
    private val onDismiss: () -> Unit = {},
    private val restoredBounds: MapViewerDialogBounds? = null,
    private val persistenceTargetId: String,
    private val focusTargetBoundsProvider: () -> List<ScreenBounds> = { emptyList() },
    private val onSolutionSelected: (MapPlacedObjectId?) -> Unit = {},
    private val onFire: (MapPlacedObjectId) -> TacticalFireResolution? = { null },
    private val onFireResolved: (TacticalFireResolution) -> Unit = {},
) {
    private var window: MapViewerDialogWindow? = null
    private var report = TacticalTargetingReport(emptyList(), emptyList())
    private var expandedTargetId: MapPlacedObjectId? = null

    fun open(): Boolean {
        if (dialogManager.hasTarget(dialogKey)) return false
        val stage = root.stage ?: return false
        report = reportProvider() ?: return false
        window = MapViewerDialogWindow(
            root = root,
            title = MapViewerDialogTitles.unit("Tactical", unitName),
            preferredWidth = PREFERRED_WIDTH,
            preferredHeight = PREFERRED_HEIGHT,
            contentMinimumWidth = { MINIMUM_CONTENT_WIDTH },
            contentHeight = { contentHeight() },
            actions = listOf(
                MapViewerDialogAction("Fire", enabled = { expandedTargetId != null }) {
                    expandedTargetId?.let(onFire)?.let { resolution ->
                        close()
                        onFireResolved(resolution)
                    }
                },
                MapViewerDialogAction("Cancel") { close() },
            ),
            onClose = ::close,
            renderContent = ::renderContent,
            focusTargetBoundsProvider = focusTargetBoundsProvider,
            persistence = MapViewerDialogPersistence(
                type = "tactical",
                targetId = persistenceTargetId,
                restored = restoredBounds?.let { bounds -> MapViewerPersistedDialog("tactical", persistenceTargetId, bounds) },
            ),
            mode = MapViewerDialogMode.FLOATING,
        )
        dialogManager.register(
            owner = this,
            targetKey = dialogKey,
            blocksScreenPoint = { point -> window?.blocksScreenPoint(point) == true },
            persistedDialog = { window?.persistedDialog() },
            close = ::close,
        )
        window?.show(stage.views.actualVirtualWidth, stage.views.actualVirtualHeight)
        return true
    }

    fun refresh(): Boolean {
        report = reportProvider() ?: run {
            close()
            return false
        }
        if (expandedTargetId != null && report.targets.none { it.targetId == expandedTargetId }) {
            expandedTargetId = null
            onSolutionSelected(null)
        }
        window?.refresh()
        return true
    }

    private fun renderContent(parent: Container, width: Double) {
        var top = renderSectionHeader(parent, "Ready Weapons", 0.0, width)
        val selectedWeaponKeys = expandedTargetId
            ?.let { targetId -> report.targets.firstOrNull { it.targetId == targetId } }
            ?.contributions
            ?.map { it.weaponKey }
            ?.toSet()
            .orEmpty()
        report.weapons.forEach { weapon ->
            val row = parent.container().xy(0.0, top)
            MapViewerUiBoxes.box(row, width, ROW_HEIGHT, active = weapon.key in selectedWeaponKeys && !weapon.destroyed && !weapon.fired)
            MapViewerFireArcSegments.render(
                row.container().xy(width - ARC_ICON_WIDTH - TEXT_INSET, 0.0),
                ARC_ICON_WIDTH,
                ROW_HEIGHT,
                weapon.arcs,
                MapViewerUiBoxes.toRgba(if (weapon.destroyed || weapon.fired) Palette.Colours.RED else Palette.Colours.WHITE)
            )
            val status = when {
                weapon.destroyed -> " — DESTROYED"
                weapon.fired -> " — FIRED"
                else -> ""
            }
            MapViewerTextViews.leftBox(
                row,
                weapon.displayName() + status,
                width - ARC_ICON_WIDTH - TEXT_INSET * 2.0,
                ROW_HEIGHT,
                color = if (weapon.destroyed || weapon.fired) Palette.Colours.RED else Palette.Colours.WHITE,
            ) { x = TEXT_INSET }
            top += ROW_HEIGHT + ROW_GAP
        }
        top += SECTION_GAP
        top = renderSectionHeader(parent, "Firing Solutions", top, width)
        if (report.targets.isEmpty()) {
            val emptyState = parent.container().xy(0.0, top)
            MapViewerTextViews.leftBox(emptyState, "No available targets", width, ROW_HEIGHT) { x = TEXT_INSET }
            return
        }
        report.targets.forEachIndexed { index, target ->
            val expanded = target.targetId == expandedTargetId
            val panel = parent.container().xy(0.0, top)
            MapViewerCollapsiblePanel.header(
                panel, width, ROW_HEIGHT,
                "${index + 1}. ${target.targetName} | ${formatRange(target.rangeU)}U | ${target.firepower} FP", expanded
            ) {
                expandedTargetId = if (expanded) null else target.targetId
                onSolutionSelected(expandedTargetId)
                window?.refresh()
            }
            top += ROW_HEIGHT
            if (expanded) target.contributions.forEach { contribution ->
                val detail = parent.container().xy(0.0, top)
                MapViewerFireArcSegments.render(
                    detail.container().xy(width - ARC_ICON_WIDTH - TEXT_INSET, 0.0),
                    ARC_ICON_WIDTH,
                    DETAIL_ROW_HEIGHT,
                    listOf(contribution.arc),
                    MapViewerUiBoxes.toRgba(Palette.Colours.WHITE)
                )
                MapViewerTextViews.leftBox(detail, "    ${contribution.weaponName}: ${contribution.firepower}", width - ARC_ICON_WIDTH - TEXT_INSET, DETAIL_ROW_HEIGHT) {
                    x = TEXT_INSET
                }
                top += DETAIL_ROW_HEIGHT
            }
            top += ROW_GAP
        }
    }

    private fun renderSectionHeader(parent: Container, text: String, top: Double, width: Double): Double {
        MapViewerTextViews.leftBox(parent, text, width, HEADER_HEIGHT, role = MapViewerFontRole.STRONG) { y = top }
        return top + HEADER_HEIGHT + HEADER_GAP
    }

    private fun contentHeight(): Double =
        HEADER_HEIGHT + HEADER_GAP + report.weapons.size * (ROW_HEIGHT + ROW_GAP) + SECTION_GAP +
                HEADER_HEIGHT + HEADER_GAP + if (report.targets.isEmpty()) {
            ROW_HEIGHT
        } else {
            report.targets.sumOf { ROW_HEIGHT + (if (it.targetId == expandedTargetId) it.contributions.size * DETAIL_ROW_HEIGHT else 0.0) + ROW_GAP }
        }

    private fun close() {
        if (window == null) return
        dialogManager.unregister(this)
        window?.close()
        window = null
        onDismiss()
    }

    private fun formatRange(range: Double): String =
        ((range * 10.0).roundToInt() / 10.0).toString().trimEnd('0').trimEnd('.')

    private fun TacticalWeaponStatus.displayName(): String =
        if (magazineId != null || salvoRack || submunition) {
            val rounds = magazineRemaining ?: 0
            "$name - $rounds ${if (rounds == 1) "Round" else "Rounds"} Remaining"
        } else name

    private companion object {
        const val PREFERRED_WIDTH = 720.0
        const val PREFERRED_HEIGHT = 620.0
        const val MINIMUM_CONTENT_WIDTH = 560.0
        const val HEADER_HEIGHT = 34.0
        const val HEADER_GAP = 8.0
        const val ROW_HEIGHT = 42.0
        const val DETAIL_ROW_HEIGHT = 32.0
        const val ROW_GAP = 6.0
        const val SECTION_GAP = 18.0
        const val TEXT_INSET = 12.0
        const val ARC_ICON_WIDTH = 42.0
    }
}
