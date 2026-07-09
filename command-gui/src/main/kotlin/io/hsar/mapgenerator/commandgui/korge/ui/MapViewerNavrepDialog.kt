package io.hsar.mapgenerator.commandgui.korge.ui

import io.hsar.mapgenerator.commandgui.korge.interaction.MapPlacedObjectId
import io.hsar.mapgenerator.commandgui.korge.interaction.NavrepDetection
import io.hsar.mapgenerator.commandgui.korge.interaction.NavrepVisibilityReport
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
    private val title: String,
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
    private var window: MapViewerDialogWindow? = null
    private val forcedIncludedIds = mutableSetOf<MapPlacedObjectId>()
    private var currentReport = buildReport()

    fun open(): Boolean {
        if (dialogManager.hasTarget(dialogKey)) {
            return false
        }
        val stage = root.stage ?: return false
        window = MapViewerDialogWindow(
            root = root,
            title = title,
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
        val visibleText = MapViewerWrappedText.wrap(currentReport.displayText, width, TEXT_SIZE)
        var currentTop = 0.0
        val visibleTextHeight = MapViewerWrappedText.height(visibleText.size)
        MapViewerTextViews.box(
            parent = parent,
            text = visibleText.joinToString("\n"),
            width = width,
            height = visibleTextHeight,
            size = TEXT_SIZE,
            color = Palette.Colours.WHITE,
            alignment = TextAlignment.TOP_LEFT,
        )
        currentTop += visibleTextHeight + SECTION_GAP

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
            renderHiddenRow(
                parent = parent,
                check = check,
                top = currentTop + index * (ROW_HEIGHT + ROW_GAP),
                width = width,
            )
        }
    }

    private fun contentHeight(width: Double): Double {
        currentReport = buildReport()
        val visibleTextHeight = MapViewerWrappedText.height(
            MapViewerWrappedText.wrap(currentReport.displayText, width, TEXT_SIZE).size,
        )
        val hiddenSectionHeight = if (currentReport.hiddenChecks.isEmpty()) {
            ROW_HEIGHT
        } else {
            HEADER_HEIGHT + HEADER_TO_ROWS_GAP + (currentReport.hiddenChecks.size * ROW_HEIGHT) + ((currentReport.hiddenChecks.size - 1) * ROW_GAP)
        }
        return visibleTextHeight + SECTION_GAP + hiddenSectionHeight
    }

    private fun contentMinimumWidth(): Double {
        val visibleTextWidth = currentReport.displayText
            .lineSequence()
            .maxOfOrNull { line -> MapViewerTextViews.measure(line, size = TEXT_SIZE).width }
            ?: 0.0
        val hiddenTextWidth = currentReport.hiddenChecks
            .maxOfOrNull { check ->
                MapViewerTextViews.measure(hiddenMenuLabel(check), size = SMALL_TEXT_SIZE).width
            }
            ?: 0.0
        return maxOf(
            PREFERRED_MIN_CONTENT_WIDTH,
            visibleTextWidth,
            hiddenTextWidth + CHECKBOX_SIZE + CHECKBOX_LABEL_GAP,
        )
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

    private fun renderHiddenRow(parent: Container, check: io.hsar.mapgenerator.commandgui.korge.interaction.NavrepVisibilityCheck, top: Double, width: Double) {
        val included = isForcedIncluded(check)
        val row = parent.container()
        row.xy(0.0, top)
        MapViewerUiBoxes.box(row, width, ROW_HEIGHT, active = included)
        row.mouse {
            onClick {
                toggleForcedInclusion(check)
            }
        }

        val checkbox = row.container()
        checkbox.xy(ROW_INSET, (ROW_HEIGHT - CHECKBOX_SIZE) * 0.5)
        MapViewerUiBoxes.box(checkbox, CHECKBOX_SIZE, CHECKBOX_SIZE, active = included)
        if (included) {
            checkbox.solidRect(
                CHECKBOX_SIZE - CHECKBOX_MARK_INSET * 2.0,
                CHECKBOX_SIZE - CHECKBOX_MARK_INSET * 2.0,
                korlibs.image.color.RGBA(255, 255, 255, 255),
            )
                .xy(CHECKBOX_MARK_INSET, CHECKBOX_MARK_INSET)
        }

        MapViewerTextViews.leftBox(
            parent = row,
            text = hiddenMenuLabel(check),
            width = width - CHECKBOX_SIZE - CHECKBOX_LABEL_GAP - ROW_INSET * 2.0,
            height = ROW_HEIGHT,
            size = SMALL_TEXT_SIZE,
            color = Palette.Colours.WHITE,
        ) {
            x = CHECKBOX_SIZE + CHECKBOX_LABEL_GAP
        }
    }

    private fun toggleForcedInclusion(check: io.hsar.mapgenerator.commandgui.korge.interaction.NavrepVisibilityCheck) {
        if (!forcedIncludedIds.add(check.unit.id)) {
            forcedIncludedIds.remove(check.unit.id)
        }
        currentReport = buildReport()
        window?.refresh()
    }

    private fun isForcedIncluded(check: io.hsar.mapgenerator.commandgui.korge.interaction.NavrepVisibilityCheck): Boolean =
        check.unit.id in forcedIncludedIds

    private fun hiddenMenuLabel(check: io.hsar.mapgenerator.commandgui.korge.interaction.NavrepVisibilityCheck): String =
        NavrepDetection.hiddenContactLabel(
            observer = observer,
            check = check,
            mapWidth = mapWidth,
            mapHeight = mapHeight,
            gridLayer = gridLayer,
        )

    private fun buildReport(): NavrepVisibilityReport =
        NavrepDetection.visibilityReport(
            observer = observer,
            candidates = candidates,
            mapWidth = mapWidth,
            mapHeight = mapHeight,
            mapUnitsPerPixel = mapUnitsPerPixel,
            gridLayer = gridLayer,
            forcedIncludedIds = forcedIncludedIds,
        )

    private fun copyToClipboard() {
        runCatching {
            currentReport = buildReport()
            Toolkit.getDefaultToolkit()
                .systemClipboard
                .setContents(StringSelection(currentReport.copyText), null)
            onTransientNotice("NAVREP saved to clipboard.")
        }
    }

    private fun close() {
        dialogManager.unregister(this)
        window?.close()
        window = null
    }

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
        const val CHECKBOX_LABEL_GAP = 12.0
        const val CHECKBOX_MARK_INSET = 4.0
        const val ROW_INSET = 8.0
    }
}
