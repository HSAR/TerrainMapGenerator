package io.hsar.mapgenerator.commandgui.korge.interaction

import io.hsar.mapgenerator.commandgui.MapViewerFeatureFlags
import io.hsar.mapgenerator.commandgui.korge.ui.MapViewerBoxedIcon
import io.hsar.mapgenerator.commandgui.korge.ui.MapViewerChevron
import io.hsar.mapgenerator.commandgui.korge.ui.MapViewerChevronDirection
import io.hsar.mapgenerator.commandgui.korge.ui.MapViewerDialogChrome
import io.hsar.mapgenerator.commandgui.korge.ui.MapViewerTextInputValidation
import io.hsar.mapgenerator.commandgui.korge.ui.MapViewerTextViews
import io.hsar.mapgenerator.commandgui.korge.ui.MapViewerUiBoxes
import io.hsar.mapgenerator.image.Palette
import korlibs.event.MouseButton
import korlibs.image.color.RGBA
import korlibs.korge.input.mouse
import korlibs.korge.internal.KorgeInternal
import korlibs.korge.view.Container
import korlibs.korge.view.addUpdater
import korlibs.korge.view.container
import korlibs.korge.view.solidRect
import korlibs.korge.view.xy
import korlibs.math.geom.Vector2D
import java.util.concurrent.TimeUnit

