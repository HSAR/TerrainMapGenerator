package io.hsar.mapgenerator.commandgui.korge.ui

import io.hsar.mapgenerator.image.Palette
import korlibs.image.text.TextAlignment
import korlibs.korge.input.mouse
import korlibs.korge.view.Container
import korlibs.korge.view.container
import korlibs.korge.view.graphics
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
        showWindowControls: Boolean = false,
        minimized: Boolean = false,
        pinned: Boolean = false,
        onMinimize: () -> Unit = {},
        onPin: () -> Unit = {},
        onTitleClick: () -> Unit = {},
        renderResizeHandle: Boolean = true,
    ): Container {
        MapViewerUiBoxes.box(
            parent = parent,
            width = layout.bounds.width,
            height = layout.bounds.height,
            style = ::dialogPanelStyle,
        )

        val titleBar = renderTitleBar(parent, layout, title, titleTabs, onBeginMove, onTitleClick)

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

        if (showWindowControls) {
            renderWindowButton(parent, layout.minimizeButton) { frame ->
                renderMinimizeIcon(frame, layout.minimizeButton.width, layout.minimizeButton.height, minimized)
            }.mouse {
                onClick { onMinimize() }
            }
            renderWindowButton(parent, layout.pinButton, active = pinned) { frame ->
                renderPinIcon(frame, layout.pinButton.width, layout.pinButton.height)
            }.mouse {
                onClick { onPin() }
            }
        }

        if (renderResizeHandle) {
            val resize = parent.container()
            resize.xy(layout.resizeHandle.left, layout.resizeHandle.top)
            resize.solidRect(layout.resizeHandle.width, layout.resizeHandle.height, MapViewerUiBoxes.toRgba(Palette.Colours.WHITE, 0))
            MapViewerCornerBrackets.render(resize, layout.resizeHandle.width, layout.resizeHandle.height, MapViewerCornerBrackets.Corner.BOTTOM_RIGHT)
            resize.mouse {
                onDown {
                    onBeginResize()
                }
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
        onTitleClick: () -> Unit = {},
    ): Container = parent.container().also { titleBar ->
        titleBar.xy(layout.titleBar.left, layout.titleBar.top)
        refreshTitleBar(titleBar, layout, title, titleTabs, onBeginMove)
        titleBar.mouse {
            onDown {
                onBeginMove()
            }
            onClick {
                onTitleClick()
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
        (
                layout.titleBar.width -
                        layout.closeButton.width * (if (layout.showWindowControls) 3.0 else 1.0) -
                        TITLE_TEXT_RIGHT_GAP -
                        TITLE_TEXT_LEFT
                ).coerceAtLeast(0.0)

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

    private fun renderWindowButton(
        parent: Container,
        bounds: ScreenBounds,
        active: Boolean = false,
        renderIcon: (Container) -> Unit,
    ): Container =
        parent.container().also { button ->
            button.xy(bounds.left, bounds.top)
            MapViewerBoxedIcon.frame(
                parent = button,
                width = bounds.width,
                height = bounds.height,
                sharedRightEdge = true,
            ).also { frame ->
                if (active) {
                    frame.solidRect(
                        bounds.width - MapViewerBoxedIcon.STROKE_WIDTH * 2.0,
                        bounds.height - MapViewerBoxedIcon.STROKE_WIDTH * 2.0,
                        MapViewerUiBoxes.toRgba(Palette.Colours.WHITE, ACTIVE_CONTROL_FILL_ALPHA),
                    ).xy(MapViewerBoxedIcon.STROKE_WIDTH, MapViewerBoxedIcon.STROKE_WIDTH)
                }
                renderIcon(frame)
            }
        }

    private fun renderMinimizeIcon(parent: Container, width: Double, height: Double, minimized: Boolean) {
        MapViewerChevron.render(
            parent = parent,
            width = width,
            height = height,
            direction = if (minimized) MapViewerChevronDirection.DOWN else MapViewerChevronDirection.UP,
        )
    }

    private fun renderPinIcon(parent: Container, width: Double, height: Double) {
        val arm = MapViewerBoxedIcon.iconArm(width, height)
        val centerX = width * 0.5
        val centerY = height * 0.5
        parent.graphics {
            lineWidth = MapViewerBoxedIcon.STROKE_WIDTH
            stroke(MapViewerUiBoxes.toRgba(Palette.Colours.WHITE)) {
                moveTo(centerX - arm, centerY - arm * 0.8)
                lineTo(centerX + arm, centerY - arm * 0.8)
                moveTo(centerX - arm * 0.55, centerY - arm * 0.8)
                lineTo(centerX - arm * 0.55, centerY + arm * 0.25)
                lineTo(centerX + arm * 0.55, centerY + arm * 0.25)
                lineTo(centerX + arm * 0.55, centerY - arm * 0.8)
                moveTo(centerX, centerY + arm * 0.25)
                lineTo(centerX, centerY + arm)
            }
        }
    }

    private const val ACTIVE_CONTROL_FILL_ALPHA = 72
}

internal object MapViewerDialogTitleTabLayoutBuilder {
    fun tabWidth(labels: List<String>): Double =
        labels.maxOfOrNull { label ->
            MapViewerTextViews.measure(MapViewerDialogChrome.dialogTitleText(label), role = MapViewerFontRole.MAIN).width
        }
            ?.let { labelWidth -> labelWidth + TAB_HORIZONTAL_PADDING * 2.0 }
            ?: 0.0

    fun minimumShellWidth(labels: List<String>, showWindowControls: Boolean = false): Double =
        if (labels.isEmpty()) {
            0.0
        } else {
            tabWidth(labels) * labels.size +
                    MapViewerDialogLayoutBuilder.CLOSE_BUTTON_SIZE * (if (showWindowControls) 3.0 else 1.0) +
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
