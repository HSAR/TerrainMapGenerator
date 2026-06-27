package io.hsar.mapgenerator.commandgui.korge.ui

import korlibs.image.color.RGBA
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

internal class MapViewerDialogWindow(
    private val root: Container,
    private val title: String,
    private val preferredWidth: Double,
    private val preferredHeight: Double,
    private val contentHeight: (Double) -> Double,
    private val actions: List<MapViewerDialogAction> = emptyList(),
    private val onClose: () -> Unit,
    private val renderContent: (Container, Double) -> Unit,
) {
    private val layer = root.container()
    private var viewportWidth = 0
    private var viewportHeight = 0
    private var bounds: MapViewerDialogBounds? = null
    private var scrollState = MapViewerScrollState(0.0, 0.0)
    private var scrollDragState: ScrollDragState? = null
    private var dragState: DialogDragState? = null
    private var isOpen = false

    fun show(viewportWidth: Int, viewportHeight: Int) {
        if (isOpen) {
            return
        }

        isOpen = true
        layer.zIndex = Z_INDEX
        layer.mouseEnabled = true
        this.viewportWidth = viewportWidth
        this.viewportHeight = viewportHeight
        bounds = MapViewerDialogLayoutBuilder.defaultBounds(viewportWidth, viewportHeight, preferredWidth, preferredHeight)
        installInputHandlers()
        render()
    }

    fun close() {
        if (!isOpen) {
            return
        }

        isOpen = false
        layer.removeFromParent()
    }

    private fun installInputHandlers() {
        layer.onStageResized { width, height ->
            viewportWidth = width
            viewportHeight = height
            bounds = bounds?.let { with(MapViewerDialogLayoutBuilder) { it.clampedTo(width, height) } }
            render()
        }
        layer.mouse {
            onClick {
                // Consume backdrop clicks so the map never receives modal-dialog input.
            }
            onScrollAnywhere {
                if (bounds?.contains(views.input.mousePos) == true && scrollState.needsScrolling) {
                    scrollState = scrollState.scrolledBy(scrollDeltaYLines * MapViewerScrollbar.SCROLL_STEP_PX)
                    render()
                }
            }
        }
        layer.addUpdater {
            val pointer = layer.stage?.views?.input?.mousePos ?: return@addUpdater
            val leftHeld = layer.stage?.views?.input?.mouseButtonPressed(korlibs.event.MouseButton.LEFT) ?: false

            if (!leftHeld) {
                scrollDragState = null
            } else {
                updateScrollDrag(pointer)
            }

            if (scrollDragState != null) {
                return@addUpdater
            }

            val state = dragState ?: return@addUpdater
            if (!leftHeld) {
                dragState = null
                return@addUpdater
            }

            bounds = when (state) {
                is DialogDragState.Move -> state.original.movedTo(
                    left = state.original.left + pointer.x - state.startPointer.x,
                    top = state.original.top + pointer.y - state.startPointer.y,
                )

                is DialogDragState.Resize -> state.original.resizedTo(
                    width = state.original.width + pointer.x - state.startPointer.x,
                    height = state.original.height + pointer.y - state.startPointer.y,
                )
            }.let { with(MapViewerDialogLayoutBuilder) { it.clampedTo(viewportWidth, viewportHeight) } }
            render()
        }
    }

    private fun render() {
        val currentBounds = bounds ?: return
        val layout = MapViewerDialogLayoutBuilder.build(currentBounds, actions.size)
        val contentHeightValue = contentHeight(layout.contentViewport.width)
        scrollState = scrollState.withContent(
            viewportHeight = layout.contentViewport.height,
            contentHeight = contentHeightValue,
        )
        if (scrollDragState != null && !scrollState.needsScrolling) {
            scrollDragState = null
        }

        layer.removeChildren()
        layer.solidRect(viewportWidth.toDouble(), viewportHeight.toDouble(), RGBA(0, 0, 0, 160))

        val panel = layer.container()
        panel.xy(currentBounds.left, currentBounds.top)
        MapViewerDialogChrome.render(
            parent = panel,
            layout = layout,
            title = title,
            onClose = onClose,
            onBeginMove = { beginDrag(DialogDragMode.MOVE) },
            onBeginResize = { beginDrag(DialogDragMode.RESIZE) },
        )

        val contentClip = panel.clipContainer(Size2D(layout.contentViewport.width, layout.contentViewport.height)) {
            xy(layout.contentViewport.left, layout.contentViewport.top)
            val content = container()
            content.xy(0.0, -scrollState.offset)
            renderContent(content, layout.contentViewport.width)
        }
        contentClip.mouse {
            onClick {
                // Consume content clicks at the dialog layer.
            }
        }
        if (scrollState.needsScrolling) {
            MapViewerScrollbar.render(panel, layout.scrollbar, scrollState)
        }
        layout.footer?.let { footer ->
            MapViewerDialogActions.render(panel, footer, actions)
        }
    }

    private fun beginDrag(mode: DialogDragMode) {
        val pointer = layer.stage?.views?.input?.mousePos ?: return
        val currentBounds = bounds ?: return
        dragState = when (mode) {
            DialogDragMode.MOVE -> DialogDragState.Move(pointer, currentBounds)
            DialogDragMode.RESIZE -> DialogDragState.Resize(pointer, currentBounds)
        }
    }

    private fun updateScrollDrag(pointer: Vector2D) {
        val currentBounds = bounds ?: return
        val layout = MapViewerDialogLayoutBuilder.build(currentBounds, actions.size)
        if (!scrollState.needsScrolling) {
            return
        }

        val thumbBounds = MapViewerScrollbar.thumbBounds(layout.scrollbar, scrollState)
        val state = scrollDragState
        if (state == null) {
            if (layout.scrollbar.contains(pointer) && thumbBounds.contains(pointer)) {
                scrollDragState = ScrollDragState(pointer.y, scrollState.offset)
            }
            return
        }

        scrollState = scrollState.withOffset(
            MapViewerScrollbar.dragOffset(
                bounds = layout.scrollbar,
                state = scrollState,
                dragStartPointerY = state.startPointerY,
                dragStartOffset = state.startOffset,
                currentPointerY = pointer.y,
            ),
        )
        render()
    }

    private fun MapViewerDialogBounds.contains(point: Vector2D): Boolean =
        point.x in left..right && point.y in top..bottom

    private sealed interface DialogDragState {
        val startPointer: Vector2D
        val original: MapViewerDialogBounds

        data class Move(
            override val startPointer: Vector2D,
            override val original: MapViewerDialogBounds,
        ) : DialogDragState

        data class Resize(
            override val startPointer: Vector2D,
            override val original: MapViewerDialogBounds,
        ) : DialogDragState
    }

    private enum class DialogDragMode {
        MOVE,
        RESIZE,
    }

    private data class ScrollDragState(
        val startPointerY: Double,
        val startOffset: Double,
    )

    private companion object {
        const val Z_INDEX = 50_000.0
    }
}
