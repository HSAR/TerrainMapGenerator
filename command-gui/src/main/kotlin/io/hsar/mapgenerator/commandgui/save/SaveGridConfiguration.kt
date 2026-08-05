package io.hsar.mapgenerator.commandgui.save

import io.hsar.mapgenerator.grid.Coord
import io.hsar.mapgenerator.grid.GridLayerBuilder
import io.hsar.mapgenerator.grid.ReferenceSystem
import java.awt.image.BufferedImage
import java.util.*

internal data class SaveGridConfiguration(
    val mapId: String,
    val referenceSystem: ReferenceSystem,
    val startX: Coord,
    val startY: Coord,
)

internal fun defaultSaveGridConfiguration(
    mapImage: BufferedImage,
    unitsPerPixel: Double,
    seed: Long,
    referenceSystem: ReferenceSystem,
) = GridLayerBuilder.build(
    width = mapImage.width,
    height = mapImage.height,
    gridScale = referenceSystem.gridScale,
    unitsPerPixel = unitsPerPixel,
    referenceSystem = referenceSystem,
    random = Random(seed),
).let { SaveGridConfiguration(it.mapId, it.referenceSystem, it.verticalLineNumberStart, it.horizontalLineNumberStart) }
