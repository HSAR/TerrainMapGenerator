package io.hsar.mapgenerator.graph

import org.kynosarges.tektosyne.geometry.PointD
import uk.yetanother.conrec.domain.Coordinate

data class Point(val x: Double, val y: Double)

fun PointD.toPoint() = Point(x = this.x, y = this.y)

fun Coordinate.toPoint() = Point(x = this.x, y = this.y)

fun Point.toPointD() = PointD(x, y)