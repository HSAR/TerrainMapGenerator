package io.hsar.mapgenerator.commandgui.compose

import androidx.compose.foundation.layout.Column
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test

class ViewerChromeUiTest {
    @Test
    @OptIn(ExperimentalTestApi::class)
    fun `all four viewer buttons dispatch their panel labels`() = runComposeUiTest {
        val clicked = mutableListOf<String>()
        setContent {
            Column {
                listOf("GAME", "NOTES", "GRAPHICS", "UNITS").forEach { label ->
                    ViewerChromeButton(label) { clicked += label }
                }
            }
        }

        listOf("GAME", "NOTES", "GRAPHICS", "UNITS").forEach { onNodeWithText(it).performClick() }

        assertThat(clicked, equalTo(listOf("GAME", "NOTES", "GRAPHICS", "UNITS")))
    }
}
