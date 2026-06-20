package io.hsar.mapgenerator.commandgui.korge.interaction

import io.hsar.mapgenerator.commandgui.korge.ui.MapViewerTextViews
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

@OptIn(KorgeInternal::class)
internal class ContextMenuManager(
    private val root: Container,
) {
    private val layer = root.container()
    private var state: ContextMenuState? = null
    private var previousLeftHeld = false
    private var previousRightHeld = false
    private var closeRequested = false

    init {
        layer.zIndex = CONTEXT_MENU_Z_INDEX
        layer.mouseEnabled = true
        layer.visible = false
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
        if (actions.isEmpty()) {
            return
        }

        close()
        layer.visible = true
        val stage = root.stage ?: return
        val stageWidth = stage.views.actualVirtualWidth.toDouble()
        val stageHeight = stage.views.actualVirtualHeight.toDouble()
        val menuWidth = 180.0
        val menuHeight = (actions.size * ACTION_HEIGHT) + PADDING * 2
        val left = screenPoint.x.coerceIn(0.0, (stageWidth - menuWidth).coerceAtLeast(0.0))
        val top = screenPoint.y.coerceIn(0.0, (stageHeight - menuHeight).coerceAtLeast(0.0))

        val backdrop = layer.container()
        backdrop.solidRect(stageWidth, stageHeight, RGBA(0, 0, 0, 0))
        backdrop.mouse {
            onDown {
                if (button == MouseButton.LEFT) {
                    close()
                }
            }
        }

        val menu = layer.container()
        menu.xy(left, top)
        menu.solidRect(menuWidth, menuHeight, RGBA(255, 255, 255, 255))
        menu.solidRect(menuWidth - 4.0, menuHeight - 4.0, RGBA(48, 58, 58, 255)) {
            x = 2.0
            y = 2.0
        }

        actions.forEachIndexed { index, action ->
            val option = menu.container()
            option.xy(PADDING, PADDING + (index * ACTION_HEIGHT))
            option.solidRect(menuWidth - PADDING * 2, ACTION_HEIGHT - 4.0, RGBA(255, 255, 255, 16)) {
                x = 0.0
                y = 2.0
            }
            option.mouse {
                onDown {
                    if (button == MouseButton.LEFT) {
                        ContextMenuActionExecutor.execute(
                            action = action.onSelected,
                            dismiss = ::requestClose,
                            onActionSelected = onActionSelected,
                        )
                    }
                }
            }
            MapViewerTextViews.leftBox(
                parent = option,
                text = action.label,
                width = menuWidth - PADDING * 2,
                height = ACTION_HEIGHT - 4.0,
                color = Palette.Colours.WHITE,
            ) {
                x = 12.0
            }
        }

        state = ContextMenuState(
            menu = menu,
            backdrop = backdrop,
            bounds = ScreenBounds(
                left = left,
                top = top,
                width = menuWidth,
                height = menuHeight,
            ),
        )
    }

    fun close() {
        state?.menu?.removeFromParent()
        state?.backdrop?.removeFromParent()
        layer.removeChildren()
        layer.visible = false
        state = null
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

        if (leftHeld && !previousLeftHeld && ContextMenuDismissal.shouldDismiss(current.bounds, pointer)) {
            close()
        }
        if (rightHeld && !previousRightHeld && ContextMenuDismissal.shouldDismiss(current.bounds, pointer)) {
            close()
        }

        previousLeftHeld = leftHeld
        previousRightHeld = rightHeld
    }

    private data class ContextMenuState(
        val menu: Container,
        val backdrop: Container,
        val bounds: ScreenBounds,
    )

    private companion object {
        private const val CONTEXT_MENU_Z_INDEX = 35_000.0
        private const val ACTION_HEIGHT = 44.0
        private const val PADDING = 4.0
    }
}

internal object ContextMenuDismissal {
    fun shouldDismiss(bounds: ScreenBounds, pointer: Vector2D) =
        !bounds.contains(pointer)
}

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
