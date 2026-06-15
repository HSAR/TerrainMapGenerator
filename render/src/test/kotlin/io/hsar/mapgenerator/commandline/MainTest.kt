package io.hsar.mapgenerator.commandline

import com.beust.jcommander.JCommander
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.hasItem
import org.junit.jupiter.api.Test

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

    private fun testCommand(): Command =
        object : Command("test") {
            override fun run() = Unit
        }
}
