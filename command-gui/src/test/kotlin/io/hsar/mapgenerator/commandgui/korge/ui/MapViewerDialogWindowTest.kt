package io.hsar.mapgenerator.commandgui.korge.ui

import io.hsar.mapgenerator.image.Palette
import korlibs.korge.view.Container
import korlibs.korge.view.Text
import korlibs.math.geom.Vector2D
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test

class MapViewerDialogWindowTest {
    @Test
    fun `close button is title height and flush with the title bar right edge`() {
        val layout = MapViewerDialogLayoutBuilder.build(
            bounds = MapViewerDialogBounds(left = 0.0, top = 0.0, width = 400.0, height = 300.0),
        )

        assertThat(layout.closeButton.width, equalTo(layout.closeButton.height))
        assertThat(layout.closeButton.height, equalTo(layout.titleBar.height + MapViewerDialogLayoutBuilder.BORDER_INSET))
        assertThat(layout.closeButton.top, equalTo(layout.titleBar.top - MapViewerDialogLayoutBuilder.BORDER_INSET))
        assertThat(
            layout.closeButton.top + layout.closeButton.height,
            equalTo(layout.titleBar.top + layout.titleBar.height + MapViewerDialogLayoutBuilder.BORDER_INSET),
        )
        assertThat(layout.closeButton.left + layout.closeButton.width, equalTo(layout.titleBar.left + layout.titleBar.width))
    }

    @Test
    fun `non modal dialog only blocks points inside its bounds`() {
        val root = Container()
        val window = MapViewerDialogWindow(
            root = root,
            title = "Test",
            focusTargetBoundsProvider = { emptyList() },
            preferredWidth = 400.0,
            preferredHeight = 300.0,
            contentHeight = { 0.0 },
            actions = emptyList(),
            onClose = {},
            renderContent = { _, _ -> },
            mode = MapViewerDialogMode.FLOATING,
        )

        window.show(800, 600)

        assertThat(window.blocksScreenPoint(Vector2D(400.0, 300.0)), equalTo(true))
        assertThat(window.blocksScreenPoint(Vector2D(10.0, 10.0)), equalTo(false))
    }

    @Test
    fun `modal dialog blocks the whole screen`() {
        val root = Container()
        val window = MapViewerDialogWindow(
            root = root,
            title = "Test",
            preferredWidth = 400.0,
            preferredHeight = 300.0,
            contentHeight = { 0.0 },
            actions = emptyList(),
            onClose = {},
            renderContent = { _, _ -> },
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
            initialPosition = { Vector2D(16.0, 64.0) },
            preferredWidth = 400.0,
            preferredHeight = 300.0,
            contentHeight = { 0.0 },
            actions = emptyList(),
            onClose = {},
            renderContent = { _, _ -> },
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
            focusTargetBoundsProvider = { emptyList() },
            preferredWidth = 400.0,
            preferredHeight = 300.0,
            contentHeight = { 0.0 },
            actions = emptyList(),
            onClose = {},
            renderContent = { _, _ -> },
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
            focusTargetBoundsProvider = { emptyList() },
            preferredWidth = 400.0,
            preferredHeight = 300.0,
            contentHeight = { 0.0 },
            actions = emptyList(),
            onClose = {},
            renderContent = { _, _ -> renderCount++ },
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
    fun `refreshing a dialog title does not rebuild its content tree`() {
        val root = Container()
        var title = "Edit Unit - A"
        var renderCount = 0
        val window = MapViewerDialogWindow(
            root = root,
            title = "Edit Unit",
            titleProvider = { title },
            focusTargetBoundsProvider = { emptyList() },
            preferredWidth = 400.0,
            preferredHeight = 300.0,
            contentHeight = { 0.0 },
            actions = emptyList(),
            onClose = {},
            renderContent = { _, _ -> renderCount++ },
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
