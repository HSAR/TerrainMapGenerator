package io.hsar.mapgenerator.commandgui.compose

import io.hsar.mapgenerator.commandgui.MapViewerRenderData
import io.hsar.mapgenerator.commandgui.MapViewerSavedMap
import io.hsar.mapgenerator.commandgui.MapViewerSavedOperation
import io.hsar.mapgenerator.commandgui.MapViewerSettings
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.nullValue
import org.junit.jupiter.api.Test
import java.awt.image.BufferedImage
import java.nio.file.Path
import java.time.Instant
import java.util.*

class ComposeLaunchStateTest {

    @Test
    fun `creates an operation and returns to launchpad`() {
        val state = ComposeLaunchState()

        state.createOperation(MapViewerSettings(operationName = "New Operation"))
        val launchedName = state.activeMap?.settings?.operationName
        state.returnToLaunchpad()

        assertThat(launchedName, equalTo("New Operation"))
        assertThat(state.activeMap, nullValue())
    }

    @Test
    fun `opens a saved map without discarding its map state`() {
        val image = BufferedImage(32, 24, BufferedImage.TYPE_INT_ARGB)
        val path = Path.of("operation.oct.zip")
        val loaded = MapViewerSavedMap(
            operation = MapViewerSavedOperation(
                path = path,
                operationId = UUID.fromString("00000000-0000-0000-0000-000000000001"),
                operationName = "Loaded Operation",
                updatedAt = Instant.parse("2026-07-29T12:00:00Z"),
                createdAt = Instant.parse("2026-07-28T12:00:00Z"),
                thumbnailAssetId = null,
                thumbnail = null,
            ),
            settings = MapViewerSettings(operationName = "Loaded Operation"),
            renderData = MapViewerRenderData(image),
            camera = null,
        )
        val state = ComposeLaunchState()

        state.openSavedMap(loaded)

        assertThat(state.activeMap?.renderData?.baseImage, equalTo(image))
        assertThat(state.activeMap?.sourcePath, equalTo(path.toString()))
    }
}
