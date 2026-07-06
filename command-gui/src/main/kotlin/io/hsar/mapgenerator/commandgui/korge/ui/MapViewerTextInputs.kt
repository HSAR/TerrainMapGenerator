package io.hsar.mapgenerator.commandgui.korge.ui

import io.hsar.mapgenerator.image.Palette
import korlibs.image.color.RGBA
import korlibs.image.text.TextAlignment
import korlibs.korge.annotations.KorgeExperimental
import korlibs.korge.input.mouse
import korlibs.korge.ui.UITextInput
import korlibs.korge.ui.uiTextInput
import korlibs.korge.view.Container
import korlibs.korge.view.Text
import korlibs.korge.view.ViewRenderer
import korlibs.korge.view.clipContainer
import korlibs.korge.view.container
import korlibs.korge.view.xy
import korlibs.math.geom.Margin
import korlibs.math.geom.Size2D

@OptIn(KorgeExperimental::class)
internal object MapViewerTextInputs {
    fun input(
        parent: Container,
        text: String,
        width: Double,
        height: Double,
        x: Double = 0.0,
        y: Double = 0.0,
        role: MapViewerFontRole = MapViewerFontRole.MAIN,
        textSize: Double = MapViewerTextViews.DEFAULT_TEXT_SIZE,
        alignment: TextAlignment = TextAlignment.MIDDLE_LEFT,
        wrapWords: Boolean = false,
        selectEndWhenClickBelowText: Boolean = false,
        frameStyle: (Boolean) -> UiFrameStyle = MapViewerUiBoxes::defaultStyle,
    ): MapViewerInputField =
        parent.container().let { field ->
            field.xy(x, y)
            MapViewerUiBoxes.box(field, width, height, style = frameStyle)

            val inputHost = field.container().also {
                it.xy(2.0, 2.0)
                it.zIndex = 1.0
            }
            val visibleTextWidth = width - 12.0
            fun trackedValue(value: String): MapViewerInputTextWrap.WrappedText =
                if (wrapWords) {
                    MapViewerInputTextWrap.wrapTracked(value, visibleTextWidth, role, textSize)
                } else {
                    MapViewerInputTextWrap.WrappedText(value, emptySet())
                }

            var currentLogicalText = text
            var currentSoftBreakIndexes = emptySet<Int>()
            var applyingTrackedWrap = false
            val initialTrackedText = trackedValue(text).also {
                currentSoftBreakIndexes = it.softBreakIndexes
            }
            val visualLineHeight = MapViewerTextViews.measure("Mg", role, textSize).height.coerceAtLeast(textSize)
            val input = inputHost.uiTextInput(initialTrackedText.text, Size2D(width - 4.0, height - 4.0)).apply {
                skin = ViewRenderer { }
                padding = Margin(4f, 6f, 4f, 6f)
                controller.textColor = RGBA(255, 255, 255, 0)
                font = MapViewerKorgeFonts.font(role)
                this.textSize = textSize
                mouse {
                    onClick {
                        focus()
                        if (selectEndWhenClickBelowText &&
                            MapViewerInputClickPlacement.isBelowText(currentPosLocal.y, this@apply.text, visualLineHeight)
                        ) {
                            controller.select(this@apply.text.length, this@apply.text.length)
                        }
                    }
                }
            }
            val visibleTextClip = field.clipContainer(Size2D(width - 12.0, height - 4.0)) {
                zIndex = 10.0
                mouseEnabled = false
            }.also {
                it.xy(6.0, 2.0)
            }
            val visibleTextHeight = height - 4.0
            val visibleText: Text = MapViewerTextViews.box(
                parent = visibleTextClip,
                text = input.text,
                width = visibleTextWidth,
                height = visibleTextHeight,
                role = role,
                size = textSize,
                color = Palette.Colours.WHITE,
                alignment = alignment,
            ).also {
                it.mouseEnabled = false
            }

            input.controller.onTextUpdated.add {
                if (!applyingTrackedWrap) {
                    val selectionStart = MapViewerInputTextWrap.logicalIndexForVisualIndex(
                        input.controller.selectionStart,
                        input.text,
                        currentSoftBreakIndexes,
                    )
                    val selectionEnd = MapViewerInputTextWrap.logicalIndexForVisualIndex(
                        input.controller.selectionEnd,
                        input.text,
                        currentSoftBreakIndexes,
                    )
                    currentLogicalText = MapViewerInputTextWrap.unwrapSoft(input.text, currentSoftBreakIndexes)
                    val tracked = trackedValue(currentLogicalText)
                    currentSoftBreakIndexes = tracked.softBreakIndexes
                    if (wrapWords && tracked.text != input.text) {
                        applyingTrackedWrap = true
                        input.text = tracked.text
                        input.controller.select(
                            MapViewerInputTextWrap.visualIndexForLogicalIndex(selectionStart, tracked),
                            MapViewerInputTextWrap.visualIndexForLogicalIndex(selectionEnd, tracked),
                        )
                        applyingTrackedWrap = false
                    }
                }
                visibleText.text = input.text
            }

            MapViewerInputField(
                root = field,
                input = input,
                visibleText = visibleText,
                textProvider = { currentLogicalText },
                onSetText = { value ->
                    currentLogicalText = value
                    trackedValue(value).also {
                        currentSoftBreakIndexes = it.softBreakIndexes
                        input.text = it.text
                        visibleText.text = it.text
                    }
                },
            )
        }
}

