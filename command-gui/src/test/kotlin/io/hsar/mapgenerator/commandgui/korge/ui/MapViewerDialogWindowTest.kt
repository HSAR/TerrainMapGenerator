package io.hsar.mapgenerator.commandgui.korge.ui

import io.hsar.mapgenerator.commandgui.korge.interaction.CameraViewport
import io.hsar.mapgenerator.commandgui.korge.scene.MapViewerSceneLayers
import io.hsar.mapgenerator.image.Palette
import korlibs.korge.view.Container
import korlibs.korge.view.Text
import korlibs.korge.view.container
import korlibs.math.geom.Vector2D
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test

class MapViewerDialogWindowTest {
    @Test
    fun `viewer controls sit to the left of the close button`() {
        val layout = MapViewerDialogLayoutBuilder.build(
            bounds = MapViewerDialogBounds(left = 0.0, top = 0.0, width = 400.0, height = 300.0),
            showWindowControls = true,
        )

        assertThat(layout.minimizeButton.right, equalTo(layout.pinButton.left))
        assertThat(layout.pinButton.right, equalTo(layout.closeButton.left))
        assertThat(layout.closeButton.right, equalTo(layout.titleBar.right))
        assertThat(
            MapViewerDialogChrome.titleTextWidth(layout),
            equalTo(
                layout.titleBar.width - layout.closeButton.width * 3.0 -
                        MapViewerDialogChrome.TITLE_TEXT_RIGHT_GAP - MapViewerDialogChrome.TITLE_TEXT_LEFT,
            ),
        )
    }

    @Test
    fun `close button is title height and flush with the title bar right edge`() {
        val layout = MapViewerDialogLayoutBuilder.build(
            bounds = MapViewerDialogBounds(left = 0.0, top = 0.0, width = 400.0, height = 300.0),
        )

        assertThat(layout.closeButton.width, equalTo(layout.closeButton.height))
        assertThat(layout.showWindowControls, equalTo(false))
        assertThat(layout.closeButton.height, equalTo(layout.titleBar.height + MapViewerDialogLayoutBuilder.BORDER_INSET * 2.0))
        assertThat(layout.closeButton.top, equalTo(layout.titleBar.top - MapViewerDialogLayoutBuilder.BORDER_INSET))
        assertThat(
            layout.closeButton.top + layout.closeButton.height,
            equalTo(layout.titleBar.top + layout.titleBar.height + MapViewerDialogLayoutBuilder.BORDER_INSET),
        )
        assertThat(layout.closeButton.left + layout.closeButton.width, equalTo(layout.titleBar.left + layout.titleBar.width))
    }

    @Test
    fun `title text uses the space between its left inset and close button`() {
        val layout = MapViewerDialogLayoutBuilder.build(
            bounds = MapViewerDialogBounds(left = 0.0, top = 0.0, width = 360.0, height = 300.0),
        )

        assertThat(
            MapViewerDialogChrome.titleTextWidth(layout),
            equalTo(
                layout.titleBar.width - layout.closeButton.width -
                        MapViewerDialogChrome.TITLE_TEXT_RIGHT_GAP - MapViewerDialogChrome.TITLE_TEXT_LEFT,
            ),
        )
        assertThat(MapViewerDialogChrome.TITLE_TEXT_RIGHT_GAP, equalTo(14.0))
        assertThat(
            MapViewerTextViews.ellipsize(
                MapViewerDialogChrome.dialogTitleText("Background: Ridged Badlands"),
                MapViewerDialogChrome.titleTextWidth(layout),
            ),
            equalTo("BACKGROUND: RIDGED BADLANDS"),
        )
    }

    @Test
    fun `non modal dialog only blocks points inside its bounds`() {
        val root = Container()
        val window = MapViewerDialogWindow(
            root = root,
            title = "Test",
            persistence = MapViewerDialogPersistence("test"),
            focusTargetBoundsProvider = { emptyList() },
            preferredWidth = 400.0,
            preferredHeight = 300.0,
            contentHeight = { 0.0 },
            actions = emptyList(),
            onClose = {},
            renderContent = { _, _, _ -> },
            mode = MapViewerDialogMode.FLOATING,
        )

        window.show(800, 600)

        assertThat(window.blocksScreenPoint(Vector2D(400.0, 300.0)), equalTo(true))
        assertThat(window.blocksScreenPoint(Vector2D(10.0, 10.0)), equalTo(false))
    }

