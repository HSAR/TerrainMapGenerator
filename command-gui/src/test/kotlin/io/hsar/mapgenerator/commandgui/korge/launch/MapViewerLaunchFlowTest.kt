package io.hsar.mapgenerator.commandgui.korge.launch

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test

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
}
