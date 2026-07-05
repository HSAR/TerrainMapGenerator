package io.hsar.mapgenerator.commandgui.korge.ui

import io.hsar.mapgenerator.image.Palette
import korlibs.korge.input.mouse
import korlibs.korge.view.Container
import korlibs.korge.view.container
import korlibs.korge.view.solidRect
import korlibs.korge.view.xy
import java.util.*

internal object MapViewerDialogChrome {
    fun render(
        parent: Container,
        layout: MapViewerDialogLayout,
        title: String,
        titleTabs: List<MapViewerDialogTitleTab> = emptyList(),
        onClose: () -> Unit,
        onBeginMove: () -> Unit,
        onBeginResize: () -> Unit,
    ) {
        MapViewerUiBoxes.box(
            parent = parent,
            width = layout.bounds.width,
            height = layout.bounds.height,
            style = ::dialogPanelStyle,
        )

        val titleBar = parent.container()
        titleBar.xy(layout.titleBar.left, layout.titleBar.top)
        titleBar.solidRect(layout.titleBar.width, layout.titleBar.height, MapViewerUiBoxes.toRgba(Palette.Colours.DARK, TITLE_BAR_ALPHA))
        val titleWidth = (layout.titleBar.width - layout.closeButton.width - TITLE_TEXT_RIGHT_GAP - TITLE_TEXT_LEFT).coerceAtLeast(0.0)
        if (titleTabs.isEmpty()) {
            val fittedTitle = MapViewerTextViews.ellipsize(
                text = dialogTitleText(title),
                maxWidth = titleWidth,
                role = MapViewerFontRole.STRONG,
                size = MapViewerTextViews.DEFAULT_TEXT_SIZE,
            )
            MapViewerTextViews.leftBox(
                parent = titleBar,
                text = fittedTitle,
                width = titleWidth,
                height = layout.titleBar.height,
                role = MapViewerFontRole.STRONG,
                color = Palette.Colours.WHITE,
            ) {
                x = TITLE_TEXT_LEFT
            }
        } else {
            renderTitleTabs(
                parent = titleBar,
                tabs = titleTabs,
                width = titleWidth,
                height = layout.titleBar.height,
                onBeginMove = onBeginMove,
            )
        }
        titleBar.mouse {
            onDown {
                onBeginMove()
            }
        }

        parent.solidRect(layout.body.width, MapViewerDialogLayoutBuilder.BORDER_INSET, MapViewerUiBoxes.toRgba(Palette.Colours.WHITE, MapViewerUiStateStyle.ACTIVE_ALPHA)) {
            x = layout.body.left
            y = layout.body.top
        }

        val close = parent.container()
        close.xy(layout.closeButton.left, layout.closeButton.top)
        MapViewerUiBoxes.button(close, layout.closeButton.width, layout.closeButton.height)
        MapViewerTextViews.box(
            parent = close,
            text = "X",
            width = layout.closeButton.width,
            height = layout.closeButton.height,
            role = MapViewerFontRole.STRONG,
            color = Palette.Colours.WHITE,
        )
        close.mouse {
            onClick {
                onClose()
            }
        }

        val resize = parent.container()
        resize.xy(layout.resizeHandle.left, layout.resizeHandle.top)
        resize.solidRect(layout.resizeHandle.width, layout.resizeHandle.height, MapViewerUiBoxes.toRgba(Palette.Colours.WHITE, 0))
        resize.solidRect(18.0, 2.0, MapViewerUiBoxes.toRgba(Palette.Colours.WHITE, 220)) {
            x = 4.0
            y = 18.0
        }
        resize.solidRect(2.0, 18.0, MapViewerUiBoxes.toRgba(Palette.Colours.WHITE, 220)) {
            x = 18.0
            y = 4.0
        }
        resize.mouse {
            onDown {
                onBeginResize()
            }
        }
    }

    private fun renderTitleTabs(
        parent: Container,
        tabs: List<MapViewerDialogTitleTab>,
        width: Double,
        height: Double,
        onBeginMove: () -> Unit,
    ) {
        val tabWidth = MapViewerDialogTitleTabLayoutBuilder.tabWidth(tabs.map { it.label })
        tabs.forEachIndexed { index, tab ->
            val tabButton = parent.container()
            tabButton.xy(index * tabWidth, 0.0)
            tabButton.solidRect(
                tabWidth,
                height,
                MapViewerUiBoxes.toRgba(Palette.Colours.WHITE, if (tab.active) ACTIVE_TAB_FILL_ALPHA else INACTIVE_TAB_FILL_ALPHA),
            )
            if (index > 0) {
                tabButton.solidRect(1.0, height, MapViewerUiBoxes.toRgba(Palette.Colours.WHITE, MapViewerUiStateStyle.ACTIVE_ALPHA))
            }
            MapViewerTextViews.box(
                parent = tabButton,
                text = dialogTitleText(tab.label),
                width = tabWidth,
                height = height,
                role = MapViewerFontRole.STRONG,
                color = Palette.Colours.WHITE,
            )
            tabButton.mouse {
                onDown {
                    tab.onSelected()
                    onBeginMove()
                }
            }
        }
        parent.solidRect(1.0, height, MapViewerUiBoxes.toRgba(Palette.Colours.WHITE, MapViewerUiStateStyle.ACTIVE_ALPHA)) {
            x = (tabWidth * tabs.size).coerceAtMost(width)
        }
    }

    const val TITLE_BAR_ALPHA = 235
    internal const val TITLE_TEXT_LEFT = 14.0
    internal const val TITLE_TEXT_RIGHT_GAP = 56.0
    internal const val DIALOG_PANEL_FILL_ALPHA = 220

    internal fun dialogPanelStyle(active: Boolean): UiFrameStyle =
        UiFrameStyle(
            fillColor = Palette.Colours.DARK,
            fillAlpha = DIALOG_PANEL_FILL_ALPHA,
            borderColor = Palette.Colours.WHITE,
            borderAlpha = MapViewerUiStateStyle.ACTIVE_ALPHA,
        )

    private const val ACTIVE_TAB_FILL_ALPHA = 0
    private const val INACTIVE_TAB_FILL_ALPHA = 30

    internal fun dialogTitleText(text: String): String =
        text.uppercase(Locale.ROOT)
}

internal object MapViewerDialogTitleTabLayoutBuilder {
    fun tabWidth(labels: List<String>): Double =
        labels.maxOfOrNull { label ->
            MapViewerTextViews.measure(MapViewerDialogChrome.dialogTitleText(label), role = MapViewerFontRole.STRONG).width
        }
            ?.let { labelWidth -> labelWidth + TAB_HORIZONTAL_PADDING * 2.0 }
            ?: 0.0

    fun minimumShellWidth(labels: List<String>): Double =
        if (labels.isEmpty()) {
            0.0
        } else {
            tabWidth(labels) * labels.size +
                    MapViewerDialogLayoutBuilder.CLOSE_BUTTON_SIZE +
                    MapViewerDialogChrome.TITLE_TEXT_RIGHT_GAP +
                    MapViewerDialogChrome.TITLE_TEXT_LEFT +
                    MapViewerDialogLayoutBuilder.BORDER_INSET * 2.0
        }

    private const val TAB_HORIZONTAL_PADDING = 20.0
}

internal data class MapViewerDialogTitleTab(
    val label: String,
    val active: Boolean,
    val onSelected: () -> Unit,
)
