package io.hsar.mapgenerator.commandgui.save

import java.nio.file.Path
import java.util.zip.ZipFile

internal class OperationSaveReader {
    fun read(path: Path): ReadOperationSave =
        ZipFile(path.toFile()).use { zip ->
            ReadOperationSave(
                game = zip.getInputStream(zip.getEntry(GAME_JSON)).use { input ->
                    OperationSaveJson.objectMapper.readValue(input, GameSaveDto::class.java)
                },
                ledger = zip.getInputStream(zip.getEntry(LEDGER_JSON)).use { input ->
                    OperationSaveJson.objectMapper.readValue(input, LedgerFileDto::class.java)
                },
            )
        }

    private companion object {
        const val GAME_JSON = "game.json"
        const val LEDGER_JSON = "ledger.json"
    }
}
