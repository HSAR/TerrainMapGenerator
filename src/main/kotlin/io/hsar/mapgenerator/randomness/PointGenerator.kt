package io.hsar.mapgenerator.randomness

import com.raylabz.opensimplex.Range
import com.raylabz.opensimplex.RangedValue
import io.hsar.mapgenerator.graph.toPoint

object PointGenerator {

    fun randomDoublePoint(rangeX: Range = Range(0.0, 1.0), rangeY: Range = Range(0.0, 1.0)): Pair<RangedValue, RangedValue> {
        val x = RandomGenerator.generateUniform(rangeX.minimumValue, rangeX.maximumValue)
        val y = RandomGenerator.generateUniform(rangeY.minimumValue, rangeY.maximumValue)

        return RangedValue(rangeX, x) to RangedValue(rangeY, y)
    }

    fun adjustPointRange(points: Collection<Pair<RangedValue, RangedValue>>, rangeX: Range, rangeY: Range) =
        points.map { it.toPoint(rangeX, rangeY) }

}