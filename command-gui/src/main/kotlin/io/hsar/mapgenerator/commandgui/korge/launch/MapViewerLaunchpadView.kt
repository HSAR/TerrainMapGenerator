package io.hsar.mapgenerator.commandgui.korge.launch

import io.hsar.mapgenerator.commandgui.korge.ui.MapViewerFontRole
import io.hsar.mapgenerator.commandgui.korge.ui.MapViewerScrollState
import io.hsar.mapgenerator.commandgui.korge.ui.MapViewerScrollbar
import io.hsar.mapgenerator.commandgui.korge.ui.MapViewerTextViews
import io.hsar.mapgenerator.commandgui.korge.ui.MapViewerThumbnailViews
import io.hsar.mapgenerator.commandgui.korge.ui.MapViewerUiBoxes
import io.hsar.mapgenerator.commandgui.korge.ui.ScreenBounds
import io.hsar.mapgenerator.commandgui.save.SavedOperationSummary
import io.hsar.mapgenerator.image.Palette
import korlibs.event.MouseButton
import korlibs.korge.input.mouse
import korlibs.korge.view.Container
import korlibs.korge.view.addUpdater
import korlibs.korge.view.container
import korlibs.korge.view.onStageResized
import korlibs.korge.view.xy
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

internal class MapViewerLaunchpadView(
    private val root: Container,
    viewportWidth: Int,
    viewportHeight: Int,
    savedOperations: List<SavedOperationSummary>? = null,
    private val onCreateNewOperation: () -> Unit,
    private val onLoadSavedOperation: (SavedOperationSummary) -> Unit,
    private val onOpenSavePicker: () -> Unit,
    private val onExitProgram: () -> Unit,
    private val onOngoingOperationsEntered: () -> Unit = {},
    private val onTitleChanged: (String) -> Unit = {},
) {
    private val layer = root.container()
    private var layout = MapViewerLaunchpadLayoutBuilder.build(viewportWidth, viewportHeight)
    private var mode = LaunchpadMode.HOME
    private var scrollState = MapViewerScrollState(0.0, 0.0)
    private var scrollDragState: ScrollDragState? = null
    private var savedOperations = savedOperations

    init {
        layer.zIndex = Z_INDEX
        layer.onStageResized { width, height ->
            render(width, height)
        }
        layer.addUpdater {
            if (mode == LaunchpadMode.EXISTING_OPERATIONS) {
                val pointer = layer.stage?.views?.input?.mousePos ?: return@addUpdater
                val leftHeld = layer.stage?.views?.input?.mouseButtonPressed(MouseButton.LEFT) ?: false
                if (!leftHeld) {
                    scrollDragState = null
                } else {
                    updateScrollDrag(pointer)
                }
            } else {
                scrollDragState = null
            }
        }
        layer.mouse {
            onScrollAnywhere {
                if (mode == LaunchpadMode.EXISTING_OPERATIONS && layout.savePanelBounds.contains(views.input.mousePos)) {
                    if (scrollState.needsScrolling) {
                        scrollState = scrollState.scrolledBy(scrollDeltaYLines * MapViewerScrollbar.SCROLL_STEP_PX)
                        render(
                            layer.stage?.views?.gameWindow?.width ?: layout.viewportWidth.toInt(),
                            layer.stage?.views?.gameWindow?.height ?: layout.viewportHeight.toInt(),
                        )
                    }
                }
            }
        }
        render(viewportWidth, viewportHeight)
    }

    fun close() {
        layer.removeFromParent()
    }

    private fun render(viewportWidth: Int, viewportHeight: Int) {
        layout = MapViewerLaunchpadLayoutBuilder.build(viewportWidth, viewportHeight)
        scrollState = scrollState.withContent(layout.saveListBounds.height, totalContentHeight())
        layer.removeChildren()
        when (mode) {
            LaunchpadMode.HOME -> renderHome(
                MapViewerLaunchpadTemplate.renderScreenShell(layer, layout.homePanelBounds, screenTitle()),
            )

            LaunchpadMode.EXISTING_OPERATIONS -> renderExistingOperations(
                MapViewerLaunchpadTemplate.renderScreenShell(layer, layout.savePanelBounds, screenTitle(), onBack = {
                    mode = LaunchpadMode.HOME
                    scrollState = scrollState.copy(offset = 0.0)
                    rerender()
                }),
            )
        }
        onTitleChanged(screenTitle())
    }

    private fun renderHome(panel: Container) {
        val bounds = layout.homePanelBounds
        MapViewerTextViews.box(panel, "Welcome, Commander.", bounds.width, 28.0)
            .xy(0.0, 62.0)

        val buttonLeft = (bounds.width - BUTTON_WIDTH) * 0.5
        MapViewerLaunchUi.button(panel, BUTTON_WIDTH, BUTTON_HEIGHT, "New Operation", uppercase = true, onClick = onCreateNewOperation)
            .xy(buttonLeft, 126.0)
        MapViewerLaunchUi.button(
            parent = panel,
            width = BUTTON_WIDTH,
            height = BUTTON_HEIGHT,
            label = "Ongoing Operations",
            uppercase = true,
            onClick = {
                mode = LaunchpadMode.EXISTING_OPERATIONS
                savedOperations = null
                scrollState = scrollState.copy(offset = 0.0)
                rerender()
                onOngoingOperationsEntered()
            },
        ).xy(buttonLeft, 188.0)
        MapViewerLaunchUi.button(panel, BUTTON_WIDTH, BUTTON_HEIGHT, "Exit", uppercase = true, onClick = onExitProgram)
            .xy(buttonLeft, 250.0)
    }

    fun updateSavedOperations(summaries: List<SavedOperationSummary>) {
        savedOperations = summaries
        scrollState = scrollState.copy(offset = 0.0)
        if (mode == LaunchpadMode.EXISTING_OPERATIONS) {
            rerender()
        }
    }

    private fun renderExistingOperations(panel: Container) {
        val bounds = layout.savePanelBounds
        val listBounds = layout.saveListBounds

        val content = panel.container().also {
            it.xy(listBounds.left + 12.0, listBounds.top + 12.0 - scrollState.offset)
        }
        val operations = savedOperations
        if (operations == null) {
            MapViewerLaunchUi.label(content, "Loading saved operations...", color = Palette.Colours.LIGHT)
                .xy(12.0, 12.0)
        } else if (operations.isEmpty()) {
            MapViewerLaunchUi.label(content, "No valid saves found in the default save directory.", color = Palette.Colours.LIGHT)
                .xy(12.0, 12.0)
        } else {
            operations.forEachIndexed { index, summary ->
                renderSaveRow(content, summary, index)
            }
        }
        MapViewerScrollbar.render(panel, layout.scrollbarBounds, scrollState) { pointerY ->
            scrollDragState = ScrollDragState(pointerY, scrollState.offset)
        }

        MapViewerLaunchUi.button(
            parent = panel,
            width = 280.0,
            height = BUTTON_HEIGHT,
            label = "Browse For Save File",
            uppercase = true,
            onClick = onOpenSavePicker,
        ).xy((bounds.width - 280.0) * 0.5, bounds.height - BUTTON_HEIGHT - 24.0)

    }

    private fun renderSaveRow(parent: Container, summary: SavedOperationSummary, index: Int) {
        val rowTop = index * SAVE_ROW_HEIGHT
        val rowWidth = layout.saveRowWidth
        val rowHeight = SAVE_ROW_HEIGHT - 12.0
        val row = parent.container()
        row.xy(0.0, rowTop)
        MapViewerUiBoxes.box(row, rowWidth, rowHeight)
        row.mouse {
            onClick { onLoadSavedOperation(summary) }
        }

        val borderInset = 4.0
        val previewFrameHeight = rowHeight - (borderInset * 2.0)
        val previewFrameWidth = previewFrameHeight * 16.0 / 9.0
        val previewFrameLeft = borderInset
        val previewFrameTop = borderInset
        MapViewerThumbnailViews.render(row, summary.thumbnail, previewFrameLeft, previewFrameTop, previewFrameWidth, previewFrameHeight)

        val textLeft = previewFrameLeft + previewFrameWidth + 18.0
        MapViewerTextViews.leftBox(
            parent = row,
            text = summary.operationName,
            width = layout.savePanelBounds.width - textLeft,
            height = 28.0,
            role = MapViewerFontRole.STRONG,
            color = Palette.Colours.WHITE,
        ).xy(textLeft, 12.0)
        MapViewerTextViews.leftBox(
            parent = row,
            text = formatTimestamp(summary.updatedAt),
            width = layout.savePanelBounds.width - textLeft,
            height = 28.0,
            color = Palette.Colours.LIGHT,
        ).xy(textLeft, 44.0)
    }

    private fun totalContentHeight(): Double =
        (savedOperations?.size ?: 0) * SAVE_ROW_HEIGHT

    private fun updateScrollDrag(pointer: korlibs.math.geom.Vector2D) {
        if (!scrollState.needsScrolling) {
            return
        }

        val state = scrollDragState
        if (state == null) {
            return
        }

        scrollState = scrollState.withOffset(
            MapViewerScrollbar.dragOffset(
                bounds = layout.scrollbarBounds,
                state = scrollState,
                dragStartPointerY = state.startPointerY,
                dragStartOffset = state.startOffset,
                currentPointerY = pointer.y,
            ),
        )
        rerender()
    }

    private fun rerender() {
        render(
            layer.stage?.views?.gameWindow?.width ?: layout.viewportWidth.toInt(),
            layer.stage?.views?.gameWindow?.height ?: layout.viewportHeight.toInt(),
        )
    }

    private fun screenTitle(): String =
        when (mode) {
            LaunchpadMode.HOME -> "Launchpad"
            LaunchpadMode.EXISTING_OPERATIONS -> "Ongoing Operations"
        }

    private fun formatTimestamp(instant: java.time.Instant): String =
        TIMESTAMP_FORMATTER.format(instant.atZone(ZoneId.systemDefault()))

    private companion object {
        const val Z_INDEX = 60_000.0
        const val BUTTON_WIDTH = 300.0
        const val BUTTON_HEIGHT = 44.0
        const val SAVE_ROW_HEIGHT = 108.0
        val TIMESTAMP_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm")
    }
}

