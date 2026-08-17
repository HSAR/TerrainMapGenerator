package io.hsar.mapgenerator.commandgui.save

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import java.io.IOException
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

class OperationArchiveCommitterTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `successful commit replaces an existing target`() {
        val target = tempDir.resolve("operation.oct.zip")
        Files.writeString(target, "previous")

        OperationArchiveCommitter().write(target) { Files.writeString(it, "replacement") }

        assertThat(Files.readString(target), equalTo("replacement"))
        assertThat(extraFiles(target), equalTo(emptyList()))
    }

    @Test
    fun `failed archive generation leaves the previous target untouched and cleans staging`() {
        val target = tempDir.resolve("operation.oct.zip")
        Files.writeString(target, "previous")

        assertThrows<IOException> {
            OperationArchiveCommitter().write(target) { staged ->
                Files.writeString(staged, "partial")
                throw IOException("generation failed")
            }
        }

        assertThat(Files.readString(target), equalTo("previous"))
        assertThat(extraFiles(target), equalTo(emptyList()))
    }

    @Test
    fun `unsupported atomic move uses the replacement fallback`() {
        val target = tempDir.resolve("operation.oct.zip")
        Files.writeString(target, "previous")
        val committer = OperationArchiveCommitter { source, destination, atomic ->
            if (atomic) throw AtomicMoveNotSupportedException(source.toString(), destination.toString(), "test")
            Files.move(source, destination, StandardCopyOption.REPLACE_EXISTING)
        }

        committer.write(target) { Files.writeString(it, "replacement") }

        assertThat(Files.readString(target), equalTo("replacement"))
        assertThat(extraFiles(target), equalTo(emptyList()))
    }

    @Test
    fun `fallback commit failure restores the previous target and cleans staging`() {
        val target = tempDir.resolve("operation.oct.zip")
        Files.writeString(target, "previous")
        var fallbackMoves = 0
        val committer = OperationArchiveCommitter { source, destination, atomic ->
            if (atomic) throw AtomicMoveNotSupportedException(source.toString(), destination.toString(), "test")
            fallbackMoves++
            if (fallbackMoves == 2) throw IOException("replacement failed")
            Files.move(source, destination, StandardCopyOption.REPLACE_EXISTING)
        }

        assertThrows<IOException> {
            committer.write(target) { Files.writeString(it, "replacement") }
        }

        assertThat(Files.readString(target), equalTo("previous"))
        assertThat(extraFiles(target), equalTo(emptyList()))
    }

    private fun extraFiles(target: Path) = Files.list(tempDir).use { paths ->
        paths.filter { it != target }.map { it.fileName.toString() }.toList()
    }
}
