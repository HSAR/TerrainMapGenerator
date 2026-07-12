package io.hsar.mapgenerator.commandgui.korge.ui

import io.hsar.mapgenerator.commandgui.korge.interaction.MapPlacedObjectId
import io.hsar.mapgenerator.commandgui.korge.interaction.SensorDetection
import io.hsar.mapgenerator.commandgui.korge.interaction.SitrepContactState
import io.hsar.mapgenerator.commandgui.korge.interaction.SitrepLostContact
import io.hsar.mapgenerator.commandgui.korge.interaction.SitrepVisibilityReport
import io.hsar.mapgenerator.commandgui.korge.interaction.UnitAffiliation
import io.hsar.mapgenerator.commandgui.korge.interaction.UnitSnapshot
import io.hsar.mapgenerator.commandgui.korge.interaction.unitClassLabel
import io.hsar.mapgenerator.grid.GridLayer
import io.hsar.mapgenerator.image.Palette
import korlibs.image.text.TextAlignment
import korlibs.korge.view.Container
import korlibs.korge.view.container
import korlibs.korge.view.xy
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import kotlin.math.hypot

internal class MapViewerSitrepDialog(
    private val root: Container,
    private val dialogManager: MapViewerDialogManager,
    private val dialogKey: Any,
    private val unitName: String,
    private var observer: UnitSnapshot,
    private var candidates: List<UnitSnapshot>,
    private val initialContactState: SitrepContactState = SitrepContactState(),
    private val previousContactState: SitrepContactState = SitrepContactState(),
    private val onContactStateChanged: (SitrepContactState) -> Unit = {},
    private val onObserverCustomPropertiesChanged: (UnitSnapshot, UnitSnapshot) -> Unit = { _, _ -> },
    private val mapWidth: Int,
    private val mapHeight: Int,
    private val mapUnitsPerPixel: Double,
    private val gridLayer: GridLayer? = null,
    private val onTransientNotice: (String) -> Unit = {},
    private val preferredWidth: Double = PREFERRED_WIDTH,
    private val preferredHeight: Double = PREFERRED_HEIGHT,
) {
    private enum class ReportMode(val label: String, val emptySummaryText: String) {
        SCIREP("SCIREP", "No hostile contacts to report."),
        NAVREP("NAVREP", "No allied contacts to report."),

        ;

        fun candidateFilter(observerAffiliation: UnitAffiliation?): (UnitSnapshot) -> Boolean =
            when (this) {
                NAVREP -> { candidate -> candidate.template.affiliation == observerAffiliation }
                SCIREP -> { candidate -> candidate.template.affiliation != observerAffiliation }
            }
    }

    private var window: MapViewerDialogWindow? = null
    private var reportMode = ReportMode.SCIREP
    private val reportStatesByMode = mutableMapOf<ReportMode, UnitReportState>()
    private val sierraContactNumbers = initialContactState.sierraContactNumbers.toMutableMap()
    private val usedSierraContactNumbers = (
            initialContactState.usedSierraContactNumbers + sierraContactNumbers.values
            ).toMutableSet()
    private var nextSierraContactNumber = (usedSierraContactNumbers.maxOrNull() ?: 0) + 1
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
            sync = { payload ->
                (payload as? UnitSnapshot)?.let { updateObserver(it) }
            },
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
        renderSensorOptions(parent, width, currentTop)
        currentTop += SENSOR_OPTIONS_HEIGHT + SECTION_GAP
        renderReportSections(parent, width, currentTop)
    }

    private fun renderSensorOptions(parent: Container, width: Double, top: Double) {
        val optionWidth = (width - SENSOR_OPTION_SPLIT_GAP) / 2.0
        renderSensorOption(
            parent = parent,
            label = "Silent Running",
            checked = observer.sensorOptionEnabled(SILENT_RUNNING_KEY),
            y = top,
            left = 0.0,
            width = optionWidth,
            onToggle = { updateObserverSensorOption(SILENT_RUNNING_KEY) },
        )
        renderSensorOption(
            parent = parent,
            label = "Active Sensors",
            checked = observer.sensorOptionEnabled(ACTIVE_SENSORS_KEY),
            y = top,
            left = optionWidth + SENSOR_OPTION_SPLIT_GAP,
            width = optionWidth,
            onToggle = { updateObserverSensorOption(ACTIVE_SENSORS_KEY) },
        )
    }

    private fun renderSensorOption(
        parent: Container,
        label: String,
        checked: Boolean,
        y: Double,
        left: Double,
        width: Double,
        onToggle: () -> Unit,
    ) {
        val row = parent.container().xy(left, y)
        MapViewerUiBoxes.box(row, width, SENSOR_OPTION_ROW_HEIGHT, active = checked)
        renderCheckbox(row, ROW_INSET, checked, onToggle)
        MapViewerTextViews.leftBox(
            parent = row,
            text = label,
            width = width - ROW_INSET - CHECKBOX_SIZE - CHECKBOX_LABEL_GAP,
            height = SENSOR_OPTION_ROW_HEIGHT,
            size = SMALL_TEXT_SIZE,
            color = Palette.Colours.WHITE,
        ) {
            x = ROW_INSET + CHECKBOX_SIZE + CHECKBOX_LABEL_GAP
        }
    }

    private fun updateObserverSensorOption(key: String) {
        val before = observer
        val enabled = !before.sensorOptionEnabled(key)
        val after = before.copy(customProperties = before.customProperties + (key to enabled.toString()))
        onObserverCustomPropertiesChanged(before, after)
        observer = after
        candidates = candidates.map { candidate -> if (candidate.id == after.id) after else candidate }
        currentReport = buildReport()
        window?.refresh()
    }

    private fun updateObserver(updatedObserver: UnitSnapshot) {
        val previousNaturalReport = initialReportFor(reportMode)
        val updatedCandidates = candidates.map { candidate ->
            if (candidate.id == updatedObserver.id) updatedObserver else candidate
        }
        val updatedObserverSnapshot = if (updatedObserver.id == observer.id) updatedObserver else observer
        val updatedNaturalReport = SensorDetection.visibilityReport(
            observer = updatedObserverSnapshot,
            candidates = updatedCandidates,
            mapWidth = mapWidth,
            mapHeight = mapHeight,
            mapUnitsPerPixel = mapUnitsPerPixel,
            gridLayer = gridLayer,
            includedIds = null,
            candidateFilter = reportMode.candidateFilter(updatedObserverSnapshot.template.affiliation),
            emptySummaryText = reportMode.emptySummaryText,
        )
        if (updatedObserver.id == observer.id) observer = updatedObserver
        candidates = updatedCandidates
        val naturalReport = initialReportFor(reportMode)
        val reportState = currentReportState()
        val previousDetectedIds = previousNaturalReport.detectedChecks.mapTo(mutableSetOf()) { it.unit.id }
        val updatedDetectedIds = updatedNaturalReport.detectedChecks.mapTo(mutableSetOf()) { it.unit.id }
        val previousIdentifiedIds = previousNaturalReport.visibleChecks.mapTo(mutableSetOf()) { it.unit.id }
        val updatedIdentifiedIds = updatedNaturalReport.visibleChecks.mapTo(mutableSetOf()) { it.unit.id }
        reportState.sensorContactIds.removeAll(previousDetectedIds - updatedDetectedIds)
        reportState.identifiedIds.removeAll(previousIdentifiedIds - updatedIdentifiedIds)
        reportState.sensorContactIds += naturalReport.detectedChecks.map { it.unit.id }
        reportState.identifiedIds += naturalReport.visibleChecks.map { it.unit.id }
        currentReport = buildReport()
        window?.refresh()
    }

    private fun UnitSnapshot.sensorOptionEnabled(key: String): Boolean =
        customProperties[key]?.equals("true", ignoreCase = true) == true

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
                y = currentTop + EMPTY_ROW_TOP_INSET
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
        currentTop += CONTACT_SECTION_GAP

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
                y = currentTop + EMPTY_ROW_TOP_INSET
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
        currentTop += CONTACT_SECTION_GAP

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
                y = currentTop + EMPTY_ROW_TOP_INSET
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
                SENSOR_OPTIONS_HEIGHT +
                SECTION_GAP +
                reportTextHeight +
                SECTION_GAP +
                sectionHeight(currentReport.visibleChecks.size) +
                CONTACT_SECTION_GAP +
                sectionHeight(currentReport.unidentifiedChecks.size) +
                CONTACT_SECTION_GAP +
                sectionHeight(currentReport.hiddenChecks.size)
    }

    private fun contentMinimumWidth(): Double {
        val dropdownWidth = MapViewerInlineInputs.minimumWidth("Report", reportModeInputMinimumWidth())
        val visibleTextWidth = MapViewerWrappedText.wrap(
            currentReport.displayText,
            PREFERRED_MIN_CONTENT_WIDTH,
            TEXT_SIZE,
        ).maxOfOrNull { line -> MapViewerTextViews.measure(line, size = TEXT_SIZE).width }
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
        check: io.hsar.mapgenerator.commandgui.korge.interaction.SensorVisibilityCheck,
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
        MapViewerCheckbox.render(
            parent = parent,
            x = x,
            y = (ROW_HEIGHT - CHECKBOX_SIZE) * 0.5,
            size = CHECKBOX_SIZE,
            checked = checked,
            onToggle = onToggle,
        )
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
        if (mode == ReportMode.SCIREP && initialContactState.sensorContactIds.isNotEmpty()) {
            val naturalReport = initialReportFor(mode)
            val sensorContactIds = initialContactState.sensorContactIds + naturalReport.detectedChecks.map { it.unit.id }
            UnitReportState(
                sensorContactIds = sensorContactIds.toMutableSet(),
                identifiedIds = (
                        initialContactState.identifiedIds + naturalReport.visibleChecks.map { it.unit.id }
                        ).intersect(sensorContactIds).toMutableSet(),
                lostContacts = initialContactState.lostContacts.toMutableList(),
            )
        } else {
            initialReportFor(mode).detectedChecks
                .mapTo(mutableSetOf()) { it.unit.id }
                .let { initiallyDetectedIds ->
                    UnitReportState(
                        sensorContactIds = initiallyDetectedIds.toMutableSet(),
                        identifiedIds = initialReportFor(mode).visibleChecks
                            .mapTo(mutableSetOf()) { it.unit.id },
                        lostContacts = mutableListOf(),
                    )
                }
        }

    private fun initialReportFor(mode: ReportMode): SitrepVisibilityReport =
        SensorDetection.visibilityReport(
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

    private fun buildReport(): SitrepVisibilityReport {
        val reportState = currentReportState()
        val selectedIds = reportState.sensorContactIds union reportState.identifiedIds
        val identifiedIds = reportState.sensorContactIds intersect reportState.identifiedIds
        val detectedOnlyIds = selectedIds - identifiedIds
        reportState.sensorContactIds.addAll(detectedOnlyIds)
        reportState.identifiedIds.removeAll(detectedOnlyIds)
        val unidentifiedIds = reportState.sensorContactIds - identifiedIds
        val report = SensorDetection.visibilityReport(
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
        if (reportMode != ReportMode.SCIREP) {
            return report
        }

        val physicallyDetectedIds = report.detectedChecks.mapTo(mutableSetOf()) { it.unit.id }
        val matchedLostContactIds = mutableSetOf<MapPlacedObjectId>()
        val lowConfidenceContacts = mutableMapOf<MapPlacedObjectId, SitrepLostContact>()
        report.detectedChecks
            .filter { it.unit.id !in sierraContactNumbers }
            .forEach { check ->
                val matchingLost = (previousContactState.lostContacts + reportState.lostContacts)
                    .asSequence()
                    .filter { it.contactId !in matchedLostContactIds }
                    .filter { distanceTo(it.lastSeenPosition) <= LOW_CONFIDENCE_DISTANCE_UNITS }
                    .minByOrNull { distanceTo(it.lastSeenPosition) }
                if (matchingLost != null) {
                    sierraContactNumbers[check.unit.id] = matchingLost.sierraNumber
                    matchedLostContactIds += matchingLost.contactId
                    lowConfidenceContacts[check.unit.id] = matchingLost
                }
            }
        (report.visibleChecks + report.unidentifiedChecks).forEach { check ->
            if (check.unit.id !in sierraContactNumbers) {
                while (nextSierraContactNumber in usedSierraContactNumbers) nextSierraContactNumber++
                sierraContactNumbers[check.unit.id] = nextSierraContactNumber
                usedSierraContactNumbers += nextSierraContactNumber++
            }
        }
        val newlyLost = previousContactState.sensorContactIds
            .filter { it !in physicallyDetectedIds }
            .mapNotNull { contactId ->
                previousContactState.lastSeenContacts[contactId]
                    ?: previousContactState.lastSeenPositions[contactId]?.let { position ->
                        previousContactState.sierraContactNumbers[contactId]?.let { number ->
                            SitrepLostContact(
                                contactId = contactId,
                                sierraNumber = number,
                                lastSeenPosition = position,
                                lastSeenRangeUnits = 0.0,
                                wasIdentified = contactId in previousContactState.identifiedIds,
                            )
                        }
                    }
            }
        val historicalLost = (previousContactState.lostContacts + reportState.lostContacts + newlyLost)
            .filterNot { it.contactId in matchedLostContactIds }
            .distinctBy { it.contactId }
        val currentLastSeen = report.detectedChecks
            .filter { it.unit.id in reportState.sensorContactIds }
            .associate { check ->
                check.unit.id to SitrepLostContact(
                    contactId = check.unit.id,
                    sierraNumber = sierraContactNumbers.getValue(check.unit.id),
                    lastSeenPosition = check.unit.mapCoordinate,
                    lastSeenRangeUnits = check.distanceUnits,
                    wasIdentified = check.unit.id in identifiedIds,
                    unitTypeName = check.unit.template.name,
                    unitClassCode = check.unit.unitClassLabel(),
                )
            }
        onContactStateChanged(
            SitrepContactState(
                sensorContactIds = reportState.sensorContactIds.toSet(),
                identifiedIds = reportState.identifiedIds.toSet(),
                sierraContactNumbers = sierraContactNumbers.toMap(),
                usedSierraContactNumbers = usedSierraContactNumbers.toSet(),
                lastSeenPositions = currentLastSeen.mapValues { it.value.lastSeenPosition },
                lastSeenContacts = currentLastSeen,
                lostContacts = historicalLost,
            ),
        )
        val scirepChecks = (report.visibleChecks + report.unidentifiedChecks)
            .sortedBy { sierraContactNumbers.getValue(it.unit.id) }
        val scirepText = SensorDetection.scirepText(
            checks = scirepChecks,
            identifiedIds = report.visibleChecks.map { it.unit.id }.toSet(),
            contactNumbers = sierraContactNumbers,
            mapWidth = mapWidth,
            mapHeight = mapHeight,
            gridLayer = gridLayer,
            emptySummaryText = reportMode.emptySummaryText,
            identificationAnnouncements = report.visibleChecks.filter { it.unit.id !in previousContactState.identifiedIds },
            lostContacts = newlyLost.filter { it.contactId !in matchedLostContactIds },
            lowConfidenceContacts = lowConfidenceContacts,
        )
        return report.copy(displayText = scirepText, copyText = scirepText)
    }

    private fun reportRowLabel(check: io.hsar.mapgenerator.commandgui.korge.interaction.SensorVisibilityCheck): String =
        SensorDetection.hiddenContactLabel(
            observer = observer,
            check = check,
            mapWidth = mapWidth,
            mapHeight = mapHeight,
            gridLayer = gridLayer,
        )

    private fun reportModeInputMinimumWidth(): Double =
        MapViewerDropdownLayoutBuilder.minimumMenuWidth(ReportMode.entries.map { it.label }, hasIcons = false)

    private fun distanceTo(position: io.hsar.mapgenerator.commandgui.korge.interaction.MapCoordinate): Double =
        hypot(
            (observer.mapCoordinate.x - position.x) * mapWidth,
            (observer.mapCoordinate.y - position.y) * mapHeight,
        ) * mapUnitsPerPixel

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
            HEADER_HEIGHT + HEADER_TO_ROWS_GAP + EMPTY_ROW_TOP_INSET + ROW_HEIGHT
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
        val lostContacts: MutableList<SitrepLostContact>,
    )

    private companion object {
        const val SILENT_RUNNING_KEY = "silentRunning"
        const val ACTIVE_SENSORS_KEY = "activeSensors"
        const val PREFERRED_WIDTH = 900.0
        const val PREFERRED_HEIGHT = 680.0
        const val PREFERRED_MIN_CONTENT_WIDTH = 600.0
        const val TEXT_SIZE = 22.0
        const val SMALL_TEXT_SIZE = 18.0
        const val HEADER_TEXT_SIZE = 22.0
        const val HEADER_HEIGHT = 34.0
        const val SECTION_GAP = 20.0
        const val CONTACT_SECTION_GAP = 8.0
        const val HEADER_TO_ROWS_GAP = 8.0
        const val ROW_HEIGHT = 34.0
        const val ROW_GAP = 8.0
        const val SENSOR_OPTION_ROW_HEIGHT = 34.0
        const val SENSOR_OPTION_SPLIT_GAP = 8.0
        const val SENSOR_OPTIONS_HEIGHT = SENSOR_OPTION_ROW_HEIGHT
        const val CHECKBOX_SIZE = 16.0
        const val CHECKBOX_GAP = 8.0
        const val CHECKBOX_LABEL_GAP = 12.0
        const val ROW_INSET = 8.0
        const val EMPTY_ROW_TOP_INSET = 2.0
        const val LOW_CONFIDENCE_DISTANCE_UNITS = 3.0
    }
}
