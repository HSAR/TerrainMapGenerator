package io.hsar.mapgenerator.commandgui.korge.ui

import com.fasterxml.jackson.databind.JsonNode
import io.hsar.mapgenerator.commandgui.ViewerPoint
import io.hsar.mapgenerator.commandgui.korge.scene.MapViewerSceneLayers
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
    fun persisted(
        bounds: MapViewerDialogBounds,
        minimized: Boolean,
        pinned: Boolean,
        pinAnchor: Vector2D?,
    ): MapViewerPersistedDialog =
        MapViewerPersistedDialog(
            type,
            targetId,
            bounds,
            draft(),
            minimized,
            pinned,
            pinAnchor?.let { ViewerPoint(it.x, it.y) },
        )
}

internal class MapViewerDialogWindow(
    private val root: Container,
    private val focusShadowRoot: Container? = null,
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
    private val renderContent: (Container, Double, Double) -> Unit,
    private val scrollTargetAt: (Vector2D) -> Boolean = { false },
    private val mode: MapViewerDialogMode = MapViewerDialogMode.FLOATING,
    private val dialogManager: MapViewerDialogManager? = null,
) {
    private val layer = root.container()
    private val backdropLayer = layer.container().also {
        it.zIndex = 0.0
        it.mouseEnabled = false
    }
    private val focusShadowLayer = resolveFocusShadowParent().container().also {
        it.zIndex = if (it.parent == layer) 0.0 else Z_INDEX
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
    private var minimized = false
    private var pinned = false
    private var pinAnchor: Vector2D? = null
    private var dockedPosition: Vector2D? = null
    private var lastViewport: io.hsar.mapgenerator.commandgui.korge.interaction.CameraViewport? = null

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
        minimized = persistence.restored?.minimized == true && hasViewerWindowControls()
        pinned = persistence.restored?.let { it.pinned && it.pinAnchor != null } == true &&
                hasViewerWindowControls()
        pinAnchor = persistence.restored?.pinAnchor
            .takeIf { pinned }
            ?.let { Vector2D(it.x, it.y) }
        lastViewport = dialogManager?.viewport()
        installInputHandlers()
        if (minimized && !pinned) dock()
        if (pinned) updatePinnedPosition()
        render()
    }

    fun close() {
        if (!isOpen) {
            return
        }

        isOpen = false
        dialogManager?.undockMinimized(this)
        clearFocusShadow()
        if (focusShadowLayer.parent != layer) {
            focusShadowLayer.removeFromParent()
        }
        layer.removeFromParent()
    }

    fun blocksScreenPoint(point: Vector2D): Boolean =
        isOpen && (
                mode == MapViewerDialogMode.MODAL ||
                        displayBounds()?.contains(point) == true ||
                        (!minimized && resizeHandleBounds()?.contains(point) == true)
                )

    val isModal: Boolean
        get() = mode == MapViewerDialogMode.MODAL

    fun persistedDialog(): MapViewerPersistedDialog? =
        bounds?.let { persistence.persisted(it, minimized, pinned, pinAnchor) }

    val currentBounds: MapViewerDialogBounds?
        get() = bounds

    internal val currentDisplayBounds: MapViewerDialogBounds?
        get() = displayBounds()

    internal val isMinimized: Boolean
        get() = minimized

    internal val isPinned: Boolean
        get() = pinned

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
            if (!pinned) {
                bounds = bounds?.let { with(MapViewerDialogLayoutBuilder) { it.clampedTo(width, height, minimumWidth()) } }
            }
            dialogManager?.resizeDock(width)
            if (pinned) updatePinnedPosition()
            render()
        }
        layer.mouse {
            onClick {
                // Consume backdrop clicks so the map never receives modal-dialog input.
            }
            onScrollAnywhere {
                val pointer = views.input.mousePos
                if (!minimized &&
                    displayBounds()?.contains(pointer) == true &&
                    !scrollTargetAt(pointer) &&
                    scrollState.needsScrolling
                ) {
                    scrollState = scrollState.scrolledBy(scrollDeltaYLines * MapViewerScrollbar.SCROLL_STEP_PX)
                    updateScrollPosition()
                }
            }
        }
        layer.addUpdater {
            val pointer = layer.stage?.views?.input?.mousePos ?: return@addUpdater
            val leftHeld = layer.stage?.views?.input?.mouseButtonPressed(korlibs.event.MouseButton.LEFT) ?: false
            refreshTitleIfChanged()
            refreshPinnedPositionIfRequired()
            val focusTargetBounds = focusTargetBoundsProvider()
            if (focusTargetBounds != lastFocusTargetBounds) {
                renderFocusShadow(focusTargetBounds)
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
                ).let {
                    if (pinned) {
                        it.resizedTo(
                            it.width.coerceAtLeast(minimumWidth()),
                            it.height.coerceAtLeast(MapViewerDialogLayoutBuilder.MIN_HEIGHT),
                        )
                    } else {
                        with(MapViewerDialogLayoutBuilder) {
                            it.resizedWithinViewport(viewportWidth, viewportHeight, minimumWidth())
                        }
                    }
                }
            }.let {
                if (pinned) it else with(MapViewerDialogLayoutBuilder) {
                    it.clampedTo(viewportWidth, viewportHeight, minimumWidth())
                }
            }
            when (state) {
                is DialogDragState.Move -> {
                    moveTo(nextBounds, focusTargetBounds)
                    updatePinAnchor()
                }
                is DialogDragState.Resize -> {
                    bounds = nextBounds
                    updatePinAnchor()
                    render(focusTargetBounds)
                }
            }
        }
    }

    private fun render(focusTargetBounds: List<ScreenBounds> = focusTargetBoundsProvider()) {
        val currentBounds = displayBounds() ?: return
        val minimumWidth = minimumWidth()
        val clampedBounds = currentBounds.let { current ->
            with(MapViewerDialogLayoutBuilder) { current.clampedTo(viewportWidth, viewportHeight, minimumWidth) }
        }
        if (!pinned && !minimized && clampedBounds != currentBounds) {
            bounds = clampedBounds
            render(focusTargetBounds)
            return
        }
        val layout = MapViewerDialogLayoutBuilder.build(currentBounds, actions.size, hasViewerWindowControls())
        if (minimized) {
            renderMinimized(layout)
            return
        }
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
        renderBackdrop()
        renderFocusShadow(focusTargetBounds)

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
            showWindowControls = hasViewerWindowControls(),
            pinned = pinned,
            onMinimize = ::minimize,
            onPin = ::togglePinned,
        )
        renderedTitle = resolvedTitle

        panel.clipContainer(Size2D(layout.contentViewport.width, layout.contentViewport.height)) {
            xy(layout.contentViewport.left, layout.contentViewport.top)
            zIndex = CONTENT_Z_INDEX
            val content = container()
            contentContainer = content
            content.xy(0.0, -scrollState.offset)
            renderContent(content, layout.contentViewport.width, layout.contentViewport.height)
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

    private fun renderMinimized(layout: MapViewerDialogLayout) {
        contentContainer = null
        scrollbarContainer = null
        titleBar = null
        panelLayer.removeChildren()
        backdropLayer.removeChildren()
        clearFocusShadow()
        panelLayer.xy(layout.bounds.left, layout.bounds.top)
        val panel = panelLayer.container()
        panel.solidRect(layout.bounds.width, layout.bounds.height, RGBA(0, 0, 0, 0)).mouse {
            onClick {
                // Consume clicks within the minimized frame so they do not reach the map.
            }
        }
        val resolvedTitle = titleProvider()
        titleBar = MapViewerDialogChrome.render(
            parent = panel,
            layout = layout,
            title = resolvedTitle,
            titleTabs = emptyList(),
            onClose = onClose,
            onBeginMove = {},
            onBeginResize = {},
            showWindowControls = true,
            minimized = true,
            pinned = pinned,
            onMinimize = ::restore,
            onPin = ::togglePinned,
            onTitleClick = ::restore,
            renderResizeHandle = false,
        )
        renderedTitle = resolvedTitle
    }

    internal fun moveTo(bounds: MapViewerDialogBounds, focusTargetBounds: List<ScreenBounds> = focusTargetBoundsProvider()) {
        if (!isOpen) {
            return
        }

        this.bounds = bounds
        panelLayer.xy(bounds.left, bounds.top)
        renderFocusShadow(focusTargetBounds)
    }

    private fun beginDrag(mode: DialogDragMode) {
        if (minimized) return
        val pointer = layer.stage?.views?.input?.mousePos ?: return
        val currentBounds = bounds ?: return
        dragState = when (mode) {
            DialogDragMode.MOVE -> DialogDragState.Move(pointer, currentBounds)
            DialogDragMode.RESIZE -> DialogDragState.Resize(pointer, currentBounds)
        }
    }

    private fun updateScrollDrag(pointer: Vector2D) {
        val currentBounds = bounds ?: return
        val layout = MapViewerDialogLayoutBuilder.build(currentBounds, actions.size, hasViewerWindowControls())
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
        val currentBounds = displayBounds() ?: return
        titleBar?.let {
            MapViewerDialogChrome.refreshTitleBar(
                titleBar = it,
                layout = MapViewerDialogLayoutBuilder.build(currentBounds, actions.size, hasViewerWindowControls()),
                title = title,
                titleTabs = if (minimized) emptyList() else titleTabs(),
                onBeginMove = { if (!minimized) beginDrag(DialogDragMode.MOVE) },
            )
        }
        renderedTitle = title
    }

    private fun updateScrollPosition() {
        contentContainer?.y = -scrollState.offset
        val currentBounds = bounds ?: return
        val layout = MapViewerDialogLayoutBuilder.build(currentBounds, actions.size, hasViewerWindowControls())
        scrollbarContainer?.let { scrollbar ->
            scrollbar.removeChildren()
            MapViewerScrollbar.render(scrollbar, layout.scrollbar, scrollState) { pointerY ->
                scrollDragState = ScrollDragState(pointerY, scrollState.offset)
            }
        }
    }

    private fun renderBackdrop() {
        backdropLayer.removeChildren()
        if (mode == MapViewerDialogMode.MODAL) {
            backdropLayer.solidRect(viewportWidth.toDouble(), viewportHeight.toDouble(), RGBA(0, 0, 0, 160))
        }
    }

    private fun renderFocusShadow(focusTargetBounds: List<ScreenBounds>) {
        if (minimized) {
            lastFocusTargetBounds = focusTargetBounds
            focusShadowLayer.removeChildren()
            return
        }
        val currentBounds = bounds ?: return
        lastFocusTargetBounds = focusTargetBounds
        focusShadowLayer.removeChildren()
        focusShadowShape(currentBounds, focusTargetBounds)?.let { shape ->
            focusShadowLayer.graphics {
                fill(FOCUS_SHADOW_COLOR) {
                    path(shape.toVectorPath())
                }
            }
        }
    }

    private fun resizeHandleBounds(): ScreenBounds? {
        val currentBounds = bounds ?: return null
        return MapViewerDialogLayoutBuilder
            .build(currentBounds, actions.size, hasViewerWindowControls())
            .resizeHandle
            .offsetBy(currentBounds.left, currentBounds.top)
    }

    private fun MapViewerDialogBounds.contains(point: Vector2D): Boolean =
        point.x in left..right && point.y in top..bottom

    private fun minimumWidth(): Double =
        maxOf(
            MapViewerDialogLayoutBuilder.minimumShellWidth(contentMinimumWidth(), actions.size),
            MapViewerDialogTitleTabLayoutBuilder.minimumShellWidth(titleTabs().map { it.label }, hasViewerWindowControls()),
            if (hasViewerWindowControls()) {
                MapViewerDialogLayoutBuilder.CLOSE_BUTTON_SIZE * 3.0 +
                        MapViewerDialogChrome.TITLE_TEXT_LEFT +
                        MapViewerDialogChrome.TITLE_TEXT_RIGHT_GAP +
                        MapViewerDialogLayoutBuilder.BORDER_INSET * 2.0
            } else {
                0.0
            },
        )

    private fun hasViewerWindowControls(): Boolean =
        mode == MapViewerDialogMode.FLOATING && dialogManager?.supportsViewerWindowControls == true

    private fun displayBounds(): MapViewerDialogBounds? =
        bounds?.let { expanded ->
            if (!minimized) {
                expanded
            } else {
                val position = if (pinned) projectedPinPosition(expanded.width) else dockedPosition
                val resolvedPosition = position ?: Vector2D(expanded.left, MapViewerDialogLayoutBuilder.OUTER_PADDING)
                MapViewerDialogBounds(
                    left = resolvedPosition.x,
                    top = resolvedPosition.y,
                    width = expanded.width,
                    height = MapViewerDialogLayoutBuilder.CLOSE_BUTTON_SIZE,
                )
            }
        }

    internal fun minimize() {
        if (!hasViewerWindowControls() || minimized) return
        minimized = true
        dragState = null
        if (pinned) updatePinnedPosition() else dock()
        render()
    }

    internal fun restore() {
        if (!minimized) return
        dialogManager?.undockMinimized(this)
        dockedPosition = null
        minimized = false
        if (pinned) updatePinnedPosition()
        render()
    }

    internal fun togglePinned() {
        if (!hasViewerWindowControls()) return
        if (!pinned) {
            val displayed = displayBounds() ?: return
            pinned = true
            dialogManager?.undockMinimized(this)
            dockedPosition = null
            setPinAnchor(displayed)
        } else {
            val displayed = displayBounds()
            pinned = false
            pinAnchor = null
            displayed?.let { bounds = bounds?.movedTo(it.left, it.top) }
            if (minimized) dock()
        }
        render()
    }

    private fun dock() {
        val width = bounds?.width ?: return
        dialogManager?.dockMinimized(this, width, viewportWidth) { position ->
            dockedPosition = position
            panelLayer.xy(position.x, position.y)
        }
    }

    private fun updatePinAnchor() {
        if (!pinned) return
        setPinAnchor(displayBounds() ?: return)
    }

    private fun setPinAnchor(currentBounds: MapViewerDialogBounds) {
        val viewport = dialogManager?.viewport() ?: return
        pinAnchor = Vector2D(
            viewport.left + currentBounds.right / viewport.zoom,
            viewport.top + currentBounds.top / viewport.zoom,
        )
        lastViewport = viewport
    }

    private fun projectedPinPosition(width: Double): Vector2D? {
        val anchor = pinAnchor ?: return null
        val viewport = dialogManager?.viewport() ?: return null
        return Vector2D(viewport.screenX(anchor.x) - width, viewport.screenY(anchor.y))
    }

    private fun updatePinnedPosition() {
        if (!pinned) return
        val expanded = bounds ?: return
        val position = projectedPinPosition(expanded.width) ?: return
        if (!minimized) bounds = expanded.movedTo(position.x, position.y)
        panelLayer.xy(position.x, position.y)
        renderFocusShadow(focusTargetBoundsProvider())
        lastViewport = dialogManager?.viewport()
    }

    private fun clearFocusShadow() {
        lastFocusTargetBounds = emptyList()
        focusShadowLayer.removeChildren()
    }

    private fun resolveFocusShadowParent(): Container =
        focusShadowRoot
            ?: root.parent?.children
                ?.filterIsInstance<Container>()
                ?.firstOrNull { it.name == MapViewerSceneLayers.UI_UNDERLAY_NAME }
            ?: layer

    internal fun refreshPinnedPositionIfRequired() {
        if (!pinned) return
        val viewport = dialogManager?.viewport() ?: return
        if (viewport != lastViewport) updatePinnedPosition()
    }

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
