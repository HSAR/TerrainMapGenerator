package io.hsar.mapgenerator.commandgui.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import io.hsar.mapgenerator.commandgui.MapViewerAsyncOperation
import io.hsar.mapgenerator.commandgui.MapViewerAsyncState
import io.hsar.mapgenerator.commandgui.MapViewerAsyncWork
import io.hsar.mapgenerator.commandgui.MapViewerCancellationSignal
import io.hsar.mapgenerator.commandgui.MapViewerLoadFailure
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.concurrent.CancellationException

internal class ComposeAsyncWorkController<T>(
    private val scope: CoroutineScope,
) {
    var work by mutableStateOf(MapViewerAsyncWork<T>())
        private set

    private var job: Job? = null
    private var cancellation: MapViewerCancellationSignal? = null

    val running: Boolean
        get() = work.state is MapViewerAsyncState.Running

    val failure: MapViewerLoadFailure?
        get() = (work.state as? MapViewerAsyncState.Failed)?.failure

    fun start(
        operation: MapViewerAsyncOperation,
        block: suspend (MapViewerCancellationSignal) -> T,
        onSuccess: (T) -> Unit = {},
    ) {
        cancel()
        work = work.start(operation)
        val requestId = requireNotNull(work.runningRequestId)
        val signal = MapViewerCancellationSignal()
        cancellation = signal
        job = scope.launch {
            try {
                val value = block(signal)
                val completed = work.succeed(requestId, value)
                if (completed !== work) {
                    work = completed
                    onSuccess(value)
                }
            } catch (_: CancellationException) {
                work = work.cancel(requestId)
            } catch (failure: Exception) {
                work = work.fail(requestId, MapViewerLoadFailure.from(failure))
            }
        }
    }

    fun cancel() {
        val requestId = work.runningRequestId
        cancellation?.cancel()
        job?.cancel()
        if (requestId != null) work = work.cancel(requestId)
        cancellation = null
        job = null
    }
}

@Composable
internal fun <T> rememberComposeAsyncWorkController(scope: CoroutineScope): ComposeAsyncWorkController<T> {
    val controller = remember(scope) { ComposeAsyncWorkController<T>(scope) }
    DisposableEffect(controller) { onDispose(controller::cancel) }
    return controller
}
