package io.hsar.mapgenerator.commandgui.korge.ui

import io.hsar.mapgenerator.image.Palette
import korlibs.image.text.TextAlignment
import korlibs.korge.annotations.KorgeExperimental
import korlibs.korge.input.mouse
import korlibs.korge.text.TextEditController
import korlibs.korge.text.editText
import korlibs.korge.ui.blur
import korlibs.korge.ui.focus
import korlibs.korge.view.Container
import korlibs.korge.view.Text
import korlibs.korge.view.addUpdater
import korlibs.korge.view.clipContainer
import korlibs.korge.view.container
import korlibs.korge.view.xy
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
        scrollable: Boolean = false,
        onLogicalTextUpdated: (String) -> Unit = {},
        frameStyle: (Boolean) -> UiFrameStyle = MapViewerUiBoxes::defaultStyle,
    ): MapViewerInputField =
        parent.container().let { field ->
            field.xy(x, y)
            MapViewerUiBoxes.box(field, width, height, style = frameStyle)

            val scrollbarGutter = if (scrollable) MapViewerScrollbar.WIDTH + 4.0 else 0.0
            val visibleTextWidth = width - 12.0 - scrollbarGutter
            fun trackedValue(value: String): MapViewerInputTextWrap.WrappedText =
                if (wrapWords) {
                    MapViewerInputTextWrap.wrapTracked(value, visibleTextWidth, role, textSize)
                } else {
                    MapViewerInputTextWrap.WrappedText(value, emptySet())
                }

            var currentLogicalText = text
            var currentSoftBreakIndexes = emptySet<Int>()
            var previousVisualText: String
            var applyingTrackedWrap = false
            var scrollState = MapViewerScrollState(0.0, 0.0)
            var scrollbarLayer: Container? = null
            var scrollDragState: InputScrollDragState? = null
            var selectionBeforeRejectedChange: InputSelection? = null
            val initialTrackedText = trackedValue(text).also {
                currentSoftBreakIndexes = it.softBreakIndexes
            }
            previousVisualText = initialTrackedText.text
            val visualLineHeight = MapViewerTextViews.measure("Mg", role, textSize).height.coerceAtLeast(textSize)
            val visibleTextClip = field.clipContainer(Size2D(visibleTextWidth, height - 4.0)) {
                zIndex = 10.0
            }.also {
                it.xy(6.0, 2.0)
            }
            val visibleTextHeight = height - 4.0
            val visibleText: Text = MapViewerTextViews.box(
                parent = visibleTextClip,
                text = initialTrackedText.text,
                width = visibleTextWidth,
                height = visibleTextHeight,
                role = role,
                size = textSize,
                color = Palette.Colours.WHITE,
                alignment = TextAlignment.TOP_LEFT,
            )
            val controller = visibleText.editText()
            controller.acceptTextChange = { _, newText ->
                MapViewerTextInputContent.isAccepted(newText).also { accepted ->
                    if (!accepted) {
                        selectionBeforeRejectedChange = InputSelection(controller.selectionStart, controller.selectionEnd)
                    }
                }
            }
            visibleText.mouse {
                onClick {
                    controller.focus()
                    if (selectEndWhenClickBelowText &&
                        MapViewerInputClickPlacement.isBelowText(currentPosLocal.y, visibleText.text, visualLineHeight)
                    ) {
                        controller.select(visibleText.text.length, visibleText.text.length)
                    }
                }
            }

            fun updateTextPosition() {
                val textWidth = MapViewerTextViews.measure(visibleText.text, role, textSize).width
                visibleText.x = if (alignment == TextAlignment.MIDDLE_RIGHT) visibleTextWidth - textWidth else 0.0
                visibleText.y = if (alignment == TextAlignment.MIDDLE_LEFT || alignment == TextAlignment.MIDDLE_RIGHT) {
                    (visibleTextHeight - visualLineHeight) * 0.5 - scrollState.offset
                } else {
                    -scrollState.offset
                }
            }

            fun updateScrollState() {
                val lineCount = visibleText.text.count { character -> character == '\n' } + 1
                scrollState = scrollState.withContent(
                    viewportHeight = visibleTextHeight,
                    contentHeight = lineCount * visualLineHeight + 4.0,
                )
                updateTextPosition()
            }

            fun renderScrollbar() {
                scrollbarLayer?.removeFromParent()
                scrollbarLayer = null
                if (!scrollable || !scrollState.needsScrolling) return
                scrollbarLayer = field.container().also { layer ->
                    layer.zIndex = 20.0
                    MapViewerScrollbar.render(
                        parent = layer,
                        bounds = ScreenBounds(
                            left = width - MapViewerScrollbar.WIDTH - 2.0,
                            top = 2.0,
                            width = MapViewerScrollbar.WIDTH,
                            height = height - 4.0,
                        ),
                        state = scrollState,
                        onThumbDown = { pointerY ->
                            scrollDragState = InputScrollDragState(pointerY, scrollState.offset)
                        },
                    )
                }
            }

            fun updateScroll(delta: Double) {
                scrollState = scrollState.scrolledBy(delta)
                updateTextPosition()
                renderScrollbar()
            }

            field.mouse {
                onScrollAnywhere {
                    val pointer = field.stage?.views?.input?.mousePos
                    if (pointer != null && field.getGlobalBounds().contains(pointer) && scrollState.needsScrolling) {
                        updateScroll(scrollDeltaYLines * MapViewerScrollbar.SCROLL_STEP_PX)
                    }
                }
            }
            field.addUpdater {
                val drag = scrollDragState ?: return@addUpdater
                val stage = field.stage ?: return@addUpdater
                if (!stage.views.input.mouseButtonPressed(korlibs.event.MouseButton.LEFT)) {
                    scrollDragState = null
                    return@addUpdater
                }
                val bounds = field.getGlobalBounds()
                scrollState = scrollState.withOffset(
                    MapViewerScrollbar.dragOffset(
                        bounds = ScreenBounds(
                            left = bounds.left + width - MapViewerScrollbar.WIDTH - 2.0,
                            top = bounds.top + 2.0,
                            width = MapViewerScrollbar.WIDTH,
                            height = height - 4.0,
                        ),
                        state = scrollState,
                        dragStartPointerY = drag.pointerY,
                        dragStartOffset = drag.offset,
                        currentPointerY = stage.views.input.mousePos.y,
                    ),
                )
                updateTextPosition()
                renderScrollbar()
            }
            field.addUpdater {
                selectionBeforeRejectedChange?.let { selection ->
                    controller.select(selection.start, selection.end)
                    selectionBeforeRejectedChange = null
                }
            }

            controller.onTextUpdated.add {
                if (!applyingTrackedWrap) {
                    val selectionStart = MapViewerInputTextWrap.logicalIndexForVisualIndex(
                        controller.selectionStart,
                        visibleText.text,
                        currentSoftBreakIndexes,
                    )
                    val selectionEnd = MapViewerInputTextWrap.logicalIndexForVisualIndex(
                        controller.selectionEnd,
                        visibleText.text,
                        currentSoftBreakIndexes,
                    )
                    currentLogicalText = MapViewerInputTextWrap.logicalTextAfterEdit(
                        previousVisualText = previousVisualText,
                        currentVisualText = visibleText.text,
                        previousLogicalText = currentLogicalText,
                        previousSoftBreakIndexes = currentSoftBreakIndexes,
                    )
                    val tracked = trackedValue(currentLogicalText)
                    currentSoftBreakIndexes = tracked.softBreakIndexes
                    if (wrapWords && tracked.text != visibleText.text) {
                        applyingTrackedWrap = true
                        controller.text = tracked.text
                        controller.select(
                            MapViewerInputTextWrap.visualIndexForLogicalIndex(selectionStart, tracked),
                            MapViewerInputTextWrap.visualIndexForLogicalIndex(selectionEnd, tracked),
                        )
                        applyingTrackedWrap = false
                    }
                }
                previousVisualText = visibleText.text
                updateScrollState()
                renderScrollbar()
                onLogicalTextUpdated(currentLogicalText)
            }

            MapViewerInputField(
                root = field,
                controller = controller,
                textProvider = { MapViewerInputTextWrap.unwrapSoft(visibleText.text, currentSoftBreakIndexes) },
                consumesScrollAt = { point ->
                    scrollable && scrollState.needsScrolling && field.getGlobalBounds().contains(point)
                },
                onSetText = { value ->
                    currentLogicalText = value
                    trackedValue(value).also {
                        currentSoftBreakIndexes = it.softBreakIndexes
                        controller.text = it.text
                        previousVisualText = it.text
                        updateScrollState()
                        renderScrollbar()
                    }
                    onLogicalTextUpdated(currentLogicalText)
                },
            ).also {
                updateScrollState()
                renderScrollbar()
            }
        }

    private data class InputScrollDragState(
        val pointerY: Double,
        val offset: Double,
    )

    private data class InputSelection(
        val start: Int,
        val end: Int,
    )
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

