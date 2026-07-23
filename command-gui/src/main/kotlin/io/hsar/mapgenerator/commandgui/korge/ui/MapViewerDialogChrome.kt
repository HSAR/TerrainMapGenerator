package io.hsar.mapgenerator.commandgui.korge.ui

import io.hsar.mapgenerator.image.Palette
import korlibs.image.text.TextAlignment
import korlibs.korge.input.mouse
import korlibs.korge.view.Container
import korlibs.korge.view.container
import korlibs.korge.view.solidRect
import korlibs.korge.view.xy

internal object MapViewerDialogChrome {
    fun render(
        parent: Container,
        layout: MapViewerDialogLayout,
        title: String,
        titleTabs: List<MapViewerDialogTitleTab> = emptyList(),
        onClose: () -> Unit,
        onBeginMove: () -> Unit,
        onBeginResize: () -> Unit,
    ): Container {
        MapViewerUiBoxes.box(
            parent = parent,
            width = layout.bounds.width,
            height = layout.bounds.height,
            style = ::dialogPanelStyle,
        )

        val titleBar = renderTitleBar(parent, layout, title, titleTabs, onBeginMove)

        parent.solidRect(layout.body.width, MapViewerDialogLayoutBuilder.BORDER_INSET, MapViewerUiBoxes.toRgba(Palette.Colours.WHITE, MapViewerUiStateStyle.ACTIVE_ALPHA)) {
            x = layout.body.left
            y = layout.body.top
        }

        val close = parent.container()
        close.xy(layout.closeButton.left, layout.closeButton.top)
        MapViewerBoxedIcon.frame(
            parent = close,
            width = layout.closeButton.width,
            height = layout.closeButton.height,
            sharedRightEdge = true,
        ).also { frame ->
            MapViewerCross.render(frame, layout.closeButton.width, layout.closeButton.height)
        }
        close.mouse {
            onClick {
                onClose()
            }
        }

        val resize = parent.container()
        resize.xy(layout.resizeHandle.left, layout.resizeHandle.top)
        resize.solidRect(layout.resizeHandle.width, layout.resizeHandle.height, MapViewerUiBoxes.toRgba(Palette.Colours.WHITE, 0))
        MapViewerCornerBrackets.render(resize, layout.resizeHandle.width, layout.resizeHandle.height, MapViewerCornerBrackets.Corner.BOTTOM_RIGHT)
        resize.mouse {
            onDown {
                onBeginResize()
            }
        }
        return titleBar
    }

    fun renderTitleBar(
        parent: Container,
        layout: MapViewerDialogLayout,
        title: String,
        titleTabs: List<MapViewerDialogTitleTab> = emptyList(),
        onBeginMove: () -> Unit,
    ): Container = parent.container().also { titleBar ->
        titleBar.xy(layout.titleBar.left, layout.titleBar.top)
        refreshTitleBar(titleBar, layout, title, titleTabs, onBeginMove)
        titleBar.mouse {
            onDown {
                onBeginMove()
            }
        }
    }

    fun refreshTitleBar(
        titleBar: Container,
        layout: MapViewerDialogLayout,
        title: String,
        titleTabs: List<MapViewerDialogTitleTab> = emptyList(),
        onBeginMove: () -> Unit,
    ) {
        titleBar.removeChildren()
        titleBar.solidRect(layout.titleBar.width, layout.titleBar.height, MapViewerUiBoxes.toRgba(Palette.Colours.DARK, TITLE_BAR_ALPHA))
        val titleWidth = titleTextWidth(layout)
        if (titleTabs.isEmpty()) {
            val fittedTitle = MapViewerTextViews.ellipsize(
                text = dialogTitleText(title),
                maxWidth = titleWidth,
                role = MapViewerFontRole.MAIN,
                size = MapViewerTextViews.DEFAULT_TEXT_SIZE,
            )
            MapViewerTextViews.titleBox(
                parent = titleBar,
                text = fittedTitle,
                width = titleWidth,
                height = layout.titleBar.height,
                color = Palette.Colours.WHITE,
                alignment = TextAlignment.MIDDLE_LEFT,
            ) { x = TITLE_TEXT_LEFT }
        } else {
            renderTitleTabs(
                parent = titleBar,
                tabs = titleTabs,
                width = titleWidth,
                height = layout.titleBar.height,
                onBeginMove = onBeginMove,
            )
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
            MapViewerTextViews.titleBox(
                parent = tabButton,
                text = dialogTitleText(tab.label),
                width = tabWidth,
                height = height,
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
    internal const val TITLE_TEXT_RIGHT_GAP = 14.0
    internal const val DIALOG_PANEL_FILL_ALPHA = 220

    internal fun titleTextWidth(layout: MapViewerDialogLayout): Double =
        (layout.titleBar.width - layout.closeButton.width - TITLE_TEXT_RIGHT_GAP - TITLE_TEXT_LEFT).coerceAtLeast(0.0)

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
        MapViewerTextViews.titleText(text)
}

internal object MapViewerDialogTitleTabLayoutBuilder {
    fun tabWidth(labels: List<String>): Double =
        labels.maxOfOrNull { label ->
            MapViewerTextViews.measure(MapViewerDialogChrome.dialogTitleText(label), role = MapViewerFontRole.MAIN).width
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
