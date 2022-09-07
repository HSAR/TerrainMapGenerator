package io.hsar.mapgenerator.randomness

import org.kynosarges.tektosyne.geometry.PointD
import java.util.concurrent.ThreadLocalRandom

object PointGenerator {

    fun randomDoublePoint(maxX: Double = 1.0, maxY: Double = 1.0) = PointGenerator.randomDoublePoint(0.0, maxX, 0.0, maxY)

    fun randomDoublePoint(minX: Double = 0.0, maxX: Double = 1.0, minY: Double = 0.0, maxY: Double = 1.0): PointD {
        val x = ThreadLocalRandom.current().nextDouble(minX, maxX)
        val y = ThreadLocalRandom.current().nextDouble(minY, maxY)

        return PointD(x, y)
    }

}