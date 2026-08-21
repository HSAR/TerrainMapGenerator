package io.hsar.mapgenerator.commandgui

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.concurrent.CancellationException

class MapViewerLoadingTest {
    @Test
    fun `work accepts only the current request completion`() {
        val first = MapViewerAsyncWork<String>().start(MapViewerAsyncOperation.OPEN_ARCHIVE)
        val second = first.start(MapViewerAsyncOperation.OPEN_ARCHIVE)

        val stale = second.succeed(first.runningRequestId!!, "stale")
        val completed = stale.succeed(second.runningRequestId!!, "current")

        assertThat(stale, equalTo(second))
        assertThat(
            completed.state,
            equalTo(MapViewerAsyncState.Succeeded(2L, MapViewerAsyncOperation.OPEN_ARCHIVE, "current")),
        )
    }

    @Test
    fun `cancelled request cannot later succeed`() {
        val running = MapViewerAsyncWork<String>().start(MapViewerAsyncOperation.DECODE_IMAGE)
        val cancelled = running.cancel(running.runningRequestId!!)

        val late = cancelled.succeed(running.runningRequestId!!, "late")

        assertThat(late, equalTo(cancelled))
        assertThat(cancelled.state, equalTo(MapViewerAsyncState.Cancelled(1L, MapViewerAsyncOperation.DECODE_IMAGE)))
    }

    @Test
    fun `cancellation signal observes explicit cancellation`() {
        val signal = MapViewerCancellationSignal()
        signal.cancel()

        assertThrows<CancellationException> { signal.ensureActive() }
    }
}
