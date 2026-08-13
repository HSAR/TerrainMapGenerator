package io.hsar.mapgenerator.commandgui.korge.ui

import io.hsar.mapgenerator.commandgui.operation.OperationEcmRules
import io.hsar.mapgenerator.commandgui.korge.interaction.MapPlacedObjectId
import io.hsar.mapgenerator.commandgui.korge.interaction.TacticalFireResolution
import io.hsar.mapgenerator.commandgui.korge.interaction.TacticalTarget
import io.hsar.mapgenerator.commandgui.korge.interaction.TacticalTargetingReport
import io.hsar.mapgenerator.commandgui.korge.interaction.TacticalWeaponContribution
import io.hsar.mapgenerator.commandgui.korge.interaction.TacticalWeaponStatus
import io.hsar.mapgenerator.image.Palette
import korlibs.korge.input.mouse
import korlibs.korge.view.Container
import korlibs.korge.view.container
import korlibs.korge.view.xy
import kotlin.math.roundToInt

internal class MapViewerTacticalDialog(
    private val root: Container,
    private val dialogManager: MapViewerDialogManager,
    private val dialogKey: Any,
    private var unitName: String,
    private val reportProvider: () -> TacticalTargetingReport?,
    private val dialogName: String = "Tactical",
    private val fireActionLabel: String = "Fire",
    private val onDismiss: () -> Unit = {},
    private val restored: MapViewerPersistedDialog? = null,
    private val persistenceTargetId: String,
    private val focusTargetBoundsProvider: () -> List<ScreenBounds> = { emptyList() },
    private val onSolutionSelected: (MapPlacedObjectId?) -> Unit = {},
    private val onSelectionChanged: (TacticalTarget?, Set<String>) -> Unit = { _, _ -> },
    private val contributionArc: (TacticalTarget, TacticalWeaponContribution) -> String = { _, contribution -> contribution.arc },
    private val canFire: (TacticalTarget, Set<String>) -> Boolean = { _, keys -> keys.isNotEmpty() },
    private val onFire: (MapPlacedObjectId, Set<String>) -> TacticalFireResolution? = { _, _ -> null },
    private val onFireResolved: (TacticalFireResolution) -> Unit = {},
) {
    private var window: MapViewerDialogWindow? = null
    private var report = TacticalTargetingReport(emptyList(), emptyList())
    private var expandedTargetId: MapPlacedObjectId? = null
    private var selectedWeaponKeys = emptySet<String>()

    fun open(): Boolean {
        if (dialogManager.hasTarget(dialogKey)) return false
        val stage = root.stage ?: return false
        report = reportProvider() ?: return false
        if (report.targets.size == 1) {
            expandedTargetId = report.targets.single().targetId
            selectedWeaponKeys = report.targets.single().contributions.mapTo(linkedSetOf(), TacticalWeaponContribution::weaponKey)
        }
        notifySelectionChanged()
        window = MapViewerDialogWindow(
            root = root,
            title = MapViewerDialogTitles.unit(dialogName, unitName),
            preferredWidth = PREFERRED_WIDTH,
            preferredHeight = PREFERRED_HEIGHT,
            contentMinimumWidth = { MINIMUM_CONTENT_WIDTH },
            contentHeight = { contentHeight() },
            actions = listOf(
                MapViewerDialogAction(fireActionLabel, enabled = {
                    expandedTargetId?.let { id -> report.targets.firstOrNull { it.targetId == id } }
                        ?.let { canFire(it, selectedWeaponKeys) } == true
                }) {
                    expandedTargetId?.let { targetId -> onFire(targetId, selectedWeaponKeys) }?.let { resolution ->
                        close()
                        if (!resolution.interceptPending) onFireResolved(resolution)
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
                restored = restored,
            ),
            mode = MapViewerDialogMode.FLOATING,
            dialogManager = dialogManager,
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
            selectedWeaponKeys = emptySet()
            onSolutionSelected(null)
        }
        expandedTargetId?.let { targetId ->
            val available = report.targets.firstOrNull { it.targetId == targetId }?.contributions?.mapTo(linkedSetOf(), TacticalWeaponContribution::weaponKey).orEmpty()
            selectedWeaponKeys = selectedWeaponKeys.intersect(available)
        }
        notifySelectionChanged()
        window?.refresh()
        return true
    }

    fun refreshPresentation() {
        window?.refresh()
    }

    fun refreshUnitName(name: String) {
        if (unitName == name) return
        unitName = name
        window?.refreshTitle()
    }

    private fun renderContent(parent: Container, width: Double, contentViewportHeight: Double) {
        var top = renderSectionHeader(parent, "Ready Weapons", 0.0, width)
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
            weapon.key.takeIf { it in selectableWeaponKeys() && !weapon.destroyed && !weapon.fired }?.let { weaponKey ->
                row.mouse {
                    onClick {
                        selectedWeaponKeys = if (weaponKey in selectedWeaponKeys) selectedWeaponKeys - weaponKey else selectedWeaponKeys + weaponKey
                        notifySelectionChanged()
                        window?.refresh()
                    }
                }
            }
            top += ROW_HEIGHT + ROW_GAP
        }
        top += SECTION_GAP
        top = renderSectionHeader(parent, "Firing Solutions - ${(report.fireControlCapacity - report.fireControlUsed).coerceAtLeast(0)} Remaining", top, width)
        if (report.targets.isEmpty()) {
            val emptyState = parent.container().xy(0.0, top)
            MapViewerTextViews.leftBox(emptyState, "No available targets", width, ROW_HEIGHT) { x = TEXT_INSET }
            return
        }
        report.targets.forEach { target ->
            val expanded = target.targetId == expandedTargetId
            val panel = parent.container().xy(0.0, top)
            MapViewerCollapsiblePanel.header(
                panel, width, ROW_HEIGHT,
                if (target.manualEcm) "${target.shipClassPrefix()}${target.targetName} | ECM" else
                    "${target.shipClassPrefix()}${target.targetName} | ${formatRange(target.rangeU)}U | ${target.firepower} FP",
                expanded,
            ) {
                expandedTargetId = if (expanded) null else target.targetId
                selectedWeaponKeys = if (expanded) emptySet() else target.contributions.mapTo(linkedSetOf(), TacticalWeaponContribution::weaponKey)
                onSolutionSelected(expandedTargetId)
                notifySelectionChanged()
                window?.refresh()
            }
            top += ROW_HEIGHT
            if (expanded) target.contributions.forEach { contribution ->
                val detail = parent.container().xy(0.0, top)
                MapViewerFireArcSegments.render(
                    detail.container().xy(width - ARC_ICON_WIDTH - TEXT_INSET, 0.0),
                    ARC_ICON_WIDTH,
                    DETAIL_ROW_HEIGHT,
                    listOf(contributionArc(target, contribution)),
                    MapViewerUiBoxes.toRgba(Palette.Colours.WHITE)
                )
                val contributionText = report.weapons.firstOrNull { it.key == contribution.weaponKey }
                    ?.takeIf(TacticalWeaponStatus::ecm)
                    ?.let { "    ${contribution.weaponName}: ${OperationEcmRules.CAPABILITY_PER_MODULE.roundToInt()} U°" }
                    ?: "    ${contribution.weaponName}: ${contribution.firepower}"
                MapViewerTextViews.leftBox(detail, contributionText, width - ARC_ICON_WIDTH - TEXT_INSET, DETAIL_ROW_HEIGHT) {
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
        onSelectionChanged(null, emptySet())
        onDismiss()
    }

    private fun formatRange(range: Double): String =
        ((range * 10.0).roundToInt() / 10.0).toString().trimEnd('0').trimEnd('.')

    private fun TacticalWeaponStatus.displayName(): String =
        if (magazineId != null || salvoRack || submunition) {
            val rounds = magazineRemaining ?: 0
            "$name - $rounds ${if (rounds == 1) "Round" else "Rounds"} Remaining"
        } else name

    private fun TacticalTarget.shipClassPrefix(): String = shipClass?.let { "$it " }.orEmpty()

    private fun selectableWeaponKeys(): Set<String> = expandedTargetId
        ?.let { targetId -> report.targets.firstOrNull { it.targetId == targetId } }
        ?.contributions
        ?.mapTo(linkedSetOf(), TacticalWeaponContribution::weaponKey)
        .orEmpty()

    private fun notifySelectionChanged() {
        onSelectionChanged(
            expandedTargetId?.let { targetId -> report.targets.firstOrNull { it.targetId == targetId } },
            selectedWeaponKeys,
        )
    }

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
