package io.hsar.mapgenerator.commandgui.korge.ui

import io.hsar.mapgenerator.commandgui.korge.interaction.MapPlacedObjectId
import io.hsar.mapgenerator.commandgui.korge.interaction.SensorDetection
import io.hsar.mapgenerator.commandgui.korge.interaction.SensorReportMode
import io.hsar.mapgenerator.commandgui.korge.interaction.SitrepContactState
import io.hsar.mapgenerator.commandgui.korge.interaction.SitrepLostContact
import io.hsar.mapgenerator.commandgui.korge.interaction.SitrepVisibilityReport
import io.hsar.mapgenerator.commandgui.korge.interaction.UnitAffiliation
import io.hsar.mapgenerator.commandgui.korge.interaction.KorgeUnitQueryAdapter
import io.hsar.mapgenerator.commandgui.korge.interaction.UnitSnapshot
import io.hsar.mapgenerator.commandgui.korge.interaction.displayName
import io.hsar.mapgenerator.commandgui.korge.interaction.unitClassLabel
import io.hsar.mapgenerator.grid.GridLayer
import io.hsar.mapgenerator.image.Palette
import korlibs.image.text.TextAlignment
import korlibs.korge.view.Container
import korlibs.korge.view.addUpdater
import korlibs.korge.view.container
import korlibs.korge.view.xy
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.concurrent.thread

internal object SitrepSectionLayout {
    fun rowsHeight(rowCount: Int, rowHeight: Double, rowGap: Double, emptyRowTopInset: Double): Double =
        if (rowCount <= 0) emptyRowTopInset + rowHeight
        else (rowCount * rowHeight) + ((rowCount - 1) * rowGap)

    fun sectionHeight(
        rowCount: Int,
        headerHeight: Double,
        headerToRowsGap: Double,
        rowHeight: Double,
        rowGap: Double,
        emptyRowTopInset: Double,
    ): Double =
        headerHeight + headerToRowsGap + rowsHeight(rowCount, rowHeight, rowGap, emptyRowTopInset)
}

