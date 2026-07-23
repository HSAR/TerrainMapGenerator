package io.hsar.mapgenerator.commandgui.korge.ui

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test

class MapViewerDebouncerTest {
    @Test
    fun `waits for typing to settle before publishing the latest value`() {
        var now = 0L
        val published = mutableListOf<String>()
        val debouncer = MapViewerDebouncer<String>(delayMilliseconds = 350, nowMilliseconds = { now }) { published += it }

        debouncer.schedule("4")
        now = 100
        debouncer.schedule("49")
        now = 200
        debouncer.schedule("499")
        debouncer.flush()
        now = 550
        debouncer.flush()

        assertThat(published, equalTo(listOf("499")))
    }

    @Test
    fun `does not publish a cancelled value`() {
        var now = 0L
        val published = mutableListOf<String>()
        val debouncer = MapViewerDebouncer<String>(delayMilliseconds = 350, nowMilliseconds = { now }) { published += it }

        debouncer.schedule("401")
        debouncer.cancel()
        now = 350
        debouncer.flush()

        assertThat(published, equalTo(emptyList()))
    }
}
