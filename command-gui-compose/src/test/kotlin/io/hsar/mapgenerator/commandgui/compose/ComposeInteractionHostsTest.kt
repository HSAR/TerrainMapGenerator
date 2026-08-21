package io.hsar.mapgenerator.commandgui.compose

import kotlinx.coroutines.runBlocking
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.containsString
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.instanceOf
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.CancellationException
import javax.imageio.ImageIO

class ComposeInteractionHostsTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `loss prevention modal pre-empts user requests and distinct requests retain FIFO order`() {
        val state = ComposeModalHostState()
        state.show(request("first"))
        state.show(request("second"))
        state.show(request("second"))

        state.show(request("save", ComposeModalPriority.LOSS_PREVENTION))

        assertThat(state.active?.key, equalTo("save"))
        assertThat(state.pendingKeys, equalTo(listOf("first", "second")))
        state.dismiss()
        assertThat(state.active?.key, equalTo("first"))
        state.dismiss()
        assertThat(state.active?.key, equalTo("second"))
    }

    @Test
    fun `cancel invokes only the active modal cancel action`() {
        var cancelled = 0
        val state = ComposeModalHostState()
        state.show(ComposeModalRequest("modal", onCancelled = { cancelled++ }) {})

        state.dismiss(cancelled = true)

        assertThat(cancelled, equalTo(1))
        assertThat(state.active, equalTo(null))
    }

    @Test
    fun `transient messages retain five active notices and fade oldest first`() {
        val state = ComposeTransientMessageHostState(durationMillis = 10_000, fadeMillis = 420, maximumVisible = 5)
        repeat(6) { state.show("Notice ${it + 1}", nowMillis = it.toLong()) }

        assertThat(state.messages.filter { it.fadingSinceMillis == null }.map { it.text }, equalTo(listOf("Notice 2", "Notice 3", "Notice 4", "Notice 5", "Notice 6")))
        assertThat(state.messages.single { it.text == "Notice 1" }.fadingSinceMillis, equalTo(5L))
        state.advance(426)
        assertThat(state.messages.map { it.text }, equalTo(listOf("Notice 2", "Notice 3", "Notice 4", "Notice 5", "Notice 6")))
    }

    @Test
    fun `selected file decoding distinguishes success unsupported and unreadable`() = runBlocking {
        val file = tempDir.resolve("input.dat").also { Files.writeString(it, "value") }.toFile()

        val selected = decodeSelectedFile(file, "unsupported") { it.readText() }
        val unsupported = decodeSelectedFile(file, "unsupported") { null as String? }
        val unreadable = decodeSelectedFile(file, "unsupported") { error("cannot read") }

        assertThat(selected, instanceOf(ComposeFileSelection.Selected::class.java))
        assertThat(unsupported, equalTo(ComposeFileSelection.Unsupported(file, "unsupported")))
        assertThat(unreadable, equalTo(ComposeFileSelection.Unreadable(file, "cannot read")))
    }

    @Test
    fun `selected file decoding preserves cancellation`() = runBlocking {
        val file = tempDir.resolve("cancel.dat").also { Files.writeString(it, "value") }.toFile()

        org.junit.jupiter.api.assertThrows<CancellationException> {
            runBlocking { decodeSelectedFile(file, "unsupported") { throw CancellationException("cancelled") } }
        }
    }

    @Test
    fun `build metadata and required presentation resources are packaged`() {
        val build = ComposeBuildInfo.load()
        val resources = ComposePresentationResources.load()

        assertThat(build.version, equalTo("0.0.1-SNAPSHOT"))
        assertThat(resources.projectLicense, containsString("MIT License"))
        assertThat(resources.thirdPartyNotices, containsString("TV Nord EF"))
    }

    @Test
    fun `diagnostics expose renderer and GPU cache configuration`() {
        // Arrange
        val build = ComposeBuildInfo(version = "test", renderer = "SOFTWARE", gpuCacheLimit = "512M")

        // Act
        val diagnostics = build.diagnostics("available")

        // Assert
        assertThat(diagnostics, containsString("Renderer: SOFTWARE"))
        assertThat(diagnostics, containsString("GPU cache: 512M"))
    }

    @Test
    fun `all application icon resolutions are packaged square and non-empty`() {
        listOf(64, 128, 256, 512).forEach { size ->
            val image = requireNotNull(javaClass.classLoader.getResourceAsStream("logo${size.toString().padStart(3, '0')}.png")).use(ImageIO::read)
            assertThat(image.width, equalTo(size))
            assertThat(image.height, equalTo(size))
            assertThat((image.getRGB(size / 2, size / 2) ushr 24) > 0, equalTo(true))
        }
    }

    private fun request(key: String, priority: ComposeModalPriority = ComposeModalPriority.USER) =
        ComposeModalRequest(key, priority) {}
}