    @Test
    fun `non modal dialog persists its configured identity draft and bounds`() {
        val root = Container()
        val window = MapViewerDialogWindow(
            root = root,
            title = "Test",
            persistence = MapViewerDialogPersistence(
                type = "unit-edit",
                targetId = "unit-id",
                draft = { io.hsar.mapgenerator.commandgui.save.OperationSaveJson.objectMapper.readTree("""{"name":"Alpha"}""") },
            ),
            preferredWidth = 400.0,
            preferredHeight = 300.0,
            contentHeight = { 0.0 },
            actions = emptyList(),
            onClose = {},
            renderContent = { _, _, _ -> },
            mode = MapViewerDialogMode.FLOATING,
        )

        window.show(800, 600)

        val persisted = window.persistedDialog()

        assertThat(persisted?.type, equalTo("unit-edit"))
        assertThat(persisted?.targetId, equalTo("unit-id"))
        assertThat(persisted?.draft?.path("name")?.textValue(), equalTo("Alpha"))
        assertThat(persisted?.bounds?.width, equalTo(400.0))
    }

    @Test
    fun `non modal dialog restores bounds from its persistence descriptor`() {
        val root = Container()
        val window = MapViewerDialogWindow(
            root = root,
            title = "Test",
            persistence = MapViewerDialogPersistence(
                type = "unit-edit",
                restored = MapViewerPersistedDialog(
                    type = "unit-edit",
                    bounds = MapViewerDialogBounds(16.0, 32.0, 500.0, 350.0),
                ),
            ),
            preferredWidth = 400.0,
            preferredHeight = 300.0,
            contentHeight = { 0.0 },
            actions = emptyList(),
            onClose = {},
            renderContent = { _, _, _ -> },
            mode = MapViewerDialogMode.FLOATING,
        )

        window.show(800, 600)

        assertThat(window.currentBounds, equalTo(MapViewerDialogBounds(16.0, 32.0, 500.0, 350.0)))
    }

    @Test
    fun `modal dialog blocks the whole screen`() {
        val root = Container()
        val window = MapViewerDialogWindow(
            root = root,
            title = "Test",
            persistence = MapViewerDialogPersistence("test"),
            preferredWidth = 400.0,
            preferredHeight = 300.0,
            contentHeight = { 0.0 },
            actions = emptyList(),
            onClose = {},
            renderContent = { _, _, _ -> },
            mode = MapViewerDialogMode.MODAL,
        )

        window.show(800, 600)

        assertThat(window.blocksScreenPoint(Vector2D(400.0, 300.0)), equalTo(true))
        assertThat(window.blocksScreenPoint(Vector2D(10.0, 10.0)), equalTo(true))
    }

    @Test
    fun `dialog can open at a caller supplied default position`() {
        val root = Container()
        val window = MapViewerDialogWindow(
            root = root,
            title = "Test",
            persistence = MapViewerDialogPersistence("test"),
            initialPosition = { Vector2D(16.0, 64.0) },
            preferredWidth = 400.0,
            preferredHeight = 300.0,
            contentHeight = { 0.0 },
            actions = emptyList(),
            onClose = {},
            renderContent = { _, _, _ -> },
            mode = MapViewerDialogMode.FLOATING,
        )

        window.show(800, 600)

        assertThat(window.blocksScreenPoint(Vector2D(20.0, 68.0)), equalTo(true))
        assertThat(window.blocksScreenPoint(Vector2D(400.0, 300.0)), equalTo(true))
        assertThat(window.blocksScreenPoint(Vector2D(410.0, 370.0)), equalTo(false))
    }

    @Test
    fun `resize handle blocks points just outside the dialog body`() {
        val root = Container()
        val window = MapViewerDialogWindow(
            root = root,
            title = "Test",
            persistence = MapViewerDialogPersistence("test"),
            focusTargetBoundsProvider = { emptyList() },
            preferredWidth = 400.0,
            preferredHeight = 300.0,
            contentHeight = { 0.0 },
            actions = emptyList(),
            onClose = {},
            renderContent = { _, _, _ -> },
            mode = MapViewerDialogMode.FLOATING,
        )

        window.show(800, 600)

        assertThat(window.blocksScreenPoint(Vector2D(602.0, 440.0)), equalTo(true))
        assertThat(window.blocksScreenPoint(Vector2D(610.0, 460.0)), equalTo(false))
    }

