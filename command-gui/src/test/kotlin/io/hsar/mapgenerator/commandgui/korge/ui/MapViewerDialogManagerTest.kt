package io.hsar.mapgenerator.commandgui.korge.ui

import korlibs.math.geom.Vector2D
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test

class MapViewerDialogManagerTest {
    @Test
    fun `updating a registered target dispatches sync without closing it`() {
        val manager = MapViewerDialogManager()
        var syncPayload: Any? = null
        var closed = false

        manager.register(
            owner = "dialog",
            targetKey = "unit-1",
            blocksScreenPoint = { false },
            sync = { payload -> syncPayload = payload },
            close = { closed = true },
        )

        manager.updateTarget("unit-1", MapViewerDialogPositionSync("5414", "8198"))

        assertThat(closed, equalTo(false))
        assertThat(syncPayload, equalTo(MapViewerDialogPositionSync("5414", "8198")))
        assertThat(manager.blocksScreenPoint(Vector2D(0.0, 0.0)), equalTo(false))
    }
}