internal class MapViewerSitrepDialog(
    private val root: Container,
    private val dialogManager: MapViewerDialogManager,
    private val dialogKey: Any,
    private val unitName: String,
    private var observer: UnitSnapshot,
    private var unitQueryService: KorgeUnitQueryAdapter,
    private val initialContactState: SitrepContactState = SitrepContactState(),
    private val previousContactState: SitrepContactState = SitrepContactState(),
    private val onContactStateChanged: (SitrepContactState) -> Unit = {},
    private val onObserverCustomPropertiesChanged: (UnitSnapshot, UnitSnapshot) -> Unit = { _, _ -> },
    private val ecmIntensityProvider: (UnitSnapshot) -> Int = { 0 },
    private val gridLayer: GridLayer? = null,
    private val onTransientNotice: (String) -> Unit = {},
    private val preferredWidth: Double = PREFERRED_WIDTH,
    private val preferredHeight: Double = PREFERRED_HEIGHT,
    private val restored: MapViewerPersistedDialog? = null,
) {
    private enum class ReportMode(
        val label: String,
        val emptySummaryText: String,
        val sensorReportMode: SensorReportMode,
    ) {
        SCIREP("SCIREP", "No hostile contacts to report.", SensorReportMode.SCIREP),
        NAVREP("NAVREP", "No allied contacts to report.", SensorReportMode.NAVREP),

        ;

        fun candidateFilter(observerAffiliation: UnitAffiliation?): (UnitSnapshot) -> Boolean =
            when (this) {
                NAVREP -> { candidate -> candidate.template.affiliation == observerAffiliation }
                SCIREP -> { _ -> true }
            }
    }

    private var window: MapViewerDialogWindow? = null
    private var reportMode = ReportMode.SCIREP
    private val sharedReportState by lazy { initialReportState(ReportMode.SCIREP) }
    private val reportLock = Any()
    private val pendingReports = ConcurrentLinkedQueue<PendingReport>()
    private var reportGeneration = 0L
    private var reportReady = false
    private var lastCalculatedContactState: SitrepContactState? = null
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
                    requestReportRefresh()
                }
            }
        },
        labelForValue = { it?.label ?: reportMode.label },
        menuWidth = MapViewerDropdownLayoutBuilder.minimumMenuWidth(ReportMode.entries.map { it.label }, hasIcons = false),
    )
    private var currentReport = emptyReport()

    init {
        root.addUpdater { consumePendingReport() }
    }

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
                MapViewerDialogAction("Copy", enabled = { reportReady }) { copyToClipboard() },
                MapViewerDialogAction("Dismiss") { close() },
            ),
            onClose = ::close,
            renderContent = ::renderContent,
            persistence = MapViewerDialogPersistence(
                type = "sitrep",
                targetId = observer.id.value.toString(),
                restored = restored,
            ),
            mode = MapViewerDialogMode.FLOATING,
            dialogManager = dialogManager,
        )
        dialogManager.register(
            owner = this,
            targetKey = dialogKey,
            blocksScreenPoint = { point -> window?.blocksScreenPoint(point) == true },
            sync = { payload ->
                (payload as? UnitSnapshot)?.let { updateObserver(it) }
            },
            persistedDialog = { window?.persistedDialog() },
            close = ::close,
        )
        window?.show(stage.views.actualVirtualWidth, stage.views.actualVirtualHeight)
        requestReportRefresh()
        return true
    }

    private fun renderContent(parent: Container, width: Double, contentViewportHeight: Double) {
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
        currentTop += sensorOptionsHeight() + SECTION_GAP
        renderReportSections(parent, width, currentTop)
    }

    private fun renderSensorOptions(parent: Container, width: Double, top: Double) {
        val optionWidth = (width - SENSOR_OPTION_SPLIT_GAP) / 2.0
        val firstOption = if (reportMode == ReportMode.SCIREP) {
            "Silent Running" to SILENT_RUNNING_KEY
        } else {
            "In Nebula" to IN_NEBULA_KEY
        }
        val secondOption = if (reportMode == ReportMode.SCIREP) {
            "Active Sensors" to ACTIVE_SENSORS_KEY
        } else {
            "In Warp" to ACTIVE_WARP_KEY
        }
        renderSensorOption(
            parent = parent,
            label = firstOption.first,
            checked = observer.sensorOptionEnabled(firstOption.second),
            y = top,
            left = 0.0,
            width = optionWidth,
            onToggle = { updateObserverSensorOption(firstOption.second) },
        )
        renderSensorOption(
            parent = parent,
            label = secondOption.first,
            checked = observer.sensorOptionEnabled(secondOption.second),
            y = top,
            left = optionWidth + SENSOR_OPTION_SPLIT_GAP,
            width = optionWidth,
            onToggle = { updateObserverSensorOption(secondOption.second) },
        )
        if (reportMode == ReportMode.NAVREP) {
            renderSensorOption(
                parent = parent,
                label = "Maximum Warp",
                checked = observer.sensorOptionEnabled(MAXIMUM_WARP_KEY),
                y = top + SENSOR_OPTION_ROW_HEIGHT + SENSOR_OPTION_ROW_GAP,
                left = 0.0,
                width = width,
                onToggle = { updateObserverSensorOption(MAXIMUM_WARP_KEY) },
            )
        }
    }

    private fun sensorOptionsHeight(): Double =
        if (reportMode == ReportMode.NAVREP) SENSOR_OPTION_ROW_HEIGHT * 2.0 + SENSOR_OPTION_ROW_GAP else SENSOR_OPTION_ROW_HEIGHT

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
        synchronized(reportLock) {
            val before = observer
            val enabled = !before.sensorOptionEnabled(key)
            val after = before.copy(
                customProperties = before.customProperties + when {
                    key == MAXIMUM_WARP_KEY && enabled -> mapOf(MAXIMUM_WARP_KEY to "true", ACTIVE_WARP_KEY to "true")
                    key == ACTIVE_WARP_KEY && !enabled && before.sensorOptionEnabled(MAXIMUM_WARP_KEY) ->
                        mapOf(ACTIVE_WARP_KEY to "false", MAXIMUM_WARP_KEY to "false")

                    else -> mapOf(key to enabled.toString())
                },
            )
            onObserverCustomPropertiesChanged(before, after)
            observer = after
            unitQueryService = unitQueryService.replacing(after)
        }
        requestReportRefresh()
    }

    private fun updateObserver(updatedObserver: UnitSnapshot) {
        synchronized(reportLock) {
            val previousNaturalReport = initialReportFor(reportMode)
            val updatedObserverSnapshot = if (updatedObserver.id == observer.id) updatedObserver else observer
            val updatedNaturalReport = SensorDetection.visibilityReport(
                observer = updatedObserverSnapshot,
                unitQueryService = unitQueryService.replacing(updatedObserver),
                gridLayer = gridLayer,
                includedIds = null,
                candidateFilter = reportMode.candidateFilter(updatedObserverSnapshot.template.affiliation),
                emptySummaryText = reportMode.emptySummaryText,
                sensorReportMode = reportMode.sensorReportMode,
                ecmIntensity = ecmIntensityProvider(updatedObserverSnapshot),
            )
            if (updatedObserver.id == observer.id) observer = updatedObserver
            unitQueryService = unitQueryService.replacing(updatedObserver)
            val naturalReport = initialReportFor(reportMode)
            val reportState = currentReportState()
            val previousDetectedIds = previousNaturalReport.trackableChecks().mapTo(mutableSetOf()) { it.unit.id }
            val updatedDetectedIds = updatedNaturalReport.trackableChecks().mapTo(mutableSetOf()) { it.unit.id }
            val previousIdentifiedIds = previousNaturalReport.visibleChecks.mapTo(mutableSetOf()) { it.unit.id }
            val updatedIdentifiedIds = updatedNaturalReport.visibleChecks.mapTo(mutableSetOf()) { it.unit.id }
            reportState.sensorContactIds.removeAll(previousDetectedIds - updatedDetectedIds)
            reportState.identifiedIds.removeAll(previousIdentifiedIds - updatedIdentifiedIds)
            reportState.sensorContactIds += naturalReport.trackableChecks().map { it.unit.id }
            reportState.identifiedIds += naturalReport.visibleChecks.map { it.unit.id }
        }
        requestReportRefresh()
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

        currentTop += rowsHeight(currentReport.visibleChecks.size)
        currentTop += CONTACT_SECTION_GAP

        renderUnidentifiedHeader(parent, width, currentTop)
        currentTop += HEADER_HEIGHT + HEADER_TO_ROWS_GAP

        if (currentReport.unidentifiedChecks.isEmpty()) {
            MapViewerTextViews.leftBox(
                parent = parent,
                text = if (reportMode == ReportMode.NAVREP) {
                    "Detected ships are shown in SCIREP."
                } else {
                    "No unidentified contacts remain."
                },
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

        currentTop += rowsHeight(currentReport.unidentifiedChecks.size)
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
        val reportTextHeight = MapViewerWrappedText.height(MapViewerWrappedText.wrap(currentReport.displayText, width, TEXT_SIZE).size)
        return MapViewerDialogSpacing.INPUT_HEIGHT +
                SECTION_GAP +
                sensorOptionsHeight() +
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
        synchronized(reportLock) {
            currentReportState().sensorContactIds.toggle(unitId)
        }
        requestReportRefresh()
    }

    private fun toggleIdentification(unitId: MapPlacedObjectId) {
        synchronized(reportLock) {
            currentReportState().identifiedIds.toggle(unitId)
        }
        requestReportRefresh()
    }

    private fun currentReportState(): UnitReportState = sharedReportState

    private fun ensureSierraContactNumber(unitId: MapPlacedObjectId) {
        if (unitId in sierraContactNumbers) return
        while (nextSierraContactNumber in usedSierraContactNumbers) nextSierraContactNumber++
        sierraContactNumbers[unitId] = nextSierraContactNumber
        usedSierraContactNumbers += nextSierraContactNumber++
    }

    private fun initialReportState(mode: ReportMode): UnitReportState =
        if (mode == ReportMode.SCIREP && initialContactState.sensorContactIds.isNotEmpty()) {
            val naturalReport = initialReportFor(mode)
            val sensorContactIds = initialContactState.sensorContactIds + naturalReport.trackableChecks().map { it.unit.id }
            UnitReportState(
                sensorContactIds = sensorContactIds.toMutableSet(),
                identifiedIds = (
                        initialContactState.identifiedIds + naturalReport.visibleChecks.map { it.unit.id }
                        ).intersect(sensorContactIds).toMutableSet(),
                lostContacts = initialContactState.lostContacts.toMutableList(),
            )
        } else {
            val naturalReport = initialReportFor(mode)
            naturalReport.trackableChecks().mapTo(mutableSetOf()) { it.unit.id }
                .let { initiallyDetectedIds ->
                    UnitReportState(
                        sensorContactIds = initiallyDetectedIds.toMutableSet(),
                        identifiedIds = naturalReport.visibleChecks.mapTo(mutableSetOf()) { it.unit.id },
                        lostContacts = mutableListOf(),
                    )
                }
        }

    private fun initialReportFor(mode: ReportMode): SitrepVisibilityReport =
        SensorDetection.visibilityReport(
            observer = observer,
            unitQueryService = unitQueryService,
            gridLayer = gridLayer,
            includedIds = null,
            candidateFilter = mode.candidateFilter(observer.template.affiliation),
            emptySummaryText = mode.emptySummaryText,
            sensorReportMode = mode.sensorReportMode,
            ecmIntensity = ecmIntensityProvider(observer),
        )

    private fun requestReportRefresh() {
        val generation = ++reportGeneration
        reportReady = false
        currentReport = emptyReport()
        window?.refresh()
        thread(name = "command-gui-sitrep-report", isDaemon = true) {
            val pending = synchronized(reportLock) {
                val report = buildReport(notifyContactState = false)
                PendingReport(generation, report, lastCalculatedContactState)
            }
            pendingReports += pending
        }
    }

    private fun consumePendingReport() {
        var pending: PendingReport? = null
        while (true) {
            val next = pendingReports.poll() ?: break
            if (next.generation == reportGeneration) {
                pending = next
            }
        }
        pending?.let { result ->
            currentReport = result.report
            reportReady = true
            result.contactState?.let(onContactStateChanged)
            window?.refresh()
        }
    }

    private fun emptyReport(): SitrepVisibilityReport =
        SitrepVisibilityReport(
            displayText = "Calculating SITREP...",
            copyText = "",
            visibleChecks = emptyList(),
            detectedChecks = emptyList(),
            unidentifiedChecks = emptyList(),
            hiddenChecks = emptyList(),
        )

    private fun SitrepVisibilityReport.trackableChecks() =
        (detectedChecks + ecmGhostChecks).distinctBy { it.unit.id }

    private fun buildReport(notifyContactState: Boolean = true): SitrepVisibilityReport {
        val reportState = currentReportState()
        val ecmIntensity = ecmIntensityProvider(observer)
        val assessment = SensorDetection.visibilityReport(
            observer = observer,
            unitQueryService = unitQueryService,
            gridLayer = gridLayer,
            includedIds = null,
            candidateFilter = reportMode.candidateFilter(observer.template.affiliation),
            emptySummaryText = reportMode.emptySummaryText,
            sensorReportMode = reportMode.sensorReportMode,
            ecmIntensity = ecmIntensity,
        )
        val destroyedContactIds = assessment.trackableChecks()
            .filter { it.unit.destroyed }
            .map { it.unit.id }
            .toSet()
        reportState.sensorContactIds += assessment.trackableChecks()
            .filterNot { it.unit.destroyed }
            .map { it.unit.id }
        reportState.sensorContactIds.removeAll(destroyedContactIds)
        reportState.identifiedIds.removeAll(destroyedContactIds)
        reportState.identifiedIds.removeAll(
            assessment.trackableChecks().filter { it.ecmContact != io.hsar.mapgenerator.commandgui.korge.interaction.EcmSensorContact.NONE }
                .map { it.unit.id },
        )
        val selectedIds = reportState.sensorContactIds union reportState.identifiedIds
        val identifiedIds = reportState.sensorContactIds intersect reportState.identifiedIds
        val detectedOnlyIds = selectedIds - identifiedIds
        reportState.sensorContactIds.addAll(detectedOnlyIds)
        reportState.identifiedIds.removeAll(detectedOnlyIds)
        val unidentifiedIds = reportState.sensorContactIds - identifiedIds
        val report = SensorDetection.visibilityReport(
            observer = observer,
            unitQueryService = unitQueryService,
            gridLayer = gridLayer,
            includedIds = identifiedIds,
            unidentifiedIds = unidentifiedIds,
            candidateFilter = reportMode.candidateFilter(observer.template.affiliation),
            emptySummaryText = reportMode.emptySummaryText,
            sensorReportMode = reportMode.sensorReportMode,
            ecmIntensity = ecmIntensity,
        )
        if (reportMode != ReportMode.SCIREP) {
            val currentChecksById = assessment.trackableChecks().associateBy { it.unit.id }
            val lostContacts = previousContactState.sensorContactIds
                .filter { it !in currentChecksById || currentChecksById.getValue(it).unit.destroyed }
                .mapNotNull { contactId ->
                    previousContactState.lastSeenContacts[contactId]
                        ?.let { lost ->
                            val currentCheck = currentChecksById[contactId]
                            lost.copy(
                                wasFriendly = lost.wasFriendly || currentCheck?.unit?.template?.affiliation?.let {
                                    it == observer.template.affiliation
                                } == true,
                                lostDueToEnemyAction = lost.wasIdentified &&
                                        (lost.wasFriendly || currentCheck?.unit?.template?.affiliation?.let {
                                            it == observer.template.affiliation
                                        } == true) &&
                                        currentCheck?.destroyedWithinIdentificationRange() == true,
                            )
                        }
                }
                .filter { it.wasIdentified && it.wasFriendly }
            report.trackableChecks().forEach { check ->
                ensureSierraContactNumber(check.unit.id)
            }
            val identificationText = SensorDetection.navrepIdentificationText(
                observer = observer,
                checks = report.visibleChecks,
                contactNumbers = sierraContactNumbers,
                mapWidth = unitQueryService.mapWidth,
                mapHeight = unitQueryService.mapHeight,
                gridLayer = gridLayer,
                previouslyDetectedIds = previousContactState.sensorContactIds,
                previouslyIdentifiedIds = previousContactState.identifiedIds,
            )
            val observerPositionText = SensorDetection.navrepObserverPositionText(
                observer,
                unitQueryService.mapWidth,
                unitQueryService.mapHeight,
                gridLayer,
            )
            val conditionsText = report.displayText.substringBefore(observerPositionText).trim()
            val lostContactText = lostContacts.joinToString("\n") { "${SensorDetection.lostContactText(it)}." }
            val reportText = if (report.visibleChecks.isEmpty() && lostContacts.isEmpty()) {
                report.displayText
            } else {
                listOf(conditionsText, observerPositionText).filter { it.isNotBlank() }.joinToString("\n")
            }
            return report.copy(
                unidentifiedChecks = emptyList(),
                displayText = listOf(reportText, identificationText, lostContactText)
                    .filter { it.isNotBlank() }
                    .joinToString("\n"),
                copyText = listOf(reportText, identificationText, lostContactText)
                    .filter { it.isNotBlank() }
                    .joinToString("\n"),
            )
        }

        val scirepReport = report.copy(
            visibleChecks = report.visibleChecks.filter {
                it.unit.template.affiliation != observer.template.affiliation
            },
            hiddenChecks = report.hiddenChecks.filter {
                it.unit.template.affiliation != observer.template.affiliation
            },
        )

        val currentChecksById = report.trackableChecks().associateBy { it.unit.id }
        val activeTrackableChecks = currentChecksById.values.filterNot { it.unit.destroyed }
        val physicallyDetectedIds = currentChecksById.values
            .filterNot { it.unit.destroyed }
            .mapTo(mutableSetOf()) { it.unit.id }
        val matchedLostContactIds = mutableSetOf<MapPlacedObjectId>()
        val lowConfidenceContacts = mutableMapOf<MapPlacedObjectId, SitrepLostContact>()
        activeTrackableChecks
            .filter { it.unit.id !in sierraContactNumbers }
            .forEach { check ->
                val matchingLost = (previousContactState.lostContacts + reportState.lostContacts)
                    .asSequence()
                    .filter { it.contactId !in matchedLostContactIds }
                    .filter { unitQueryService.distanceBetween(observer.mapCoordinate, it.lastSeenPosition) <= LOW_CONFIDENCE_DISTANCE_UNITS }
                    .minByOrNull { unitQueryService.distanceBetween(observer.mapCoordinate, it.lastSeenPosition) }
                if (matchingLost != null) {
                    sierraContactNumbers[check.unit.id] = matchingLost.sierraNumber
                    matchedLostContactIds += matchingLost.contactId
                    lowConfidenceContacts[check.unit.id] = matchingLost
                }
            }
        (
                activeTrackableChecks +
                        scirepReport.visibleChecks +
                        scirepReport.unidentifiedChecks
                ).distinctBy { it.unit.id }.forEach { check ->
            ensureSierraContactNumber(check.unit.id)
        }
        val newlyLost = previousContactState.sensorContactIds
            .filter { it !in physicallyDetectedIds }
            .mapNotNull { contactId ->
                previousContactState.lastSeenContacts[contactId]
                    ?.let { lost ->
                        val currentCheck = currentChecksById[contactId]
                        lost.copy(
                            wasFriendly = lost.wasFriendly || currentCheck?.unit?.template?.affiliation?.let {
                                it == observer.template.affiliation
                            } == true,
                            lostDueToEnemyAction = lost.wasIdentified &&
                                    (lost.wasFriendly || currentCheck?.unit?.template?.affiliation?.let {
                                        it == observer.template.affiliation
                                    } == true) &&
                                    currentCheck?.destroyedWithinIdentificationRange() == true,
                            destroyedByFriendlyAction = lost.wasIdentified &&
                                    !(lost.wasFriendly || currentCheck?.unit?.template?.affiliation?.let {
                                        it == observer.template.affiliation
                                    } == true) &&
                                    currentCheck?.destroyedWithinIdentificationRange() == true,
                        )
                    }
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
        val currentLastSeen = activeTrackableChecks
            .filter { it.unit.id in reportState.sensorContactIds }
            .associate { check ->
                check.unit.id to SitrepLostContact(
                    contactId = check.unit.id,
                    sierraNumber = sierraContactNumbers.getValue(check.unit.id),
                    lastSeenPosition = check.unit.mapCoordinate,
                    lastSeenRangeUnits = check.distanceUnits,
                    wasIdentified = check.unit.id in identifiedIds,
                    wasFriendly = observer.template.affiliation != null &&
                            check.unit.template.affiliation == observer.template.affiliation,
                    identifiedAffiliation = check.unit.lostContactAffiliationDescription(),
                    unitTypeName = check.unit.template.name,
                    unitClassCode = check.unit.unitClassLabel(),
                    unitInstanceName = check.unit.displayName().takeIf { it != check.unit.template.name },
                )
            }
        val calculatedContactState = SitrepContactState(
                sensorContactIds = reportState.sensorContactIds.toSet(),
                identifiedIds = reportState.identifiedIds.toSet(),
                sierraContactNumbers = sierraContactNumbers.toMap(),
                usedSierraContactNumbers = usedSierraContactNumbers.toSet(),
                lastSeenPositions = currentLastSeen.mapValues { it.value.lastSeenPosition },
                lastSeenContacts = currentLastSeen,
                lostContacts = historicalLost,
        )
        lastCalculatedContactState = calculatedContactState
        if (notifyContactState) onContactStateChanged(calculatedContactState)
        val scirepChecks = (scirepReport.visibleChecks + scirepReport.unidentifiedChecks)
            .sortedBy { sierraContactNumbers.getValue(it.unit.id) }
        val scirepText = SensorDetection.scirepText(
            checks = scirepChecks,
            identifiedIds = scirepReport.visibleChecks.map { it.unit.id }.toSet(),
            contactNumbers = sierraContactNumbers,
            mapWidth = unitQueryService.mapWidth,
            mapHeight = unitQueryService.mapHeight,
            gridLayer = gridLayer,
            emptySummaryText = reportMode.emptySummaryText,
            observerAffiliation = observer.template.affiliation,
            identificationAnnouncements = scirepReport.visibleChecks.filter { it.unit.id !in previousContactState.identifiedIds },
            previouslyDetectedIds = previousContactState.sensorContactIds,
            lostContacts = newlyLost.filter { it.contactId !in matchedLostContactIds && !(it.wasIdentified && it.wasFriendly) },
            lowConfidenceContacts = lowConfidenceContacts,
            sensorConditionsPoor = reportMode == ReportMode.SCIREP && observer.sensorOptionEnabled(IN_NEBULA_KEY),
            electronicWarfare = ecmIntensity > 0,
        )
        return scirepReport.copy(displayText = scirepText, copyText = scirepText)
    }

    private fun io.hsar.mapgenerator.commandgui.korge.interaction.SensorVisibilityCheck.destroyedWithinIdentificationRange(): Boolean =
        unit.destroyed && distanceUnits <= identificationRangeUnits

    private fun UnitSnapshot.lostContactAffiliationDescription(): String =
        when (template.affiliation) {
            UnitAffiliation.Friend -> if (observer.template.affiliation == UnitAffiliation.Hostile) "enemy" else "allied"
            UnitAffiliation.Hostile -> "enemy"
            UnitAffiliation.Neutral -> "neutral"
            UnitAffiliation.Unknown -> "unknown"
            null -> "unaffiliated"
        }

    private fun reportRowLabel(check: io.hsar.mapgenerator.commandgui.korge.interaction.SensorVisibilityCheck): String =
        SensorDetection.hiddenContactLabel(
            observer = observer,
            check = check,
            mapWidth = unitQueryService.mapWidth,
            mapHeight = unitQueryService.mapHeight,
            gridLayer = gridLayer,
        )

    private fun reportModeInputMinimumWidth(): Double =
        MapViewerDropdownLayoutBuilder.minimumMenuWidth(ReportMode.entries.map { it.label }, hasIcons = false)

    private fun title(): String =
        "${reportMode.label} - $unitName"

    private fun copyToClipboard() {
        if (!reportReady) return
        runCatching {
            Toolkit.getDefaultToolkit()
                .systemClipboard
                .setContents(StringSelection(currentReport.copyText), null)
            onTransientNotice("${reportMode.label} saved to clipboard.")
        }
    }

    private fun close() {
        reportGeneration++
        reportReady = false
        dialogManager.unregister(this)
        window?.close()
        window = null
        reportModeDropdown.close()
    }

    private fun sectionHeight(rowCount: Int): Double =
        SitrepSectionLayout.sectionHeight(
            rowCount,
            HEADER_HEIGHT,
            HEADER_TO_ROWS_GAP,
            ROW_HEIGHT,
            ROW_GAP,
            EMPTY_ROW_TOP_INSET,
        )

    private fun rowsHeight(rowCount: Int): Double =
        SitrepSectionLayout.rowsHeight(rowCount, ROW_HEIGHT, ROW_GAP, EMPTY_ROW_TOP_INSET)

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

    private data class PendingReport(
        val generation: Long,
        val report: SitrepVisibilityReport,
        val contactState: SitrepContactState?,
    )

    private companion object {
        const val SILENT_RUNNING_KEY = "silentRunning"
        const val ACTIVE_SENSORS_KEY = "activeSensors"
        const val IN_NEBULA_KEY = "inNebula"
        const val ACTIVE_WARP_KEY = "activeWarp"
        const val MAXIMUM_WARP_KEY = "maximumWarp"
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
        const val SENSOR_OPTION_ROW_GAP = 8.0
        const val SENSOR_OPTION_SPLIT_GAP = 8.0
        const val CHECKBOX_SIZE = 16.0
        const val CHECKBOX_GAP = 8.0
        const val CHECKBOX_LABEL_GAP = 12.0
        const val ROW_INSET = 8.0
        const val EMPTY_ROW_TOP_INSET = 2.0
        const val LOW_CONFIDENCE_DISTANCE_UNITS = 3.0
    }
}
