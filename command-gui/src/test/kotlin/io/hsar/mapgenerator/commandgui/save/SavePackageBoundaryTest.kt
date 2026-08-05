package io.hsar.mapgenerator.commandgui.save

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path
import java.util.Collections.emptyList

class SavePackageBoundaryTest {
    @Test
    fun `neutral save package has no renderer imports`() {
        val moduleRelative = Path.of("src/main/kotlin/io/hsar/mapgenerator/commandgui/save")
        val sourceDirectory = moduleRelative.takeIf(Files::isDirectory) ?: Path.of("command-gui").resolve(moduleRelative)

        val violations = Files.walk(sourceDirectory).use { it.toList() }
            .filter { Files.isRegularFile(it) && it.fileName.toString().endsWith(".kt") }
            .flatMap { path ->
                val lines = Files.readAllLines(path)
                if (lines.firstOrNull() != "package io.hsar.mapgenerator.commandgui.save") emptyList()
                else lines.filter { it.startsWith("import io.hsar.mapgenerator.commandgui.korge") || it.startsWith("import korlibs") }
                    .map { "${path.fileName}: $it" }
            }

        assertThat(violations, equalTo(emptyList()))
    }
}
