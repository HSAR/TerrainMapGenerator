package io.hsar.mapgenerator.commandline

import com.beust.jcommander.JCommander
import io.hsar.mapgenerator.commandgui.MapViewerFeatureFlag
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.hasItem
import org.hamcrest.Matchers.nullValue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import java.awt.image.BufferedImage
import java.io.File
import java.util.Collections.emptyList
import javax.imageio.ImageIO

class MainTest {

    @Test
    fun `available commands include the GUI launcher`() {
        val commandNames = availableCommands().map { it.name }

        assertThat(commandNames, hasItem("show-gui"))
    }

    @Test
    fun `commands default to twenty metres per pixel`() {
        assertThat(testCommand().configuredMetresPerPixel, equalTo(20.0))
    }

    @Test
    fun `commands accept kebab case metres per pixel option`() {
        val command = testCommand()

        JCommander.newBuilder()
            .addObject(command)
            .build()
            .parse("--metres-per-pixel", "12.5")

        assertThat(command.configuredMetresPerPixel, equalTo(12.5))
    }

    @Test
    fun `GUI launcher accepts the compose viewer option`() {
        val commander = JCommander.newBuilder()
            .addCommand(LaunchGuiCommand.name, LaunchGuiCommand)
            .build()

        assertDoesNotThrow {
            commander.parse("show-gui", "--viewer", "compose")
        }
    }

    @Test
    fun `startup log message is only emitted when feature flags are set`() {
        assertThat(emptyList<MapViewerFeatureFlag>().startupLogMessage(), nullValue())
        assertThat(
            listOf(MapViewerFeatureFlag.OLYMPIAD_DAWN_EXPERIMENTAL).startupLogMessage(),
            equalTo("Enabled experimental GUI feature flag(s): OLYMPIAD_DAWN_EXPERIMENTAL"),
        )
    }

    @Test
    fun `initial setup inputs load the explicit map path`() {
        val imageFile = createImageFile("explicit-map.png")

        val inputs = initialSetupInputs(imageFile.absolutePath, emptyList())

        assertThat(inputs.mapImagePath, equalTo(imageFile.absolutePath))
        assertThat(inputs.mapImage?.width, equalTo(8))
        assertThat(inputs.mapImage?.height, equalTo(6))
    }

    @Test
    fun `initial setup inputs fall back to the first legacy image path`() {
        val imageFile = createImageFile("legacy-map.png")

        val inputs = initialSetupInputs(null, listOf(imageFile.absolutePath))

        assertThat(inputs.mapImagePath, equalTo(imageFile.absolutePath))
        assertThat(inputs.mapImage?.width, equalTo(8))
        assertThat(inputs.mapImage?.height, equalTo(6))
    }

    private fun testCommand(): Command =
        object : Command("test") {
            override fun run() = Unit
        }

    private fun createImageFile(name: String): File =
        File.createTempFile(name.removeSuffix(".png"), ".png").also { file ->
            file.deleteOnExit()
            ImageIO.write(BufferedImage(8, 6, BufferedImage.TYPE_INT_ARGB), "png", file)
        }
}

private fun List<MapViewerFeatureFlag>.startupLogMessage(): String? =
    takeIf { it.isNotEmpty() }?.let { flags ->
        "Enabled experimental GUI feature flag(s): ${flags.joinToString(", ")}"
    }
