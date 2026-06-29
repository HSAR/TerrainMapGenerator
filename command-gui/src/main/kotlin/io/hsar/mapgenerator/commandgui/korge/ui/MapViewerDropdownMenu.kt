package io.hsar.mapgenerator.commandgui.korge.ui

import io.hsar.mapgenerator.image.Palette
import korlibs.event.MouseButton
import korlibs.image.color.RGBA
import korlibs.korge.input.mouse
import korlibs.korge.internal.KorgeInternal
import korlibs.korge.view.Container
import korlibs.korge.view.Text
import korlibs.korge.view.addUpdater
import korlibs.korge.view.container
import korlibs.korge.view.solidRect
import korlibs.korge.view.xy
import korlibs.math.geom.Vector2D
import java.awt.Color

internal data class MapViewerDropdownOption<T>(
    val value: T?,
    val label: String,
    val swatchColor: Color? = null,
)

internal class MapViewerDropdownController<T>(
    private val onSelected: (T?) -> Unit,
) {
    var isOpen: Boolean = false
        private set

    var menuBounds: ScreenBounds? = null
        private set

    var selectedValue: T? = null
        private set

    fun open(bounds: ScreenBounds) {
        isOpen = true
        menuBounds = bounds
    }

    fun select(value: T?) {
        selectedValue = value
        dismiss()
        onSelected(value)
    }

    fun dismiss() {
        isOpen = false
        menuBounds = null
    }
}

