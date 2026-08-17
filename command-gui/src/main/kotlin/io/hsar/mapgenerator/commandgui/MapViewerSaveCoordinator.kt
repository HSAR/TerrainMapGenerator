package io.hsar.mapgenerator.commandgui

import io.hsar.mapgenerator.commandgui.operation.OperationDocument
import io.hsar.mapgenerator.commandgui.operation.OperationUiState

data class MapViewerSaveSnapshot(
    val revision: Long,
    val document: OperationDocument,
    val camera: MapViewerSavedCamera?,
    val uiState: OperationUiState,
)

enum class MapViewerSaveReason { MANUAL, AUTOSAVE, EXIT }

enum class MapViewerExitTarget { LAUNCHPAD, DESKTOP }

enum class MapViewerSaveStatus { CLEAN, DIRTY, SAVING, FAILED }

data class MapViewerSaveAttempt(
    val id: Long,
    val reason: MapViewerSaveReason,
    val snapshot: MapViewerSaveSnapshot,
)

sealed interface MapViewerSaveCompletion {
    data class Success(val document: OperationDocument) : MapViewerSaveCompletion
    data class Failure(val message: String) : MapViewerSaveCompletion
}

fun interface MapViewerSaveExecutor {
    fun save(attempt: MapViewerSaveAttempt, completion: (MapViewerSaveCompletion) -> Unit)
}

data class MapViewerSaveCoordinatorState(
    val currentRevision: Long,
    val savedRevision: Long,
    val inFlightRevision: Long?,
    val status: MapViewerSaveStatus,
    val failure: String?,
    val failureCount: Int,
    val pendingExit: MapViewerExitTarget?,
    val nextScheduledAtMillis: Long?,
) {
    val dirty get() = currentRevision > savedRevision
    val awaitingExitRetry get() = pendingExit != null && status == MapViewerSaveStatus.FAILED
    val manualSaveEnabled get() = inFlightRevision == null || currentRevision > inFlightRevision

    companion object {
        fun initial(snapshot: MapViewerSaveSnapshot, savedRevision: Long = if (snapshot.document.persisted) snapshot.revision else 0L) =
            MapViewerSaveCoordinatorState(
                currentRevision = snapshot.revision,
                savedRevision = savedRevision,
                inFlightRevision = null,
                status = if (snapshot.revision > savedRevision) MapViewerSaveStatus.DIRTY else MapViewerSaveStatus.CLEAN,
                failure = null,
                failureCount = 0,
                pendingExit = null,
                nextScheduledAtMillis = null,
            )
    }
}