    @Test
    fun `moving a dialog does not rebuild its content tree`() {
        val root = Container()
        var renderCount = 0
        val window = MapViewerDialogWindow(
            root = root,
            title = "Test",
            persistence = MapViewerDialogPersistence("test"),
            focusTargetBoundsProvider = { emptyList() },
            preferredWidth = 400.0,
            preferredHeight = 300.0,
            contentHeight = { 0.0 },
            actions = emptyList(),
            onClose = {},
            renderContent = { _, _, _ -> renderCount++ },
            mode = MapViewerDialogMode.FLOATING,
        )

        window.show(800, 600)
        window.moveTo(
            bounds = MapViewerDialogBounds(
                left = 120.0,
                top = 140.0,
                width = 400.0,
                height = 300.0,
            ),
            focusTargetBounds = emptyList(),
        )

        assertThat(renderCount, equalTo(1))
        assertThat(window.blocksScreenPoint(Vector2D(140.0, 160.0)), equalTo(true))
    }

    @Test
    fun `focus shadows render in the shared ui underlay and clear when minimized or closed`() {
        val stage = Container()
        val underlay = stage.container().also { it.name = MapViewerSceneLayers.UI_UNDERLAY_NAME }
        val root = stage.container()
        val window = MapViewerDialogWindow(
            root = root,
            title = "Test",
            persistence = MapViewerDialogPersistence("test"),
            focusTargetBoundsProvider = { listOf(ScreenBounds(left = 40.0, top = 40.0, width = 20.0, height = 20.0)) },
            preferredWidth = 400.0,
            preferredHeight = 300.0,
            contentHeight = { 0.0 },
            onClose = {},
            renderContent = { _, _, _ -> },
            dialogManager = MapViewerDialogManager { CameraViewport(500.0, 500.0, 1.0, 800, 600, 1000, 1000) },
        )

        window.show(800, 600)
        val shadowContainer = underlay.children.single() as Container

        assertThat(underlay.children.size, equalTo(1))
        assertThat(shadowContainer.children.size, equalTo(1))
        assertThat(root.children.size, equalTo(1))

        window.minimize()

        assertThat(shadowContainer.children.size, equalTo(0))

        window.close()

        assertThat(underlay.children.size, equalTo(0))
    }

    @Test
    fun `minimized dialogs dock across the top and restore their expanded bounds`() {
        val viewport = CameraViewport(500.0, 500.0, 1.0, 700, 600, 1000, 1000)
        val manager = MapViewerDialogManager { viewport }
        val windows = (1..3).map { index ->
            MapViewerDialogWindow(
                root = Container(),
                title = "Test $index",
                persistence = MapViewerDialogPersistence("test-$index"),
                preferredWidth = 300.0,
                preferredHeight = 300.0,
                contentHeight = { 0.0 },
                onClose = {},
                renderContent = { _, _, _ -> },
                dialogManager = manager,
            ).also { it.show(700, 600) }
        }
        val original = windows.first().currentBounds

        windows.forEach(MapViewerDialogWindow::minimize)

        assertThat(windows[0].currentDisplayBounds?.left, equalTo(16.0))
        assertThat(windows[1].currentDisplayBounds?.left, equalTo(324.0))
        assertThat(windows[2].currentDisplayBounds?.left, equalTo(16.0))
        assertThat(windows[2].currentDisplayBounds?.top, equalTo(66.0))
        assertThat(windows.all { it.isMinimized }, equalTo(true))
        assertThat(windows.first().blocksScreenPoint(Vector2D(20.0, 20.0)), equalTo(true))
        assertThat(windows.first().blocksScreenPoint(Vector2D(100.0, 100.0)), equalTo(false))

        windows.first().restore()

        assertThat(windows.first().currentBounds, equalTo(original))
        assertThat(windows.first().isMinimized, equalTo(false))
        assertThat(windows[1].currentDisplayBounds?.left, equalTo(16.0))
    }

    @Test
    fun `pinned dialog follows its top right map anchor without rerendering content`() {
        var viewport = CameraViewport(500.0, 500.0, 1.0, 800, 600, 2000, 2000)
        val manager = MapViewerDialogManager { viewport }
        var renderCount = 0
        val window = MapViewerDialogWindow(
            root = Container(),
            title = "Pinned",
            persistence = MapViewerDialogPersistence("test"),
            preferredWidth = 400.0,
            preferredHeight = 300.0,
            contentHeight = { 0.0 },
            onClose = {},
            renderContent = { _, _, _ -> renderCount++ },
            dialogManager = manager,
        )
        window.show(800, 600)
        window.togglePinned()
        val renderCountAfterPinning = renderCount

        viewport = viewport.copy(centerX = 600.0)
        window.refreshPinnedPositionIfRequired()

        assertThat(window.isPinned, equalTo(true))
        assertThat(window.currentDisplayBounds, equalTo(MapViewerDialogBounds(100.0, 150.0, 400.0, 300.0)))
        assertThat(renderCount, equalTo(renderCountAfterPinning))

        window.minimize()
        viewport = viewport.copy(centerY = 600.0)
        window.refreshPinnedPositionIfRequired()

        assertThat(window.currentDisplayBounds, equalTo(MapViewerDialogBounds(100.0, 50.0, 400.0, 42.0)))
        assertThat(renderCount, equalTo(renderCountAfterPinning))

        viewport = viewport.copy(centerX = 1500.0)
        window.refreshPinnedPositionIfRequired()

        assertThat(window.currentDisplayBounds?.right, equalTo(-400.0))
        assertThat(renderCount, equalTo(renderCountAfterPinning))
    }

