package io.hsar.mapgenerator.commandgui.korge.launch

/** The coordinate space shared by launchpad menus, the persistent shell, and its background. */
internal data class MapViewerViewport(
    val width: Int,
    val height: Int,
)

/**
 * Resolves KorGE's usable virtual viewport. The actual virtual dimensions are authoritative;
 * the window dimensions are only a startup fallback while KorGE is still reporting zeroes.
 */
internal object MapViewerViewportResolver {
    fun resolve(actualWidth: Int, actualHeight: Int, fallbackWidth: Int, fallbackHeight: Int): MapViewerViewport =
        if (actualWidth > 0 && actualHeight > 0) {
            MapViewerViewport(actualWidth, actualHeight)
        } else {
            MapViewerViewport(fallbackWidth, fallbackHeight)
        }
}
