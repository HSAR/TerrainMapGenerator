package io.hsar.mapgenerator.commandgui.korge.launch

import io.hsar.mapgenerator.commandgui.AssetSource
import io.hsar.mapgenerator.commandgui.GridColourSelector
import io.hsar.mapgenerator.commandgui.MapViewerSetupInputs
import io.hsar.mapgenerator.commandgui.korge.interaction.BatchLedgerEntry
import io.hsar.mapgenerator.commandgui.korge.interaction.CreateUnitTemplateAction
import io.hsar.mapgenerator.commandgui.korge.interaction.DeleteUnitTemplateAction
import io.hsar.mapgenerator.commandgui.korge.interaction.EditUnitTemplateAction
import io.hsar.mapgenerator.commandgui.korge.interaction.LedgerEntry
import io.hsar.mapgenerator.commandgui.korge.interaction.MapViewerUnitTemplate
import io.hsar.mapgenerator.commandgui.korge.interaction.UnitAffiliation
import io.hsar.mapgenerator.commandgui.korge.interaction.UnitTemplateId
import io.hsar.mapgenerator.commandgui.korge.interaction.UnitTemplateSnapshot
import io.hsar.mapgenerator.commandgui.korge.overlay.GridOverlayLinesView
import io.hsar.mapgenerator.commandgui.korge.overlay.GridOverlayState
import io.hsar.mapgenerator.commandgui.korge.overlay.GridOverlayView
import io.hsar.mapgenerator.commandgui.korge.ui.MapViewerChevron
import io.hsar.mapgenerator.commandgui.korge.ui.MapViewerChevronDirection
import io.hsar.mapgenerator.commandgui.korge.ui.MapViewerCollapsiblePanel
import io.hsar.mapgenerator.commandgui.korge.ui.MapViewerCornerBrackets
import io.hsar.mapgenerator.commandgui.korge.ui.MapViewerDialogManager
import io.hsar.mapgenerator.commandgui.korge.ui.MapViewerDialogSpacing
import io.hsar.mapgenerator.commandgui.korge.ui.MapViewerDropdownMenu
import io.hsar.mapgenerator.commandgui.korge.ui.MapViewerDropdownOption
import io.hsar.mapgenerator.commandgui.korge.ui.MapViewerIconFit
import io.hsar.mapgenerator.commandgui.korge.ui.MapViewerIconLoader
import io.hsar.mapgenerator.commandgui.korge.ui.MapViewerImageViews
import io.hsar.mapgenerator.commandgui.korge.ui.MapViewerScrollState
import io.hsar.mapgenerator.commandgui.korge.ui.MapViewerScrollbar
import io.hsar.mapgenerator.commandgui.korge.ui.MapViewerSwatchViews
import io.hsar.mapgenerator.commandgui.korge.ui.MapViewerTextInputValidation
import io.hsar.mapgenerator.commandgui.korge.ui.MapViewerTextInputs
import io.hsar.mapgenerator.commandgui.korge.ui.MapViewerTextViews
import io.hsar.mapgenerator.commandgui.korge.ui.MapViewerTransientNotice
import io.hsar.mapgenerator.commandgui.korge.ui.MapViewerUiBoxes
import io.hsar.mapgenerator.commandgui.korge.ui.MapViewerUiStateStyle
import io.hsar.mapgenerator.commandgui.korge.ui.MapViewerUnitCreateDialog
import io.hsar.mapgenerator.commandgui.korge.ui.ScreenBounds
import io.hsar.mapgenerator.commandgui.korge.ui.UiFrameStyle
import io.hsar.mapgenerator.commandgui.korge.ui.UnitAffiliationUi
import io.hsar.mapgenerator.commandgui.save.OperationSaveLoader
import io.hsar.mapgenerator.commandgui.save.OperationSaveWriter
import io.hsar.mapgenerator.graph.Point
import io.hsar.mapgenerator.graph.Rectangle
import io.hsar.mapgenerator.grid.CoordAxis
import io.hsar.mapgenerator.grid.GridLayer
import io.hsar.mapgenerator.grid.GridLayerBuilder
import io.hsar.mapgenerator.grid.ReferenceSystem
import io.hsar.mapgenerator.image.Palette
import korlibs.event.MouseButton
import korlibs.image.awt.toBMP32
import korlibs.image.bitmap.Bitmap32
import korlibs.image.color.RGBA
import korlibs.image.text.TextAlignment
import korlibs.korge.annotations.KorgeExperimental
import korlibs.korge.input.mouse
import korlibs.korge.view.Container
import korlibs.korge.view.addUpdater
import korlibs.korge.view.clipContainer
import korlibs.korge.view.container
import korlibs.korge.view.onStageResized
import korlibs.korge.view.solidRect
import korlibs.korge.view.xy
import korlibs.math.geom.Size2D
import korlibs.math.geom.Vector2D
import korlibs.render.awt.AwtGameWindow
import java.awt.Component
import java.awt.EventQueue
import java.awt.RenderingHints
import java.awt.datatransfer.DataFlavor
import java.awt.dnd.DnDConstants
import java.awt.dnd.DropTarget
import java.awt.dnd.DropTargetAdapter
import java.awt.dnd.DropTargetDragEvent
import java.awt.dnd.DropTargetDropEvent
import java.awt.image.BufferedImage
import java.io.File
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import javax.imageio.ImageIO
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter
import kotlin.concurrent.thread
import kotlin.math.roundToInt