@OptIn(KorgeInternal::class)
internal class MapViewerDropdownMenu<T>(
    private val root: Container,
    private val options: List<MapViewerDropdownOption<T>>,
    private val onSelected: (T?) -> Unit,
    private val labelForValue: (T?) -> String,
    private val swatchForValue: (T?) -> Color? = { null },
    private val menuWidth: Double = DEFAULT_MENU_WIDTH,
) {
    private val layer = root.container()
    private val controller = MapViewerDropdownController(onSelected)
    private var triggerLabel: Text? = null
    private var triggerSwatch: Container? = null
    private var closeAfterRelease = false
    private var popupState: PopupState<T>? = null

    init {
        layer.zIndex = Z_INDEX
        layer.mouseEnabled = true
        layer.visible = false
        root.addUpdater {
            updatePopupPosition()
            if (closeAfterRelease && root.stage?.views?.input?.mouseButtons == 0) {
                closeAfterRelease = false
                closePopup()
            }
        }
    }

    fun close() {
        closePopup()
    }

    fun renderField(
        parent: Container,
        label: String,
        value: T?,
        x: Double,
        y: Double,
        width: Double,
        height: Double = DEFAULT_FIELD_HEIGHT,
    ) {
        val labelContainer = parent.container()
        labelContainer.xy(x, y)
        MapViewerTextViews.label(labelContainer, label, color = Palette.Colours.WHITE)

        val field = parent.container()
        field.xy(x, y + 28.0)
        MapViewerUiBoxes.button(field, width, height)

        val swatchHost = field.container().also { it.xy(TEXT_LEFT_PADDING, (height - TRIGGER_SWATCH_SIZE) * 0.5) }
        MapViewerSwatchViews.render(swatchHost, swatchForValue(value), TRIGGER_SWATCH_SIZE)

        val labelHost = field.container().also { it.xy(TEXT_LEFT_PADDING + TRIGGER_SWATCH_SLOT_WIDTH, 0.0) }
        triggerLabel = MapViewerTextViews.leftBox(
            parent = labelHost,
            text = labelForValue(value),
            width = width - TRAILING_AREA_WIDTH - TRIGGER_SWATCH_SLOT_WIDTH - TEXT_LEFT_PADDING,
            height = height,
            color = Palette.Colours.WHITE,
        )
        triggerSwatch = swatchHost

        val caretHost = field.container().also { it.xy(width - CARET_WIDTH - CARET_RIGHT_PADDING, 0.0) }
        MapViewerTextViews.box(
            parent = caretHost,
            text = "v",
            width = CARET_WIDTH,
            height = height,
            color = Palette.Colours.WHITE,
        )

        field.mouse {
            onClick {
                openPopup(
                    anchorSource = field,
                    triggerWidth = width,
                    triggerHeight = height,
                    selectedValue = value,
                )
            }
        }
    }

    private fun openPopup(
        anchorSource: Container,
        triggerWidth: Double,
        triggerHeight: Double,
        selectedValue: T?,
    ) {
        val stage = root.stage ?: return
        val stageWidth = stage.views.actualVirtualWidth
        val stageHeight = stage.views.actualVirtualHeight
        val resolvedMenuWidth = maxOf(menuWidth, triggerWidth)
        val menuHeight = MENU_PADDING * 2.0 + (options.size * OPTION_HEIGHT)
        val bounds = MapViewerDropdownLayoutBuilder.menuBounds(
            anchor = anchorSource.globalPos,
            triggerHeight = triggerHeight,
            viewportWidth = stageWidth,
            viewportHeight = stageHeight,
            menuWidth = resolvedMenuWidth,
            menuHeight = menuHeight,
        )

        closePopup()
        controller.open(bounds)
        layer.visible = true

        val backdrop = layer.container()
        backdrop.solidRect(stageWidth.toDouble(), stageHeight.toDouble(), RGBA(0, 0, 0, 0))
        backdrop.mouse {
            onDown {
                if (button == MouseButton.LEFT) {
                    closePopup()
                }
            }
        }

        val menu = layer.container()
        menu.xy(bounds.left, bounds.top)
        MapViewerUiBoxes.box(menu, bounds.width, bounds.height)
        controller.open(bounds)
        popupState = PopupState(
            anchorSource = anchorSource,
            triggerHeight = triggerHeight,
            triggerWidth = resolvedMenuWidth,
            menuHeight = menuHeight,
            selectedValue = selectedValue,
            backdrop = backdrop,
            menu = menu,
        )

        options.forEachIndexed { index, option ->
            val top = MENU_PADDING + (index * OPTION_HEIGHT)
            val optionRow = menu.container()
            optionRow.xy(MENU_PADDING, top)
            optionRow.solidRect(
                bounds.width - MENU_PADDING * 2.0,
                OPTION_HEIGHT - OPTION_ROW_INSET,
                RGBA(255, 255, 255, if (option.value == selectedValue) 32 else 16),
            ) {
                y = OPTION_ROW_INSET * 0.5
            }
            optionRow.mouse {
                onClick {
                    if (button == MouseButton.LEFT) {
                        controller.select(option.value)
                        updateTriggerDisplay(option.value)
                        closeAfterRelease = true
                    }
                }
            }

            val chip = optionRow.container()
            chip.xy(10.0, (OPTION_HEIGHT - CHIP_SIZE) * 0.5)
            MapViewerSwatchViews.render(chip, option.swatchColor, CHIP_SIZE)

            val optionTextHost = optionRow.container().also { it.xy(OPTION_TEXT_LEFT, 0.0) }
            MapViewerTextViews.leftBox(
                parent = optionTextHost,
                text = option.label,
                width = bounds.width - MENU_PADDING * 2.0 - OPTION_TEXT_LEFT,
                height = OPTION_HEIGHT - OPTION_ROW_INSET,
                color = Palette.Colours.WHITE,
            )
        }
    }

    private fun updatePopupPosition() {
        val popup = popupState ?: return
        if (!layer.visible) {
            return
        }

        val stage = root.stage ?: return
        val bounds = MapViewerDropdownLayoutBuilder.menuBounds(
            anchor = popup.anchorSource.globalPos,
            triggerHeight = popup.triggerHeight,
            viewportWidth = stage.views.actualVirtualWidth,
            viewportHeight = stage.views.actualVirtualHeight,
            menuWidth = popup.triggerWidth,
            menuHeight = popup.menuHeight,
        )
        controller.open(bounds)
        popup.menu.xy(bounds.left, bounds.top)
    }

    private fun updateTriggerDisplay(value: T?) {
        triggerLabel?.text = labelForValue(value)
        triggerSwatch?.let { MapViewerSwatchViews.render(it, swatchForValue(value), TRIGGER_SWATCH_SIZE) }
    }

    private fun closePopup() {
        closeAfterRelease = false
        controller.dismiss()
        popupState = null
        layer.removeChildren()
        layer.visible = false
    }

    private data class PopupState<T>(
        val anchorSource: Container,
        val triggerHeight: Double,
        val triggerWidth: Double,
        val menuHeight: Double,
        val selectedValue: T?,
        val backdrop: Container,
        val menu: Container,
    )

    private companion object {
        const val DEFAULT_MENU_WIDTH = 200.0
        const val DEFAULT_FIELD_HEIGHT = 32.0
        private const val Z_INDEX = 60_000.0
        private const val MENU_PADDING = 4.0
        private const val OPTION_HEIGHT = 40.0
        private const val OPTION_ROW_INSET = 4.0
        private const val CHIP_SIZE = 12.0
        private const val TRIGGER_SWATCH_SIZE = 30.0
        private const val TRIGGER_SWATCH_SLOT_WIDTH = 40.0
        private const val TEXT_LEFT_PADDING = 8.0
        private const val OPTION_TEXT_LEFT = 30.0
        private const val CARET_WIDTH = 18.0
        private const val CARET_RIGHT_PADDING = 8.0
        private const val TRAILING_AREA_WIDTH = 44.0
    }
}

internal object MapViewerDropdownLayoutBuilder {
    const val MENU_GAP = 6.0

    fun menuBounds(
        anchor: Vector2D,
        triggerHeight: Double,
        viewportWidth: Int,
        viewportHeight: Int,
        menuWidth: Double,
        menuHeight: Double,
    ): ScreenBounds {
        val left = anchor.x.coerceIn(0.0, (viewportWidth - menuWidth).coerceAtLeast(0.0))
        val belowTop = anchor.y + triggerHeight + MENU_GAP
        val aboveTop = anchor.y - menuHeight - MENU_GAP
        val preferredTop = if (belowTop + menuHeight <= viewportHeight.toDouble()) belowTop else aboveTop
        val top = preferredTop.coerceIn(0.0, (viewportHeight - menuHeight).coerceAtLeast(0.0))
        return ScreenBounds(left = left, top = top, width = menuWidth, height = menuHeight)
    }
}
