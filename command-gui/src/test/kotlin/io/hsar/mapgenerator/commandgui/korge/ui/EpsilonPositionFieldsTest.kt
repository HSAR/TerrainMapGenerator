package io.hsar.mapgenerator.commandgui.korge.ui

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test

class EpsilonPositionFieldsTest {
    @Test
    fun `splits an epsilon reference into X and Y offsets`() {
        val fields = EpsilonPositionFields.parse("001-999")

        assertThat(fields.x, equalTo("001"))
        assertThat(fields.y, equalTo("999"))
    }

    @Test
    fun `retains an incomplete epsilon reference while it is being edited`() {
        val fields = EpsilonPositionFields.parse("001-")

        assertThat(fields.x, equalTo("001"))
        assertThat(fields.y, equalTo(""))
    }
}
