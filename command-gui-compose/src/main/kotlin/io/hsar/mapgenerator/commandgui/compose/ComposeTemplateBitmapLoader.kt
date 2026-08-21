package io.hsar.mapgenerator.commandgui.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import io.hsar.mapgenerator.commandgui.UnitIconDisplaySize
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.util.UUID
import java.util.concurrent.CancellationException
import kotlin.math.roundToInt

internal enum class ComposeTemplateBitmapKind { UNIT, GRAPHIC }

internal data class ComposeTemplateBitmapKey(
    val kind: ComposeTemplateBitmapKind,
    val id: UUID,
    val source: BufferedImage,
)

internal data class ComposeTemplateBitmapRequest(val key: ComposeTemplateBitmapKey) {
    val image get() = key.source
}

/** Keeps only currently rendered template textures and cancels conversions that leave the viewport. */
internal class ComposeTemplateBitmapLoader(private val scope: CoroutineScope) {
    var ready by mutableStateOf<Map<ComposeTemplateBitmapKey, ImageBitmap>>(emptyMap())
        private set

    private val jobs = mutableMapOf<ComposeTemplateBitmapKey, Job>()
    private val permits = Semaphore(2)
    private var activeKeys = emptySet<ComposeTemplateBitmapKey>()
    private var disposed = false

    fun request(requests: List<ComposeTemplateBitmapRequest>) {
        if (disposed) return
        activeKeys = requests.mapTo(linkedSetOf(), ComposeTemplateBitmapRequest::key)
        jobs.keys.filter { it !in activeKeys }.forEach { key -> jobs.remove(key)?.cancel() }
        ready = ready.filterKeys { it in activeKeys }
        requests.filter { it.key !in ready && it.key !in jobs }.forEach { request ->
            jobs[request.key] = scope.launch {
                try {
                    permits.withPermit {
                        val bitmap = withContext(Dispatchers.Default) {
                            request.image.forMapTexture(request.key.kind).toImageBitmap()
                        }
                        if (!disposed && request.key in activeKeys) ready = ready + (request.key to bitmap)
                    }
                } catch (_: CancellationException) {
                    // Expected when visible objects or operations change.
                } finally {
                    val currentJob = currentCoroutineContext()[Job]
                    if (jobs[request.key] === currentJob) jobs.remove(request.key)
                }
            }
        }
    }

    fun dispose() {
        disposed = true
        jobs.values.forEach(Job::cancel)
        jobs.clear()
        activeKeys = emptySet()
        ready = emptyMap()
    }
}

internal fun ComposeTemplateBitmapRequest.keyedBitmap(bitmaps: Map<ComposeTemplateBitmapKey, ImageBitmap>) = bitmaps[key]

private fun BufferedImage.forMapTexture(kind: ComposeTemplateBitmapKind): BufferedImage {
    if (kind != ComposeTemplateBitmapKind.UNIT || width <= UnitIconDisplaySize.MAX_PX && height <= UnitIconDisplaySize.MAX_PX) return this
    val scale = UnitIconDisplaySize.MAX_PX / maxOf(width, height)
    val targetWidth = (width * scale).roundToInt().coerceAtLeast(1)
    val targetHeight = (height * scale).roundToInt().coerceAtLeast(1)
    return BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB).also { target ->
        target.createGraphics().use { graphics ->
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC)
            graphics.drawImage(this, 0, 0, targetWidth, targetHeight, null)
        }
    }
}

private inline fun <T : java.awt.Graphics> T.use(block: (T) -> Unit) {
    try {
        block(this)
    } finally {
        dispose()
    }
}
