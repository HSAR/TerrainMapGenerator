package io.hsar.mapgenerator.commandgui

import io.hsar.mapgenerator.commandgui.save.OperationToolStorage
import io.hsar.mapgenerator.commandgui.operation.OperationDestroyedUnitDisplayMode
import io.hsar.mapgenerator.commandgui.operation.OperationFireArcDisplayMode
import java.nio.file.Path

data class MapViewerDisplaySettings(
    val destroyedUnitDisplayMode: OperationDestroyedUnitDisplayMode = OperationDestroyedUnitDisplayMode.Full,
    val unitIconSizePx: Double = UnitIconDisplaySize.DEFAULT_PX,
    val fireArcDisplayMode: OperationFireArcDisplayMode = OperationFireArcDisplayMode.AllUnits,
)

/** Access to viewer preferences that apply across operations, rather than to a saved map. */
class MapViewerGlobalSettings(
    rootDirectory: Path = OperationToolStorage.defaultRootDirectory(),
) {
    private val storage = OperationToolStorage(rootDirectory)

    fun displaySettings(): MapViewerDisplaySettings = storage.readSettings().let { settings ->
        MapViewerDisplaySettings(
            destroyedUnitDisplayMode = OperationDestroyedUnitDisplayMode.fromSettingsValue(settings.displayDestroyedUnits),
            unitIconSizePx = UnitIconDisplaySize.coerce(settings.unitIconSizePx),
            fireArcDisplayMode = OperationFireArcDisplayMode.fromSettingsValue(settings.fireArcDisplayMode),
        )
    }

    fun unitIconSizePx(): Double =
        UnitIconDisplaySize.coerce(storage.readSettings().unitIconSizePx)

    fun updateUnitIconSizePx(sizePx: Double): Double =
        UnitIconDisplaySize.coerce(sizePx).also { size ->
            storage.writeSettings(storage.readSettings().copy(unitIconSizePx = size))
        }

    fun updateDestroyedUnitDisplayMode(mode: OperationDestroyedUnitDisplayMode) = mode.also {
        storage.writeSettings(storage.readSettings().copy(displayDestroyedUnits = mode.label))
    }

    fun updateFireArcDisplayMode(mode: OperationFireArcDisplayMode) = mode.also {
        storage.writeSettings(storage.readSettings().copy(fireArcDisplayMode = mode.label))
    }
}
