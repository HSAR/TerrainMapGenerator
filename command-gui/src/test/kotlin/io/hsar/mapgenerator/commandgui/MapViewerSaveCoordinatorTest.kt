package io.hsar.mapgenerator.commandgui

import io.hsar.mapgenerator.commandgui.operation.OperationDocument
import io.hsar.mapgenerator.commandgui.operation.OperationTimeline
import io.hsar.mapgenerator.commandgui.operation.OperationTimelineEntries
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.awt.image.BufferedImage
import java.nio.file.Path

class MapViewerSaveCoordinatorTest {
    @TempDir
    lateinit var tempDir: Path

    private var now = 1_000L
    private val executor = FakeSaveExecutor()
    private val exits = mutableListOf<MapViewerExitTarget>()
    private val failures = mutableListOf<String>()

    @Test
    fun `debounces mutations and saves the latest revision`() {
        val coordinator = coordinator(snapshot(1))
        coordinator.observe(snapshot(2))

        now += 1_999L
        coordinator.wake()
        assertThat(executor.attempts.size, equalTo(0))

        now += 1L
        coordinator.wake()
        assertThat(executor.attempts.single().snapshot.revision, equalTo(2L))
        assertThat(executor.attempts.single().reason, equalTo(MapViewerSaveReason.AUTOSAVE))
    }

    @Test
    fun `autosave advances a rewound timeline to its live head`() {
        val timeline = OperationTimeline()
            .record(OperationTimelineEntries.turn("Turn 1"))
            .record(OperationTimelineEntries.turn("Turn 2"))
            .undo()
        val coordinator = coordinator(snapshot(1).let { snapshot ->
            snapshot.copy(document = snapshot.document.copy(timeline = timeline))
        })

        now += 2_000L
        coordinator.wake()

        val savedTimeline = executor.attempts.single().snapshot.document.timeline
        assertThat(savedTimeline.appliedCount, equalTo(savedTimeline.entries.size))
        assertThat(savedTimeline.canRedo, equalTo(false))
    }

    @Test
    fun `serializes writes and coalesces a manual request onto the newest revision`() {
        val coordinator = coordinator(snapshot(1))
        coordinator.requestSave()
        coordinator.observe(snapshot(2))
        coordinator.requestSave()

        assertThat(executor.attempts.map { it.snapshot.revision }, equalTo(listOf(1L)))

        executor.succeed(0)

        assertThat(executor.attempts.map { it.snapshot.revision }, equalTo(listOf(1L, 2L)))
        assertThat(executor.attempts.last().reason, equalTo(MapViewerSaveReason.MANUAL))
    }

    @Test
    fun `mutation during save remains dirty and follows the trailing deadline`() {
        val coordinator = coordinator(snapshot(1))
        coordinator.requestSave()
        coordinator.observe(snapshot(2))
        now += 2_000L

        executor.succeed(0)

        assertThat(executor.attempts.map { it.snapshot.revision }, equalTo(listOf(1L, 2L)))
        assertThat(executor.attempts.last().reason, equalTo(MapViewerSaveReason.AUTOSAVE))
    }

    @Test
    fun `ordinary failure retries with capped backoff and notifies once per series`() {
        val coordinator = coordinator(snapshot(1))
        coordinator.requestSave()
        executor.fail(0, "disk unavailable")

        assertThat(coordinator.state.nextScheduledAtMillis, equalTo(now + 2_000L))
        assertThat(failures, equalTo(listOf("disk unavailable")))

        now += 2_000L
        coordinator.wake()
        executor.fail(1, "still unavailable")
        assertThat(coordinator.state.nextScheduledAtMillis, equalTo(now + 4_000L))
        assertThat(failures, equalTo(listOf("disk unavailable")))

        repeat(5) {
            now = coordinator.state.nextScheduledAtMillis!!
            coordinator.wake()
            executor.fail(executor.attempts.lastIndex, "still unavailable")
        }
        assertThat(coordinator.state.nextScheduledAtMillis, equalTo(now + 60_000L))
    }

    @Test
    fun `manual retry bypasses failure backoff and success resets it`() {
        val coordinator = coordinator(snapshot(1))
        coordinator.requestSave()
        executor.fail(0, "failed")

        coordinator.requestSave()
        assertThat(executor.attempts.size, equalTo(2))
        executor.succeed(1)

        coordinator.observe(snapshot(2))
        coordinator.requestSave()
        executor.fail(2, "new series")
        assertThat(coordinator.state.nextScheduledAtMillis, equalTo(now + 2_000L))
        assertThat(failures, equalTo(listOf("failed", "new series")))
    }