class MapViewerSaveCoordinator(
    initialSnapshot: MapViewerSaveSnapshot,
    private val saver: MapViewerSaveExecutor,
    private val nowMillis: () -> Long = System::currentTimeMillis,
    private val completionDispatcher: ((() -> Unit) -> Unit) = { it() },
    private val debounceMillis: Long = 2_000L,
    private val retryDelaysMillis: List<Long> = listOf(2_000L, 4_000L, 8_000L, 16_000L, 32_000L, 60_000L),
    private val onStateChanged: (MapViewerSaveCoordinatorState) -> Unit = {},
    private val onSaveSucceeded: (OperationDocument, Long, MapViewerSaveReason) -> Unit = { _, _, _ -> },
    private val onFailureSeriesStarted: (String) -> Unit = {},
    private val onExitReady: (MapViewerExitTarget) -> Unit = {},
    initialSavedRevision: Long = if (initialSnapshot.document.persisted) initialSnapshot.revision else 0L,
) {
    private var latestSnapshot = initialSnapshot
    private var savedRevision = initialSavedRevision
    private var inFlight: MapViewerSaveAttempt? = null
    private var pendingExit: MapViewerExitTarget? = null
    private var scheduledAtMillis: Long? = initialSnapshot.revision
        .takeIf { it > initialSavedRevision }
        ?.let { nowMillis() + debounceMillis }
    private var failure: String? = null
    private var failureCount = 0
    private var failureSeriesNotified = false
    private var queuedImmediateSave = false
    private var nextAttemptId = 1L
    private var disposed = false

    var state: MapViewerSaveCoordinatorState = stateValue()
        private set

    fun observe(snapshot: MapViewerSaveSnapshot) {
        if (disposed || snapshot.revision < latestSnapshot.revision) return
        val changedRevision = snapshot.revision > latestSnapshot.revision
        latestSnapshot = snapshot
        if (snapshot.revision <= savedRevision) {
            scheduledAtMillis = null
            failure = null
        } else if (changedRevision && failure == null) {
            scheduledAtMillis = nowMillis() + debounceMillis
        }
        publish()
    }

    fun requestSave(reason: MapViewerSaveReason = MapViewerSaveReason.MANUAL) {
        if (disposed || latestSnapshot.revision <= savedRevision && failure == null) return
        if (inFlight != null) {
            if (latestSnapshot.revision > inFlight!!.snapshot.revision) queuedImmediateSave = true
            publish()
            return
        }
        startSave(reason)
    }

    fun requestExit(target: MapViewerExitTarget) {
        if (disposed) return
        val wasAwaitingExitRetry = pendingExit != null && failure != null && inFlight == null
        pendingExit = target
        scheduledAtMillis = null
        when {
            inFlight != null || wasAwaitingExitRetry -> publish()
            latestSnapshot.revision <= savedRevision -> completeExit()
            else -> startSave(MapViewerSaveReason.EXIT)
        }
    }

    fun retryExit() {
        if (disposed || pendingExit == null || inFlight != null) return
        startSave(MapViewerSaveReason.EXIT)
    }

    fun cancelExit() {
        if (disposed || pendingExit == null) return
        pendingExit = null
        if (latestSnapshot.revision > savedRevision && inFlight == null) {
            scheduledAtMillis = nowMillis() + retryDelay()
        }
        publish()
    }

    fun wake() {
        if (disposed || inFlight != null || pendingExit != null || latestSnapshot.revision <= savedRevision) return
        if (nowMillis() >= (scheduledAtMillis ?: Long.MAX_VALUE)) startSave(MapViewerSaveReason.AUTOSAVE)
    }

    fun dispose() {
        disposed = true
        scheduledAtMillis = null
        queuedImmediateSave = false
    }

    private fun startSave(reason: MapViewerSaveReason) {
        if (disposed || inFlight != null) return
        if (latestSnapshot.revision <= savedRevision) {
            pendingExit?.let { completeExit() } ?: publish()
            return
        }
        scheduledAtMillis = null
        failure = null
        queuedImmediateSave = false
        val attempt = MapViewerSaveAttempt(nextAttemptId++, reason, latestSnapshot)
        inFlight = attempt
        publish()
        runCatching { saver.save(attempt) { completion -> completionDispatcher { complete(attempt.id, completion) } } }
            .onFailure { thrown ->
                completionDispatcher {
                    complete(attempt.id, MapViewerSaveCompletion.Failure(thrown.message ?: thrown::class.simpleName ?: "Unknown save failure"))
                }
            }
    }

    private fun complete(attemptId: Long, completion: MapViewerSaveCompletion) {
        if (disposed || inFlight?.id != attemptId) return
        val completedAttempt = inFlight ?: return
        inFlight = null
        when (completion) {
            is MapViewerSaveCompletion.Success -> completeSuccess(completedAttempt, completion.document)
            is MapViewerSaveCompletion.Failure -> completeFailure(completion.message)
        }
    }

    private fun completeSuccess(attempt: MapViewerSaveAttempt, document: OperationDocument) {
        savedRevision = maxOf(savedRevision, attempt.snapshot.revision)
        failure = null
        failureCount = 0
        failureSeriesNotified = false
        onSaveSucceeded(document, attempt.snapshot.revision, attempt.reason)
        when {
            pendingExit != null && latestSnapshot.revision <= savedRevision -> completeExit()
            pendingExit != null -> startSave(MapViewerSaveReason.EXIT)
            queuedImmediateSave && latestSnapshot.revision > savedRevision -> startSave(MapViewerSaveReason.MANUAL)
            latestSnapshot.revision > savedRevision -> {
                scheduledAtMillis = scheduledAtMillis ?: nowMillis() + debounceMillis
                if (nowMillis() >= scheduledAtMillis!!) startSave(MapViewerSaveReason.AUTOSAVE) else publish()
            }
            else -> {
                scheduledAtMillis = null
                publish()
            }
        }
    }

    private fun completeFailure(message: String) {
        failure = message.ifBlank { "Unknown save failure" }
        failureCount++
        if (!failureSeriesNotified) {
            failureSeriesNotified = true
            onFailureSeriesStarted(failure!!)
        }
        scheduledAtMillis = if (pendingExit == null) nowMillis() + retryDelay() else null
        publish()
    }

    private fun completeExit() {
        val target = pendingExit ?: return
        pendingExit = null
        scheduledAtMillis = null
        publish()
        onExitReady(target)
    }

    private fun retryDelay() = retryDelaysMillis
        .ifEmpty { listOf(60_000L) }
        .getOrElse((failureCount - 1).coerceAtLeast(0)) { retryDelaysMillis.lastOrNull() ?: 60_000L }

    private fun publish() {
        state = stateValue()
        onStateChanged(state)
    }

    private fun stateValue() = MapViewerSaveCoordinatorState(
        currentRevision = latestSnapshot.revision,
        savedRevision = savedRevision,
        inFlightRevision = inFlight?.snapshot?.revision,
        status = when {
            inFlight != null -> MapViewerSaveStatus.SAVING
            failure != null -> MapViewerSaveStatus.FAILED
            latestSnapshot.revision > savedRevision -> MapViewerSaveStatus.DIRTY
            else -> MapViewerSaveStatus.CLEAN
        },
        failure = failure,
        failureCount = failureCount,
        pendingExit = pendingExit,
        nextScheduledAtMillis = scheduledAtMillis,
    )
}