internal object MapViewerTextInputContent {
    fun isAccepted(text: String): Boolean =
        text.none { character -> character.isISOControl() && character !in LAYOUT_WHITESPACE }

    private val LAYOUT_WHITESPACE = setOf('\t', '\n', '\r')
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

    fun logicalTextAfterEdit(
        previousVisualText: String,
        currentVisualText: String,
        previousLogicalText: String,
        previousSoftBreakIndexes: Set<Int>,
    ): String {
        if (previousVisualText == currentVisualText) return previousLogicalText

        val prefixLength = commonPrefixLength(previousVisualText, currentVisualText)
        val suffixLength = commonSuffixLength(previousVisualText, currentVisualText, prefixLength)
        val previousSuffixStart = previousVisualText.length - suffixLength
        val currentSuffixStart = currentVisualText.length - suffixLength
        val logicalPrefix = unwrapSoftSegment(
            text = previousVisualText.substring(0, prefixLength),
            offset = 0,
            softBreakIndexes = previousSoftBreakIndexes,
        )
        val insertedText = currentVisualText.substring(prefixLength, currentSuffixStart)
        val logicalSuffix = unwrapSoftSegment(
            text = previousVisualText.substring(previousSuffixStart),
            offset = previousSuffixStart,
            softBreakIndexes = previousSoftBreakIndexes,
        )
        return logicalPrefix + insertedText + logicalSuffix
    }

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
        if (line.isBlank() || line.endsWith(' ') || maxWidth <= 0.0 || fits(line, maxWidth, role, size)) {
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

    private fun commonPrefixLength(first: String, second: String): Int =
        first.zip(second).takeWhile { (left, right) -> left == right }.count()

    private fun commonSuffixLength(first: String, second: String, prefixLength: Int): Int =
        first.asSequence()
            .drop(prefixLength)
            .toList()
            .asReversed()
            .zip(second.asSequence().drop(prefixLength).toList().asReversed())
            .takeWhile { (left, right) -> left == right }
            .count()

    private fun unwrapSoftSegment(text: String, offset: Int, softBreakIndexes: Set<Int>): String =
        text.mapIndexed { index, character ->
            if (character == '\n' && index + offset in softBreakIndexes) ' ' else character
        }.joinToString("")

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
    val controller: TextEditController,
    private val textProvider: () -> String = { controller.text },
    private val consumesScrollAt: (korlibs.math.geom.Vector2D) -> Boolean = { false },
    private val onSetText: (String) -> Unit = { value ->
        controller.text = value
    },
) {
    var isFocused: Boolean = false
        private set

    init {
        controller.onFocused.add {
            isFocused = true
        }
        controller.onFocusLost.add {
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
        controller.focus()
    }

    fun blur() {
        isFocused = false
        controller.blur()
    }

    fun selectAll() {
        controller.selectAll()
    }

    fun onClick(block: () -> Unit) {
        root.mouse {
            onClick { block() }
        }
    }

    fun consumesScrollAt(point: korlibs.math.geom.Vector2D): Boolean =
        consumesScrollAt(point)
}
