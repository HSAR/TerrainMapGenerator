package io.hsar.mapgenerator.commandgui.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.focusable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.Modifier
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import io.hsar.mapgenerator.commandgui.MapViewerCommand
import io.hsar.mapgenerator.commandgui.MapViewerCommandItem
import io.hsar.mapgenerator.commandgui.MapViewerDisplaySettings
import io.hsar.mapgenerator.commandgui.MapViewerPanel
import io.hsar.mapgenerator.commandgui.MapViewerRenderData
import io.hsar.mapgenerator.commandgui.MapViewerSettings
import io.hsar.mapgenerator.commandgui.MapViewerSaveCoordinatorState
import io.hsar.mapgenerator.commandgui.operation.OperationDocument
import io.hsar.mapgenerator.commandgui.operation.OperationDockRegion
import io.hsar.mapgenerator.commandgui.operation.OperationEditorDraft
import io.hsar.mapgenerator.commandgui.operation.OperationNoteDraft
import io.hsar.mapgenerator.commandgui.operation.OperationObjectId
import io.hsar.mapgenerator.commandgui.operation.OperationTimeline
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection
import java.awt.image.BufferedImage
import java.time.Instant
import java.util.UUID

@OptIn(ExperimentalTestApi::class, ExperimentalComposeUiApi::class)
class Phase4ClosureUiTest {
    @Test
    fun `restored docks focus active tabs cycle and move through the overflow menu`() = runComposeUiTest {
        var state by mutableStateOf(
            ComposeOperationState(document())
                .togglePanel(MapViewerPanel.GAME)
                .togglePanel(MapViewerPanel.NOTES)
                .togglePanel(MapViewerPanel.GRAPHICS)
                .togglePanel(MapViewerPanel.UNITS)
                .let { opened -> opened.dockEditor(opened.workspace.activeEditorId!!, OperationDockRegion.BOTTOM) },
        )
        val noDrag: (io.hsar.mapgenerator.commandgui.operation.OperationEditorId?) -> Unit = {}
        val editorActions = ComposeViewerEditorActions(
            displaySettings = MapViewerDisplaySettings(),
            saveState = MapViewerSaveCoordinatorState.initial(state.saveSnapshot()),
            onCommand = {},
            onSave = {},
            onEditDrawingName = {},
            onDestroyedUnitDisplayModeChanged = {},
            onUnitIconSizeChanged = {},
            onFireArcDisplayModeChanged = {},
        )

        setContent {
            NordTheme {
                CompositionLocalProvider(LocalViewerEditorActions provides editorActions) {
                    Column(Modifier.size(1200.dp, 800.dp).onKeyEvent { event ->
                        dockCycleDirection(event, state.workspaceMode)?.let { direction ->
                            state = state.cycleDockTabs(direction)
                            true
                        } ?: false
                    }) {
                        Row(Modifier.height(560.dp)) {
                            DockedWorkspace(state, OperationDockRegion.LEFT, null, null, noDrag, {}, {}, { state = it })
                            Box(Modifier.weight(1f))
                            DockedWorkspace(state, OperationDockRegion.RIGHT, null, null, noDrag, {}, {}, { state = it })
                        }
                        DockedWorkspace(state, OperationDockRegion.BOTTOM, null, null, noDrag, {}, {}, { state = it })
                    }
                }
            }
        }

        onNodeWithTag("dock-left").fetchSemanticsNode()
        onNodeWithTag("dock-right").fetchSemanticsNode()
        onNodeWithTag("dock-bottom").fetchSemanticsNode()
        val initialLeftSize = state.workspace.dockRegions.single { it.region == OperationDockRegion.LEFT }.size
        onNodeWithTag("dock-left-resize").performTouchInput {
            down(center)
            moveBy(Offset(40f, 0f), delayMillis = 300)
            up()
        }
        runOnIdle {
            assertThat(state.workspace.dockRegions.single { it.region == OperationDockRegion.LEFT }.size > initialLeftSize, equalTo(true))
        }
        onNodeWithTag("dock-tab-unit-types").assertIsFocused()
        onNodeWithTag("dock-tab-operation-notes").performClick().assertIsFocused()
        onNodeWithTag("dock-tab-operation-notes").performKeyInput {
            keyDown(Key.CtrlLeft)
            pressKey(Key.Tab)
            keyUp(Key.CtrlLeft)
        }
        onNodeWithTag("dock-tab-game").assertIsFocused()
        onNodeWithTag("dock-tab-game").performKeyInput {
            keyDown(Key.CtrlLeft)
            keyDown(Key.ShiftLeft)
            pressKey(Key.Tab)
            keyUp(Key.ShiftLeft)
            keyUp(Key.CtrlLeft)
        }
        onNodeWithTag("dock-tab-operation-notes").assertIsFocused()

        onNodeWithTag("dock-bottom-collapse").performClick()
        runOnIdle {
            assertThat(state.workspace.dockRegions.single { it.region == OperationDockRegion.BOTTOM }.collapsed, equalTo(true))
        }
        onNodeWithTag("dock-tab-overflow-operation-notes").performClick()
    }