@OptIn(KorgeExperimental::class)
internal class MapViewerNewOperationView(
    private val root: Container,
    viewportWidth: Int,
    viewportHeight: Int,
    initialSetupInputs: MapViewerSetupInputs = MapViewerSetupInputs(),
    private val initialUnitsPerPixel: Double = 20.0,
    private val initialSeed: Long = System.currentTimeMillis(),
    private val onLaunch: (NewOperationDraft) -> Unit,
    private val onBack: () -> Unit,
    private val onTitleChanged: (String) -> Unit = {},
) {
    private val layer = root.container()
    private val transientNotice = MapViewerTransientNotice(root, viewportHeightProvider = { viewportHeight })
    private val unitRows = mutableListOf<UnitRow>()
    private val initialGridConfiguration = initialSetupInputs.mapImage
        ?.let { defaultGridConfiguration(it, initialUnitsPerPixel, initialSeed) }
        ?: defaultGridConfiguration(DEFAULT_GRID_WIDTH, DEFAULT_GRID_HEIGHT, initialUnitsPerPixel, initialSeed)
    private var operationName = OperationNameGenerator.generate(Random(initialSeed))
    private var coordinateSystemSelection = CoordinateSystemSelection.MGRS
    private val coordinateSystemInputs = mutableMapOf(
        CoordinateSystemSelection.MGRS to CoordinateInputState.from(initialGridConfiguration, initialUnitsPerPixel.toString()),
    )
    private val referenceSystemDropdown = MapViewerDropdownMenu(
        root = root,
        options = CoordinateSystemSelection.values().map { system ->
            MapViewerDropdownOption(system, system.label)
        },
        onSelected = { selected ->
            selectCoordinateSystem(selected ?: CoordinateSystemSelection.MGRS)
            mapPreviewDirty = true
            refreshLaunchButton()
            render(layout.viewportWidth, layout.viewportHeight)
        },
        labelForValue = { value -> (value ?: CoordinateSystemSelection.MGRS).label },
        menuWidth = 180.0,
    )
    private var operationNameInput: io.hsar.mapgenerator.commandgui.korge.ui.MapViewerInputField? = null
    private val coordinateInputFields = mutableMapOf<CoordinateSystemSelection, CoordinateInputFields>()
    private var mapPreviewContainer: Container? = null
    private var mapPreviewBounds: ScreenBounds? = null
    private var mapPreviewDirty = true
    private var mapImage: MapImageRow? = initialSetupInputs.mapImagePath?.let { path ->
        val image = initialSetupInputs.mapImage
        if (image == null) {
            null
        } else {
            MapImageRow(
                name = File(path).nameWithoutExtension.ifBlank { File(path).name },
                imagePath = path,
                image = image,
                assetSource = AssetSource.fromFile(File(path)),
            )
        }
    }
    private var statusContainer: Container? = null
    private var launchButtonContainer: Container? = null
    private var mapImageValidationTooltip: Container? = null
    private var panelFocus = MapViewerNewOperationPanelFocus.OPERATIONAL_DETAILS
    private var layout = MapViewerNewOperationLayoutBuilder.build(viewportWidth, viewportHeight, panelFocus)
    private var dragState: ReorderDragState? = null
    private var unitScrollState = MapViewerScrollState(0.0, 0.0)
    private var unitScrollDragState: ScrollDragState? = null
    private var unitScrollContent: Container? = null
    private var unitListWidth = 0.0
    private var selectedUnitRowId: Long? = null
    private var unitIconDropTargetCollapsed = false
    private val collapsedUnitAffiliationGroups = mutableSetOf<UnitAffiliation?>()
    private var embeddedUnitTypeEditor: MapViewerUnitCreateDialog? = null
    private var unitScrollbarContainer: Container? = null
    private var unitScrollbarBounds: ScreenBounds? = null
    private var unitIconDropTargetBounds: ScreenBounds? = null
    private var nativeDropTargetComponent: Component? = null
    private var pendingDroppedUnitIconFiles: List<File> = emptyList()
    private val pendingMapImageLoads = ConcurrentLinkedQueue<Result<MapImageRow>>()
    private val pendingMapPreviews = ConcurrentLinkedQueue<Result<MapPreview>>()
    private val pendingUnitIconLoads = ConcurrentLinkedQueue<List<UnitIconLoad>>()
    private val pendingUnitTypeImports = ConcurrentLinkedQueue<Result<List<UnitTemplateSnapshot>>>()
    private val pendingUnitTypeSnapshots = ArrayDeque<UnitTemplateSnapshot>()
    private var importedUnitTypeCount = 0
    private var unitTypesImportInProgress = false
    private var lastUnitIconDirectory: File? = null
    private var nextRowId = 1L
    private var operationNameWasValid = MapViewerTextInputValidation.isPresent(operationName)
    private var pendingOperationNameFocus = false
    private val mapPreviewRevision = MapViewerAsyncRevision()
    private var requestedMapPreviewSignature: String? = null
    private var preparedMapPreview: MapPreview? = null
    private var nativeDropTargetInstallScheduled = false
    private var closed = false

    init {
        layer.zIndex = Z_INDEX
        layer.onStageResized { width, height ->
            captureInputs()
            render(width, height)
        }
        layer.addUpdater {
            updateDrag(layer.stage?.views?.input?.mousePos, layer.stage?.views?.input?.mouseButtonPressed(MouseButton.LEFT) == true)
            val pointer = layer.stage?.views?.input?.mousePos
            if (layer.stage?.views?.input?.mouseButtonPressed(MouseButton.LEFT) != true) unitScrollDragState = null
            pointer?.let(::updateUnitScrollDrag)
            consumePendingDroppedUnitIconFiles()
            consumePendingMapImageLoads()
            consumePendingMapPreviews()
            consumePendingUnitIconLoads()
            consumePendingUnitTypeImports()
            transientNotice.update()
            updatePreviewIfNeeded()
        }
        layer.mouse {
            onScrollAnywhere {
                if (layout.unitsPanelBounds.contains(views.input.mousePos) && unitScrollState.needsScrolling) {
                    updateUnitScrollOffset(unitScrollState.offset + scrollDeltaYLines * MapViewerScrollbar.SCROLL_STEP_PX)
                }
            }
        }
        render(viewportWidth, viewportHeight)
        scheduleNativeFileDropTargetInstall()
    }

    fun close() {
        closed = true
        nativeDropTargetComponent?.dropTarget = null
        referenceSystemDropdown.close()
        transientNotice.dispose()
        layer.removeFromParent()
    }

    fun reconcileViewport(viewportWidth: Int, viewportHeight: Int) {
        if (viewportWidth != layout.viewportWidth || viewportHeight != layout.viewportHeight) {
            render(viewportWidth, viewportHeight)
        }
    }

    private fun render(viewportWidth: Int, viewportHeight: Int) {
        captureInputs()
        hideMapImageValidationTooltip()
        embeddedUnitTypeEditor?.dispose()
        embeddedUnitTypeEditor = null
        layout = MapViewerNewOperationLayoutBuilder.build(viewportWidth, viewportHeight, panelFocus)
        layer.removeChildren()
        coordinateInputFields.clear()
        renderHeader()
        renderMapSection()
        renderUnitTypes()
        renderPanelFocusButton()
        renderButtons()
        onTitleChanged(screenTitle())
    }


    private fun renderHeader() {
        MapViewerLaunchpadTemplate.renderScreenShell(layer, layout.screenHeaderBounds, screenTitle(), onBack)
    }

    private fun renderMapSection() {
        if (panelFocus == MapViewerNewOperationPanelFocus.UNIT_TYPES) {
            renderCompactOperationalDetails()
            return
        }
        val panel = MapViewerLaunchUi.panel(layer, layout.mapPanelBounds)
        val panelWidth = layout.mapPanelBounds.width
        val coordinateRowLayout = MapViewerNewOperationCoordinateRowLayoutBuilder.build(
            panelWidth = panelWidth,
            labelTop = COORDINATE_LABEL_TOP,
            inputHeight = INPUT_HEIGHT,
            leftPadding = CONTENT_PADDING,
        )
        val imageButtonWidth = 160.0
        val imageButtonLeft = panelWidth - CONTENT_PADDING - imageButtonWidth
        val imageValidationWidth = MapViewerTextInputValidation.VALIDATION_ICON_SIZE + FIELD_GAP
        val operationNameWidth = imageButtonLeft - CONTENT_PADDING - FIELD_GAP - imageValidationWidth
        val previewTop = 250.0

        MapViewerLaunchUi.title(
            panel,
            "Operational Details",
            panelWidth - CONTENT_PADDING * 2.0,
            34.0,
            alignment = TextAlignment.TOP_LEFT,
        )
            .xy(CONTENT_PADDING, CONTENT_PADDING)
        MapViewerLaunchUi.label(panel, "Operation Name").xy(CONTENT_PADDING, 60.0)
        MapViewerLaunchUi.label(panel, "Map Image").xy(imageButtonLeft, 60.0)
        operationNameInput = textInput(
            parent = panel,
            text = operationName,
            bounds = ScreenBounds(
                left = CONTENT_PADDING,
                top = 84.0,
                width = operationNameWidth,
                height = INPUT_HEIGHT,
            ),
            frameStyle = MapViewerTextInputValidation.frameStyle(MapViewerTextInputValidation.isPresent(operationName)),
        )
        focusOperationNameIfPending()
        wireOperationNameValidation(operationNameInput)

        MapViewerLaunchUi.button(panel, imageButtonWidth, 36.0, "Choose Map Image") {
            chooseMapImage()
        }.xy(imageButtonLeft, 84.0)

        mapImage?.let { image ->
            MapViewerLaunchUi.label(panel, image.name, color = Palette.Colours.LIGHT, scale = 0.85).xy(imageButtonLeft, 124.0)
        } ?: run {
            renderMapImageValidationIndicator(
                parent = panel,
                left = imageButtonLeft - imageValidationWidth,
                top = 84.0,
            )
        }

        referenceSystemDropdown.renderField(
            parent = panel,
            label = "Coordinate System",
            value = coordinateSystemSelection,
            x = coordinateRowLayout.coordinateSystem.labelBounds.left,
            y = coordinateRowLayout.coordinateSystem.labelBounds.top,
            width = coordinateRowLayout.coordinateSystem.inputBounds.width,
            height = coordinateRowLayout.coordinateSystem.inputBounds.height,
        )

        val coordinateInputs = currentCoordinateInputs()
        MapViewerLaunchUi.label(panel, "Map ID").xy(coordinateRowLayout.mapId.labelBounds.left, coordinateRowLayout.mapId.labelBounds.top)
        val mapIdInput = textInput(
            parent = panel,
            text = coordinateInputs.mapIdText,
            bounds = coordinateRowLayout.mapId.inputBounds,
        )
        wirePreviewRefresh(mapIdInput)

        MapViewerLaunchUi.label(panel, "Starting X").xy(coordinateRowLayout.startX.labelBounds.left, coordinateRowLayout.startX.labelBounds.top)
        MapViewerLaunchUi.label(panel, "Starting Y").xy(coordinateRowLayout.startY.labelBounds.left, coordinateRowLayout.startY.labelBounds.top)
        MapViewerLaunchUi.label(panel, scaleLabel()).xy(coordinateRowLayout.scale.labelBounds.left, coordinateRowLayout.scale.labelBounds.top)
        val gridStartXInput = textInput(
            parent = panel,
            text = coordinateInputs.gridStartXText,
            bounds = coordinateRowLayout.startX.inputBounds,
        )
        wirePreviewRefresh(gridStartXInput)
        val gridStartYInput = textInput(
            parent = panel,
            text = coordinateInputs.gridStartYText,
            bounds = coordinateRowLayout.startY.inputBounds,
        )
        wirePreviewRefresh(gridStartYInput)

        val unitsPerPixelInput = textInput(
            parent = panel,
            text = coordinateInputs.scaleText,
            bounds = coordinateRowLayout.scale.inputBounds,
        )
        wirePreviewRefresh(unitsPerPixelInput)
        coordinateInputFields[coordinateSystemSelection] = CoordinateInputFields(
            mapId = mapIdInput,
            gridStartX = gridStartXInput,
            gridStartY = gridStartYInput,
            scale = unitsPerPixelInput,
        )

        mapPreviewBounds = ScreenBounds(
            left = CONTENT_PADDING,
            top = previewTop,
            width = panelWidth - CONTENT_PADDING * 2.0,
            height = layout.mapPanelBounds.height - previewTop - CONTENT_PADDING,
        )
        mapPreviewContainer = panel.container().also {
            it.xy(mapPreviewBounds!!.left, mapPreviewBounds!!.top)
        }
        renderMapPreview(mapPreviewContainer!!, mapPreviewBounds!!, mapImage)
        mapPreviewDirty = false
    }

    private fun renderCompactOperationalDetails() {
        val panel = MapViewerLaunchUi.panel(layer, layout.compactDetailsPanelBounds)
        val panelWidth = layout.compactDetailsPanelBounds.width
        val fieldWidth = panelWidth - CONTENT_PADDING * 2.0
        val inputHeight = INPUT_HEIGHT
        val rowHeight = 76.0
        val left = CONTENT_PADDING
        var top = CONTENT_PADDING

        MapViewerLaunchUi.title(panel, "Operational Details", fieldWidth, 34.0, alignment = TextAlignment.TOP_LEFT).xy(left, top)
        top += 36.0
        MapViewerLaunchUi.label(panel, "Operation Name").xy(left, top)
        operationNameInput = textInput(
            parent = panel,
            text = operationName,
            bounds = ScreenBounds(left, top + 24.0, fieldWidth, inputHeight),
            frameStyle = MapViewerTextInputValidation.frameStyle(MapViewerTextInputValidation.isPresent(operationName)),
        )
        focusOperationNameIfPending()
        wireOperationNameValidation(operationNameInput)
        top += 84.0

        referenceSystemDropdown.renderField(
            parent = panel,
            label = "Coordinate System",
            value = coordinateSystemSelection,
            x = left,
            y = top,
            width = fieldWidth,
            height = inputHeight,
        )
        top += rowHeight

        val coordinateInputs = currentCoordinateInputs()
        val fields = listOf(
            "Map ID" to coordinateInputs.mapIdText,
            "Starting X" to coordinateInputs.gridStartXText,
            "Starting Y" to coordinateInputs.gridStartYText,
            scaleLabel() to coordinateInputs.scaleText,
        )
        val inputs = fields.mapIndexed { index, (label, text) ->
            val fieldTop = top + index * rowHeight
            MapViewerLaunchUi.label(panel, label).xy(left, fieldTop)
            textInput(panel, text, ScreenBounds(left, fieldTop + 24.0, fieldWidth, inputHeight)).also(::wirePreviewRefresh)
        }
        coordinateInputFields[coordinateSystemSelection] = CoordinateInputFields(
            mapId = inputs[0],
            gridStartX = inputs[1],
            gridStartY = inputs[2],
            scale = inputs[3],
        )

        val previewTop = top + fields.size * rowHeight
        mapPreviewBounds = ScreenBounds(
            left = CONTENT_PADDING,
            top = previewTop,
            width = panelWidth - CONTENT_PADDING * 2.0,
            height = (layout.compactDetailsPanelBounds.height - previewTop - CONTENT_PADDING).coerceAtLeast(80.0),
        )
        mapPreviewContainer = panel.container().also { it.xy(mapPreviewBounds!!.left, mapPreviewBounds!!.top) }
        renderMapPreview(mapPreviewContainer!!, mapPreviewBounds!!, mapImage)
        mapPreviewDirty = false
    }

    private fun renderUnitTypes() {
        val panel = MapViewerLaunchUi.panel(layer, layout.unitsPanelBounds)
        val panelWidth = layout.unitsPanelBounds.width
        MapViewerLaunchUi.title(
            panel,
            "Unit Types",
            panelWidth - CONTENT_PADDING * 2.0,
            34.0,
            alignment = TextAlignment.TOP_LEFT,
        )
            .xy(CONTENT_PADDING, CONTENT_PADDING)
        renderUnitIconDropTarget(panel, panelWidth - CONTENT_PADDING * 2.0)

        if (!unitIconDropTargetCollapsed) {
            val actionWidth = (panelWidth - CONTENT_PADDING * 2.0 - FIELD_GAP) * 0.5
            val actionTop = unitActionTop()
            MapViewerLaunchUi.button(
                panel,
                actionWidth,
                36.0,
                if (panelFocus == MapViewerNewOperationPanelFocus.UNIT_TYPES) "Add Unit Type" else "Add",
            ) { addUnitType() }.xy(CONTENT_PADDING, actionTop)
            MapViewerLaunchUi.button(
                panel,
                actionWidth,
                36.0,
                if (panelFocus == MapViewerNewOperationPanelFocus.UNIT_TYPES) "Import Unit Types" else "Import",
                disabled = unitTypesImportInProgress,
            ) { importUnitTypesFromSave() }.xy(CONTENT_PADDING + actionWidth + FIELD_GAP, actionTop)
        }

        val scrollbarWidth = MapViewerScrollbar.WIDTH
        val listViewportHeight = (layout.unitsPanelBounds.height - unitListTop() - CONTENT_PADDING).coerceAtLeast(0.0)
        val editorWidth = if (panelFocus == MapViewerNewOperationPanelFocus.UNIT_TYPES) {
            ((panelWidth - CONTENT_PADDING * 2.0 - scrollbarWidth - FIELD_GAP * 2.0) * 0.5).coerceAtLeast(0.0)
        } else {
            0.0
        }
        val listWidth = if (editorWidth > 0.0) {
            editorWidth
        } else {
            (panelWidth - CONTENT_PADDING * 2.0 - scrollbarWidth - FIELD_GAP).coerceAtLeast(0.0)
        }
        unitListWidth = listWidth
        val listContentHeight = if (panelFocus == MapViewerNewOperationPanelFocus.UNIT_TYPES) {
            focusedUnitSelectorContentHeight()
        } else {
            unitRows.size * unitRowHeight()
        }
        unitScrollState = unitScrollState.withContent(listViewportHeight, listContentHeight)
        panel.clipContainer(Size2D(listWidth, listViewportHeight)) {
            xy(CONTENT_PADDING, unitListTop())
            val content = container().also {
                it.y = -unitScrollState.offset
                unitScrollContent = it
            }
            if (panelFocus == MapViewerNewOperationPanelFocus.UNIT_TYPES) {
                renderFocusedUnitSelector(content, listWidth)
            } else {
                unitRows.forEachIndexed { index, row ->
                    renderUnitRow(content, row, index * unitRowHeight(), listWidth)
                }
            }
        }
        val scrollbarBounds = ScreenBounds(
            left = CONTENT_PADDING + listWidth + FIELD_GAP,
            top = unitListTop(),
            width = scrollbarWidth,
            height = listViewportHeight,
        )
        unitScrollbarBounds = scrollbarBounds
        unitScrollbarContainer = panel.container()
        MapViewerScrollbar.render(unitScrollbarContainer!!, scrollbarBounds, unitScrollState) { pointerY ->
            unitScrollDragState = ScrollDragState(pointerY, unitScrollState.offset)
        }

        if (panelFocus == MapViewerNewOperationPanelFocus.UNIT_TYPES) {
            renderEmbeddedUnitTypeEditor(panel, CONTENT_PADDING + listWidth + FIELD_GAP * 2.0 + scrollbarWidth, editorWidth)
        }
    }

    private fun renderEmbeddedUnitTypeEditor(panel: Container, editorLeft: Double, editorWidth: Double) {
        val row = unitRows.firstOrNull { it.id == selectedUnitRowId }
        if (row == null) {
            MapViewerTextViews.box(panel, "Select a unit type", editorWidth, 32.0, color = Palette.Colours.LIGHT)
                .xy(editorLeft, unitListTop())
            return
        }
        val editor = MapViewerUnitCreateDialog(
            root = root,
            dialogManager = MapViewerDialogManager(),
            dialogTitle = "Edit Unit Type",
            initialTemplate = MapViewerUnitTemplate(
                id = UnitTemplateId(UUID(0L, row.id)),
                name = row.name,
                bitmap = row.bitmap,
                assetSource = row.assetSource,
                affiliation = row.affiliation,
                customProperties = row.customProperties,
            ),
            onCreated = { updated ->
                row.name = updated.name
                row.bitmap = updated.bitmap
                row.assetSource = updated.assetSource
                row.affiliation = updated.affiliation
                row.customProperties = updated.customProperties
                render(layout.viewportWidth, layout.viewportHeight)
            },
            onDismiss = {},
            onTransientNotice = transientNotice::show,
        )
        embeddedUnitTypeEditor = editor
        panel.container().also {
            it.xy(editorLeft, unitListTop())
            editor.renderEmbedded(it, editorWidth)
        }
    }

    private fun renderPanelFocusButton() {
        val button = MapViewerLaunchUi.button(
            parent = layer,
            width = layout.panelFocusButtonBounds.width,
            height = layout.panelFocusButtonBounds.height,
            label = "",
        ) {
            panelFocus = when (panelFocus) {
                MapViewerNewOperationPanelFocus.OPERATIONAL_DETAILS -> MapViewerNewOperationPanelFocus.UNIT_TYPES
                MapViewerNewOperationPanelFocus.UNIT_TYPES -> MapViewerNewOperationPanelFocus.OPERATIONAL_DETAILS
            }
            render(layout.viewportWidth, layout.viewportHeight)
        }.xy(layout.panelFocusButtonBounds.left, layout.panelFocusButtonBounds.top)
        MapViewerChevron.render(
            parent = button,
            width = layout.panelFocusButtonBounds.width,
            height = layout.panelFocusButtonBounds.height,
            direction = if (panelFocus == MapViewerNewOperationPanelFocus.OPERATIONAL_DETAILS) {
                MapViewerChevronDirection.LEFT
            } else {
                MapViewerChevronDirection.RIGHT
            },
        )
    }

    private fun renderUnitIconDropTarget(parent: Container, width: Double) {
        MapViewerCollapsiblePanel.header(
            parent = parent,
            width = width,
            height = UNIT_DROP_HEADER_HEIGHT,
            label = "DROP UNIT ICONS HERE",
            expanded = !unitIconDropTargetCollapsed,
        ) {
            unitIconDropTargetCollapsed = !unitIconDropTargetCollapsed
            render(layout.viewportWidth, layout.viewportHeight)
        }.xy(CONTENT_PADDING, DROP_TARGET_TOP)
        if (unitIconDropTargetCollapsed) {
            unitIconDropTargetBounds = null
            return
        }
        val dropTarget = parent.container()
        dropTarget.xy(CONTENT_PADDING, DROP_TARGET_TOP + UNIT_DROP_HEADER_HEIGHT + FIELD_GAP)
        dropTarget.solidRect(width, DROP_TARGET_HEIGHT, RGBA(80, 80, 80, 190))
        dropTarget.solidRect(width - 2.0, DROP_TARGET_HEIGHT - 2.0, RGBA(45, 45, 45, 210)) {
            x = 1.0
            y = 1.0
        }
        MapViewerCornerBrackets.renderAll(dropTarget, width, DROP_TARGET_HEIGHT, BRACKET_INSET)
        MapViewerTextViews.box(
            parent = dropTarget,
            text = "DROP UNIT ICONS HERE",
            width = width,
            height = DROP_TARGET_HEIGHT,
            size = 18.0,
            color = Palette.Colours.LIGHT,
        )
        unitIconDropTargetBounds = dropTarget.getGlobalBounds().let { bounds ->
            ScreenBounds(bounds.left, bounds.top, bounds.width, bounds.height)
        }
    }

    private fun renderButtons() {
        launchButtonContainer = layer.container().also {
            it.xy(layout.launchButtonBounds.left, layout.launchButtonBounds.top)
        }
        refreshLaunchButton()
    }

    private fun renderFocusedUnitSelector(parent: Container, rowWidth: Double) {
        var top = 0.0
        UNIT_AFFILIATION_GROUPS.forEach { group ->
            val groupRows = unitRows.filter { it.affiliation == group.affiliation }
            if (groupRows.size < 2) {
                groupRows.forEach { row ->
                    renderUnitRow(parent, row, top, rowWidth)
                    top += unitRowHeight()
                }
                return@forEach
            }
            val collapsed = group.affiliation in collapsedUnitAffiliationGroups
            MapViewerCollapsiblePanel.header(
                parent = parent,
                width = rowWidth,
                height = UNIT_GROUP_HEADER_HEIGHT,
                label = group.label,
                expanded = !collapsed,
            ) {
                if (!collapsed && unitRows.firstOrNull { it.id == selectedUnitRowId }?.affiliation == group.affiliation) {
                    selectedUnitRowId = null
                }
                if (collapsed) collapsedUnitAffiliationGroups.remove(group.affiliation) else collapsedUnitAffiliationGroups.add(group.affiliation)
                render(layout.viewportWidth, layout.viewportHeight)
            }.xy(0.0, top)
            top += UNIT_GROUP_HEADER_HEIGHT + UNIT_GROUP_GAP
            if (!collapsed) {
                groupRows.forEach { row ->
                    renderUnitRow(parent, row, top, rowWidth)
                    top += unitRowHeight()
                }
            }
        }
    }

    private fun focusedUnitSelectorContentHeight(): Double =
        UNIT_AFFILIATION_GROUPS.sumOf { group ->
            val count = unitRows.count { it.affiliation == group.affiliation }
            if (count < 2) count * unitRowHeight() else {
                UNIT_GROUP_HEADER_HEIGHT + UNIT_GROUP_GAP + if (group.affiliation in collapsedUnitAffiliationGroups) 0.0 else count * unitRowHeight()
            }
        }

    private fun renderUnitRow(parent: Container, row: UnitRow, top: Double, rowWidth: Double) {
        val rowHeight = unitRowHeight()
        val rowContainer = parent.container()
        rowContainer.xy(0.0, top)
        rowContainer.mouse {
            onClick {
                if (panelFocus == MapViewerNewOperationPanelFocus.UNIT_TYPES && selectedUnitRowId != row.id) {
                    selectedUnitRowId = row.id
                    render(layout.viewportWidth, layout.viewportHeight)
                }
            }
        }
        val selected = selectedUnitRowId == row.id
        MapViewerUiBoxes.box(
            rowContainer,
            rowWidth,
            rowHeight,
            style = { active -> MapViewerUiBoxes.defaultStyle(active && (panelFocus != MapViewerNewOperationPanelFocus.UNIT_TYPES || selected)) },
        )

        renderPreviewFrame(
            parent = rowContainer,
            bitmap = row.bitmap,
            left = PREVIEW_LEFT,
            top = (rowHeight - PREVIEW_HEIGHT) * 0.5,
        )

        if (panelFocus == MapViewerNewOperationPanelFocus.UNIT_TYPES) {
            val swatchHost = rowContainer.container()
            swatchHost.xy(rowWidth - 34.0, (rowHeight - 24.0) * 0.5)
            MapViewerSwatchViews.render(swatchHost, UnitAffiliationUi.swatchColor(row.affiliation), 24.0)
            MapViewerTextViews.leftBox(
                parent = rowContainer,
                text = row.name,
                width = rowWidth - PREVIEW_WIDTH - 18.0,
                height = rowHeight,
                color = MapViewerUiStateStyle.text(selected),
            ).xy(PREVIEW_LEFT + PREVIEW_WIDTH + 10.0, 0.0)
            return
        }

        val handleLeft = rowWidth - HANDLE_WIDTH - 10.0
        val affiliationWidth = if (panelFocus == MapViewerNewOperationPanelFocus.UNIT_TYPES) 150.0 else 0.0
        val nameLeft = PREVIEW_LEFT + PREVIEW_WIDTH + 10.0
        val nameRight = if (affiliationWidth > 0.0) handleLeft - affiliationWidth - 10.0 else handleLeft - 10.0
        row.input = textInput(
            parent = rowContainer,
            text = row.name,
            bounds = ScreenBounds(
                left = nameLeft,
                top = (rowHeight - INPUT_HEIGHT) * 0.5,
                width = nameRight - nameLeft,
                height = INPUT_HEIGHT,
            ),
        )

        renderHandle(
            parent = rowContainer,
            x = rowWidth - HANDLE_WIDTH - 10.0,
            y = (rowHeight - HANDLE_HEIGHT) * 0.5,
            rowId = row.id,
            index = unitRows.indexOf(row),
        )
    }

    private fun renderPreviewFrame(parent: Container, bitmap: Bitmap32, left: Double, top: Double) {
        val frame = parent.container()
        frame.xy(left, top)
        MapViewerUiBoxes.box(frame, PREVIEW_WIDTH, PREVIEW_HEIGHT)
        val fittedIcon = MapViewerIconFit.inside(bitmap, PREVIEW_WIDTH - 12.0, PREVIEW_HEIGHT - 12.0)
        MapViewerImageViews.icon(frame, bitmap) {
            scale = fittedIcon.scale
            this.x = 6.0 + fittedIcon.x
            this.y = 6.0 + fittedIcon.y
        }
    }

    private fun renderHandle(parent: Container, x: Double, y: Double, rowId: Long, index: Int) {
        val handle = parent.container()
        handle.xy(x, y)
        MapViewerUiBoxes.button(handle, HANDLE_WIDTH, HANDLE_HEIGHT)

        val grip = handle.container()
        grip.xy(7.0, 7.0)
        repeat(3) { barIndex ->
            grip.solidRect(12.0, 2.0, RGBA(255, 255, 255, 255)) {
                this.y = barIndex * 4.0
            }
        }

        handle.mouse {
            onDown {
                if (button == MouseButton.LEFT) {
                    dragState = ReorderDragState(rowId = rowId, startIndex = index)
                }
            }
        }
    }

    private fun chooseMapImage() {
        val file = chooseRasterImageFile() ?: return
        showStatus("Loading map image...")
        thread(name = "command-gui-map-image-load", isDaemon = true) {
            pendingMapImageLoads += runCatching {
                val image = ImageIO.read(file) ?: error("Could not load map image")
                MapImageRow(
                    name = file.nameWithoutExtension,
                    imagePath = file.absolutePath,
                    image = image,
                    assetSource = AssetSource.fromFile(file),
                )
            }
        }
    }

    private fun addUnitType() {
        val file = chooseUnitImageFile() ?: return
        addUnitTypesFromFiles(listOf(file))
        render(layout.viewportWidth, layout.viewportHeight)
    }

    private fun addUnitTypesFromFiles(files: List<File>) {
        showStatus("Loading unit types...")
        thread(name = "command-gui-unit-icon-load", isDaemon = true) {
            pendingUnitIconLoads += files.map { file ->
                UnitIconLoad(file, MapViewerIconLoader.load(file))
            }
        }
    }

    private fun importUnitTypesFromSave() {
        val file = chooseSaveFile() ?: return
        unitTypesImportInProgress = true
        showStatus("Importing unit types...")
        render(layout.viewportWidth, layout.viewportHeight)
        thread(name = "command-gui-unit-type-import", isDaemon = true) {
            pendingUnitTypeImports += runCatching {
                OperationSaveLoader().load(file.toPath()).ledgerEntries.latestUnitTemplates()
            }
        }
    }

    private fun consumePendingUnitTypeImports() {
        pendingUnitTypeImports.poll()?.let { result ->
            result
                .onSuccess { templates ->
                    pendingUnitTypeSnapshots.addAll(templates)
                    importedUnitTypeCount = templates.size
                    if (templates.isEmpty()) {
                        unitTypesImportInProgress = false
                        showStatus("No unit types found in save")
                        render(layout.viewportWidth, layout.viewportHeight)
                    }
                }
                .onFailure {
                    unitTypesImportInProgress = false
                    showStatus("Could not load save file")
                    render(layout.viewportWidth, layout.viewportHeight)
                }
        }
        if (pendingUnitTypeSnapshots.isEmpty()) return

        val importedRows = buildList {
            repeat(IMPORT_BATCH_SIZE) {
                if (pendingUnitTypeSnapshots.isNotEmpty()) {
                    add(pendingUnitTypeSnapshots.removeFirst().template.toUnitRow())
                }
            }
        }
        val newRows = importedRows.filter(::upsertUnitRow)
        val firstIndex = unitRows.size - newRows.size
        if (panelFocus == MapViewerNewOperationPanelFocus.UNIT_TYPES) {
            render(layout.viewportWidth, layout.viewportHeight)
        } else unitScrollContent?.let { content ->
            newRows.forEachIndexed { index, row ->
                renderUnitRow(content, row, (firstIndex + index) * unitRowHeight(), unitListWidth)
            }
            unitScrollState = unitScrollState.withContent(
                contentHeight = unitRows.size * unitRowHeight(),
            )
            updateUnitScrollOffset(unitScrollState.offset)
        } ?: render(layout.viewportWidth, layout.viewportHeight)

        if (pendingUnitTypeSnapshots.isEmpty()) {
            unitTypesImportInProgress = false
            showStatus("$importedUnitTypeCount unit type${if (importedUnitTypeCount == 1) "" else "s"} imported")
        }
    }

    private fun launch() {
        captureInputs()
        val draft = currentDraft()
        when {
            !MapViewerTextInputValidation.isPresent(draft.operationName) -> showStatus("Enter an operation name")
            draft.mapImage == null -> showStatus("Choose a map image")
            draft.unitsPerPixel <= 0.0 -> showStatus("Enter a ${referenceSystem().displayUnit}-per-pixel value")
            draft.gridConfiguration == null -> showStatus("Enter a map ID and starting coordinates")
            else -> onLaunch(draft)
        }
    }

    private fun currentDraft(): NewOperationDraft =
        NewOperationDraft(
            operationName = operationName,
            mapImage = mapImage?.let { image ->
                OperationMapImage(
                    name = image.name,
                    imagePath = image.imagePath,
                    image = image.image,
                    assetSource = image.assetSource,
                )
            },
            unitsPerPixel = currentUnitsPerPixel() ?: -1.0,
            referenceSystem = referenceSystem(),
            gridConfiguration = parseGridConfiguration(),
            unitTemplates = unitRows.map { row ->
                MapViewerUnitTemplate(
                    id = UnitTemplateId(UUID.randomUUID()),
                    name = row.name,
                    bitmap = row.bitmap,
                    assetSource = row.assetSource,
                    affiliation = row.affiliation,
                    customProperties = row.customProperties,
                )
            },
        )

    private fun captureInputs() {
        operationNameInput?.text?.trim()?.let { operationName = it }
        coordinateInputFields[coordinateSystemSelection]?.let { fields ->
            coordinateSystemInputs[coordinateSystemSelection] = CoordinateInputState(
                mapIdText = fields.mapId.text.trim(),
                gridStartXText = fields.gridStartX.text.trim(),
                gridStartYText = fields.gridStartY.text.trim(),
                scaleText = fields.scale.text.trim(),
            )
        }
        unitRows.forEach { row ->
            row.name = row.input?.text?.trim().orEmpty().ifBlank { row.name }
        }
    }

    private fun parseGridConfiguration(): GridConfiguration? {
        val inputs = currentCoordinateInputs()
        val referenceSystem = referenceSystem()
        val mapId = inputs.mapIdText.trim()
        val startX = referenceSystem.parseCoord(inputs.gridStartXText, CoordAxis.X, referenceSystem.gridScale)
        val startY = referenceSystem.parseCoord(inputs.gridStartYText, CoordAxis.Y, referenceSystem.gridScale)
        return if (mapId.isNotBlank() && startX != null && startY != null) {
            GridConfiguration(
                mapId = mapId,
                referenceSystem = referenceSystem,
                startX = startX,
                startY = startY,
            )
        } else {
            null
        }
    }

    private fun renderMapPreview(parent: Container, bounds: ScreenBounds, mapImage: MapImageRow?) {
        parent.removeChildren()
        MapViewerUiBoxes.box(parent, bounds.width, bounds.height)

        if (mapImage == null) {
            MapViewerLaunchUi.label(parent, "Map preview appears here", color = Palette.Colours.LIGHT)
                .xy(16.0, (bounds.height - 18.0) * 0.5)
            return
        }

        val signature = previewSignature(bounds)
        preparedMapPreview
            ?.takeIf { it.signature == signature }
            ?.let { preview ->
                renderPreparedMapPreview(parent, bounds, preview)
                return
            }
        MapViewerLaunchUi.label(parent, "Loading map preview...", color = Palette.Colours.LIGHT)
            .xy(16.0, (bounds.height - 18.0) * 0.5)
        requestMapPreview(mapImage, bounds, signature)
    }

    private fun renderPreparedMapPreview(parent: Container, bounds: ScreenBounds, preview: MapPreview) {
        val imageContainer = parent.container().also { it.xy(8.0 + preview.left, 8.0 + preview.top) }
        MapViewerImageViews.icon(imageContainer, preview.bitmap)

        val lineOverlay = GridOverlayLinesView(preview.gridLayer)
        val labelOverlay = GridOverlayView(preview.gridLayer)
        imageContainer.addChild(lineOverlay)
        imageContainer.addChild(labelOverlay)
        val state = GridOverlayState(
            viewport = Rectangle(Point.ORIGIN, Point(preview.width.toDouble(), preview.height.toDouble())),
            mapWorldX = 0.0,
            mapWorldY = 0.0,
            mapOffsetX = 0.0,
            mapOffsetY = 0.0,
            scale = 1.0,
            pixelWidth = preview.width,
            pixelHeight = preview.height,
            backgroundWidth = preview.width,
            backgroundHeight = preview.height,
            visibleCanvasLeft = 0.0,
            visibleCanvasTop = 0.0,
            visibleCanvasRight = preview.width.toDouble(),
            visibleCanvasBottom = preview.height.toDouble(),
            activeReference = null,
            measurement = null,
            gridLayer = preview.gridLayer,
        )
        lineOverlay.update(state)
        labelOverlay.update(state)
    }

    private fun requestMapPreview(mapImage: MapImageRow, bounds: ScreenBounds, signature: String) {
        if (requestedMapPreviewSignature == signature) return
        requestedMapPreviewSignature = signature
        val revision = mapPreviewRevision.next()
        val configuration = parseGridConfiguration()
        val unitsPerPixel = currentUnitsPerPixel() ?: initialScaleInputAsUnitsPerPixel()
        val referenceSystem = referenceSystem()
        thread(name = "command-gui-map-preview", isDaemon = true) {
            pendingMapPreviews += runCatching {
                prepareMapPreview(revision, signature, mapImage, bounds, configuration, unitsPerPixel, referenceSystem)
            }
        }
    }

    private fun prepareMapPreview(
        revision: Long,
        signature: String,
        mapImage: MapImageRow,
        bounds: ScreenBounds,
        configuration: GridConfiguration?,
        unitsPerPixel: Double,
        referenceSystem: ReferenceSystem,
    ): MapPreview {
        val scale = minOf((bounds.width - 16.0) / mapImage.image.width, (bounds.height - 16.0) / mapImage.image.height, 1.0)
        val fittedIcon = MapPreviewFit(
            scale = scale,
            width = mapImage.image.width * scale,
            height = mapImage.image.height * scale,
            x = ((bounds.width - 16.0) - mapImage.image.width * scale) * 0.5,
            y = ((bounds.height - 16.0) - mapImage.image.height * scale) * 0.5,
        )
        val width = fittedIcon.width.roundToInt().coerceAtLeast(1)
        val height = fittedIcon.height.roundToInt().coerceAtLeast(1)
        val scaledImage = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB).also { target ->
            target.createGraphics().let { graphics ->
                graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
                graphics.drawImage(mapImage.image, 0, 0, width, height, null)
                graphics.dispose()
            }
        }
        val gridConfiguration = configuration ?: defaultGridConfiguration(mapImage.image, unitsPerPixel, initialSeed, referenceSystem)
        return MapPreview(
            revision = revision,
            signature = signature,
            bitmap = scaledImage.toBMP32().premultipliedIfRequired(),
            left = fittedIcon.x,
            top = fittedIcon.y,
            width = width,
            height = height,
            gridLayer = GridLayerBuilder.build(
                width = width,
                height = height,
                gridScale = gridConfiguration.referenceSystem.gridScale,
                unitsPerPixel = previewUnitsPerPixel(unitsPerPixel, fittedIcon.scale),
                mapId = gridConfiguration.mapId,
                referenceSystem = gridConfiguration.referenceSystem,
                verticalLineNumberStart = gridConfiguration.startX,
                horizontalLineNumberStart = gridConfiguration.startY,
                colour = GridColourSelector.bestPresetFor(mapImage.image),
            ),
        )
    }

    private fun updatePreviewIfNeeded() {
        if (!mapPreviewDirty) {
            return
        }
        val preview = mapPreviewContainer ?: return
        val bounds = mapPreviewBounds ?: return
        val signature = previewSignature(bounds)
        mapPreviewDirty = false
        renderMapPreview(preview, bounds, mapImage)
    }

    private fun selectCoordinateSystem(selection: CoordinateSystemSelection) {
        if (selection == coordinateSystemSelection) {
            return
        }
        captureInputs()
        val previousGridConfiguration = parseGridConfiguration()
        coordinateSystemSelection = selection
        translatedGridConfiguration(previousGridConfiguration).let { gridConfiguration ->
            coordinateSystemInputs[selection] = currentCoordinateInputs().copy(
                mapIdText = gridConfiguration.mapId,
                gridStartXText = gridConfiguration.formatStartX(),
                gridStartYText = gridConfiguration.formatStartY(),
            )
            coordinateInputFields.remove(selection)
        }
    }

    private fun translatedGridConfiguration(previousGridConfiguration: GridConfiguration?): GridConfiguration {
        val fallback = regeneratedGridConfiguration()
        return previousGridConfiguration
            ?.let { gridConfiguration -> translateGridConfiguration(gridConfiguration, fallback.mapId, referenceSystem()) }
            ?: fallback
    }

    private fun regeneratedGridConfiguration(): GridConfiguration =
        mapImage
            ?.let { image ->
                defaultGridConfiguration(
                    mapImage = image.image,
                    unitsPerPixel = currentUnitsPerPixel() ?: initialScaleInputAsUnitsPerPixel(),
                    seed = initialSeed,
                    referenceSystem = referenceSystem(),
                )
            }
            ?: defaultGridConfiguration(
                width = DEFAULT_GRID_WIDTH,
                height = DEFAULT_GRID_HEIGHT,
                unitsPerPixel = currentUnitsPerPixel() ?: initialScaleInputAsUnitsPerPixel(),
                seed = initialSeed,
                referenceSystem = referenceSystem(),
            )

    private fun previewSignature(bounds: ScreenBounds? = mapPreviewBounds): String =
        currentCoordinateInputs().let { inputs ->
            buildString {
                append(mapImage?.imagePath.orEmpty()).append('|')
                append(coordinateSystemSelection.name).append('|')
                append(inputs.scaleText).append('|')
                append(inputs.mapIdText).append('|')
                append(inputs.gridStartXText).append('|')
                append(inputs.gridStartYText)
                bounds?.let { append('|').append(it.width).append('|').append(it.height) }
            }
        }

    private fun updateDrag(mousePos: Vector2D?, leftHeld: Boolean) {
        val drag = dragState ?: return
        if (!leftHeld || mousePos == null) {
            dragState = null
            return
        }

        val currentIndex = unitRows.indexOfFirst { it.id == drag.rowId }
        if (currentIndex < 0) {
            dragState = null
            return
        }

        val listTop = layout.unitsPanelBounds.top + unitListTop()
        val relativeY = mousePos.y - listTop + unitScrollState.offset
        val targetIndex = (relativeY / unitRowHeight()).toInt().coerceIn(0, unitRows.lastIndex)
        if (targetIndex != currentIndex) {
            unitRows.move(currentIndex, targetIndex)
            dragState = drag.copy(startIndex = targetIndex)
            render(layout.viewportWidth, layout.viewportHeight)
        }
    }

    private fun installNativeFileDropTarget() {
        val component = (root.stage?.views?.gameWindow as? AwtGameWindow)?.frame?.contentPane ?: return
        if (nativeDropTargetComponent === component) {
            return
        }
        nativeDropTargetComponent?.dropTarget = null
        nativeDropTargetComponent = component
        component.dropTarget = DropTarget(component, object : DropTargetAdapter() {
            override fun dragEnter(event: DropTargetDragEvent) {
                if (event.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                    event.acceptDrag(DnDConstants.ACTION_COPY)
                } else {
                    event.rejectDrag()
                }
            }

            override fun drop(event: DropTargetDropEvent) {
                val point = Vector2D(event.location.x.toDouble(), event.location.y.toDouble())
                if (unitIconDropTargetBounds?.contains(point) != true || !event.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                    event.rejectDrop()
                    return
                }
                event.acceptDrop(DnDConstants.ACTION_COPY)
                pendingDroppedUnitIconFiles = event.transferable
                    .getTransferData(DataFlavor.javaFileListFlavor)
                    .let { it as? List<*> }
                    .orEmpty()
                    .filterIsInstance<File>()
                event.dropComplete(pendingDroppedUnitIconFiles.isNotEmpty())
            }
        })
    }

    private fun scheduleNativeFileDropTargetInstall() {
        if (nativeDropTargetInstallScheduled) return
        nativeDropTargetInstallScheduled = true
        EventQueue.invokeLater {
            if (!closed) installNativeFileDropTarget()
        }
    }

    private fun consumePendingMapImageLoads() {
        pendingMapImageLoads.poll()?.fold(
            onSuccess = { image ->
                mapImage = image
                preparedMapPreview = null
                requestedMapPreviewSignature = null
                mapPreviewDirty = true
                render(layout.viewportWidth, layout.viewportHeight)
            },
            onFailure = {
                showStatus("Could not load map image")
            },
        )
    }

    private fun consumePendingMapPreviews() {
        pendingMapPreviews.poll()?.fold(
            onSuccess = { preview ->
                if (mapPreviewRevision.accepts(preview.revision) && preview.signature == previewSignature()) {
                    preparedMapPreview = preview
                    requestedMapPreviewSignature = null
                    mapPreviewContainer?.let { container ->
                        mapPreviewBounds?.let { bounds -> renderMapPreview(container, bounds, mapImage) }
                    }
                }
            },
            onFailure = {
                if (requestedMapPreviewSignature == previewSignature()) {
                    requestedMapPreviewSignature = null
                    showStatus("Could not prepare map preview")
                }
            },
        )
    }

    private fun consumePendingUnitIconLoads() {
        pendingUnitIconLoads.poll()?.let { loads ->
            val rows = loads.mapNotNull { load ->
                load.bitmap?.let { bitmap ->
                    lastUnitIconDirectory = load.file.parentFile
                    UnitRow(
                        id = nextRowId++,
                        name = load.file.nameWithoutExtension,
                        bitmap = bitmap,
                        assetSource = AssetSource.fromFile(load.file),
                    )
                }
            }
            rows.forEach(::upsertUnitRow)
            val failures = loads.count { it.bitmap == null }
            when {
                rows.isNotEmpty() -> showStatus("${rows.size} unit type${if (rows.size == 1) "" else "s"} added")
                failures > 0 -> showStatus("Could not load unit type${if (failures == 1) "" else "s"}")
            }
            if (rows.isNotEmpty()) render(layout.viewportWidth, layout.viewportHeight)
        }
    }

    private fun consumePendingDroppedUnitIconFiles() {
        val files = pendingDroppedUnitIconFiles
        if (files.isEmpty()) {
            return
        }
        pendingDroppedUnitIconFiles = emptyList()
        addUnitTypesFromFiles(files)
        render(layout.viewportWidth, layout.viewportHeight)
    }

    private fun upsertUnitRow(row: UnitRow): Boolean {
        val existingIndex = unitRows.indexOfFirst { it.name == row.name }
        if (existingIndex >= 0) {
            unitRows[existingIndex] = row.copy(id = unitRows[existingIndex].id)
            return false
        } else {
            unitRows += row
            return true
        }
    }

    private fun showStatus(text: String) {
        statusContainer?.removeChildren()
        statusContainer?.let { MapViewerLaunchUi.label(it, text, color = Palette.Colours.RED) }
    }

    private fun renderMapImageValidationIndicator(parent: Container, left: Double, top: Double) {
        val marker = parent.container()
        marker.xy(left, top)
        MapViewerTextInputValidation.validationFailedIndicator(marker).xy(
            (MAP_IMAGE_VALIDATION_WIDTH - MapViewerTextInputValidation.VALIDATION_ICON_SIZE) * 0.5,
            (36.0 - MapViewerTextInputValidation.VALIDATION_ICON_SIZE) * 0.5,
        )
        marker.mouse {
            onOver {
                showMapImageValidationTooltip(marker.globalPos + Vector2D(MAP_IMAGE_VALIDATION_WIDTH, 0.0))
            }
            onOut {
                hideMapImageValidationTooltip()
            }
        }
    }

    private fun showMapImageValidationTooltip(anchor: Vector2D) {
        hideMapImageValidationTooltip()
        mapImageValidationTooltip = MapViewerTextInputValidation.validationExplanationTooltip(
            parent = layer,
            anchor = anchor,
            text = "Choose a map image to start the operation.",
            anchorHeight = 36.0,
        )
    }

    private fun hideMapImageValidationTooltip() {
        mapImageValidationTooltip?.removeFromParent()
        mapImageValidationTooltip = null
    }

    private fun refreshLaunchButton() {
        val container = launchButtonContainer ?: return
        container.removeChildren()
        MapViewerLaunchUi.button(
            parent = container,
            width = 190.0,
            height = 42.0,
            label = "Launch Operation",
            disabled = !isCurrentOperationLaunchable(),
            uppercase = true,
        ) {
            launch()
        }
    }

    private fun isCurrentOperationLaunchable(): Boolean =
        NewOperationDraft(
            operationName = operationName,
            mapImage = mapImage?.let { image ->
                OperationMapImage(
                    name = image.name,
                    imagePath = image.imagePath,
                    image = image.image,
                    assetSource = image.assetSource,
                )
            },
            unitsPerPixel = currentUnitsPerPixel() ?: -1.0,
            referenceSystem = referenceSystem(),
            gridConfiguration = parseGridConfiguration(),
        ).isLaunchable()

    private fun screenTitle(): String =
        "New Operation"

    private fun updateUnitScrollDrag(pointer: Vector2D) {
        val drag = unitScrollDragState ?: return
        if (!unitScrollState.needsScrolling) return
        val scrollbarWidth = MapViewerScrollbar.WIDTH
        val listViewportHeight = (layout.unitsPanelBounds.height - unitListTop() - CONTENT_PADDING).coerceAtLeast(0.0)
        val bounds = ScreenBounds(
            left = layout.unitsPanelBounds.left + (unitScrollbarBounds?.left ?: (CONTENT_PADDING + unitListWidth + FIELD_GAP)),
            top = layout.unitsPanelBounds.top + unitListTop(),
            width = scrollbarWidth,
            height = listViewportHeight,
        )
        unitScrollState = unitScrollState.withOffset(
            MapViewerScrollbar.dragOffset(bounds, unitScrollState, drag.startPointerY, drag.startOffset, pointer.y),
        )
        updateUnitScrollOffset(unitScrollState.offset)
    }

    private fun updateUnitScrollOffset(offset: Double) {
        unitScrollState = unitScrollState.withOffset(offset)
        unitScrollContent?.y = -unitScrollState.offset
        unitScrollbarContainer?.let { scrollbar ->
            scrollbar.removeChildren()
            unitScrollbarBounds?.let { bounds ->
                MapViewerScrollbar.render(scrollbar, bounds, unitScrollState) { pointerY ->
                    unitScrollDragState = ScrollDragState(pointerY, unitScrollState.offset)
                }
            }
        }
    }

    private fun unitRowHeight(): Double =
        ROW_HEIGHT

    private fun unitListTop(): Double =
        unitActionTop() + 60.0

    private fun unitActionTop(): Double =
        if (unitIconDropTargetCollapsed) DROP_TARGET_TOP + UNIT_DROP_HEADER_HEIGHT + FIELD_GAP else DROP_TARGET_TOP + UNIT_DROP_HEADER_HEIGHT + FIELD_GAP + DROP_TARGET_HEIGHT + FIELD_GAP

    private fun scaleLabel(): String =
        when (referenceSystem()) {
            ReferenceSystem.MGRS -> "Pixels Per Metre"
            ReferenceSystem.EPSILON -> "Pixels Per U"
        }

    private fun currentUnitsPerPixel(): Double? =
        currentCoordinateInputs()
            .scaleText
            .toDoubleOrNull()
            ?.takeIf { it > 0.0 }
            ?.let(referenceSystem()::scaleInputToUnitsPerPixel)

    private fun initialScaleInputAsUnitsPerPixel(): Double =
        referenceSystem().scaleInputToUnitsPerPixel(initialUnitsPerPixel)

    private fun referenceSystem(): ReferenceSystem =
        coordinateSystemSelection.referenceSystem

    private fun currentCoordinateInputs(): CoordinateInputState =
        coordinateSystemInputs.getOrPut(coordinateSystemSelection) {
            defaultCoordinateInputs().let { gridConfiguration ->
                CoordinateInputState.from(gridConfiguration, initialUnitsPerPixel.toString())
            }
        }

    private fun defaultCoordinateInputs(): GridConfiguration =
        mapImage
            ?.let { image ->
                defaultGridConfiguration(
                    mapImage = image.image,
                    unitsPerPixel = initialScaleInputAsUnitsPerPixel(),
                    seed = initialSeed,
                    referenceSystem = referenceSystem(),
                )
            }
            ?: defaultGridConfiguration(
                width = DEFAULT_GRID_WIDTH,
                height = DEFAULT_GRID_HEIGHT,
                unitsPerPixel = initialScaleInputAsUnitsPerPixel(),
                seed = initialSeed,
                referenceSystem = referenceSystem(),
            )

    private fun textInput(
        parent: Container,
        text: String,
        bounds: ScreenBounds,
        frameStyle: (Boolean) -> UiFrameStyle = MapViewerUiBoxes::defaultStyle,
    ): io.hsar.mapgenerator.commandgui.korge.ui.MapViewerInputField =
        MapViewerTextInputs.input(parent, text, bounds.width, bounds.height, bounds.left, bounds.top, frameStyle = frameStyle)

    private fun focusOperationNameIfPending() {
        if (pendingOperationNameFocus) {
            operationNameInput?.focus()
            pendingOperationNameFocus = false
        }
    }

    private fun wireOperationNameValidation(input: io.hsar.mapgenerator.commandgui.korge.ui.MapViewerInputField?) {
        input ?: return
        input.controller.onTextUpdated.add {
            val wasValid = operationNameWasValid
            captureInputs()
            operationNameWasValid = MapViewerTextInputValidation.isPresent(operationName)
            refreshLaunchButton()
            if (operationNameWasValid != wasValid) {
                pendingOperationNameFocus = true
                render(layout.viewportWidth, layout.viewportHeight)
            }
        }
    }

    private fun wirePreviewRefresh(input: io.hsar.mapgenerator.commandgui.korge.ui.MapViewerInputField?) {
        input ?: return
        input.controller.onTextUpdated.add {
            captureInputs()
            mapPreviewDirty = true
            refreshLaunchButton()
        }
    }

    private fun chooseRasterImageFile(): File? =
        chooseImageFile("Map image files", null, "png", "jpg", "jpeg", "gif", "bmp", "webp")

    private fun chooseUnitImageFile(): File? =
        chooseImageFile("Unit image files", lastUnitIconDirectory, "png", "jpg", "jpeg", "gif", "bmp", "webp", "svg")

    private fun chooseImageFile(description: String, initialDirectory: File? = null, vararg extensions: String): File? =
        JFileChooser().apply {
            isAcceptAllFileFilterUsed = true
            fileFilter = FileNameExtensionFilter(description, *extensions)
            initialDirectory?.let { currentDirectory = it }
        }.let { chooser ->
            if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) chooser.selectedFile else null
        }

    private fun MapViewerUnitTemplate.toUnitRow(): UnitRow =
        UnitRow(
            id = nextRowId++,
            name = name,
            bitmap = bitmap,
            assetSource = assetSource,
            affiliation = affiliation,
            customProperties = customProperties,
        )

    private fun chooseSaveFile(): File? =
        OperationSaveWriter().let { writer ->
            writer.ensureDefaultStorage()
            JFileChooser(writer.defaultSaveDirectory().toFile()).apply {
                isAcceptAllFileFilterUsed = true
                fileFilter = FileNameExtensionFilter("Operational Command Tool saves", "zip")
            }.let { chooser ->
                if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) chooser.selectedFile else null
            }
        }

    private data class MapImageRow(
        val name: String,
        val imagePath: String,
        val image: BufferedImage,
        val assetSource: AssetSource?,
    )

    private data class MapPreviewFit(
        val scale: Double,
        val x: Double,
        val y: Double,
        val width: Double,
        val height: Double,
    )

    private data class MapPreview(
        val revision: Long,
        val signature: String,
        val bitmap: Bitmap32,
        val left: Double,
        val top: Double,
        val width: Int,
        val height: Int,
        val gridLayer: GridLayer,
    )

    private data class UnitIconLoad(
        val file: File,
        val bitmap: Bitmap32?,
    )

    private data class UnitRow(
        override val id: Long,
        override var name: String,
        var bitmap: Bitmap32,
        var assetSource: AssetSource?,
        var affiliation: UnitAffiliation? = null,
        var customProperties: Map<String, String> = emptyMap(),
        var input: io.hsar.mapgenerator.commandgui.korge.ui.MapViewerInputField? = null,
    ) : ReorderRow

    private data class UnitAffiliationGroup(
        val affiliation: UnitAffiliation?,
        val label: String,
    )

    private data class ReorderDragState(
        val rowId: Long,
        val startIndex: Int,
    )

    private data class ScrollDragState(
        val startPointerY: Double,
        val startOffset: Double,
    )

    private data class CoordinateInputState(
        val mapIdText: String,
        val gridStartXText: String,
        val gridStartYText: String,
        val scaleText: String,
    ) {
        companion object {
            fun from(gridConfiguration: GridConfiguration, scaleText: String): CoordinateInputState =
                CoordinateInputState(
                    mapIdText = gridConfiguration.mapId,
                    gridStartXText = gridConfiguration.formatStartX(),
                    gridStartYText = gridConfiguration.formatStartY(),
                    scaleText = scaleText,
                )
        }
    }

    private data class CoordinateInputFields(
        val mapId: io.hsar.mapgenerator.commandgui.korge.ui.MapViewerInputField,
        val gridStartX: io.hsar.mapgenerator.commandgui.korge.ui.MapViewerInputField,
        val gridStartY: io.hsar.mapgenerator.commandgui.korge.ui.MapViewerInputField,
        val scale: io.hsar.mapgenerator.commandgui.korge.ui.MapViewerInputField,
    )

    private interface ReorderRow {
        val id: Long
        var name: String
    }

    private enum class CoordinateSystemSelection(
        val label: String,
        val referenceSystem: ReferenceSystem,
    ) {
        MGRS("MGRS", ReferenceSystem.MGRS),
        EPSILON("Epsilon", ReferenceSystem.EPSILON),
    }

    private companion object {
        const val Z_INDEX = 60_000.0
        const val CONTENT_PADDING = 24.0
        const val DROP_TARGET_TOP = 72.0
        const val DROP_TARGET_HEIGHT = 64.0
        const val LIST_TOP = 208.0
        const val ROW_HEIGHT = 44.0
        const val IMPORT_BATCH_SIZE = 8
        const val UNIT_GROUP_HEADER_HEIGHT = 36.0
        const val UNIT_GROUP_GAP = 8.0
        const val UNIT_DROP_HEADER_HEIGHT = 36.0
        const val PREVIEW_WIDTH = 64.0
        const val PREVIEW_HEIGHT = 36.0
        const val PREVIEW_LEFT = 0.0
        const val INPUT_HEIGHT = 40.0
        const val COORDINATE_LABEL_TOP = 144.0
        const val HANDLE_WIDTH = 28.0
        const val HANDLE_HEIGHT = 28.0
        const val FIELD_GAP = 12.0
        const val MAP_IMAGE_VALIDATION_WIDTH = 24.0
        const val DEFAULT_GRID_WIDTH = 1_000
        const val DEFAULT_GRID_HEIGHT = 1_000
        const val BRACKET_INSET = 5.0
        val UNIT_AFFILIATION_GROUPS = listOf(
            UnitAffiliationGroup(UnitAffiliation.Friend, "Friend"),
            UnitAffiliationGroup(UnitAffiliation.Hostile, "Hostile"),
            UnitAffiliationGroup(UnitAffiliation.Neutral, "Neutral"),
            UnitAffiliationGroup(UnitAffiliation.Unknown, "Unknown"),
            UnitAffiliationGroup(null, "Unaffiliated"),
        )
        private fun <T> MutableList<T>.move(fromIndex: Int, toIndex: Int) {
            if (fromIndex == toIndex) return
            val item = removeAt(fromIndex)
            add(if (toIndex > fromIndex) toIndex - 1 else toIndex, item)
        }
    }
}

