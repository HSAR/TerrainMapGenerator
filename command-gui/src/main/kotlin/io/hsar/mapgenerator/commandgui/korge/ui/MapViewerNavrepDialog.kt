package io.hsar.mapgenerator.commandgui.korge.ui

import io.hsar.mapgenerator.commandgui.korge.interaction.MapPlacedObjectId
import io.hsar.mapgenerator.commandgui.korge.interaction.NavrepDetection
import io.hsar.mapgenerator.commandgui.korge.interaction.NavrepVisibilityReport
import io.hsar.mapgenerator.commandgui.korge.interaction.UnitAffiliation
import io.hsar.mapgenerator.commandgui.korge.interaction.UnitSnapshot
import io.hsar.mapgenerator.grid.GridLayer
import io.hsar.mapgenerator.image.Palette
import korlibs.image.text.TextAlignment
import korlibs.korge.input.mouse
import korlibs.korge.view.Container
import korlibs.korge.view.container
import korlibs.korge.view.solidRect
import korlibs.korge.view.xy
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

internal class MapViewerNavrepDialog(
    private val root: Container,
    private val dialogManager: MapViewerDialogManager,
    private val dialogKey: Any,
    private val unitName: String,
    private val observer: UnitSnapshot,
    private val candidates: List<UnitSnapshot>,
    private val mapWidth: Int,
    private val mapHeight: Int,
    private val mapUnitsPerPixel: Double,
    private val gridLayer: GridLayer? = null,
    private val onTransientNotice: (String) -> Unit = {},
    private val preferredWidth: Double = PREFERRED_WIDTH,
    private val preferredHeight: Double = PREFERRED_HEIGHT,
) {
    private enum class ReportMode(val label: String, val emptySummaryText: String) {
        NAVREP("NAVREP", "No allied contacts to report."),
        SCIREP("SCIREP", "No hostile contacts to report."),

        ;

        fun candidateFilter(observerAffiliation: UnitAffiliation?): (UnitSnapshot) -> Boolean =
            when (this) {
                NAVREP -> { candidate -> candidate.template.affiliation == observerAffiliation }
                SCIREP -> { candidate -> candidate.template.affiliation != observerAffiliation }
            }
    }

    private var window: MapViewerDialogWindow? = null
    private var reportMode = ReportMode.NAVREP
    private val reportStatesByMode = mutableMapOf<ReportMode, UnitReportState>()
    private val reportModeDropdown = MapViewerDropdownMenu(
        root = root,
        options = ReportMode.entries.map { MapViewerDropdownOption(it, it.label) },
        onSelected = { selected ->
            selected?.let { nextMode ->
                if (reportMode != nextMode) {
                    reportMode = nextMode
                    currentReport = buildReport()
                    window?.refresh()
                }
            }
        },
        labelForValue = { it?.label ?: reportMode.label },
        menuWidth = MapViewerDropdownLayoutBuilder.minimumMenuWidth(ReportMode.entries.map { it.label }, hasIcons = false),
    )
    private var currentReport = buildReport()

    fun open(): Boolean {
        if (dialogManager.hasTarget(dialogKey)) {
            return false
        }
        val stage = root.stage ?: return false
        window = MapViewerDialogWindow(
            root = root,
            title = title(),
            titleProvider = { title() },
            preferredWidth = preferredWidth,
            preferredHeight = preferredHeight,
            contentMinimumWidth = ::contentMinimumWidth,
            contentHeight = ::contentHeight,
            actions = listOf(
                MapViewerDialogAction("Copy") { copyToClipboard() },
                MapViewerDialogAction("Dismiss") { close() },
            ),
            onClose = ::close,
            renderContent = ::renderContent,
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

    private fun renderContent(parent: Container, width: Double) {
        currentReport = buildReport()
        var currentTop = 0.0
        MapViewerInlineInputs.dropdown(
            parent = parent,
            label = "Report",
            dropdown = reportModeDropdown,
            value = reportMode,
            x = 0.0,
            y = currentTop,
            width = width,
            inputMinimumWidth = reportModeInputMinimumWidth(),
        )
        currentTop += MapViewerDialogSpacing.INPUT_HEIGHT + SECTION_GAP
        renderReportSections(parent, width, currentTop)
    }

    private fun renderReportSections(parent: Container, width: Double, top: Double) {
        var currentTop = top
        val reportText = MapViewerWrappedText.wrap(currentReport.displayText, width, TEXT_SIZE)
        val reportTextHeight = MapViewerWrappedText.height(reportText.size)
        MapViewerTextViews.box(
            parent = parent,
            text = reportText.joinToString("\n"),
            width = width,
            height = reportTextHeight,
            size = TEXT_SIZE,
            color = Palette.Colours.WHITE,
            alignment = TextAlignment.TOP_LEFT,
        ) {
            y = currentTop
        }
        currentTop += reportTextHeight + SECTION_GAP

        renderIdentifiedHeader(parent, width, currentTop)
        currentTop += HEADER_HEIGHT + HEADER_TO_ROWS_GAP

        if (currentReport.visibleChecks.isEmpty()) {
            MapViewerTextViews.leftBox(
                parent = parent,
                text = "No identified contacts remain.",
                width = width,
                height = ROW_HEIGHT,
                size = SMALL_TEXT_SIZE,
                color = Palette.Colours.LIGHT,
            ) {
                y = currentTop + 2.0
            }
        } else {
            currentReport.visibleChecks.forEachIndexed { index, check ->
                renderRow(
                    parent = parent,
                    check = check,
                    top = currentTop + index * (ROW_HEIGHT + ROW_GAP),
                    width = width,
                )
            }
        }

        currentTop += sectionHeight(currentReport.visibleChecks.size)
        currentTop += SECTION_GAP

        renderUnidentifiedHeader(parent, width, currentTop)
        currentTop += HEADER_HEIGHT + HEADER_TO_ROWS_GAP

        if (currentReport.unidentifiedChecks.isEmpty()) {
            MapViewerTextViews.leftBox(
                parent = parent,
                text = "No unidentified contacts remain.",
                width = width,
                height = ROW_HEIGHT,
                size = SMALL_TEXT_SIZE,
                color = Palette.Colours.LIGHT,
            ) {
                y = currentTop + 2.0
            }
        } else {
            currentReport.unidentifiedChecks.forEachIndexed { index, check ->
                renderRow(
                    parent = parent,
                    check = check,
                    top = currentTop + index * (ROW_HEIGHT + ROW_GAP),
                    width = width,
                )
            }
        }

        currentTop += sectionHeight(currentReport.unidentifiedChecks.size)
        currentTop += SECTION_GAP

        renderHiddenHeader(parent, width, currentTop)
        currentTop += HEADER_HEIGHT + HEADER_TO_ROWS_GAP

        if (currentReport.hiddenChecks.isEmpty()) {
            MapViewerTextViews.leftBox(
                parent = parent,
                text = "No hidden contacts remain.",
                width = width,
                height = ROW_HEIGHT,
                size = SMALL_TEXT_SIZE,
                color = Palette.Colours.LIGHT,
            ) {
                y = currentTop + 2.0
            }
            return
        }

        currentReport.hiddenChecks.forEachIndexed { index, check ->
            renderRow(
                parent = parent,
                check = check,
                top = currentTop + index * (ROW_HEIGHT + ROW_GAP),
                width = width,
            )
        }
    }

    private fun contentHeight(width: Double): Double {
        currentReport = buildReport()
        val reportTextHeight = MapViewerWrappedText.height(MapViewerWrappedText.wrap(currentReport.displayText, width, TEXT_SIZE).size)
        return MapViewerDialogSpacing.INPUT_HEIGHT +
                SECTION_GAP +
                reportTextHeight +
                SECTION_GAP +
                sectionHeight(currentReport.visibleChecks.size) +
                SECTION_GAP +
                sectionHeight(currentReport.unidentifiedChecks.size) +
                SECTION_GAP +
                sectionHeight(currentReport.hiddenChecks.size)
    }

    private fun contentMinimumWidth(): Double {
        val dropdownWidth = MapViewerInlineInputs.minimumWidth("Report", reportModeInputMinimumWidth())
        val visibleTextWidth = currentReport.displayText
            .lineSequence()
            .maxOfOrNull { line -> MapViewerTextViews.measure(line, size = TEXT_SIZE).width }
            ?: 0.0
        val identifiedTextWidth = currentReport.visibleChecks
            .maxOfOrNull { check -> MapViewerTextViews.measure(reportRowLabel(check), size = SMALL_TEXT_SIZE).width }
            ?: 0.0
        val hiddenTextWidth = currentReport.hiddenChecks
            .maxOfOrNull { check -> MapViewerTextViews.measure(reportRowLabel(check), size = SMALL_TEXT_SIZE).width }
            ?: 0.0
        val unidentifiedTextWidth = currentReport.unidentifiedChecks
            .maxOfOrNull { check -> MapViewerTextViews.measure(reportRowLabel(check), size = SMALL_TEXT_SIZE).width }
            ?: 0.0
        return maxOf(
            PREFERRED_MIN_CONTENT_WIDTH,
            dropdownWidth,
            visibleTextWidth,
            identifiedTextWidth + rowControlsWidth(),
            unidentifiedTextWidth + rowControlsWidth(),
            hiddenTextWidth + rowControlsWidth(),
        )
    }

    private fun renderIdentifiedHeader(parent: Container, width: Double, top: Double) {
        MapViewerTextViews.leftBox(
            parent = parent,
            text = "Identified (${currentReport.visibleChecks.size})",
            width = width,
            height = HEADER_HEIGHT,
            role = MapViewerFontRole.STRONG,
            size = HEADER_TEXT_SIZE,
            color = Palette.Colours.WHITE,
        ) {
            y = top
        }
    }

    private fun renderUnidentifiedHeader(parent: Container, width: Double, top: Double) {
        MapViewerTextViews.leftBox(
            parent = parent,
            text = "Detected (${currentReport.unidentifiedChecks.size})",
            width = width,
            height = HEADER_HEIGHT,
            role = MapViewerFontRole.STRONG,
            size = HEADER_TEXT_SIZE,
            color = Palette.Colours.WHITE,
        ) {
            y = top
        }
    }

    private fun renderHiddenHeader(parent: Container, width: Double, top: Double) {
        MapViewerTextViews.leftBox(
            parent = parent,
            text = "Hidden (${currentReport.hiddenChecks.size})",
            width = width,
            height = HEADER_HEIGHT,
            role = MapViewerFontRole.STRONG,
            size = HEADER_TEXT_SIZE,
            color = Palette.Colours.WHITE,
        ) {
            y = top
        }
    }

    private fun renderRow(
        parent: Container,
        check: io.hsar.mapgenerator.commandgui.korge.interaction.NavrepVisibilityCheck,
        top: Double,
        width: Double,
    ) {
        val state = currentReportState()
        val hasSensorContact = check.unit.id in state.sensorContactIds
        val isIdentified = check.unit.id in state.identifiedIds
        val row = parent.container()
        row.xy(0.0, top)
        MapViewerUiBoxes.box(row, width, ROW_HEIGHT, active = hasSensorContact || isIdentified)

        renderCheckbox(
            parent = row,
            x = ROW_INSET,
            checked = hasSensorContact,
            onToggle = { toggleSensorContact(check.unit.id) },
        )
        renderCheckbox(
            parent = row,
            x = ROW_INSET + CHECKBOX_SIZE + CHECKBOX_GAP,
            checked = isIdentified,
            onToggle = { toggleIdentification(check.unit.id) },
        )

        MapViewerTextViews.leftBox(
            parent = row,
            text = reportRowLabel(check),
            width = width - rowControlsWidth() - ROW_INSET,
            height = ROW_HEIGHT,
            size = SMALL_TEXT_SIZE,
            color = Palette.Colours.WHITE,
        ) {
            x = rowControlsWidth()
        }
    }

    private fun renderCheckbox(parent: Container, x: Double, checked: Boolean, onToggle: () -> Unit) {
        val checkbox = parent.container()
        checkbox.xy(x, (ROW_HEIGHT - CHECKBOX_SIZE) * 0.5)
        MapViewerUiBoxes.box(checkbox, CHECKBOX_SIZE, CHECKBOX_SIZE, active = checked)
        if (checked) {
            checkbox.solidRect(
                CHECKBOX_SIZE - CHECKBOX_MARK_INSET * 2.0,
                CHECKBOX_SIZE - CHECKBOX_MARK_INSET * 2.0,
                korlibs.image.color.RGBA(255, 255, 255, 255),
            ).xy(CHECKBOX_MARK_INSET, CHECKBOX_MARK_INSET)
        }
        checkbox.mouse {
            onClick {
                onToggle()
            }
        }
    }

    private fun toggleSensorContact(unitId: MapPlacedObjectId) {
        currentReportState().sensorContactIds.toggle(unitId)
        currentReport = buildReport()
        window?.refresh()
    }

    private fun toggleIdentification(unitId: MapPlacedObjectId) {
        currentReportState().identifiedIds.toggle(unitId)
        currentReport = buildReport()
        window?.refresh()
    }

    private fun currentReportState(): UnitReportState =
        reportStatesByMode.getOrPut(reportMode) { initialReportState(reportMode) }

    private fun initialReportState(mode: ReportMode): UnitReportState =
        initialReportFor(mode).detectedChecks
            .mapTo(mutableSetOf()) { it.unit.id }
            .let { initiallyDetectedIds ->
                UnitReportState(
                    sensorContactIds = initiallyDetectedIds.toMutableSet(),
                    identifiedIds = initialReportFor(mode).visibleChecks
                        .mapTo(mutableSetOf()) { it.unit.id },
                )
            }

    private fun initialReportFor(mode: ReportMode): NavrepVisibilityReport =
        NavrepDetection.visibilityReport(
            observer = observer,
            candidates = candidates,
            mapWidth = mapWidth,
            mapHeight = mapHeight,
            mapUnitsPerPixel = mapUnitsPerPixel,
            gridLayer = gridLayer,
            includedIds = null,
            candidateFilter = mode.candidateFilter(observer.template.affiliation),
            emptySummaryText = mode.emptySummaryText,
        )

    private fun buildReport(): NavrepVisibilityReport {
        val reportState = currentReportState()
        val detectedIds = initialReportFor(reportMode).detectedChecks.mapTo(mutableSetOf()) { it.unit.id }
        reportState.sensorContactIds.retainAll(detectedIds)
        reportState.identifiedIds.retainAll(detectedIds)
        val identifiedIds = reportState.sensorContactIds intersect reportState.identifiedIds
        val unidentifiedIds = (reportState.sensorContactIds union reportState.identifiedIds) - identifiedIds
        return NavrepDetection.visibilityReport(
            observer = observer,
            candidates = candidates,
            mapWidth = mapWidth,
            mapHeight = mapHeight,
            mapUnitsPerPixel = mapUnitsPerPixel,
            gridLayer = gridLayer,
            includedIds = identifiedIds,
            unidentifiedIds = unidentifiedIds,
            candidateFilter = reportMode.candidateFilter(observer.template.affiliation),
            emptySummaryText = reportMode.emptySummaryText,
        )
    }

    private fun reportRowLabel(check: io.hsar.mapgenerator.commandgui.korge.interaction.NavrepVisibilityCheck): String =
        NavrepDetection.hiddenContactLabel(
            observer = observer,
            check = check,
            mapWidth = mapWidth,
            mapHeight = mapHeight,
            gridLayer = gridLayer,
        )

    private fun reportModeInputMinimumWidth(): Double =
        MapViewerDropdownLayoutBuilder.minimumMenuWidth(ReportMode.entries.map { it.label }, hasIcons = false)

    private fun title(): String =
        "${reportMode.label} - $unitName"

    private fun copyToClipboard() {
        runCatching {
            currentReport = buildReport()
            Toolkit.getDefaultToolkit()
                .systemClipboard
                .setContents(StringSelection(currentReport.copyText), null)
            onTransientNotice("${reportMode.label} saved to clipboard.")
        }
    }

    private fun close() {
        dialogManager.unregister(this)
        window?.close()
        window = null
        reportModeDropdown.close()
    }

    private fun sectionHeight(rowCount: Int): Double =
        if (rowCount <= 0) {
            ROW_HEIGHT
        } else {
            HEADER_HEIGHT + HEADER_TO_ROWS_GAP + (rowCount * ROW_HEIGHT) + ((rowCount - 1) * ROW_GAP)
        }

    private fun rowControlsWidth(): Double =
        ROW_INSET + CHECKBOX_SIZE * 2.0 + CHECKBOX_GAP + CHECKBOX_LABEL_GAP

    private fun MutableSet<MapPlacedObjectId>.toggle(unitId: MapPlacedObjectId) {
        if (!add(unitId)) {
            remove(unitId)
        }
    }

    private data class UnitReportState(
        val sensorContactIds: MutableSet<MapPlacedObjectId>,
        val identifiedIds: MutableSet<MapPlacedObjectId>,
    )

    private companion object {
        const val PREFERRED_WIDTH = 900.0
        const val PREFERRED_HEIGHT = 680.0
        const val PREFERRED_MIN_CONTENT_WIDTH = 600.0
        const val TEXT_SIZE = 22.0
        const val SMALL_TEXT_SIZE = 18.0
        const val HEADER_TEXT_SIZE = 22.0
        const val HEADER_HEIGHT = 34.0
        const val SECTION_GAP = 20.0
        const val HEADER_TO_ROWS_GAP = 8.0
        const val ROW_HEIGHT = 34.0
        const val ROW_GAP = 8.0
        const val CHECKBOX_SIZE = 16.0
        const val CHECKBOX_GAP = 8.0
        const val CHECKBOX_LABEL_GAP = 12.0
        const val CHECKBOX_MARK_INSET = 4.0
        const val ROW_INSET = 8.0
    }
}
