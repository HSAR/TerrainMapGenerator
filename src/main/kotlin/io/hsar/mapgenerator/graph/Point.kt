package io.hsar.mapgenerator.graph

import com.raylabz.opensimplex.Range
import com.raylabz.opensimplex.RangedValue
import org.kynosarges.tektosyne.geometry.PointD
import uk.yetanother.conrec.domain.Coordinate

data class Point(val x: Double, val y: Double) {

    fun translate(deltaX: Double, deltaY: Double) = Point(x + deltaX, y + deltaY)

    companion object {
        val ORIGIN = Point(0.0, 0.0)
    }
}

fun Collection<Point>.translate(deltaX: Double, deltaY: Double) = this.map { it.translate(deltaX, deltaY) }

fun PointD.toPoint() = Point(x = this.x, y = this.y)

fun Coordinate.toPoint() = Point(x = this.x, y = this.y)

fun Point.toPointD() = PointD(x, y)

fun Pair<RangedValue, RangedValue>.toPoint(rangeX: Range, rangeY: Range) = this
    .let { (x, y) ->
        Point(x.getValue(rangeX), y.getValue(rangeY))
    }