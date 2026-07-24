package io.hsar.mapgenerator.commandgui.korge.interaction

internal const val DISPLAY_NAME_PROPERTY = "displayName"

internal fun MapPlacedObjectSnapshot.displayName(): String? =
    when (this) {
        is DrawingSnapshot -> customProperties[DISPLAY_NAME_PROPERTY]
        is GraphicSnapshot -> customProperties[DISPLAY_NAME_PROPERTY]
        is UnitSnapshot -> null
    }?.trim()?.takeIf(String::isNotEmpty)

internal fun MapPlacedObjectSnapshot.withDisplayName(value: String?): MapPlacedObjectSnapshot {
    val properties = when (this) {
        is DrawingSnapshot -> customProperties
        is GraphicSnapshot -> customProperties
        is UnitSnapshot -> return this
    }.let { current -> value?.let { current + (DISPLAY_NAME_PROPERTY to it) } ?: current - DISPLAY_NAME_PROPERTY }

    return when (this) {
        is DrawingSnapshot -> copy(customProperties = properties)
        is GraphicSnapshot -> copy(customProperties = properties)
        is UnitSnapshot -> this
    }
}