    @Test
    fun `dock overflow menu action moves the tab across regions`() = runComposeUiTest {
        var state by mutableStateOf(ComposeOperationState(document()).togglePanel(MapViewerPanel.NOTES))
        setContent {
            NordTheme {
                val editor = state.workspace.editor(io.hsar.mapgenerator.commandgui.operation.OperationWorkspaceState.DEFAULT_NOTES_ID)!!
                val dock = state.workspace.dockContaining(editor.id)!!
                Column {
                    DockTabOverflowItems(editor, dock, 0, state, {}, { state = it })
                }
            }
        }

        onNodeWithTag("dock-tab-move-operation-notes-right").performClick()

        runOnIdle {
            assertThat(
                state.workspace.dockContaining(io.hsar.mapgenerator.commandgui.operation.OperationWorkspaceState.DEFAULT_NOTES_ID)?.region,
                equalTo(OperationDockRegion.RIGHT),
            )
        }
    }

    @Test
    fun `context menu owns keyboard navigation through a submenu`() = runComposeUiTest {
        val selected = mutableListOf<String>()
        val child = MapViewerCommandItem("Open Note", command = MapViewerCommand.CloseEscapeMenu)
        val menu = ComposeMapContextMenu(
            OperationObjectId(UUID(0, 4)),
            "Example",
            IntOffset(20, 20),
            listOf(MapViewerCommandItem("Notes", children = listOf(child))),
        )
        setContent {
            NordTheme {
                Box(Modifier.size(600.dp, 400.dp)) {
                    ComposeMapContextMenu(menu, { selected += it.label }, {})
                }
            }
        }

        onNodeWithTag("viewer-context-menu").assertIsFocused().performKeyInput {
            pressKey(Key.DirectionRight)
            pressKey(Key.Enter)
        }

        assertThat(selected, equalTo(listOf("Open Note")))
    }

    @Test
    fun `note text retains focus multiline clipboard shortcuts and committed IME text`() = runComposeUiTest {
        val clipboard = TestClipboard()
        var state by mutableStateOf(ComposeOperationState(document()).openNewNoteEditor())
        val editor = state.workspace.editors.single()
        var bubbledShortcuts = 0
        setContent {
            CompositionLocalProvider(LocalClipboard provides clipboard) {
                NordTheme {
                    Box(
                        Modifier.size(800.dp, 720.dp).focusable().onKeyEvent { event ->
                            if (event.type == KeyEventType.KeyDown && (
                                    event.key == Key.Delete || event.isCtrlPressed && event.key in setOf(Key.A, Key.C, Key.X, Key.V, Key.Z, Key.Y)
                                )) bubbledShortcuts++
                            true
                        },
                    ) {
                        val current = state.workspace.editor(editor.id)!!
                        NoteEditorContent(
                            current.id,
                            (current.draft as OperationEditorDraft.Note).value,
                            state,
                            { state = it },
                            Modifier.fillMaxSize(),
                        )
                    }
                }
            }
        }

        onNodeWithTag("note-title").performClick().assertIsFocused().performTextInput("題名")
        onNodeWithTag("note-body").performClick().assertIsFocused().performTextInput("Line one\n日本語かな")
        onNodeWithTag("note-body").assertTextContains("Line one\n日本語かな")
        onNodeWithTag("note-body").performKeyInput {
            keyDown(Key.CtrlLeft)
            pressKey(Key.A)
            pressKey(Key.C)
            keyUp(Key.CtrlLeft)
        }
        assertThat(clipboard.text(), equalTo("Line one\n日本語かな"))

        clipboard.content = ClipEntry(StringSelection("Pasted\nΔ"))
        onNodeWithTag("note-body").performKeyInput {
            keyDown(Key.CtrlLeft)
            pressKey(Key.A)
            pressKey(Key.V)
            keyUp(Key.CtrlLeft)
        }
        onNodeWithTag("note-body").assertTextContains("Pasted\nΔ")
        runOnIdle {
            val draft = state.workspace.editor(editor.id)!!.draft as OperationEditorDraft.Note
            assertThat(draft.value, equalTo(OperationNoteDraft(draft.value.id, "題名", "Pasted\nΔ")))
            assertThat(bubbledShortcuts, equalTo(0))
        }
    }

    private fun document() = OperationDocument(
        operationId = UUID.fromString("00000000-0000-0000-0000-000000000404"),
        operationName = "Phase 4 closure",
        createdAt = Instant.EPOCH,
        updatedAt = Instant.EPOCH,
        settings = MapViewerSettings(operationName = "Phase 4 closure"),
        renderData = MapViewerRenderData(BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB)),
        timeline = OperationTimeline(),
    )

    private class TestClipboard : Clipboard {
        var content: ClipEntry? = null

        override suspend fun setClipEntry(clipEntry: ClipEntry?) = Unit.also { content = clipEntry }

        override suspend fun getClipEntry() = content

        override val nativeClipboard: Any = this

        fun text() = content?.nativeClipEntry
            ?.let { it as java.awt.datatransfer.Transferable }
            ?.getTransferData(DataFlavor.stringFlavor) as? String
    }
}
