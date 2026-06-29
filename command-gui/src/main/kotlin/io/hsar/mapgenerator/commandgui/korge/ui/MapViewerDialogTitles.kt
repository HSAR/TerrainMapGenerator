package io.hsar.mapgenerator.commandgui.korge.ui

internal object MapViewerDialogTitles {
    fun unitType(baseTitle: String, unitTypeName: String?): String =
        unitTypeName
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?.let { "$baseTitle - $it" }
            ?: baseTitle
}
