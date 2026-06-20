package io.hsar.mapgenerator.commandgui

internal object MapViewerWindowTitle {
    private const val APP_NAME = "Operational Command Tool"

    fun format(title: String): String =
        "$APP_NAME - $title"
}
