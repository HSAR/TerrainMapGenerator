package io.hsar.mapgenerator.commandgui.compose

import io.hsar.mapgenerator.command_gui_compose.generated.resources.Res
import kotlinx.coroutines.runBlocking
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.greaterThan
import org.junit.jupiter.api.Test

class NordFontResourcesTest {

    @Test
    fun `packages regular and bold TV Nord EF faces for Compose`() = runBlocking {
        val regular = Res.readBytes("font/tv_nord_ef_regular_con.ttf")
        val bold = Res.readBytes("font/tv_nord_ef_bold_con.ttf")

        assertThat(regular.size, greaterThan(80_000))
        assertThat(bold.size, greaterThan(79_000))
    }
}
