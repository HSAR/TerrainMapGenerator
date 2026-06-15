package io.hsar.mapgenerator.graph

import com.raylabz.opensimplex.Range
import com.raylabz.opensimplex.RangedValue
import org.kynosarges.tektosyne.geometry.PointD

data class Point(val x: Double, val y: Double) {

    fun translate(deltaX: Double, deltaY: Double) = Point(x + deltaX, y + deltaY)
    fun scale(factorX: Double, factorY: Double = factorX) = Point(x * factorX, y * factorY)

    operator fun minus(other: Point) = Point(this.x - other.x, this.y - other.y)

    companion object {
        val ORIGIN = Point(0.0, 0.0)
    }
}

fun Collection<Point>.translate(deltaX: Double, deltaY: Double) = this.map { it.translate(deltaX, deltaY) }
fun Collection<Point>.scale(factorX: Double, factorY: Double = factorX) = this.map { it.scale(factorX, factorY) }

fun PointD.toPoint() = Point(x = this.x, y = this.y)

fun Point.toPointD() = PointD(x, y)

fun Pair<RangedValue, RangedValue>.toPoint(rangeX: Range, rangeY: Range) = this
    .let { (x, y) ->
        Point(x.getValue(rangeX), y.getValue(rangeY))
    }
