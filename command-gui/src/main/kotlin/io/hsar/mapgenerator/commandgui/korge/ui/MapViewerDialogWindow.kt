package io.hsar.mapgenerator.commandgui.korge.ui

import com.fasterxml.jackson.databind.JsonNode
import korlibs.image.color.RGBA
import korlibs.korge.input.mouse
import korlibs.korge.view.Container
import korlibs.korge.view.addUpdater
import korlibs.korge.view.clipContainer
import korlibs.korge.view.container
import korlibs.korge.view.graphics
import korlibs.korge.view.onStageResized
import korlibs.korge.view.solidRect
import korlibs.korge.view.xy
import korlibs.math.geom.Size2D
import korlibs.math.geom.Vector2D

internal enum class MapViewerDialogMode {
    MODAL,
    FLOATING,
}

/** Required save/restore contract for a floating dialog. */
internal data class MapViewerDialogPersistence(
    val type: String,
    val targetId: String? = null,
    val restored: MapViewerPersistedDialog? = null,
    val draft: () -> JsonNode? = { null },
) {
    fun persisted(bounds: MapViewerDialogBounds): MapViewerPersistedDialog =
        MapViewerPersistedDialog(type, targetId, bounds, draft())
}

internal class MapViewerDialogWindow(
    private val root: Container,
    private val title: String,
    private val titleProvider: () -> String = { title },
    private val titleTabs: () -> List<MapViewerDialogTitleTab> = { emptyList() },
    private val initialPosition: ((MapViewerDialogBounds) -> Vector2D?) = { null },
    private val persistence: MapViewerDialogPersistence,
    private val focusTargetBoundsProvider: (() -> List<ScreenBounds>) = { emptyList() },
    private val preferredWidth: Double,
    private val preferredHeight: Double,
    private val contentMinimumWidth: () -> Double = { 0.0 },
    private val contentHeight: (Double) -> Double,
    private val actions: List<MapViewerDialogAction> = emptyList(),
    private val onClose: () -> Unit,
    private val renderContent: (Container, Double) -> Unit,
    private val mode: MapViewerDialogMode = MapViewerDialogMode.FLOATING,
) {
    private val layer = root.container()
    private val shadowLayer = layer.container().also {
        it.zIndex = 0.0
        it.mouseEnabled = false
    }
    private val panelLayer = layer.container().also {
        it.zIndex = 1.0
    }
    private var viewportWidth = 0
    private var viewportHeight = 0
    private var bounds: MapViewerDialogBounds? = null
    private var lastFocusTargetBounds: List<ScreenBounds> = emptyList()
    private var scrollState = MapViewerScrollState(0.0, 0.0)
    private var scrollDragState: ScrollDragState? = null
    private var contentContainer: Container? = null
    private var scrollbarContainer: Container? = null
    private var titleBar: Container? = null
    private var renderedTitle: String? = null
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
        val defaultBounds = MapViewerDialogLayoutBuilder.defaultBounds(
            viewportWidth = viewportWidth,
            viewportHeight = viewportHeight,
            preferredWidth = preferredWidth,
            preferredHeight = preferredHeight,
            minimumWidth = minimumWidth(),
            actionButtonCount = actions.size,
            contentHeight = contentHeight,
        )
        bounds = persistence.restored?.bounds
            ?.let { with(MapViewerDialogLayoutBuilder) { it.clampedTo(viewportWidth, viewportHeight, minimumWidth()) } }
            ?: initialPosition(defaultBounds)
            ?.let { position -> defaultBounds.movedTo(position.x, position.y) }
            ?.let { with(MapViewerDialogLayoutBuilder) { it.clampedTo(viewportWidth, viewportHeight, minimumWidth()) } }
            ?: defaultBounds
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

    fun blocksScreenPoint(point: Vector2D): Boolean =
        isOpen && (
                mode == MapViewerDialogMode.MODAL ||
                        bounds?.contains(point) == true ||
                        resizeHandleBounds()?.contains(point) == true
                )

    val isModal: Boolean
        get() = mode == MapViewerDialogMode.MODAL

    fun persistedDialog(): MapViewerPersistedDialog? =
        bounds?.let(persistence::persisted)

    val currentBounds: MapViewerDialogBounds?
        get() = bounds

    fun refresh() {
        if (isOpen) {
            render()
        }
    }

    fun refreshTitle() {
        if (isOpen) {
            refreshTitle(titleProvider())
        }
    }

    private fun installInputHandlers() {
        layer.onStageResized { width, height ->
            viewportWidth = width
            viewportHeight = height
            bounds = bounds?.let { with(MapViewerDialogLayoutBuilder) { it.clampedTo(width, height, minimumWidth()) } }
            render()
        }
        layer.mouse {
            onClick {
                // Consume backdrop clicks so the map never receives modal-dialog input.
            }
            onScrollAnywhere {
                if (bounds?.contains(views.input.mousePos) == true && scrollState.needsScrolling) {
                    scrollState = scrollState.scrolledBy(scrollDeltaYLines * MapViewerScrollbar.SCROLL_STEP_PX)
                    updateScrollPosition()
                }
            }
        }
        layer.addUpdater {
            val pointer = layer.stage?.views?.input?.mousePos ?: return@addUpdater
            val leftHeld = layer.stage?.views?.input?.mouseButtonPressed(korlibs.event.MouseButton.LEFT) ?: false
            refreshTitleIfChanged()
            val focusTargetBounds = focusTargetBoundsProvider()
            if (focusTargetBounds != lastFocusTargetBounds) {
                renderShadow(focusTargetBounds)
            }

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

            val nextBounds = when (state) {
                is DialogDragState.Move -> state.original.movedTo(
                    left = state.original.left + pointer.x - state.startPointer.x,
                    top = state.original.top + pointer.y - state.startPointer.y,
                )

                is DialogDragState.Resize -> state.original.resizedTo(
                    width = state.original.width + pointer.x - state.startPointer.x,
                    height = state.original.height + pointer.y - state.startPointer.y,
                ).let { with(MapViewerDialogLayoutBuilder) { it.resizedWithinViewport(viewportWidth, viewportHeight, minimumWidth()) } }
            }.let { with(MapViewerDialogLayoutBuilder) { it.clampedTo(viewportWidth, viewportHeight, minimumWidth()) } }
            when (state) {
                is DialogDragState.Move -> moveTo(nextBounds, focusTargetBounds)
                is DialogDragState.Resize -> {
                    bounds = nextBounds
                    render(focusTargetBounds)
                }
            }
        }
    }

    private fun render(focusTargetBounds: List<ScreenBounds> = focusTargetBoundsProvider()) {
        val currentBounds = bounds ?: return
        val minimumWidth = minimumWidth()
        val clampedBounds = currentBounds.let { current ->
            with(MapViewerDialogLayoutBuilder) { current.clampedTo(viewportWidth, viewportHeight, minimumWidth) }
        }
        if (clampedBounds != currentBounds) {
            bounds = clampedBounds
            render(focusTargetBounds)
            return
        }
        val layout = MapViewerDialogLayoutBuilder.build(currentBounds, actions.size)
        val contentHeightValue = contentHeight(layout.contentViewport.width)
        scrollState = scrollState.withContent(
            viewportHeight = layout.contentViewport.height,
            contentHeight = contentHeightValue,
        )
        if (scrollDragState != null && !scrollState.needsScrolling) {
            scrollDragState = null
        }

        contentContainer = null
        scrollbarContainer = null
        titleBar = null
        panelLayer.removeChildren()
        renderShadow(focusTargetBounds)

        panelLayer.xy(currentBounds.left, currentBounds.top)
        val panel = panelLayer.container()
        val clickCatcher = panel.solidRect(currentBounds.width, currentBounds.height, RGBA(0, 0, 0, 0))
        clickCatcher.mouse {
            onClick {
                // Consume clicks within the dialog frame so they don't reach the map.
            }
        }
        val resolvedTitle = titleProvider()
        titleBar = MapViewerDialogChrome.render(
            parent = panel,
            layout = layout,
            title = resolvedTitle,
            titleTabs = titleTabs(),
            onClose = onClose,
            onBeginMove = { beginDrag(DialogDragMode.MOVE) },
            onBeginResize = { beginDrag(DialogDragMode.RESIZE) },
        )
        renderedTitle = resolvedTitle

        panel.clipContainer(Size2D(layout.contentViewport.width, layout.contentViewport.height)) {
            xy(layout.contentViewport.left, layout.contentViewport.top)
            zIndex = CONTENT_Z_INDEX
            val content = container()
            contentContainer = content
            content.xy(0.0, -scrollState.offset)
            renderContent(content, layout.contentViewport.width)
        }
        if (scrollState.needsScrolling) {
            scrollbarContainer = panel.container()
            MapViewerScrollbar.render(scrollbarContainer!!, layout.scrollbar, scrollState) { pointerY ->
                scrollDragState = ScrollDragState(pointerY, scrollState.offset)
            }
        }
        layout.footer?.let { footer ->
            val footerLayer = panel.container().also {
                it.xy(0.0, 0.0)
                it.zIndex = FOOTER_Z_INDEX
            }
            MapViewerDialogActions.render(footerLayer, footer.bounds, actions, footer.layout)
        }
    }

    internal fun moveTo(bounds: MapViewerDialogBounds, focusTargetBounds: List<ScreenBounds> = focusTargetBoundsProvider()) {
        if (!isOpen) {
            return
        }

        this.bounds = bounds
        panelLayer.xy(bounds.left, bounds.top)
        renderShadow(focusTargetBounds)
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

        val state = scrollDragState
        if (state == null) {
            return
        }

        val scrollbarBounds = layout.scrollbar.offsetBy(currentBounds.left, currentBounds.top)
        scrollState = scrollState.withOffset(
            MapViewerScrollbar.dragOffset(
                bounds = scrollbarBounds,
                state = scrollState,
                dragStartPointerY = state.startPointerY,
                dragStartOffset = state.startOffset,
                currentPointerY = pointer.y,
            ),
        )
        updateScrollPosition()
    }

    private fun refreshTitleIfChanged() {
        val nextTitle = titleProvider()
        if (nextTitle != renderedTitle) {
            refreshTitle(nextTitle)
        }
    }

    private fun refreshTitle(title: String) {
        val currentBounds = bounds ?: return
        titleBar?.let {
            MapViewerDialogChrome.refreshTitleBar(
                titleBar = it,
                layout = MapViewerDialogLayoutBuilder.build(currentBounds, actions.size),
                title = title,
                titleTabs = titleTabs(),
                onBeginMove = { beginDrag(DialogDragMode.MOVE) },
            )
        }
        renderedTitle = title
    }

    private fun updateScrollPosition() {
        contentContainer?.y = -scrollState.offset
        val currentBounds = bounds ?: return
        val layout = MapViewerDialogLayoutBuilder.build(currentBounds, actions.size)
        scrollbarContainer?.let { scrollbar ->
            scrollbar.removeChildren()
            MapViewerScrollbar.render(scrollbar, layout.scrollbar, scrollState) { pointerY ->
                scrollDragState = ScrollDragState(pointerY, scrollState.offset)
            }
        }
    }

    private fun renderShadow(focusTargetBounds: List<ScreenBounds>) {
        val currentBounds = bounds ?: return
        lastFocusTargetBounds = focusTargetBounds
        shadowLayer.removeChildren()
        if (mode == MapViewerDialogMode.MODAL) {
            shadowLayer.solidRect(viewportWidth.toDouble(), viewportHeight.toDouble(), RGBA(0, 0, 0, 160))
        }
        focusShadowShape(currentBounds, focusTargetBounds)?.let { shape ->
            shadowLayer.graphics {
                fill(FOCUS_SHADOW_COLOR) {
                    path(shape.toVectorPath())
                }
            }
        }
    }

    private fun resizeHandleBounds(): ScreenBounds? {
        val currentBounds = bounds ?: return null
        return MapViewerDialogLayoutBuilder
            .build(currentBounds, actions.size)
            .resizeHandle
            .offsetBy(currentBounds.left, currentBounds.top)
    }

    private fun MapViewerDialogBounds.contains(point: Vector2D): Boolean =
        point.x in left..right && point.y in top..bottom

    private fun minimumWidth(): Double =
        maxOf(
            MapViewerDialogLayoutBuilder.minimumShellWidth(contentMinimumWidth(), actions.size),
            MapViewerDialogTitleTabLayoutBuilder.minimumShellWidth(titleTabs().map { it.label }),
        )

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
        const val CONTENT_Z_INDEX = 100.0
        const val FOOTER_Z_INDEX = 1_000.0
        val FOCUS_SHADOW_COLOR = RGBA(0, 0, 0, 72)
    }
}
