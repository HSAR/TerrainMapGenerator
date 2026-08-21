package io.hsar.mapgenerator.commandgui.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import io.hsar.mapgenerator.commandgui.MapViewerLoadException
import io.hsar.mapgenerator.commandgui.MapViewerLoadFailure
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import java.util.Properties
import java.util.concurrent.CancellationException
import java.util.concurrent.atomic.AtomicLong

internal enum class ComposeModalPriority { USER, LOSS_PREVENTION }

internal data class ComposeModalRequest(
    val key: String,
    val priority: ComposeModalPriority = ComposeModalPriority.USER,
    val onCancelled: () -> Unit = {},
    val content: @Composable (dismiss: () -> Unit) -> Unit,
)

internal class ComposeModalHostState {
    private val pending = mutableListOf<ComposeModalRequest>()
    var active by mutableStateOf<ComposeModalRequest?>(null)
        private set

    val isShowing get() = active != null
    internal val pendingKeys get() = pending.map(ComposeModalRequest::key)

    fun show(request: ComposeModalRequest) {
        if (active?.key == request.key) return
        if (pending.any { it.key == request.key }) return
        val current = active
        if (current == null) {
            active = request
        } else if (request.priority > current.priority) {
            pending.add(0, current)
            active = request
        } else {
            pending += request
        }
    }

    fun dismiss(cancelled: Boolean = false) {
        val dismissed = active
        active = pending.removeFirstOrNull()
        if (cancelled) dismissed?.onCancelled?.invoke()
    }

    fun remove(key: String) {
        if (active?.key == key) dismiss() else pending.removeAll { it.key == key }
    }
}

internal enum class ComposeMessageSeverity { INFO, SUCCESS, WARNING, ERROR }

internal data class ComposeTransientMessage(
    val id: Long,
    val text: String,
    val severity: ComposeMessageSeverity,
    val createdAtMillis: Long,
    val expiresAtMillis: Long,
    val fadingSinceMillis: Long? = null,
)

internal class ComposeTransientMessageHostState(
    private val durationMillis: Long = 10_000L,
    private val fadeMillis: Long = 420L,
    private val maximumVisible: Int = 5,
) {
    private val ids = AtomicLong()
    var messages by mutableStateOf(emptyList<ComposeTransientMessage>())
        private set

    fun show(text: String, severity: ComposeMessageSeverity = ComposeMessageSeverity.INFO, nowMillis: Long = System.currentTimeMillis()) {
        val active = advance(nowMillis).filter { it.fadingSinceMillis == null }
        val next = active + ComposeTransientMessage(ids.getAndIncrement(), text, severity, nowMillis, nowMillis + durationMillis)
        val overflow = (next.size - maximumVisible).coerceAtLeast(0)
        messages = next.mapIndexed { index, message ->
            if (index < overflow) message.copy(fadingSinceMillis = nowMillis) else message
        }
    }

    fun dismiss(id: Long, nowMillis: Long = System.currentTimeMillis()) {
        messages = messages.map { if (it.id == id && it.fadingSinceMillis == null) it.copy(fadingSinceMillis = nowMillis) else it }
    }

    fun advance(nowMillis: Long = System.currentTimeMillis()): List<ComposeTransientMessage> {
        messages = messages.map { message ->
            if (message.fadingSinceMillis == null && nowMillis >= message.expiresAtMillis) message.copy(fadingSinceMillis = nowMillis) else message
        }.filterNot { message -> message.fadingSinceMillis?.let { nowMillis - it >= fadeMillis } == true }
        return messages
    }

    fun alpha(message: ComposeTransientMessage, nowMillis: Long) = message.fadingSinceMillis?.let { started ->
        (1f - (nowMillis - started).toFloat() / fadeMillis).coerceIn(0f, 1f)
    } ?: 1f
}

