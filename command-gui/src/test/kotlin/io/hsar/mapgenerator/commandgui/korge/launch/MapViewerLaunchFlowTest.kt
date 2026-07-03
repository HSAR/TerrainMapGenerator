package io.hsar.mapgenerator.commandgui.korge.launch

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import java.awt.image.BufferedImage

class MapViewerLaunchFlowTest {

    @Test
    fun `starts on launchpad`() {
        val flow = MapViewerLaunchFlow()

        assertThat(flow.state, equalTo(MapViewerLaunchState.LAUNCHPAD))
    }

    @Test
    fun `create new operation opens setup state`() {
        val flow = MapViewerLaunchFlow()

        flow.createNewOperation()

        assertThat(flow.state, equalTo(MapViewerLaunchState.NEW_OPERATION))
    }

    @Test
    fun `launch existing operation is a placeholder`() {
        val flow = MapViewerLaunchFlow()

        flow.launchExistingOperation()

        assertThat(flow.state, equalTo(MapViewerLaunchState.LAUNCHPAD))
    }

    @Test
    fun `invalid new operation does not launch viewer`() {
        val flow = MapViewerLaunchFlow().also { it.createNewOperation() }

        val launched = flow.launchNewOperation(NewOperationDraft(operationName = "Test"))

        assertThat(launched, equalTo(false))
        assertThat(flow.state, equalTo(MapViewerLaunchState.NEW_OPERATION))
    }

    @Test
    fun `new operation without grid configuration does not launch viewer`() {
        val flow = MapViewerLaunchFlow().also { it.createNewOperation() }

        val launched = flow.launchNewOperation(
            NewOperationDraft(
                operationName = "Test",
                mapImage = OperationMapImage("Map", "map.png", image()),
                unitsPerPixel = 20.0,
                gridConfiguration = null,
            ),
        )

        assertThat(launched, equalTo(false))
        assertThat(flow.state, equalTo(MapViewerLaunchState.NEW_OPERATION))
    }

    @Test
    fun `valid new operation launches viewer`() {
        val flow = MapViewerLaunchFlow().also { it.createNewOperation() }

        val launched = flow.launchNewOperation(
            NewOperationDraft(
                operationName = "Test",
                mapImage = OperationMapImage("Map", "map.png", image()),
                unitsPerPixel = 20.0,
                gridConfiguration = GridConfiguration(
                    mapId = "AA",
                    startX = io.hsar.mapgenerator.grid.MgrsCoord.fromLineNumber(1.0, 1000),
                    startY = io.hsar.mapgenerator.grid.MgrsCoord.fromLineNumber(2.0, 1000),
                ),
            ),
        )

        assertThat(launched, equalTo(true))
        assertThat(flow.state, equalTo(MapViewerLaunchState.VIEWER))
    }

    private fun image(): BufferedImage =
        BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB)
}
