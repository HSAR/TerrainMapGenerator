package io.hsar.mapgenerator.commandgui

import io.hsar.mapgenerator.commandgui.korge.scene.MapViewerScene
import java.util.*

enum class MapViewerBackend(val cliName: String) {
    KORGE("korge"),
    COMPOSE("compose"),
    ;

    companion object {
        fun fromCliName(value: String): MapViewerBackend =
            entries.firstOrNull { it.cliName.equals(value, ignoreCase = true) }
                ?: throw IllegalArgumentException(
                    "Unknown viewer '$value'. Expected one of: ${entries.joinToString { it.cliName }}",
                )
    }
}

/**
 * Runtime backend seam used while the Compose viewer and the legacy KorGE viewer coexist.
 *
 * Implementations are discovered with [ServiceLoader], keeping the public facade independent
 * of optional UI runtimes.
 */
interface MapViewerBackendProvider {
    val backend: MapViewerBackend

    fun showWindow(
        renderMap: (MapViewerSettings) -> MapViewerRenderData,
        initialSettings: MapViewerSettings,
        initialSetupInputs: MapViewerSetupInputs,
    )
}

class MapViewerFrame(
    private val renderMap: (MapViewerSettings) -> MapViewerRenderData,
    private val initialSettings: MapViewerSettings = MapViewerSettings(),
    private val initialSetupInputs: MapViewerSetupInputs = MapViewerSetupInputs(),
    private val featureFlags: Set<MapViewerFeatureFlag> = emptySet(),
    private val backend: MapViewerBackend = MapViewerBackend.KORGE,
) {

    fun showWindow() {
        MapViewerFeatureFlags.initialize(featureFlags)
        when (backend) {
            MapViewerBackend.KORGE ->
                MapViewerScene(renderMap, initialSettings, initialSetupInputs).showWindow()

            MapViewerBackend.COMPOSE ->
                ServiceLoader.load(MapViewerBackendProvider::class.java)
                    .firstOrNull { it.backend == backend }
                    ?.showWindow(renderMap, initialSettings, initialSetupInputs)
                    ?: error(
                        "Compose viewer is not available. Ensure command-gui-compose is on the runtime classpath.",
                    )
        }
    }

}
