package io.hsar.mapgenerator.commandgui

/** Runtime feature flags shared by all viewer screens and dialogs in the current process. */
object MapViewerFeatureFlags {
    @Volatile
    private var current: Set<MapViewerFeatureFlag> = emptySet()

    val enabled: Set<MapViewerFeatureFlag>
        get() = current

    fun initialize(flags: Set<MapViewerFeatureFlag>) {
        current = flags.toSet()
    }
}
