package io.hsar.mapgenerator.commandgui.save

import java.nio.file.Path
import java.util.zip.ZipFile

internal class OperationSaveReader {
    fun read(path: Path): ReadOperationSave =
        ZipFile(path.toFile()).use { zip ->
            val additionalDataEntry = zip.getEntry(ADDITIONAL_DATA_JSON)
            ReadOperationSave(
                game = zip.getInputStream(zip.getEntry(GAME_JSON)).use { input ->
                    OperationSaveJson.objectMapper.readValue(input, GameSaveDto::class.java)
                },
                ledger = zip.getInputStream(zip.getEntry(LEDGER_JSON)).use { input ->
                    OperationSaveJson.objectMapper.readValue(input, LedgerFileDto::class.java)
                },
                additionalDataJson = additionalDataEntry?.let { entry ->
                    zip.getInputStream(entry).use { it.readBytes() }
                },
            )
        }

    private companion object {
        const val GAME_JSON = "game.json"
        const val LEDGER_JSON = "ledger.json"
        const val ADDITIONAL_DATA_JSON = "additionalData.json"
    }
}
