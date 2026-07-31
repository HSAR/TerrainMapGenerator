package io.hsar.mapgenerator.commandgui

import io.hsar.mapgenerator.commandgui.save.OperationToolStorage
import java.nio.file.Path

/** Access to viewer preferences that apply across operations, rather than to a saved map. */
class MapViewerGlobalSettings(
    rootDirectory: Path = OperationToolStorage.defaultRootDirectory(),
) {
    private val storage = OperationToolStorage(rootDirectory)

    fun unitIconSizePx(): Double =
        UnitIconDisplaySize.coerce(storage.readSettings().unitIconSizePx)

    fun updateUnitIconSizePx(sizePx: Double): Double =
        UnitIconDisplaySize.coerce(sizePx).also { size ->
            storage.writeSettings(storage.readSettings().copy(unitIconSizePx = size))
        }
}
