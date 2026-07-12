package io.hsar.mapgenerator.commandgui

/** Launch-time feature flags shared by all viewer screens and dialogs. */
internal object MapViewerFeatureFlags {
    @Volatile
    private var current: Set<MapViewerFeatureFlag> = emptySet()

    val enabled: Set<MapViewerFeatureFlag>
        get() = current

    fun initialize(flags: Set<MapViewerFeatureFlag>) {
        current = flags.toSet()
    }
}
