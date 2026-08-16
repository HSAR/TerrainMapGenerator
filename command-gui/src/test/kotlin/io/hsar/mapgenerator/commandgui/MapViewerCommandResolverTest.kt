package io.hsar.mapgenerator.commandgui

import io.hsar.mapgenerator.commandgui.operation.OperationContent
import io.hsar.mapgenerator.commandgui.operation.OperationObjectId
import io.hsar.mapgenerator.commandgui.operation.OperationUnit
import io.hsar.mapgenerator.commandgui.operation.OperationUnitTemplateId
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import java.util.UUID

class MapViewerCommandResolverTest {
    private val first = unit(1)
    private val second = unit(2)

    @Test
    fun `multi selection exposes only immediate group deletion`() {
        val items = MapViewerCommandResolver.contextMenu(
            first.id,
            MapViewerCommandContext(OperationContent(objects = listOf(first, second)), setOf(first.id, second.id), readOnly = false, olympiadDawnEnabled = true),
        )

        assertThat(items.map(MapViewerCommandItem::label), equalTo(listOf("Delete Selected")))
        assertThat(items.single().destructive, equalTo(true))
        assertThat(items.single().command, equalTo(MapViewerCommand.DeleteObjects(setOf(first.id, second.id))))
    }

    @Test
    fun `historical unit menu keeps inspection and explains disabled mutations`() {
        val items = MapViewerCommandResolver.contextMenu(
            first.id,
            MapViewerCommandContext(OperationContent(objects = listOf(first)), setOf(first.id), readOnly = true, olympiadDawnEnabled = false),
        )

        assertThat(items.first { it.label == "Edit Unit" }.enabled, equalTo(true))
        assertThat(items.first { it.label == "Delete Unit" }.enabled, equalTo(false))
        assertThat(items.first { it.label == "Delete Unit" }.disabledReason, equalTo("History preview is read-only."))
    }

    @Test
    fun `feature flag exposes separate report tactical and status rows`() {
        val enabled = MapViewerCommandResolver.contextMenu(
            first.id,
            MapViewerCommandContext(OperationContent(objects = listOf(first)), setOf(first.id), readOnly = false, olympiadDawnEnabled = true),
        ).map(MapViewerCommandItem::label)
        val disabled = MapViewerCommandResolver.contextMenu(
            first.id,
            MapViewerCommandContext(OperationContent(objects = listOf(first)), setOf(first.id), readOnly = false, olympiadDawnEnabled = false),
        ).map(MapViewerCommandItem::label)

        assertThat(enabled.containsAll(listOf("SCIREP", "NAVREP", "TACTICAL", "STATUS")), equalTo(true))
        assertThat(disabled.any { it in setOf("SCIREP", "NAVREP", "TACTICAL", "STATUS") }, equalTo(false))
    }

    private fun unit(seed: Int) = OperationUnit(
        OperationObjectId(UUID(0, seed.toLong())), OperationUnitTemplateId(UUID(1, seed.toLong())), "Unit $seed", false, 0,
        MapCoordinate(0.5, 0.5), 0.05, 0.05,
    )
}
