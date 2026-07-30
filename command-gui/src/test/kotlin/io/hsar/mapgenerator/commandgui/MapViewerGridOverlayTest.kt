package io.hsar.mapgenerator.commandgui

import io.hsar.mapgenerator.grid.GridLayerBuilder
import io.hsar.mapgenerator.grid.ReferenceSystem
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.greaterThan
import org.hamcrest.Matchers.lessThan
import org.junit.jupiter.api.Test
import java.awt.Color
import java.util.*

class MapViewerGridOverlayTest {

    @Test
    fun `axis labels remain locked to the top and left while panning within the map`() {
        val first = layout(viewport(centerX = 830.0))
        val second = layout(viewport(centerX = 880.0))

        val firstMapId = first.labels.first { it.text == "31U DQ" }
        val secondMapId = second.labels.first { it.text == "31U DQ" }
        val firstTopLabels = first.labels.filter { it.centre.y == firstMapId.centre.y }
        val secondTopLabels = second.labels.filter { it.centre.y == secondMapId.centre.y }
        val firstLeftLabels = first.labels.filter { it.centre.x == firstMapId.centre.x }
        val secondLeftLabels = second.labels.filter { it.centre.x == secondMapId.centre.x }

        assertThat(secondMapId.centre.y, equalTo(firstMapId.centre.y))
        assertThat(secondMapId.centre.x, equalTo(firstMapId.centre.x))
        assertThat(secondTopLabels.map { it.centre.y }.distinct(), equalTo(firstTopLabels.map { it.centre.y }.distinct()))
        assertThat(secondLeftLabels.map { it.centre.x }.distinct(), equalTo(firstLeftLabels.map { it.centre.x }.distinct()))
    }

    @Test
    fun `grid lines fade in after leaving the sticky label gutters`() {
        val overlay = layout(viewport(centerX = 830.0))
        val verticalSegments = overlay.lineSegments
            .filter { it.start.x == it.end.x }
            .groupBy { it.start.x }
            .values
            .maxBy { it.size }
            .sortedBy { it.start.y }
        val horizontalSegments = overlay.lineSegments
            .filter { it.start.y == it.end.y }
            .groupBy { it.start.y }
            .values
            .maxBy { it.size }
            .sortedBy { it.start.x }

        assertThat(verticalSegments.first().colour.alpha, lessThan(verticalSegments.last().colour.alpha))
        assertThat(horizontalSegments.first().colour.alpha, lessThan(horizontalSegments.last().colour.alpha))
        assertThat(verticalSegments.last().colour.alpha, greaterThan(0))
        assertThat(horizontalSegments.last().colour.alpha, greaterThan(0))
    }

    @Test
    fun `edge opacity follows the original quadratic fade`() {
        assertThat(MapViewerGridOverlayLayoutBuilder.edgeOpacity(0.0, 60.0), equalTo(0.0))
        assertThat(MapViewerGridOverlayLayoutBuilder.edgeOpacity(30.0, 60.0), equalTo(0.75))
        assertThat(MapViewerGridOverlayLayoutBuilder.edgeOpacity(60.0, 60.0), equalTo(1.0))
    }

    private fun layout(viewport: MapViewport) =
        MapViewerGridOverlayLayoutBuilder.build(
            MapViewerGridOverlayRequest(
                gridLayer = gridLayer(),
                viewport = viewport,
                mapOffsetX = 330.0,
                mapOffsetY = 198.0,
                mapWidth = 1000,
                mapHeight = 600,
            ),
        ) { text, fontSize, _ ->
            ViewerPoint(text.length * fontSize * 0.5, fontSize)
        }

    private fun viewport(centerX: Double) =
        MapViewport(
            center = ViewerPoint(centerX, 498.0),
            zoom = 1.0,
            viewportWidth = 800,
            viewportHeight = 500,
            worldWidth = 1660.0,
            worldHeight = 996.0,
        )

    private fun gridLayer() =
        GridLayerBuilder.build(
            width = 1000,
            height = 600,
            gridScale = ReferenceSystem.MGRS.gridScale,
            unitsPerPixel = 10.0,
            random = Random(1L),
            mapId = "31U DQ",
            referenceSystem = ReferenceSystem.MGRS,
            verticalLineNumberStart = ReferenceSystem.MGRS.coordFromUnits(100_000.0),
            horizontalLineNumberStart = ReferenceSystem.MGRS.coordFromUnits(200_000.0),
            colour = Color(112, 107, 107),
        )
}
