package io.hsar.mapgenerator.commandgui.compose

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test

class ComposeAutosaveSchedulerTest {
    private var now = 1_000L
    private val scheduler = ComposeAutosaveScheduler({ now }, debounceMillis = 2_000L)

    @Test
    fun `uses a trailing debounce and coalesces mutations to the latest revision`() {
        assertThat(scheduler.observe(observation(revision = 1)), equalTo(2_000L))
        now += 1_500L
        assertThat(scheduler.observe(observation(revision = 2)), equalTo(2_000L))
        now += 1_999L
        assertThat(scheduler.consumeIfDue(observation(revision = 2)), equalTo(null))
        now += 1L
        assertThat(scheduler.consumeIfDue(observation(revision = 2)), equalTo(2L))
    }

    @Test
    fun `waits for an in flight save then schedules a newer dirty revision`() {
        assertThat(scheduler.observe(observation(revision = 2, savingRevision = 1)), equalTo(null))
        now += 500L
        assertThat(scheduler.observe(observation(revision = 2)), equalTo(2_000L))
    }

    @Test
    fun `failure and exit cancel pending automatic saves`() {
        scheduler.observe(observation(revision = 1))
        assertThat(scheduler.observe(observation(revision = 1, failed = true)), equalTo(null))
        now += 5_000L
        assertThat(scheduler.consumeIfDue(observation(revision = 1)), equalTo(null))

        scheduler.observe(observation(revision = 2))
        assertThat(scheduler.observe(observation(revision = 2, exiting = true)), equalTo(null))
    }

    private fun observation(
        revision: Long,
        savedRevision: Long = 0L,
        savingRevision: Long? = null,
        failed: Boolean = false,
        exiting: Boolean = false,
    ) = ComposeAutosaveObservation(revision, savedRevision, savingRevision, failed, exiting)
}
