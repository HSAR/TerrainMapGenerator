package io.hsar.mapgenerator.commandgui.korge.ui

import io.hsar.mapgenerator.image.Palette
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
        onClose: () -> Unit,
        onBeginMove: () -> Unit,
        onBeginResize: () -> Unit,
    ) {
        MapViewerUiBoxes.box(parent, layout.bounds.width, layout.bounds.height)

        val titleBar = parent.container()
        titleBar.xy(layout.titleBar.left, layout.titleBar.top)
        titleBar.solidRect(layout.titleBar.width, layout.titleBar.height, MapViewerUiBoxes.toRgba(Palette.Colours.DARK, TITLE_BAR_ALPHA))
        val titleWidth = (layout.titleBar.width - layout.closeButton.width - TITLE_TEXT_RIGHT_GAP - TITLE_TEXT_LEFT).coerceAtLeast(0.0)
        val fittedTitle = MapViewerTextViews.ellipsize(
            text = title,
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

    private const val TITLE_BAR_ALPHA = 235
    private const val TITLE_TEXT_LEFT = 14.0
    private const val TITLE_TEXT_RIGHT_GAP = 56.0
}
