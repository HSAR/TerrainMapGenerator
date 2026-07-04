package io.hsar.mapgenerator.commandgui.korge.interaction

import io.hsar.mapgenerator.commandgui.korge.overlay.GridMeasurement
import io.hsar.mapgenerator.grid.GridLayer
import kotlin.math.atan2
import kotlin.math.roundToInt
import kotlin.math.sqrt

internal object GridMeasurementCalculator {
    fun calculate(
        layer: GridLayer,
        originMapX: Double,
        originMapY: Double,
        destinationMapX: Double,
        destinationMapY: Double,
    ) = GridMeasurement(
        originMapX = originMapX,
        originMapY = originMapY,
        destinationMapX = destinationMapX,
        destinationMapY = destinationMapY,
        distanceUnits = distanceUnits(
            layer = layer,
            originMapX = originMapX,
            originMapY = originMapY,
            destinationMapX = destinationMapX,
            destinationMapY = destinationMapY,
        ),
        unitLabel = layer.referenceSystem.displayUnit,
        bearingDegrees = bearingDegrees(
            originMapX = originMapX,
            originMapY = originMapY,
            destinationMapX = destinationMapX,
            destinationMapY = destinationMapY,
        ),
    )

    private fun distanceUnits(
        layer: GridLayer,
        originMapX: Double,
        originMapY: Double,
        destinationMapX: Double,
        destinationMapY: Double,
    ) = (
            sqrt(
                ((destinationMapX - originMapX) * (destinationMapX - originMapX)) +
                        ((destinationMapY - originMapY) * (destinationMapY - originMapY))
            ) * layer.unitsPerPixel
            )

    private fun bearingDegrees(
        originMapX: Double,
        originMapY: Double,
        destinationMapX: Double,
        destinationMapY: Double,
    ) = (
            Math.toDegrees(
                atan2(
                    destinationMapX - originMapX,
                    -(destinationMapY - originMapY),
                )
            )
                .roundToInt()
                .mod(360)
            )
}