internal fun List<LedgerEntry>.latestUnitTemplates(): List<UnitTemplateSnapshot> {
    val snapshots = mutableMapOf<UnitTemplateId, UnitTemplateSnapshot>()
    val order = mutableListOf<UnitTemplateId>()
    fun upsert(snapshot: UnitTemplateSnapshot) {
        order.remove(snapshot.id)
        order.add(snapshot.index.coerceIn(0, order.size), snapshot.id)
        snapshots[snapshot.id] = snapshot
    }

    fun remove(id: UnitTemplateId) {
        order.remove(id)
        snapshots.remove(id)
    }

    fun apply(entry: LedgerEntry) {
        when (entry) {
            is BatchLedgerEntry -> entry.entries.forEach(::apply)
            is CreateUnitTemplateAction -> upsert(entry.snapshot)
            is DeleteUnitTemplateAction -> remove(entry.snapshot.id)
            is EditUnitTemplateAction -> entry.afterSnapshot?.let(::upsert)
        }
    }
    forEach(::apply)
    return order.mapNotNull(snapshots::get)
}

private fun GridConfiguration.formatStartX(): String =
    referenceSystem.formatCoord(startX, CoordAxis.X, referenceSystem.gridScale)

private fun GridConfiguration.formatStartY(): String =
    referenceSystem.formatCoord(startY, CoordAxis.Y, referenceSystem.gridScale)

