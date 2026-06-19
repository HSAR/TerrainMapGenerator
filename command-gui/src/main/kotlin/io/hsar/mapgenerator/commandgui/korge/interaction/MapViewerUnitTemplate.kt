package io.hsar.mapgenerator.commandgui.korge.interaction

import io.hsar.mapgenerator.commandgui.AssetSource
import korlibs.image.bitmap.Bitmap32

internal data class MapViewerUnitTemplate @JvmOverloads constructor(
    val name: String,
    val bitmap: Bitmap32,
    val assetSource: AssetSource? = null,
)