    @Test
    fun `refreshing a dialog title does not rebuild its content tree`() {
        val root = Container()
        var title = "Edit Unit - A"
        var renderCount = 0
        val window = MapViewerDialogWindow(
            root = root,
            title = "Edit Unit",
            persistence = MapViewerDialogPersistence("test"),
            titleProvider = { title },
            focusTargetBoundsProvider = { emptyList() },
            preferredWidth = 400.0,
            preferredHeight = 300.0,
            contentHeight = { 0.0 },
            actions = emptyList(),
            onClose = {},
            renderContent = { _, _, _ -> renderCount++ },
            mode = MapViewerDialogMode.FLOATING,
        )

        window.show(800, 600)
        title = "Edit Unit - Alpha"
        window.refreshTitle()

        assertThat(renderCount, equalTo(1))
    }

    @Test
    fun `tabbed title minimum width uses fixed equal tab widths`() {
        val tabWidth = MapViewerDialogTitleTabLayoutBuilder.tabWidth(listOf("Turns", "Much Longer Settings"))
        val minimumWidth = MapViewerDialogTitleTabLayoutBuilder.minimumShellWidth(listOf("Turns", "Much Longer Settings"))
        val expectedTabWidth = MapViewerTextViews.measure(MapViewerTextViews.titleText("Much Longer Settings"), role = MapViewerFontRole.MAIN).width +
                40.0

        assertThat(
            minimumWidth,
            equalTo(
                tabWidth * 2.0 +
                        MapViewerDialogLayoutBuilder.CLOSE_BUTTON_SIZE +
                        MapViewerDialogChrome.TITLE_TEXT_RIGHT_GAP +
                        MapViewerDialogChrome.TITLE_TEXT_LEFT +
                        MapViewerDialogLayoutBuilder.BORDER_INSET * 2.0,
            ),
        )
        assertThat(
            MapViewerDialogTitleTabLayoutBuilder.tabWidth(listOf("Turns", "Much Longer Settings", "A")),
            equalTo(tabWidth),
        )
        assertThat(tabWidth, equalTo(expectedTabWidth))
        assertThat(
            MapViewerDialogTitleTabLayoutBuilder.minimumShellWidth(
                listOf("Turns", "Much Longer Settings"),
                showWindowControls = true,
            ),
            equalTo(minimumWidth + MapViewerDialogLayoutBuilder.CLOSE_BUTTON_SIZE * 2.0),
        )
    }

    @Test
    fun `dialog panel uses the semi transparent side panel background style`() {
        val style = MapViewerDialogChrome.dialogPanelStyle(active = true)

        assertThat(style.fillColor, equalTo(Palette.Colours.DARK))
        assertThat(style.fillAlpha, equalTo(220))
        assertThat(style.borderColor, equalTo(Palette.Colours.WHITE))
        assertThat(style.borderAlpha, equalTo(255))
    }

    @Test
    fun `dialog title text is rendered in uppercase`() {
        assertThat(MapViewerDialogChrome.dialogTitleText("Edit Unit - Alpha"), equalTo("EDIT UNIT - ALPHA"))
    }

    @Test
    fun `refreshing a title bar displays the latest title`() {
        val layout = MapViewerDialogLayoutBuilder.build(
            bounds = MapViewerDialogBounds(left = 0.0, top = 0.0, width = 400.0, height = 300.0),
        )
        val titleBar = MapViewerDialogChrome.renderTitleBar(
            parent = Container(),
            layout = layout,
            title = "Edit Unit - A",
            onBeginMove = {},
        )

        MapViewerDialogChrome.refreshTitleBar(
            titleBar = titleBar,
            layout = layout,
            title = "Edit Unit - Alpha",
            onBeginMove = {},
        )

        assertThat(titleBar.children.filterIsInstance<Text>().single().text, equalTo("EDIT UNIT - ALPHA"))
    }
}