@OptIn(KorgeInternal::class)
internal class ContextMenuManager(
    private val root: Container,
) {
    private val layer = root.container()
    private val surfaceLayer = layer.container()
    private val menusLayer = layer.container()
    private var state: ContextMenuState? = null
    private var submenuState: ContextMenuState? = null
    private var submenuLastActiveAtMs: Long? = null
    private var tooltip: Container? = null
    private var previousLeftHeld = false
    private var previousRightHeld = false
    private var closeRequested = false

    init {
        layer.zIndex = CONTEXT_MENU_Z_INDEX
        layer.mouseEnabled = true
        layer.visible = false
        surfaceLayer.zIndex = 0.0
        surfaceLayer.mouseEnabled = false
        menusLayer.zIndex = 1.0
        menusLayer.mouseEnabled = true
        root.addUpdater {
            if (closeRequested) {
                closeRequested = false
                close()
            }
            val input = root.stage?.views?.input ?: return@addUpdater
            updateDismissal(
                pointer = input.mousePos,
                leftHeld = input.mouseButtonPressed(MouseButton.LEFT),
                rightHeld = input.mouseButtonPressed(MouseButton.RIGHT),
            )
            updateSubmenuHover(pointer = input.mousePos)
        }
    }

    val isOpen: Boolean
        get() = state != null

    fun blocksScreenPoint(point: Vector2D): Boolean =
        state?.let { true } ?: false

    fun open(
        target: MapContextMenuTarget,
        screenPoint: Vector2D,
        actions: List<MapPlacedObjectMenuAction> = target.contextMenuActions(),
        onActionSelected: () -> Unit = {},
    ) {
        val visibleActions = actions.visibleFor(MapViewerFeatureFlags.enabled)
        if (visibleActions.isEmpty()) {
            return
        }

        close()
        layer.visible = true
        val stage = root.stage ?: return
        val stageWidth = stage.views.actualVirtualWidth.toDouble()
        val stageHeight = stage.views.actualVirtualHeight.toDouble()
        val menuWidth = menuWidth(visibleActions)
        val menuHeight = ContextMenuLayout.menuHeight(visibleActions.size)
        val left = screenPoint.x.coerceIn(0.0, (stageWidth - menuWidth).coerceAtLeast(0.0))
        val top = screenPoint.y.coerceIn(0.0, (stageHeight - menuHeight).coerceAtLeast(0.0))

        state = renderMenu(
            actions = visibleActions,
            left = left,
            top = top,
            onActionSelected = onActionSelected,
        )
        renderSurface()
    }

    fun close() {
        state?.menu?.removeFromParent()
        submenuState?.menu?.removeFromParent()
        hideTooltip()
        surfaceLayer.removeChildren()
        menusLayer.removeChildren()
        layer.visible = false
        state = null
        submenuState = null
        submenuLastActiveAtMs = null
    }

    private fun requestClose() {
        closeRequested = true
    }

    private fun updateDismissal(pointer: Vector2D, leftHeld: Boolean, rightHeld: Boolean) {
        val current = state
        if (current == null) {
            previousLeftHeld = leftHeld
            previousRightHeld = rightHeld
            return
        }

        if (leftHeld && !previousLeftHeld) {
            val submenu = submenuState
            when {
                submenu?.bounds?.contains(pointer) == true -> selectAction(submenu, pointer)
                current.bounds.contains(pointer) -> selectAction(current, pointer)
                else -> close()
            }
        }
        if (rightHeld && !previousRightHeld && !containsPointer(pointer)) {
            close()
        }

        previousLeftHeld = leftHeld
        previousRightHeld = rightHeld
    }

    private fun containsPointer(pointer: Vector2D): Boolean =
        state?.bounds?.contains(pointer) == true || submenuState?.bounds?.contains(pointer) == true

    private fun selectAction(current: ContextMenuState, pointer: Vector2D) {
        val localY = pointer.y - current.bounds.top - PADDING
        if (localY < 0.0) {
            return
        }

        current.actions.getOrNull((localY / ACTION_HEIGHT).toInt())?.let { action ->
            if (!action.enabled) {
                return
            }
            if (action.submenuActions.isNotEmpty()) {
                toggleSubmenu(current, current.actions.indexOf(action), action)
            } else {
                ContextMenuActionExecutor.execute(
                    action = action.onSelected,
                    dismiss = ::requestClose,
                    onActionSelected = current.onActionSelected,
                )
            }
        }
    }

    private fun renderMenu(
        actions: List<MapPlacedObjectMenuAction>,
        left: Double,
        top: Double,
        onActionSelected: () -> Unit,
        sourceAction: MapPlacedObjectMenuAction? = null,
        parentBounds: ScreenBounds? = null,
        parentIndex: Int = -1,
    ): ContextMenuState {
        val menuWidth = menuWidth(actions)
        val menuHeight = ContextMenuLayout.menuHeight(actions.size)
        val menu = menusLayer.container()
        menu.xy(left, top)

        val state = ContextMenuState(
            menu = menu,
            bounds = ScreenBounds(
                left = left,
                top = top,
                width = menuWidth,
                height = menuHeight,
            ),
            actions = actions,
            onActionSelected = onActionSelected,
            sourceAction = sourceAction,
            parentBounds = parentBounds,
            parentIndex = parentIndex,
        )

        menu.solidRect(menuWidth, menuHeight, RGBA(0, 0, 0, 0))
        actions.forEachIndexed { index, action ->
            val option = menu.container()
            option.xy(ContextMenuLayout.PADDING, ContextMenuLayout.optionTop(index))
            val optionWidth = menuWidth - PADDING * 2
            val optionHeight = ACTION_HEIGHT - ContextMenuLayout.OPTION_VERTICAL_INSET * 2
            MapViewerUiBoxes.button(
                parent = option,
                width = optionWidth,
                height = optionHeight,
                active = action.enabled,
            ).xy(0.0, ContextMenuLayout.OPTION_VERTICAL_INSET)
            val labelWidth = optionWidth - optionTrailingWidth(action)
            MapViewerTextViews.leftBox(
                parent = option,
                text = action.label,
                width = labelWidth,
                height = optionHeight,
                color = Palette.Colours.WHITE,
            ) {
                x = ContextMenuLayout.LABEL_LEFT_PADDING
                alpha = if (action.enabled) 1.0 else 0.55
            }
            if (!action.enabled) {
                val warning = option.container()
                warning.xy(optionWidth - ContextMenuLayout.DISABLED_ICON_WIDTH, 0.0)
                val icon = warning.container()
                icon.xy(
                    (ContextMenuLayout.DISABLED_ICON_WIDTH - MapViewerTextInputValidation.VALIDATION_ICON_SIZE) * 0.5,
                    (ACTION_HEIGHT - 4.0 - MapViewerTextInputValidation.VALIDATION_ICON_SIZE) * 0.5,
                )
                MapViewerTextInputValidation.validationFailedIndicator(icon)
            }
            if (action.submenuActions.isNotEmpty()) {
                val arrow = option.container()
                arrow.xy(
                    optionWidth - optionTrailingWidth(action),
                    contextMenuSubmenuIndicatorTop(),
                )
                renderContextMenuSubmenuIndicator(
                    parent = arrow,
                    width = optionTrailingWidth(action),
                    height = optionHeight,
                )
            }
            option.mouse {
                onOver {
                    if (!action.enabled) {
                        scheduleSubmenuClose()
                        action.disabledTooltipText?.let { showTooltip(menu, option.globalPos + Vector2D(optionWidth, 0.0), it) }
                    } else if (action.submenuActions.isNotEmpty()) {
                        hideTooltip()
                        openSubmenu(state, index, action)
                    } else {
                        scheduleSubmenuClose()
                        hideTooltip()
                    }
                }
                onOut {
                    if (!action.enabled) {
                        hideTooltip()
                    }
                }
                onClick {
                    if (!action.enabled) {
                        return@onClick
                    }
                    if (action.submenuActions.isNotEmpty()) {
                        toggleSubmenu(state, index, action)
                    } else {
                        ContextMenuActionExecutor.execute(
                            action = action.onSelected,
                            dismiss = ::requestClose,
                            onActionSelected = state.onActionSelected,
                        )
                    }
                }
            }
        }

        return state
    }

    private fun openSubmenu(
        parent: ContextMenuState,
        parentIndex: Int,
        action: MapPlacedObjectMenuAction,
    ) {
        if (action.submenuActions.isEmpty()) {
            closeSubmenu()
            return
        }

        val stage = root.stage ?: return
        val stageWidth = stage.views.actualVirtualWidth.toDouble()
        val stageHeight = stage.views.actualVirtualHeight.toDouble()
        val menuWidth = menuWidth(action.submenuActions)
        val menuHeight = ContextMenuLayout.menuHeight(action.submenuActions.size)
        val rowTop = parent.bounds.top + (parentIndex * ACTION_HEIGHT)
        val preferredLeft = parent.bounds.right
        val left = if (preferredLeft + menuWidth <= stageWidth) {
            preferredLeft
        } else {
            (parent.bounds.left - menuWidth).coerceAtLeast(0.0)
        }
        val top = rowTop.coerceIn(0.0, (stageHeight - menuHeight).coerceAtLeast(0.0))

        closeSubmenu()
        submenuState = renderMenu(
            actions = action.submenuActions,
            left = left,
            top = top,
            onActionSelected = parent.onActionSelected,
            sourceAction = action,
            parentBounds = parent.bounds,
            parentIndex = parentIndex,
        )
        submenuLastActiveAtMs = currentTimeMs()
        renderSurface()
    }

    private fun toggleSubmenu(
        parent: ContextMenuState,
        parentIndex: Int,
        action: MapPlacedObjectMenuAction,
    ) {
        if (submenuState?.sourceAction == action) {
            closeSubmenu()
        } else {
            openSubmenu(parent, parentIndex, action)
        }
    }

    private fun closeSubmenu() {
        submenuState?.menu?.removeFromParent()
        submenuState = null
        submenuLastActiveAtMs = null
        renderSurface()
    }

    private fun scheduleSubmenuClose() {
        if (submenuState != null && submenuLastActiveAtMs == null) {
            submenuLastActiveAtMs = currentTimeMs()
        }
    }

    private fun updateSubmenuHover(pointer: Vector2D) {
        val submenu = submenuState ?: run {
            submenuLastActiveAtMs = null
            return
        }
        val keepOpenBounds = submenu.parentBounds?.let { parentBounds ->
            contextMenuSubmenuHoldBounds(parentBounds, submenu.parentIndex, submenu.bounds)
        } ?: submenu.bounds
        if (keepOpenBounds.contains(pointer)) {
            submenuLastActiveAtMs = currentTimeMs()
            return
        }

        val lastActiveAt = submenuLastActiveAtMs ?: currentTimeMs()
        if (currentTimeMs() - lastActiveAt >= ContextMenuLayout.SUBMENU_CLOSE_DELAY_MS) {
            closeSubmenu()
        }
    }

    private fun menuWidth(actions: List<MapPlacedObjectMenuAction>): Double =
        maxOf(
            ContextMenuLayout.MIN_MENU_WIDTH,
            actions.maxOfOrNull { action ->
                MapViewerTextViews.measure(action.label).width + ContextMenuLayout.LABEL_LEFT_PADDING + optionTrailingWidth(action)
            } ?: 0.0,
        ) + ContextMenuLayout.PADDING * 2

    private fun optionTrailingWidth(action: MapPlacedObjectMenuAction): Double =
        (if (action.submenuActions.isEmpty()) 0.0 else ContextMenuLayout.SUBMENU_ARROW_WIDTH) +
                (if (action.enabled) 0.0 else ContextMenuLayout.DISABLED_ICON_WIDTH)

    private fun showTooltip(parent: Container, anchor: Vector2D, text: String) {
        hideTooltip()
        tooltip = MapViewerTextInputValidation.validationExplanationTooltip(parent, anchor, text)
    }

    private fun hideTooltip() {
        tooltip?.removeFromParent()
        tooltip = null
    }

    private fun renderSurface() {
        surfaceLayer.removeChildren()
        val geometry = contextMenuSurfaceGeometry(
            panels = listOfNotNull(state?.bounds, submenuState?.bounds),
        ) ?: return
        MapViewerUiBoxes.shapePanel(
            parent = surfaceLayer,
            shape = geometry.fillArea.toVectorPath(),
            fillAlpha = MapViewerDialogChrome.DIALOG_PANEL_FILL_ALPHA,
            borderWidth = ContextMenuLayout.BORDER_WIDTH,
        ).apply {
            mouseEnabled = false
        }
    }

    private data class ContextMenuState(
        val menu: Container,
        val bounds: ScreenBounds,
        val actions: List<MapPlacedObjectMenuAction>,
        val onActionSelected: () -> Unit,
        val sourceAction: MapPlacedObjectMenuAction? = null,
        val parentBounds: ScreenBounds? = null,
        val parentIndex: Int = -1,
    )

    private companion object {
        private const val CONTEXT_MENU_Z_INDEX = 35_000.0
        private const val ACTION_HEIGHT = ContextMenuLayout.ACTION_HEIGHT
        private const val PADDING = ContextMenuLayout.PADDING
    }

    private fun currentTimeMs(): Long =
        TimeUnit.NANOSECONDS.toMillis(System.nanoTime())
}

internal object ContextMenuDismissal {
    fun shouldDismiss(bounds: ScreenBounds, pointer: Vector2D) =
        !bounds.contains(pointer)
}

internal fun renderContextMenuSubmenuIndicator(
    parent: Container,
    width: Double,
    height: Double,
) {
    val button = MapViewerBoxedIcon.frame(parent, width, height)
    MapViewerChevron.render(
        parent = button,
        width = width,
        height = height,
        direction = MapViewerChevronDirection.RIGHT,
        color = MapViewerUiBoxes.toRgba(Palette.Colours.WHITE),
    )
}

internal fun contextMenuSubmenuIndicatorTop(): Double =
    ContextMenuLayout.OPTION_VERTICAL_INSET

internal object ContextMenuActionExecutor {
    fun execute(
        action: () -> Unit,
        dismiss: () -> Unit,
        onActionSelected: () -> Unit,
    ) {
        try {
            action()
        } finally {
            dismiss()
            onActionSelected()
        }
    }
}
