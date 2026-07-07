package io.hsar.mapgenerator.commandgui.korge.ui

import io.hsar.mapgenerator.image.Palette
import korlibs.korge.view.Container
import korlibs.math.geom.Vector2D
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test

class MapViewerDialogWindowTest {
    @Test
    fun `non modal dialog only blocks points inside its bounds`() {
        val root = Container()
        val window = MapViewerDialogWindow(
            root = root,
            title = "Test",
            focusAnchorProvider = { null },
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
    fun `tabbed title minimum width uses fixed equal tab widths`() {
        val tabWidth = MapViewerDialogTitleTabLayoutBuilder.tabWidth(listOf("Turns", "Much Longer Settings"))
        val minimumWidth = MapViewerDialogTitleTabLayoutBuilder.minimumShellWidth(listOf("Turns", "Much Longer Settings"))

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
}
