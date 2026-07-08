package io.hsar.mapgenerator.commandgui.korge.ui

import io.hsar.mapgenerator.commandgui.ViewerGeometry
import io.hsar.mapgenerator.commandgui.korge.interaction.CameraViewport
import io.hsar.mapgenerator.commandgui.korge.interaction.MapCoordinate
import io.hsar.mapgenerator.commandgui.korge.interaction.MapCoordinateSpace
import io.hsar.mapgenerator.commandgui.korge.interaction.MapPlacedObjectId
import io.hsar.mapgenerator.commandgui.korge.interaction.MapViewerUnitTemplate
import io.hsar.mapgenerator.commandgui.korge.interaction.UnitSnapshot
import korlibs.image.bitmap.Bitmap32
import korlibs.image.color.RGBA
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.closeTo
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.notNullValue
import org.junit.jupiter.api.Test
import java.util.*

class MapViewerDialogFocusTargetBoundsTest {
    @Test
    fun `empty referenced objects produce no focus bounds`() {
        assertThat(
            referencedObjectFocusTargetBounds(
                referencedObjectIds = emptySet(),
                objectSnapshot = { null },
                coordinateSpace = coordinateSpace(),
                viewport = viewport(),
            ),
            equalTo(null),
        )
    }

    @Test
    fun `referenced object bounds union uses the current viewport`() {
        val first = unitSnapshot(
            id = "00000000-0000-0000-0000-000000000001",
            mapX = 0.2,
            mapY = 0.3,
            width = 0.1,
            height = 0.2,
        )
        val second = unitSnapshot(
            id = "00000000-0000-0000-0000-000000000002",
            mapX = 0.7,
            mapY = 0.6,
            width = 0.1,
            height = 0.1,
        )

        val bounds = referencedObjectFocusTargetBounds(
            referencedObjectIds = setOf(first.id, second.id),
            objectSnapshot = { id ->
                listOf(first, second).firstOrNull { it.id == id }
            },
            coordinateSpace = coordinateSpace(),
            viewport = viewport(),
        )

        assertThat(bounds, notNullValue())
        assertThat(bounds!!.left, closeTo(150.0, 1e-9))
        assertThat(bounds.top, closeTo(200.0, 1e-9))
        assertThat(bounds.width, closeTo(600.0, 1e-9))
        assertThat(bounds.height, closeTo(450.0, 1e-9))
    }

    private fun coordinateSpace(): MapCoordinateSpace =
        MapCoordinateSpace(
            geometry = ViewerGeometry(
                backgroundWidth = 1000,
                backgroundHeight = 1000,
                mapOffsetX = 0,
                mapOffsetY = 0,
            ),
            mapWidth = 1000,
            mapHeight = 1000,
        )

    private fun viewport(): CameraViewport =
        CameraViewport.centeredOnMap(
            geometry = ViewerGeometry(
                backgroundWidth = 1000,
                backgroundHeight = 1000,
                mapOffsetX = 0,
                mapOffsetY = 0,
            ),
            mapWidth = 1000,
            mapHeight = 1000,
            viewportWidth = 1000,
            viewportHeight = 1000,
            zoom = 1.0,
        )

    private fun unitSnapshot(
        id: String,
        mapX: Double,
        mapY: Double,
        width: Double,
        height: Double,
    ): UnitSnapshot =
        UnitSnapshot(
            id = MapPlacedObjectId(UUID.fromString(id)),
            unitTemplateId = io.hsar.mapgenerator.commandgui.korge.interaction.UnitTemplateId(UUID.fromString("00000000-0000-0000-0000-000000000010")),
            template = MapViewerUnitTemplate(
                id = io.hsar.mapgenerator.commandgui.korge.interaction.UnitTemplateId(UUID.fromString("00000000-0000-0000-0000-000000000010")),
                name = "Unit",
                bitmap = Bitmap32(10, 10) { _, _ -> RGBA(255, 0, 0, 255) },
            ),
            instanceName = "Unit",
            destroyed = false,
            facingDegrees = null,
            mapCoordinate = MapCoordinate(mapX, mapY),
            normalizedWidth = width,
            normalizedHeight = height,
        )
}
