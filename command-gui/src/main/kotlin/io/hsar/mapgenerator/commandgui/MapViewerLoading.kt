package io.hsar.mapgenerator.commandgui

import java.io.ByteArrayOutputStream
import java.io.File
import java.util.concurrent.CancellationException
import java.util.concurrent.atomic.AtomicBoolean

enum class MapViewerAsyncOperation {
    LIST_ARCHIVES,
    OPEN_ARCHIVE,
    DECODE_IMAGE,
    RENDER_MAP,
    CREATE_OPERATION,
}

sealed interface MapViewerAsyncState<out T> {
    data object Idle : MapViewerAsyncState<Nothing>
    data class Running(val requestId: Long, val operation: MapViewerAsyncOperation) : MapViewerAsyncState<Nothing>
    data class Succeeded<T>(val requestId: Long, val operation: MapViewerAsyncOperation, val value: T) : MapViewerAsyncState<T>
    data class Failed(val requestId: Long, val operation: MapViewerAsyncOperation, val failure: MapViewerLoadFailure) : MapViewerAsyncState<Nothing>
    data class Cancelled(val requestId: Long, val operation: MapViewerAsyncOperation) : MapViewerAsyncState<Nothing>
}

data class MapViewerAsyncWork<T>(
    val sequence: Long = 0L,
    val state: MapViewerAsyncState<T> = MapViewerAsyncState.Idle,
) {
    val runningRequestId: Long?
        get() = (state as? MapViewerAsyncState.Running)?.requestId

    fun start(operation: MapViewerAsyncOperation) = copy(
        sequence = sequence + 1L,
        state = MapViewerAsyncState.Running(sequence + 1L, operation),
    )

    fun succeed(requestId: Long, value: T) = transition(requestId) { operation ->
        MapViewerAsyncState.Succeeded(requestId, operation, value)
    }

    fun fail(requestId: Long, failure: MapViewerLoadFailure) = transition(requestId) { operation ->
        MapViewerAsyncState.Failed(requestId, operation, failure)
    }

    fun cancel(requestId: Long) = transition(requestId) { operation ->
        MapViewerAsyncState.Cancelled(requestId, operation)
    }

    fun idle() = copy(state = MapViewerAsyncState.Idle)

    private fun transition(
        requestId: Long,
        next: (MapViewerAsyncOperation) -> MapViewerAsyncState<T>,
    ) = (state as? MapViewerAsyncState.Running)
        ?.takeIf { it.requestId == requestId }
        ?.let { copy(state = next(it.operation)) }
        ?: this
}

data class MapViewerLoadLimits(
    val maximumArchiveBytes: Long = 512L * 1024L * 1024L,
    val maximumArchiveEntries: Int = 10_000,
    val maximumArchiveEntryBytes: Long = 256L * 1024L * 1024L,
    val maximumExpandedArchiveBytes: Long = 1024L * 1024L * 1024L,
    val maximumCompressionRatio: Long = 100L,
    val maximumJsonBytes: Long = 64L * 1024L * 1024L,
    val maximumEncodedImageBytes: Long = 256L * 1024L * 1024L,
    val maximumImageDimension: Int = 16_384,
    val maximumImagePixels: Long = 100_000_000L,
)

sealed interface MapViewerLoadFailure {
    val message: String

    data class LimitExceeded(
        val subject: String,
        val actual: Long,
        val limit: Long,
        val unit: String = "bytes",
    ) : MapViewerLoadFailure {
        override val message = "$subject is $actual $unit; the supported limit is $limit $unit."
    }

    data class Unsupported(override val message: String) : MapViewerLoadFailure
    data class Invalid(override val message: String) : MapViewerLoadFailure
    data class Unreadable(override val message: String) : MapViewerLoadFailure

    companion object {
        fun from(failure: Throwable): MapViewerLoadFailure = when (failure) {
            is MapViewerLoadException -> failure.failure
            else -> Unreadable(failure.message ?: failure::class.simpleName.orEmpty())
        }
    }
}

class MapViewerLoadException(val failure: MapViewerLoadFailure) : IllegalArgumentException(failure.message)

class MapViewerCancellationSignal {
    private val cancelled = AtomicBoolean(false)

    fun cancel() {
        cancelled.set(true)
    }

    fun ensureActive() {
        if (cancelled.get() || Thread.currentThread().isInterrupted) {
            throw CancellationException("Loading was cancelled")
        }
    }

    companion object {
        val NONE: MapViewerCancellationSignal
            get() = MapViewerCancellationSignal()
    }
}

fun readMapViewerFileBytes(
    file: File,
    maximumBytes: Long,
    cancellation: MapViewerCancellationSignal = MapViewerCancellationSignal.NONE,
): ByteArray {
    val size = file.length()
    if (size > maximumBytes) {
        throw MapViewerLoadException(MapViewerLoadFailure.LimitExceeded(file.name, size, maximumBytes))
    }
    return file.inputStream().buffered().use { input ->
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var total = 0L
        while (true) {
            cancellation.ensureActive()
            val count = input.read(buffer)
            if (count < 0) break
            total += count
            if (total > maximumBytes) {
                throw MapViewerLoadException(MapViewerLoadFailure.LimitExceeded(file.name, total, maximumBytes))
            }
            output.write(buffer, 0, count)
        }
        output.toByteArray()
    }
}
