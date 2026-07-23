package io.hsar.mapgenerator.commandgui.korge.ui

import io.hsar.mapgenerator.commandgui.korge.interaction.CameraViewport
import io.hsar.mapgenerator.commandgui.korge.interaction.MapCoordinateSpace
import io.hsar.mapgenerator.commandgui.korge.interaction.MapNotePatch
import io.hsar.mapgenerator.commandgui.korge.interaction.MapNoteSnapshot
import io.hsar.mapgenerator.commandgui.korge.interaction.MapPlacedObjectId
import io.hsar.mapgenerator.commandgui.korge.interaction.MapPlacedObjectSnapshot
import io.hsar.mapgenerator.commandgui.save.OperationSaveJson
import io.hsar.mapgenerator.image.Palette
import korlibs.image.color.RGBA
import korlibs.image.text.TextAlignment
import korlibs.korge.annotations.KorgeExperimental
import korlibs.korge.view.Container
import korlibs.korge.view.container
import korlibs.korge.view.solidRect
import korlibs.korge.view.xy
import java.util.*

@OptIn(KorgeExperimental::class)
internal class MapViewerNoteDialog(
    private val root: Container,
    private val dialogManager: MapViewerDialogManager,
    private val note: MapNoteSnapshot,
    private val readOnly: () -> Boolean,
    private val objectSnapshot: (MapPlacedObjectId) -> MapPlacedObjectSnapshot?,
    private val viewportProvider: () -> CameraViewport,
    private val coordinateSpace: MapCoordinateSpace,
    private val onSaved: (MapNoteSnapshot, MapNoteSnapshot) -> Unit,
    private val onDeleted: (MapNoteSnapshot) -> Unit,
    private val onClosed: (MapViewerNoteDialog) -> Unit = {},
    private val restored: MapViewerPersistedDialog? = null,
) {
    private var current = note
    private var titleValue = note.title
    private var bodyValue = note.body
    private var referencedObjectIds = note.referencedObjectIds.toMutableSet()
    private var titleInput: MapViewerInputField? = null
    private var bodyInput: MapViewerInputField? = null
    private var window: MapViewerDialogWindow? = null
    private var referenceDropTargetBounds: ScreenBounds? = null
    private var referencesExpanded = false

    init {
        restored?.draft?.let { draft ->
            titleValue = draft.path("title").textValue() ?: titleValue
            bodyValue = draft.path("body").textValue() ?: bodyValue
            val references = draft.path("references").mapNotNull { value ->
                value.textValue()?.let { runCatching { MapPlacedObjectId(UUID.fromString(it)) }.getOrNull() }
            }
            if (references.isNotEmpty()) referencedObjectIds = references.toMutableSet()
            referencesExpanded = draft.path("referencesExpanded").booleanValue()
        }
    }

    val isTextInputActive: Boolean
        get() = titleInput?.isFocused == true || bodyInput?.isFocused == true

    fun open(): Boolean {
        if (dialogManager.hasTarget(note.id)) {
            return false
        }
        val stage = root.stage ?: return false
        val focusTargetBoundsProvider = {
            referencedObjectFocusTargetBounds(
                referencedObjectIds = referencedObjectIds,
                objectSnapshot = objectSnapshot,
                coordinateSpace = coordinateSpace,
                viewport = viewportProvider(),
            )
        }
        window = MapViewerDialogWindow(
            root = root,
            title = "Note",
            titleProvider = { noteTitle(titleValue) },
            preferredWidth = PREFERRED_WIDTH,
            preferredHeight = PREFERRED_HEIGHT,
            contentMinimumWidth = { CONTENT_MINIMUM_WIDTH },
            contentHeight = ::contentHeight,
            actions = listOf(
                MapViewerDialogAction("Save", enabled = { !readOnly() }) { saveAndClose() },
                MapViewerDialogAction("Delete", enabled = { !readOnly() }) { deleteAndClose() },
                MapViewerDialogAction("Cancel") { close() },
            ),
            onClose = ::close,
            renderContent = ::renderContent,
            focusTargetBoundsProvider = { focusTargetBoundsProvider() },
            persistence = MapViewerDialogPersistence(
                type = TYPE,
                targetId = note.id.value.toString(),
                restored = restored,
                draft = {
                    OperationSaveJson.objectMapper.createObjectNode().apply {
                        put("title", titleValue)
                        put("body", bodyValue)
                        put("referencesExpanded", referencesExpanded)
                        putArray("references").also { ids -> referencedObjectIds.forEach { ids.add(it.value.toString()) } }
                    }
                },
            ),
            mode = MapViewerDialogMode.FLOATING,
            dialogManager = dialogManager,
        )
        dialogManager.register(
            owner = this,
            targetKey = note.id,
            blocksScreenPoint = { point -> window?.blocksScreenPoint(point) == true },
            isTextInputActive = { isTextInputActive },
            persistedDialog = { window?.persistedDialog() },
            close = ::close,
        )
        window?.show(stage.views.actualVirtualWidth, stage.views.actualVirtualHeight)
        return true
    }

    private fun renderContent(parent: Container, itemWidth: Double) {
        titleInput = null
        bodyInput = null
        val inlineBlock = MapViewerInlineInputs.block(listOf("Title"))
        titleInput = inlineBlock.text(
            parent = parent,
            label = "Title",
            text = titleValue,
            x = 0.0,
            y = 0.0,
            width = itemWidth,
            inputMinimumWidth = 220.0,
        ).also { input ->
            input.onClick { input.focus() }
            input.controller.onTextUpdated.add {
                titleValue = input.text
            }
        }

        val bodyTop = TITLE_ROW_HEIGHT + SECTION_GAP + referencesHeight() + SECTION_GAP
        renderReferenceControls(parent, itemWidth, TITLE_ROW_HEIGHT + SECTION_GAP)

        MapViewerTextViews.leftBox(
            parent = parent,
            text = "Body",
            width = itemWidth,
            height = LABEL_HEIGHT,
            color = Palette.Colours.WHITE,
        ) {
            y = bodyTop
        }
        bodyInput = MapViewerTextInputs.input(
            parent = parent,
            text = bodyValue,
            width = itemWidth,
            height = BODY_HEIGHT,
            y = bodyTop + LABEL_HEIGHT,
            textSize = MapViewerTextViews.DEFAULT_TEXT_SIZE - 2.0,
            alignment = TextAlignment.TOP_LEFT,
            wrapWords = true,
            selectEndWhenClickBelowText = true,
        ).also { input ->
            input.onClick { input.focus() }
            input.controller.onTextUpdated.add {
                bodyValue = input.text
            }
            input.controller.onReturnPressed.add {
                it.insertText("\n")
            }
        }
    }

    fun addReferencesAt(point: korlibs.math.geom.Vector2D, objectIds: Set<MapPlacedObjectId>): Boolean {
        if (readOnly() || objectIds.isEmpty() || referenceDropTargetBounds?.contains(point) != true) {
            return false
        }

        val updatedIds = referencedObjectIds + objectIds
        if (updatedIds != referencedObjectIds) {
            applyReferenceEdit(updatedIds)
        }
        window?.refresh()
        return true
    }

    private fun renderReferenceControls(parent: Container, itemWidth: Double, top: Double) {
        renderReferencesHeader(parent, itemWidth, top)
        referenceDropTargetBounds = null
        if (!referencesExpanded) {
            return
        }

        val dropTarget = parent.container()
        dropTarget.xy(0.0, top + CONTROL_HEIGHT + ROW_GAP)
        renderReferenceDropTarget(dropTarget, itemWidth, DROP_TARGET_HEIGHT)
        referenceDropTargetBounds = dropTarget.getGlobalBounds().let { bounds ->
            ScreenBounds(
                left = bounds.left,
                top = bounds.top,
                width = bounds.width,
                height = bounds.height,
            )
        }
        referencedObjectIds.toList().forEachIndexed { index, id ->
            renderReferenceRow(
                parent = parent,
                id = id,
                top = top + CONTROL_HEIGHT + ROW_GAP + DROP_TARGET_HEIGHT + ROW_GAP + index * (ROW_HEIGHT + ROW_GAP),
                width = itemWidth,
            )
        }
    }

    private fun renderReferencesHeader(parent: Container, itemWidth: Double, top: Double) {
        MapViewerCollapsiblePanel.header(
            parent = parent,
            width = itemWidth,
            height = CONTROL_HEIGHT,
            label = MapViewerCountText.of(referencedObjectIds.size, "reference"),
            expanded = referencesExpanded,
        ) {
            referencesExpanded = !referencesExpanded
            window?.refresh()
        }.xy(0.0, top)
    }

    private fun renderReferenceDropTarget(parent: Container, width: Double, height: Double) {
        parent.solidRect(width, height, korlibs.image.color.RGBA(80, 80, 80, 190))
        parent.solidRect(width - 2.0, height - 2.0, korlibs.image.color.RGBA(45, 45, 45, 210)) {
            x = 1.0
            y = 1.0
        }
        MapViewerCornerBrackets.renderAll(parent, width, height, BRACKET_INSET)
        MapViewerTextViews.box(
            parent = parent,
            text = "Drop selected map objects here",
            width = width,
            height = height,
            size = 20.0,
            color = Palette.Colours.LIGHT,
        )
    }

    private fun renderReferenceRow(parent: Container, id: MapPlacedObjectId, top: Double, width: Double) {
        val row = parent.container()
        row.xy(0.0, top)
        MapViewerUiBoxes.box(row, width, ROW_HEIGHT)
        objectSnapshot(id)?.let { snapshot ->
            val iconFrame = row.container()
            iconFrame.xy(ROW_INSET, (ROW_HEIGHT - ICON_SIZE) * 0.5)
            iconFrame.solidRect(ICON_SIZE, ICON_SIZE, RGBA(0, 0, 0, 0))
            val fittedIcon = MapViewerIconFit.inside(snapshot.icon, ICON_SIZE, ICON_SIZE)
            MapViewerImageViews.icon(iconFrame, snapshot.icon) {
                scale = fittedIcon.scale
                x = fittedIcon.x
                y = fittedIcon.y
            }
        }
        MapViewerTextViews.leftBox(
            parent = row,
            text = objectLabel(id),
            width = width - REMOVE_WIDTH - BUTTON_GAP - ROW_INSET * 3.0 - ICON_SIZE,
            height = ROW_HEIGHT,
            color = Palette.Colours.WHITE,
        ) {
            x = ROW_INSET * 2.0 + ICON_SIZE
        }
        val removeButton = row.container()
        removeButton.xy(width - REMOVE_WIDTH - ROW_INSET, (ROW_HEIGHT - REMOVE_HEIGHT) * 0.5)
        MapViewerActionButtons.button(
            parent = removeButton,
            width = REMOVE_WIDTH,
            height = REMOVE_HEIGHT,
            label = "Remove",
            enabled = !readOnly(),
        ) {
            applyReferenceEdit(referencedObjectIds - id)
        }
    }

    private fun objectLabel(id: MapPlacedObjectId): String =
        objectSnapshot(id)
            ?.name
            ?: "Missing object"

    private fun applyReferenceEdit(updatedIds: Set<MapPlacedObjectId>) {
        if (updatedIds == current.referencedObjectIds) {
            return
        }
        val updated = current.copy(referencedObjectIds = updatedIds)
        onSaved(current, updated)
        current = updated
        referencedObjectIds = updatedIds.toMutableSet()
        window?.refresh()
    }

    private fun saveAndClose() {
        val updated = current.copy(
            title = titleValue.ifBlank { "Untitled Note" },
            body = bodyValue,
            referencedObjectIds = referencedObjectIds.toSet(),
        )
        if (MapNotePatch.diff(current, updated).isNotEmpty()) {
            onSaved(current, updated)
            current = updated
        }
        close()
    }

    private fun deleteAndClose() {
        onDeleted(current)
        close()
    }

    private fun close() {
        dialogManager.unregister(this)
        window?.close()
        window = null
        onClosed(this)
    }

    private fun contentHeight(width: Double): Double =
        TITLE_ROW_HEIGHT +
                SECTION_GAP +
                referencesHeight() +
                SECTION_GAP +
                LABEL_HEIGHT +
                BODY_HEIGHT

    private fun referencesHeight(): Double =
        CONTROL_HEIGHT +
                if (!referencesExpanded) {
                    0.0
                } else {
                    ROW_GAP +
                            DROP_TARGET_HEIGHT +
                            ROW_GAP +
                            if (referencedObjectIds.isEmpty()) {
                                0.0
                            } else {
                                referencedObjectIds.size * (ROW_HEIGHT + ROW_GAP)
                            }
                }

    private fun noteTitle(title: String): String =
        "Note - ${title.ifBlank { "Untitled Note" }}"

    private companion object {
        const val TYPE = "note"
        const val PREFERRED_WIDTH = 700.0
        const val PREFERRED_HEIGHT = 720.0
        const val CONTENT_MINIMUM_WIDTH = 420.0
        const val TITLE_ROW_HEIGHT = MapViewerDialogSpacing.INPUT_HEIGHT
        const val BODY_HEIGHT = 132.0
        const val DROP_TARGET_HEIGHT = 72.0
        const val LABEL_HEIGHT = 28.0
        const val SECTION_GAP = 14.0
        const val CONTROL_HEIGHT = 38.0
        const val BUTTON_GAP = 12.0
        const val ROW_HEIGHT = 44.0
        const val ROW_GAP = 8.0
        const val ROW_INSET = 8.0
        const val ICON_SIZE = 32.0
        const val REMOVE_WIDTH = 92.0
        const val REMOVE_HEIGHT = 30.0
        const val BRACKET_INSET = 5.0
    }
}
