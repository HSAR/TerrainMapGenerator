package io.hsar.mapgenerator.commandgui.save

import java.nio.file.Files
import java.nio.file.Path

internal data class OperationToolSettingsDto(
    val formatVersion: Int = SETTINGS_FORMAT_VERSION,
    val displayDestroyedUnits: String? = null,
)

internal class OperationToolStorage(
    val rootDirectory: Path = defaultRootDirectory(),
) {
    val savesDirectory: Path
        get() = rootDirectory.resolve(SAVES_DIRECTORY)

    val settingsPath: Path
        get() = rootDirectory.resolve(SETTINGS_JSON)

    fun ensureExists() {
        Files.createDirectories(savesDirectory)
        if (!Files.exists(settingsPath)) {
            writeSettings(OperationToolSettingsDto())
        }
    }

    fun readSettings(): OperationToolSettingsDto {
        ensureExists()
        return OperationSaveJson.objectMapper.readValue(settingsPath.toFile(), OperationToolSettingsDto::class.java)
    }

    fun writeSettings(settings: OperationToolSettingsDto) {
        Files.createDirectories(rootDirectory)
        Files.write(
            settingsPath,
            OperationSaveJson.objectMapper.writeValueAsBytes(settings.copy(formatVersion = SETTINGS_FORMAT_VERSION)),
        )
    }

    companion object {
        fun defaultRootDirectory(): Path =
            Path.of(System.getProperty("user.home"), "Documents", "My Games", "OperationalCommandTool")
    }
}

private const val SETTINGS_FORMAT_VERSION = 1
private const val SAVES_DIRECTORY = "saves"
private const val SETTINGS_JSON = "settings.json"
