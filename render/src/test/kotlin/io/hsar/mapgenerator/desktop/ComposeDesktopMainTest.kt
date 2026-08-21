package io.hsar.mapgenerator.desktop

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import java.util.Properties

class ComposeDesktopMainTest {
    @Test
    fun `renderer cache receives a default without replacing explicit configuration`() {
        // Arrange
        val defaults = Properties()
        val explicit = Properties().apply { setProperty("skiko.gpu.resourceCacheLimit", "192M") }

        // Act
        configureComposeDesktopRendering(defaults)
        configureComposeDesktopRendering(explicit)

        // Assert
        assertThat(defaults.getProperty("skiko.gpu.resourceCacheLimit"), equalTo("512M"))
        assertThat(explicit.getProperty("skiko.gpu.resourceCacheLimit"), equalTo("192M"))
    }
}
