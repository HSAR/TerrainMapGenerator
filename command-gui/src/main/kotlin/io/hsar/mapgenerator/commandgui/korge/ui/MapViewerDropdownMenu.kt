package io.hsar.mapgenerator.commandgui.korge.ui

import io.hsar.mapgenerator.image.Palette
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
    private var ignoreOutsideUntilRelease = false
    private var wasMouseDown = false
    private var popupState: PopupState<T>? = null

    init {
        layer.zIndex = Z_INDEX
        layer.mouseEnabled = true
        layer.visible = false
        root.addUpdater {
            updatePopupPosition()
            updatePointerSelection()
            if (ignoreOutsideUntilRelease && root.stage?.views?.input?.mouseButtons == 0) {
                ignoreOutsideUntilRelease = false
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
        ignoreOutsideUntilRelease = true

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
            menu = menu,
        )

        options.forEachIndexed { index, option ->
            val top = MENU_PADDING + (index * OPTION_HEIGHT)
            val optionRow = menu.container()
            optionRow.xy(MENU_PADDING, top)
            val rowBacking = optionRow.solidRect(
                bounds.width - MENU_PADDING * 2.0,
                OPTION_HEIGHT - OPTION_ROW_INSET,
                RGBA(255, 255, 255, if (option.value == selectedValue) 32 else 16),
            )
            rowBacking.y = OPTION_ROW_INSET * 0.5
            rowBacking.mouseEnabled = false

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

            val hitArea = optionRow.solidRect(bounds.width - MENU_PADDING * 2.0, OPTION_HEIGHT, RGBA(0, 0, 0, 1))
            hitArea.mouseEnabled = false
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

    private fun updatePointerSelection() {
        val popup = popupState ?: return
        if (!layer.visible) {
            return
        }

        val stage = root.stage ?: return
        val mouseDown = stage.views.input.mouseButtons != 0
        val justPressed = mouseDown && !wasMouseDown
        wasMouseDown = mouseDown
        if (ignoreOutsideUntilRelease) {
            return
        }
        if (!justPressed) {
            return
        }

        val bounds = controller.menuBounds ?: return
        val pointer = stage.views.input.mousePos
        if (bounds.contains(pointer)) {
            selectOptionAt(pointer, bounds)
        } else {
            closePopup()
        }
    }

    private fun selectOptionAt(pointer: Vector2D, bounds: ScreenBounds) {
        val localY = pointer.y - bounds.top - MENU_PADDING
        if (localY < 0.0) {
            return
        }

        val optionIndex = (localY / OPTION_HEIGHT).toInt()
        options.getOrNull(optionIndex)?.let { option ->
            controller.select(option.value)
            updateTriggerDisplay(option.value)
            closePopup()
        }
    }

    private fun updateTriggerDisplay(value: T?) {
        triggerLabel?.text = labelForValue(value)
        triggerSwatch?.let { MapViewerSwatchViews.render(it, swatchForValue(value), TRIGGER_SWATCH_SIZE) }
    }

    private fun closePopup() {
        ignoreOutsideUntilRelease = false
        wasMouseDown = false
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
