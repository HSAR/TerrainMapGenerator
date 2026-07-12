package io.hsar.mapgenerator.commandgui.korge.ui

internal object MapViewerCountText {
    fun of(count: Int, singular: String, plural: String = "${singular}s"): String =
        "$count ${if (count == 1) singular else plural}"
}
