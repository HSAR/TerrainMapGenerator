package io.hsar.mapgenerator.commandgui.operation

import io.hsar.mapgenerator.commandgui.MapCoordinate
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import java.util.UUID

class OperationDestroyedUnitDisplayPolicyTest {
    private val active = unit(1, false)
    private val destroyed = unit(2, true)

    @Test
    fun `full partial and hide expose the contracted visibility alpha and movement`() {
        assertThat(OperationDestroyedUnitDisplayPolicy.visible(destroyed, OperationDestroyedUnitDisplayMode.Full), equalTo(true))
        assertThat(OperationDestroyedUnitDisplayPolicy.iconAlpha(destroyed, OperationDestroyedUnitDisplayMode.Partial), equalTo(0.5))
        assertThat(OperationDestroyedUnitDisplayPolicy.movable(destroyed, OperationDestroyedUnitDisplayMode.Partial), equalTo(false))
        assertThat(OperationDestroyedUnitDisplayPolicy.visible(destroyed, OperationDestroyedUnitDisplayMode.Hide), equalTo(false))
    }

    @Test
    fun `group movement and hidden selection reject destroyed members without changing objects`() {
        val objects = listOf(active, destroyed)
        val selected = setOf(active.id, destroyed.id)

        assertThat(OperationDestroyedUnitDisplayPolicy.movable(objects, selected, OperationDestroyedUnitDisplayMode.Partial), equalTo(false))
        assertThat(OperationDestroyedUnitDisplayPolicy.visibleSelection(objects, selected, OperationDestroyedUnitDisplayMode.Hide), equalTo(setOf(active.id)))
        assertThat(objects, equalTo(listOf(active, destroyed)))
    }

    private fun unit(seed: Int, destroyed: Boolean) = OperationUnit(
        OperationObjectId(UUID(0, seed.toLong())), OperationUnitTemplateId(UUID(1, seed.toLong())), "Unit $seed", destroyed, 0,
        MapCoordinate(0.5, 0.5), 0.05, 0.05,
    )
}