    @Test
    fun `clean exit is immediate while dirty exit waits for the newest revision`() {
        val clean = coordinator(snapshot(0, persisted = true))
        clean.requestExit(MapViewerExitTarget.LAUNCHPAD)
        assertThat(exits, equalTo(listOf(MapViewerExitTarget.LAUNCHPAD)))
        assertThat(executor.attempts.size, equalTo(0))

        exits.clear()
        val dirty = coordinator(snapshot(1))
        dirty.requestExit(MapViewerExitTarget.LAUNCHPAD)
        dirty.observe(snapshot(2))
        dirty.requestExit(MapViewerExitTarget.DESKTOP)
        executor.succeed(0)

        assertThat(exits, equalTo(emptyList()))
        assertThat(executor.attempts.map { it.snapshot.revision }, equalTo(listOf(1L, 2L)))
        executor.succeed(1)
        assertThat(exits, equalTo(listOf(MapViewerExitTarget.DESKTOP)))
    }

    @Test
    fun `failed exit pauses until retry or resumes autosave after cancellation`() {
        val coordinator = coordinator(snapshot(1))
        coordinator.requestExit(MapViewerExitTarget.DESKTOP)
        executor.fail(0, "denied")

        assertThat(coordinator.state.awaitingExitRetry, equalTo(true))
        assertThat(coordinator.state.nextScheduledAtMillis, equalTo(null))

        coordinator.cancelExit()
        assertThat(coordinator.state.pendingExit, equalTo(null))
        assertThat(coordinator.state.nextScheduledAtMillis, equalTo(now + 2_000L))

        coordinator.requestExit(MapViewerExitTarget.LAUNCHPAD)
        executor.fail(1, "denied")
        coordinator.retryExit()
        executor.succeed(2)
        assertThat(exits, equalTo(listOf(MapViewerExitTarget.LAUNCHPAD)))
    }

    @Test
    fun `latest exit destination wins while failure is awaiting retry`() {
        val coordinator = coordinator(snapshot(1))
        coordinator.requestExit(MapViewerExitTarget.LAUNCHPAD)
        executor.fail(0, "denied")

        coordinator.requestExit(MapViewerExitTarget.DESKTOP)
        coordinator.retryExit()
        executor.succeed(1)

        assertThat(exits, equalTo(listOf(MapViewerExitTarget.DESKTOP)))
    }

    @Test
    fun `duplicate stale and post disposal completions are ignored`() {
        val saved = mutableListOf<Long>()
        val coordinator = coordinator(snapshot(1), onSaved = { _, revision, _ -> saved += revision })
        coordinator.requestSave()
        val completion = executor.completions.single()
        completion(MapViewerSaveCompletion.Success(executor.attempts.single().snapshot.document.copy(persisted = true)))
        completion(MapViewerSaveCompletion.Failure("late"))

        coordinator.observe(snapshot(2))
        coordinator.requestSave()
        completion(MapViewerSaveCompletion.Failure("stale"))
        assertThat(coordinator.state.status, equalTo(MapViewerSaveStatus.SAVING))
        coordinator.dispose()
        executor.succeed(1)

        assertThat(saved, equalTo(listOf(1L)))
        assertThat(exits, equalTo(emptyList()))
    }

    @Test
    fun `completion is serialized through the injected dispatcher`() {
        val dispatched = mutableListOf<() -> Unit>()
        val coordinator = MapViewerSaveCoordinator(
            initialSnapshot = snapshot(1),
            saver = executor,
            nowMillis = { now },
            completionDispatcher = dispatched::add,
        )
        coordinator.requestSave()

        executor.succeed(0)
        assertThat(coordinator.state.status, equalTo(MapViewerSaveStatus.SAVING))

        dispatched.single().invoke()
        assertThat(coordinator.state.status, equalTo(MapViewerSaveStatus.CLEAN))
    }

    private fun coordinator(
        initial: MapViewerSaveSnapshot,
        onSaved: (OperationDocument, Long, MapViewerSaveReason) -> Unit = { _, _, _ -> },
    ) = MapViewerSaveCoordinator(
        initialSnapshot = initial,
        saver = executor,
        nowMillis = { now },
        onSaveSucceeded = onSaved,
        onFailureSeriesStarted = failures::add,
        onExitReady = exits::add,
    )

    private fun snapshot(revision: Long, persisted: Boolean = false): MapViewerSaveSnapshot {
        val repository = MapViewerSaveRepository(tempDir)
        val document = repository.createOperation(
            MapViewerSettings(operationName = "Coordinator"),
            MapViewerRenderData(BufferedImage(8, 8, BufferedImage.TYPE_INT_ARGB)),
        ).copy(persisted = persisted)
        return MapViewerSaveSnapshot(revision, document, null, document.uiState)
    }

    private class FakeSaveExecutor : MapViewerSaveExecutor {
        val attempts = mutableListOf<MapViewerSaveAttempt>()
        val completions = mutableListOf<(MapViewerSaveCompletion) -> Unit>()

        override fun save(attempt: MapViewerSaveAttempt, completion: (MapViewerSaveCompletion) -> Unit) {
            attempts += attempt
            completions += completion
        }

        fun succeed(index: Int) = completions[index](MapViewerSaveCompletion.Success(attempts[index].snapshot.document.copy(persisted = true)))
        fun fail(index: Int, message: String) = completions[index](MapViewerSaveCompletion.Failure(message))
    }
}
