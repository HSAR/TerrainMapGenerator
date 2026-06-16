package io.hsar.mapgenerator.commandgui

enum class MapLayer(
    override val displayName: String,
) : MapViewerLayer {
    HEIGHT("Height"),
    CONTOUR("Contour"),
    GRAPH("Graph"),
    GRID("Grid");

    override val id: String = name
}
