package io.hsar.mapgenerator.commandline

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.hasItem
import org.junit.jupiter.api.Test

class MainTest {

    @Test
    fun `available commands include the GUI launcher`() {
        val commandNames = availableCommands().map { it.name }

        assertThat(commandNames, hasItem("show-gui"))
    }
}
