package io.hsar.mapgenerator.commandgui.korge.ui

import korlibs.image.color.RGBA
import korlibs.korge.view.Container
import korlibs.korge.view.graphics
import korlibs.math.geom.Point
import kotlin.math.cos
import kotlin.math.sin

/** Compact, letter-free representation of the six standard firing sectors. */
internal object MapViewerFireArcSegments {
    private val order = listOf("F", "FS", "AS", "A", "AP", "FP")

    fun render(parent: Container, width: Double, height: Double, arcs: List<String>, color: RGBA) {
        val center = Point(width * 0.5, height * 0.5)
        val radius = (minOf(width, height) * 0.38).coerceAtLeast(3.0)
        val vertices = order.indices.map { index ->
            val angle = Math.toRadians(-120.0 + index * 60.0)
            Point(center.x + cos(angle) * radius, center.y + sin(angle) * radius)
        }
        val selected = arcs.mapNotNull(order::indexOf).toSet()
        parent.graphics {
            selected.forEach { index ->
                val next = vertices[(index + 1) % vertices.size]
                fill(RGBA(color.r, color.g, color.b, (color.a * 0.55).toInt())) {
                    moveTo(center.x, center.y)
                    lineTo(vertices[index].x, vertices[index].y)
                    lineTo(next.x, next.y)
                    close()
                }
            }
            lineWidth = 1.5
            stroke(color) {
                vertices.forEachIndexed { index, vertex ->
                    val next = vertices[(index + 1) % vertices.size]
                    if (index == 0) moveTo(vertex.x, vertex.y) else lineTo(vertex.x, vertex.y)
                    lineTo(next.x, next.y)
                }
                vertices.forEach { vertex ->
                    moveTo(center.x, center.y)
                    lineTo(vertex.x, vertex.y)
                }
            }
        }
    }
}
