package io.hsar.mapgenerator.commandgui

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test

class UnitIconDisplaySizeTest {

    @Test
    fun `scrolling changes the global icon size by the KorGE display step`() {
        val enlarged = UnitIconDisplaySize.scroll(UnitIconDisplaySize.DEFAULT_PX, -1f)
        val reduced = UnitIconDisplaySize.scroll(UnitIconDisplaySize.DEFAULT_PX, 1f)

        assertThat(enlarged, equalTo(UnitIconDisplaySize.DEFAULT_PX * UnitIconDisplaySize.SCROLL_STEP))
        assertThat(reduced, equalTo(UnitIconDisplaySize.DEFAULT_PX / UnitIconDisplaySize.SCROLL_STEP))
    }

    @Test
    fun `scrolling respects the shared icon size limits`() {
        assertThat(UnitIconDisplaySize.scroll(UnitIconDisplaySize.MAX_PX, -1f), equalTo(UnitIconDisplaySize.MAX_PX))
        assertThat(UnitIconDisplaySize.scroll(UnitIconDisplaySize.MIN_PX, 1f), equalTo(UnitIconDisplaySize.MIN_PX))
    }
}
