package io.hsar.mapgenerator.commandgui

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class MapViewerBackendTest {
    @Test
    fun `parses viewer names without case sensitivity`() {
        // Arrange / Act
        val backend = MapViewerBackend.fromCliName("Compose")

        // Assert
        assertThat(backend, equalTo(MapViewerBackend.COMPOSE))
    }

    @Test
    fun `rejects unknown viewer names with accepted values`() {
        // Arrange / Act
        val error = assertThrows<IllegalArgumentException> {
            MapViewerBackend.fromCliName("unknown")
        }

        // Assert
        assertThat(error.message?.contains("korge, compose"), equalTo(true))
    }
}