internal class MapViewerAsyncRevision {
    private var current = 0L

    fun next(): Long = ++current

    fun accepts(revision: Long): Boolean = revision == current
}

internal data class MapViewerNewOperationLayout(
    val viewportWidth: Int,
    val viewportHeight: Int,
    val headerBounds: ScreenBounds,
    val screenHeaderBounds: ScreenBounds,
    val titleBounds: ScreenBounds,
    val operationNameBounds: ScreenBounds,
    val mapPanelBounds: ScreenBounds,
    val compactDetailsPanelBounds: ScreenBounds,
    val unitsPanelBounds: ScreenBounds,
    val panelFocusButtonBounds: ScreenBounds,
    val backButtonBounds: ScreenBounds,
    val launchButtonBounds: ScreenBounds,
)

internal enum class MapViewerNewOperationPanelFocus {
    OPERATIONAL_DETAILS,
    UNIT_TYPES,
}

internal object MapViewerNewOperationLayoutBuilder {
    fun build(
        viewportWidth: Int,
        viewportHeight: Int,
        panelFocus: MapViewerNewOperationPanelFocus = MapViewerNewOperationPanelFocus.OPERATIONAL_DETAILS,
    ): MapViewerNewOperationLayout {
        val margin = 36.0
        val top = 20.0
        val shellTop = 108.0
        val shellHeight = (viewportHeight - shellTop - margin).coerceAtLeast(560.0)
        val shellWidth = minOf(viewportWidth * 0.875, viewportWidth - margin * 2.0)
        val shellLeft = (viewportWidth - shellWidth) * 0.5
        val shellPadding = 24.0
        val shellContentTop = shellTop + 86.0
        val shellContentHeight = shellHeight - 86.0 - shellPadding
        val sidebarWidth = 300.0
        val columnGap = 32.0
        val detailsWidth = shellWidth - (shellPadding * 2.0) - sidebarWidth - columnGap
        val detailsLeft = shellLeft + shellPadding
        val sidebarLeft = detailsLeft + detailsWidth + columnGap
        val unitsBounds = when (panelFocus) {
            MapViewerNewOperationPanelFocus.OPERATIONAL_DETAILS -> ScreenBounds(sidebarLeft, shellContentTop, sidebarWidth, shellContentHeight)
            MapViewerNewOperationPanelFocus.UNIT_TYPES -> ScreenBounds(sidebarLeft + sidebarWidth - detailsWidth, shellContentTop, detailsWidth, shellContentHeight)
        }
        val focusButtonLeft = when (panelFocus) {
            MapViewerNewOperationPanelFocus.OPERATIONAL_DETAILS -> detailsLeft + detailsWidth + (columnGap - PANEL_FOCUS_BUTTON_WIDTH) * 0.5
            MapViewerNewOperationPanelFocus.UNIT_TYPES -> unitsBounds.left - (columnGap + PANEL_FOCUS_BUTTON_WIDTH) * 0.5
        }
        return MapViewerNewOperationLayout(
            viewportWidth = viewportWidth,
            viewportHeight = viewportHeight,
            headerBounds = MapViewerLaunchShellLayoutBuilder.build(viewportWidth).headerBounds,
            screenHeaderBounds = ScreenBounds(shellLeft, shellTop, shellWidth, shellHeight),
            titleBounds = ScreenBounds(shellLeft, shellTop, shellWidth, 56.0),
            operationNameBounds = ScreenBounds(20.0, 62.0, detailsWidth - 40.0, 38.0),
            mapPanelBounds = ScreenBounds(detailsLeft, shellContentTop, detailsWidth, shellContentHeight),
            compactDetailsPanelBounds = ScreenBounds(detailsLeft, shellContentTop, sidebarWidth, shellContentHeight),
            unitsPanelBounds = unitsBounds,
            panelFocusButtonBounds = ScreenBounds(
                focusButtonLeft,
                shellContentTop,
                PANEL_FOCUS_BUTTON_WIDTH,
                shellContentHeight,
            ),
            backButtonBounds = ScreenBounds(shellLeft + shellPadding, shellTop + shellPadding, 120.0, 42.0),
            launchButtonBounds = ScreenBounds(shellLeft + shellWidth - shellPadding - 190.0, shellTop + shellPadding, 190.0, 42.0),
        )
    }

