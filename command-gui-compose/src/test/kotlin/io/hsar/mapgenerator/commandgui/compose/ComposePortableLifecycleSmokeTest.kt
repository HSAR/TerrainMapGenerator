package io.hsar.mapgenerator.commandgui.compose

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

class ComposePortableLifecycleSmokeTest {
    @TempDir
    lateinit var temporaryDirectory: Path

    @Test
    fun `dirty Compose close retries and reopens through the archive boundary`() {
        ComposePortableLifecycleSmoke.run(temporaryDirectory)

        assertThat(
            Files.isRegularFile(temporaryDirectory.resolve("saved operations 測試").resolve("Opération smoke.zip")),
            equalTo(true),
        )
    }
}
