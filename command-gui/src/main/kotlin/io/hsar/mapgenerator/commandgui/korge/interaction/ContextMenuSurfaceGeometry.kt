package io.hsar.mapgenerator.commandgui.korge.interaction

import korlibs.math.geom.vector.VectorPath
import korlibs.math.geom.vector.Winding
import java.awt.BasicStroke
import java.awt.Shape
import java.awt.geom.Area
import java.awt.geom.Path2D
import java.awt.geom.PathIterator

internal data class ContextMenuSurfaceGeometry(
    val fillArea: Area,
    val borderArea: Area,
)

internal fun contextMenuSurfaceGeometry(
    panels: Collection<ScreenBounds>,
    borderWidth: Double = BORDER_WIDTH,
): ContextMenuSurfaceGeometry? {
    val validPanels = panels.filter { it.width > 0.0 && it.height > 0.0 }
    if (validPanels.isEmpty()) {
        return null
    }

    val fillArea = validPanels
        .map { it.toArea() }
        .fold(Area()) { accumulated, panel ->
            accumulated.apply { add(panel) }
        }

    if (fillArea.isEmpty) {
        return null
    }

    val borderArea = Area(
        BasicStroke(
            borderWidth.toFloat(),
            BasicStroke.CAP_BUTT,
            BasicStroke.JOIN_MITER,
        ).createStrokedShape(fillArea.toPath())
    )

    return ContextMenuSurfaceGeometry(
        fillArea = fillArea,
        borderArea = borderArea,
    )
}

internal fun Area.toVectorPath(): VectorPath =
    VectorPath(winding = Winding.EVEN_ODD).also { path ->
        val iterator = getPathIterator(null)
        val coords = DoubleArray(6)
        while (!iterator.isDone) {
            when (iterator.currentSegment(coords)) {
                PathIterator.SEG_MOVETO -> path.moveTo(coords[0], coords[1])
                PathIterator.SEG_LINETO -> path.lineTo(coords[0], coords[1])
                PathIterator.SEG_QUADTO -> path.quadTo(coords[0], coords[1], coords[2], coords[3])
                PathIterator.SEG_CUBICTO -> path.cubicTo(
                    coords[0],
                    coords[1],
                    coords[2],
                    coords[3],
                    coords[4],
                    coords[5],
                )

                PathIterator.SEG_CLOSE -> path.close()
            }
            iterator.next()
        }
    }

private fun ScreenBounds.toArea(): Area =
    Area(
        Path2D.Double(Path2D.WIND_NON_ZERO).apply {
            moveTo(left, top)
            lineTo(right, top)
            lineTo(right, bottom)
            lineTo(left, bottom)
            closePath()
        },
    )

private fun Area.toPath(): Shape =
    Path2D.Double(Path2D.WIND_NON_ZERO).also { path ->
        val iterator = getPathIterator(null)
        val coords = DoubleArray(6)
        while (!iterator.isDone) {
            when (iterator.currentSegment(coords)) {
                PathIterator.SEG_MOVETO -> path.moveTo(coords[0], coords[1])
                PathIterator.SEG_LINETO -> path.lineTo(coords[0], coords[1])
                PathIterator.SEG_QUADTO -> path.quadTo(coords[0], coords[1], coords[2], coords[3])
                PathIterator.SEG_CUBICTO -> path.curveTo(
                    coords[0],
                    coords[1],
                    coords[2],
                    coords[3],
                    coords[4],
                    coords[5],
                )

                PathIterator.SEG_CLOSE -> path.closePath()
            }
            iterator.next()
        }
    }

private const val BORDER_WIDTH = 2.0
