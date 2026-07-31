package io.hsar.mapgenerator.commandgui

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class MapViewerGlobalSettingsTest {

    @TempDir
    lateinit var temporaryDirectory: Path

    @Test
    fun `persists the global unit icon size across viewer settings instances`() {
        val updated = MapViewerGlobalSettings(temporaryDirectory).updateUnitIconSizePx(48.0)
        val reloaded = MapViewerGlobalSettings(temporaryDirectory).unitIconSizePx()

        assertThat(updated, equalTo(48.0))
        assertThat(reloaded, equalTo(48.0))
    }
}
