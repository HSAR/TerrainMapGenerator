package io.hsar.mapgenerator.commandgui.korge.ui

internal object MapViewerWrappedText {
    fun wrap(text: String, width: Double, size: Double): List<String> =
        text
            .lineSequence()
            .flatMap { line ->
                if (line.isBlank()) {
                    sequenceOf("")
                } else {
                    wrapParagraph(line, width, size)
                }
            }
            .toList()

    fun height(lineCount: Int): Double =
        if (lineCount <= 0) {
            0.0
        } else {
            (lineCount * LINE_HEIGHT) + 4.0
        }

    private fun wrapParagraph(paragraph: String, width: Double, size: Double): Sequence<String> = sequence {
        var current = ""
        paragraph
            .split(Regex("\\s+"))
            .forEach { word ->
                val next = if (current.isBlank()) word else "$current $word"
                if (MapViewerTextViews.measure(next, size = size).width <= width) {
                    current = next
                } else {
                    if (current.isNotBlank()) {
                        yield(current)
                    }
                    current = word
                }
            }
        if (current.isNotBlank()) {
            yield(current)
        }
    }

    private const val LINE_HEIGHT = 28.0
}