internal object MapViewerInputClickPlacement {
    fun isBelowText(
        localY: Double,
        visualText: String,
        lineHeight: Double,
        textTop: Double = TEXT_TOP,
    ): Boolean =
        localY > textTop + visualText.lines().size.coerceAtLeast(1) * lineHeight

    private const val TEXT_TOP = 2.0
}

internal object MapViewerInputTextWrap {
    data class WrappedText(
        val text: String,
        val softBreakIndexes: Set<Int>,
    )

    fun wrap(
        text: String,
        maxWidth: Double,
        role: MapViewerFontRole = MapViewerFontRole.MAIN,
        size: Double = MapViewerTextViews.DEFAULT_TEXT_SIZE,
    ): String =
        wrapTracked(text, maxWidth, role, size).text

    fun wrapTracked(
        text: String,
        maxWidth: Double,
        role: MapViewerFontRole = MapViewerFontRole.MAIN,
        size: Double = MapViewerTextViews.DEFAULT_TEXT_SIZE,
    ): WrappedText {
        val softBreakIndexes = mutableSetOf<Int>()
        val wrappedText = buildString {
            text.split('\n').forEachIndexed { lineIndex, line ->
                if (lineIndex > 0) {
                    append('\n')
                }
                wrapLine(line, maxWidth, role, size).forEach { character ->
                    if (character == '\n') {
                        softBreakIndexes += length
                    }
                    append(character)
                }
            }
        }
        return WrappedText(wrappedText, softBreakIndexes)
    }

    fun unwrapSoft(text: String, softBreakIndexes: Set<Int>): String =
        text
            .mapIndexed { index, character -> if (character == '\n' && index in softBreakIndexes) ' ' else character }
            .joinToString("")

    fun logicalIndexForVisualIndex(visualIndex: Int, visualText: String, softBreakIndexes: Set<Int>): Int =
        visualIndex.coerceIn(0, visualText.length)

    fun visualIndexForLogicalIndex(logicalIndex: Int, wrappedText: WrappedText): Int {
        return logicalIndex.coerceIn(0, wrappedText.text.length)
    }

    private fun wrapLine(
        line: String,
        maxWidth: Double,
        role: MapViewerFontRole,
        size: Double,
    ): String {
        if (line.isBlank() || maxWidth <= 0.0 || fits(line, maxWidth, role, size)) {
            return line
        }

        return line
            .trim()
            .split(Regex("\\s+"))
            .fold(emptyList<String>()) { lines, word ->
                when {
                    lines.isEmpty() -> breakWord(word, maxWidth, role, size)
                    fits("${lines.last()} $word", maxWidth, role, size) -> lines.dropLast(1) + "${lines.last()} $word"
                    else -> lines + breakWord(word, maxWidth, role, size)
                }
            }
            .joinToString("\n")
    }

    private fun breakWord(
        word: String,
        maxWidth: Double,
        role: MapViewerFontRole,
        size: Double,
    ): List<String> =
        generateSequence(word) { remaining ->
            remaining
                .takeIf { it.isNotEmpty() && !fits(it, maxWidth, role, size) }
                ?.drop(longestPrefixThatFits(remaining, maxWidth, role, size).length)
                ?.takeIf(String::isNotEmpty)
        }
            .map { remaining -> longestPrefixThatFits(remaining, maxWidth, role, size) }
            .toList()
            .ifEmpty { listOf(word) }

    private fun longestPrefixThatFits(
        text: String,
        maxWidth: Double,
        role: MapViewerFontRole,
        size: Double,
    ): String =
        (1..text.length)
            .map { text.take(it) }
            .takeWhile { fits(it, maxWidth, role, size) }
            .lastOrNull()
            ?: text.take(1)

    private fun fits(
        text: String,
        maxWidth: Double,
        role: MapViewerFontRole,
        size: Double,
    ): Boolean =
        MapViewerTextViews.measure(text, role, size).width <= maxWidth
}

@OptIn(KorgeExperimental::class)
internal class MapViewerInputField(
    private val root: Container,
    private val input: UITextInput,
    private val visibleText: Text,
    private val textProvider: () -> String = { input.text },
    private val onSetText: (String) -> Unit = { value ->
        input.text = value
        visibleText.text = value
    },
) {
    val controller get() = input.controller
    var isFocused: Boolean = false
        private set

    init {
        input.controller.onFocused.add {
            isFocused = true
        }
        input.controller.onFocusLost.add {
            isFocused = false
        }
    }

    var text: String
        get() = textProvider()
        set(value) {
            onSetText(value)
        }

    fun focus() {
        isFocused = true
        input.focus()
    }

    fun blur() {
        isFocused = false
        input.blur()
    }

    fun selectAll() {
        input.selectAll()
    }

    fun onClick(block: () -> Unit) {
        root.mouse {
            onClick { block() }
        }
    }
}