internal sealed interface ComposeFileSelection<out T> {
    data object Cancelled : ComposeFileSelection<Nothing>
    data class Selected<T>(val file: File, val value: T) : ComposeFileSelection<T>
    data class Unreadable(val file: File, val message: String) : ComposeFileSelection<Nothing>
    data class Unsupported(val file: File, val message: String) : ComposeFileSelection<Nothing>
}

internal class ComposeFilePicker(
    private val owner: Frame?,
) {
    var open by mutableStateOf(false)
        private set

    suspend fun <T : Any> chooseAndDecode(
        title: String,
        pattern: String,
        unsupportedMessage: String,
        decode: (File) -> T?,
    ): ComposeFileSelection<T> {
        open = true
        val file = try {
            FileDialog(owner, title, FileDialog.LOAD).run {
                this.file = pattern
                isVisible = true
                this.file?.let { selected -> File(directory, selected) }
            }
        } finally {
            open = false
        } ?: return ComposeFileSelection.Cancelled
        return decodeSelectedFile(file, unsupportedMessage, decode)
    }
}

internal suspend fun <T : Any> decodeSelectedFile(
    file: File,
    unsupportedMessage: String,
    decode: (File) -> T?,
): ComposeFileSelection<T> = try {
    runInterruptible(Dispatchers.IO) { decode(file) }
        ?.let { ComposeFileSelection.Selected(file, it) }
        ?: ComposeFileSelection.Unsupported(file, unsupportedMessage)
} catch (failure: CancellationException) {
    throw failure
} catch (failure: Exception) {
    ComposeFileSelection.Unreadable(file, failure.message ?: failure::class.simpleName.orEmpty())
}

internal fun <T> ComposeFileSelection<T>.valueOrThrow(): ComposeFileSelection.Selected<T> = when (this) {
    ComposeFileSelection.Cancelled -> throw CancellationException("File selection was cancelled")
    is ComposeFileSelection.Selected -> this
    is ComposeFileSelection.Unreadable -> throw MapViewerLoadException(MapViewerLoadFailure.Unreadable(message))
    is ComposeFileSelection.Unsupported -> throw MapViewerLoadException(MapViewerLoadFailure.Unsupported(message))
}

internal val LocalComposeFilePicker = staticCompositionLocalOf<ComposeFilePicker> {
    error("Compose file picker is unavailable")
}

internal data class ComposeBuildInfo(
    val version: String,
    val backend: String = "Compose Desktop",
    val java: String = System.getProperty("java.runtime.version", System.getProperty("java.version", "unknown")),
    val operatingSystem: String = listOf(
        System.getProperty("os.name", "unknown"),
        System.getProperty("os.version", "unknown"),
        System.getProperty("os.arch", "unknown"),
    ).joinToString(" "),
) {
    fun diagnostics(fontStatus: String) = listOf(
        "Operational Command Tool $version",
        "Backend: $backend",
        "Java: $java",
        "OS: $operatingSystem",
        "Font: $fontStatus",
    ).joinToString("\n")

    companion object {
        fun load(classLoader: ClassLoader = ComposeBuildInfo::class.java.classLoader): ComposeBuildInfo {
            val properties = Properties()
            classLoader.getResourceAsStream("command-gui-compose-version.properties")?.use(properties::load)
            return ComposeBuildInfo(properties.getProperty("version")?.takeIf(String::isNotBlank) ?: "development")
        }
    }
}

internal data class ComposePresentationResources(
    val projectLicense: String,
    val thirdPartyNotices: String,
) {
    companion object {
        fun load(classLoader: ClassLoader = ComposePresentationResources::class.java.classLoader) = ComposePresentationResources(
            projectLicense = classLoader.requiredText("licenses/PROJECT_LICENSE.txt"),
            thirdPartyNotices = classLoader.requiredText("licenses/THIRD_PARTY_NOTICES.txt"),
        )
    }
}

private fun ClassLoader.requiredText(path: String) = requireNotNull(getResourceAsStream(path)) {
    "Required presentation resource $path is missing."
}.bufferedReader().use { it.readText() }