    private const val PANEL_FOCUS_BUTTON_WIDTH = 18.0
}

internal data class MapViewerNewOperationCoordinateRowLayout(
    val coordinateSystem: MapViewerNewOperationFieldLayout,
    val mapId: MapViewerNewOperationFieldLayout,
    val startX: MapViewerNewOperationFieldLayout,
    val startY: MapViewerNewOperationFieldLayout,
    val scale: MapViewerNewOperationFieldLayout,
) {
    val fields: List<MapViewerNewOperationFieldLayout>
        get() = listOf(coordinateSystem, mapId, startX, startY, scale)
}

internal data class MapViewerNewOperationFieldLayout(
    val labelBounds: ScreenBounds,
    val inputBounds: ScreenBounds,
)

internal object MapViewerNewOperationCoordinateRowLayoutBuilder {
    fun build(
        panelWidth: Double,
        labelTop: Double,
        inputHeight: Double,
        leftPadding: Double = 24.0,
        fieldGap: Double = MapViewerDialogSpacing.FIELD_GAP,
    ): MapViewerNewOperationCoordinateRowLayout {
        val fieldWidth = (panelWidth - (leftPadding * 2.0) - (fieldGap * 4.0)) / 5.0
        val inputTop = MapViewerDialogSpacing.inputTop(labelTop)
        fun field(index: Int): MapViewerNewOperationFieldLayout {
            val left = leftPadding + ((fieldWidth + fieldGap) * index)
            return MapViewerNewOperationFieldLayout(
                labelBounds = ScreenBounds(
                    left = left,
                    top = labelTop,
                    width = fieldWidth,
                    height = inputTop - labelTop,
                ),
                inputBounds = ScreenBounds(
                    left = left,
                    top = inputTop,
                    width = fieldWidth,
                    height = inputHeight,
                ),
            )
        }

        return MapViewerNewOperationCoordinateRowLayout(
            coordinateSystem = field(0),
            mapId = field(1),
            startX = field(2),
            startY = field(3),
            scale = field(4),
        )
    }
}
