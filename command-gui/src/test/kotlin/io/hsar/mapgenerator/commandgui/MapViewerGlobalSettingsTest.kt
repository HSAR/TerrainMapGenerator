package io.hsar.mapgenerator.commandgui

import io.hsar.mapgenerator.commandgui.operation.OperationDestroyedUnitDisplayMode
import io.hsar.mapgenerator.commandgui.operation.OperationFireArcDisplayMode
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.nio.file.Files

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

    @Test
    fun `round trips aggregate display settings shared by both backends`() {
        val settings = MapViewerGlobalSettings(temporaryDirectory)
        settings.updateDestroyedUnitDisplayMode(OperationDestroyedUnitDisplayMode.Partial)
        settings.updateUnitIconSizePx(54.0)
        settings.updateFireArcDisplayMode(OperationFireArcDisplayMode.SelectedOnly)

        assertThat(settings.displaySettings(), equalTo(MapViewerDisplaySettings(
            OperationDestroyedUnitDisplayMode.Partial,
            54.0,
            OperationFireArcDisplayMode.SelectedOnly,
        )))
    }

    @Test
    fun `unknown display values fall back without changing the settings format`() {
        Files.createDirectories(temporaryDirectory)
        Files.writeString(temporaryDirectory.resolve("settings.json"), """{"formatVersion":1,"displayDestroyedUnits":"unknown","unitIconSizePx":999,"fireArcDisplayMode":"unknown"}""")

        val restored = MapViewerGlobalSettings(temporaryDirectory).displaySettings()

        assertThat(restored.destroyedUnitDisplayMode, equalTo(OperationDestroyedUnitDisplayMode.Full))
        assertThat(restored.unitIconSizePx, equalTo(UnitIconDisplaySize.MAX_PX))
        assertThat(restored.fireArcDisplayMode, equalTo(OperationFireArcDisplayMode.AllUnits))
    }
}