private data class ScrollDragState(
    val startPointerY: Double,
    val startOffset: Double,
)

private enum class LaunchpadMode {
    HOME,
    EXISTING_OPERATIONS,
}

internal data class MapViewerLaunchpadLayout(
    val headerBounds: ScreenBounds,
    val homePanelBounds: ScreenBounds,
    val savePanelBounds: ScreenBounds,
    val saveListBounds: ScreenBounds,
    val scrollbarBounds: ScreenBounds,
    val saveRowWidth: Double,
    val viewportWidth: Double,
    val viewportHeight: Double,
)

internal object MapViewerLaunchpadLayoutBuilder {
    fun build(viewportWidth: Int, viewportHeight: Int): MapViewerLaunchpadLayout {
        val homePanelWidth = 420.0
        val homePanelHeight = 320.0
        val savePanelWidth = 760.0
        val savePanelHeight = (viewportHeight * 0.78).roundToInt().toDouble().coerceAtLeast(520.0)
        val saveListTop = 110.0
        val saveListBottomPadding = 88.0
        val saveListLeft = 24.0
        val scrollbarWidth = MapViewerScrollbar.WIDTH
        val saveListWidth = savePanelWidth - saveListLeft * 2.0 - scrollbarWidth - 12.0
        val saveListHeight = savePanelHeight - saveListTop - saveListBottomPadding - 24.0
        return MapViewerLaunchpadLayout(
            headerBounds = MapViewerLaunchShellLayoutBuilder.build(viewportWidth).headerBounds,
            homePanelBounds = ScreenBounds(
                left = (viewportWidth - homePanelWidth) * 0.5,
                top = (viewportHeight - homePanelHeight) * 0.5,
                width = homePanelWidth,
                height = homePanelHeight,
            ),
            savePanelBounds = ScreenBounds(
                left = (viewportWidth - savePanelWidth) * 0.5,
                top = (viewportHeight - savePanelHeight) * 0.5,
                width = savePanelWidth,
                height = savePanelHeight,
            ),
            saveListBounds = ScreenBounds(
                left = saveListLeft,
                top = saveListTop,
                width = saveListWidth,
                height = saveListHeight,
            ),
            scrollbarBounds = ScreenBounds(
                left = saveListLeft + saveListWidth + 12.0,
                top = saveListTop,
                width = scrollbarWidth,
                height = saveListHeight,
            ),
            saveRowWidth = saveListWidth - 24.0,
            viewportWidth = viewportWidth.toDouble(),
            viewportHeight = viewportHeight.toDouble(),
        )
    }
}
