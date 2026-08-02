package io.hsar.mapgenerator.commandgui.operation

import io.hsar.mapgenerator.commandgui.MapCoordinate
import io.hsar.mapgenerator.commandgui.MapSelectionFrame
import io.hsar.mapgenerator.commandgui.MapSelectionScreenBounds
import io.hsar.mapgenerator.commandgui.ViewerPoint
import java.util.Collections.emptyList
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

interface OperationSelectionProjection {
    val aspectRatio: Double

    fun toScreen(point: MapCoordinate): ViewerPoint
    fun toMap(point: ViewerPoint): MapCoordinate
    fun unitScreenBounds(unit: OperationUnit): MapSelectionScreenBounds
}

fun OperationObject.supportsResizeHandles() = this is OperationGraphic || this is OperationDrawingObject

fun OperationContent.selectionFrame(
    ids: Set<OperationObjectId>,
    projection: OperationSelectionProjection,
    screenPadding: Double = 0.0,
): MapSelectionFrame? = objects.filter { it.id in ids }.let { selected ->
    when (selected.size) {
        0 -> null
        1 -> selected.single().selectionFrame(projection, screenPadding)
        else -> selected.map { it.selectionScreenBounds(projection) }.combined().frame()
    }
}

fun OperationObject.selectionScreenBounds(projection: OperationSelectionProjection): MapSelectionScreenBounds = when (this) {
    is OperationUnit -> projection.unitScreenBounds(this)
    else -> selectionBounds(projection.aspectRatio).let { bounds ->
        listOf(
            projection.toScreen(MapCoordinate(bounds.left, bounds.top)),
            projection.toScreen(MapCoordinate(bounds.right, bounds.bottom)),
        ).let { points -> MapSelectionScreenBounds(points.minOf { it.x }, points.minOf { it.y }, points.maxOf { it.x }, points.maxOf { it.y }) }
    }
}

fun OperationContent.objectAtScreen(
    point: ViewerPoint,
    projection: OperationSelectionProjection,
    drawingTolerance: Double,
) = objects.asReversed().firstOrNull { value ->
    if (value is OperationDrawingObject) value.contains(projection.toMap(point), drawingTolerance, projection.aspectRatio)
    else value.selectionScreenBounds(projection).contains(point)
}

fun OperationContent.objectsOverlappingSelectionBox(
    box: OperationSelectionBounds,
    aspectRatio: Double,
) = objects.filter { it.overlapsSelectionBox(box, aspectRatio) }

fun OperationObject.overlapsSelectionBox(box: OperationSelectionBounds, aspectRatio: Double): Boolean = when (this) {
    is OperationUnit, is OperationGraphic -> selectionBounds(aspectRatio).overlaps(box)
    is OperationDrawingObject -> drawing.overlapsSelectionBox(box, aspectRatio)
}

private fun OperationObject.selectionFrame(projection: OperationSelectionProjection, padding: Double): MapSelectionFrame = when (this) {
    is OperationUnit -> projection.unitScreenBounds(this).expanded(padding).frame()
    is OperationGraphic -> selectionScreenBounds(projection).expanded(padding).frame()
    is OperationDrawingObject -> drawing.selectionFrame(projection, padding)
}

private fun OperationDrawing.selectionFrame(projection: OperationSelectionProjection, padding: Double): MapSelectionFrame = when (this) {
    is OperationFreeformDrawing -> selectionBounds(projection.aspectRatio).toFrame(projection)
    is OperationLineDrawing -> segmentFrame(projection.toScreen(start.coordinate()), projection.toScreen(end.coordinate()), padding.coerceAtLeast(4.0))
    is OperationRectangleDrawing -> rectangleCorners().map { it.rotateAround(centre(), rotationDegrees, projection.aspectRatio) }
        .map { projection.toScreen(it.coordinate()) }.toFrame()

    is OperationOvalDrawing -> listOf(
        MapCoordinate(centre.x - xRadius, centre.y - yRadius),
        MapCoordinate(centre.x + xRadius, centre.y - yRadius),
        MapCoordinate(centre.x + xRadius, centre.y + yRadius),
        MapCoordinate(centre.x - xRadius, centre.y + yRadius),
    ).map { it.rotateAround(centre.coordinate(), rotationDegrees, projection.aspectRatio) }.map(projection::toScreen).toFrame()
}

private fun OperationDrawing.overlapsSelectionBox(box: OperationSelectionBounds, aspectRatio: Double) = when (this) {
    is OperationRectangleDrawing -> rectangleCorners().map { it.rotateAround(centre(), rotationDegrees, aspectRatio) }.closedSegments()
        .any { (a, b) -> segmentBounds(a, b).overlaps(box) }

    is OperationOvalDrawing -> ovalPoints(aspectRatio).closedSegments().any { (a, b) -> segmentBounds(a, b).overlaps(box) }
    else -> selectionBounds(aspectRatio).overlaps(box)
}

private fun OperationDrawing.selectionBounds(aspectRatio: Double): OperationSelectionBounds = when (this) {
    is OperationFreeformDrawing -> points.bounds()
    is OperationLineDrawing -> listOf(start, end).bounds()
    is OperationRectangleDrawing -> rectangleCorners().map { it.rotateAround(centre(), rotationDegrees, aspectRatio) }.bounds()
    is OperationOvalDrawing -> ovalPoints(aspectRatio).bounds()
}

private fun OperationOvalDrawing.ovalPoints(aspectRatio: Double) = (0 until 48).map { index ->
    val angle = Math.PI * 2.0 * index / 48.0
    OperationDrawingPoint(centre.x + xRadius * cos(angle), centre.y + yRadius * sin(angle))
        .rotateAround(centre.coordinate(), rotationDegrees, aspectRatio)
}

private fun OperationRectangleDrawing.rectangleCorners() = listOf(
    OperationDrawingPoint(start.x, start.y), OperationDrawingPoint(end.x, start.y),
    OperationDrawingPoint(end.x, end.y), OperationDrawingPoint(start.x, end.y),
)

private fun OperationRectangleDrawing.centre() = MapCoordinate((start.x + end.x) * .5, (start.y + end.y) * .5)
private fun OperationDrawingPoint.rotateAround(pivot: MapCoordinate, degrees: Double, aspectRatio: Double): OperationDrawingPoint {
    val radians = Math.toRadians(degrees)
    val dx = (x - pivot.x) * aspectRatio
    val dy = y - pivot.y
    return OperationDrawingPoint(pivot.x + (dx * cos(radians) - dy * sin(radians)) / aspectRatio, pivot.y + dx * sin(radians) + dy * cos(radians))
}

private fun MapCoordinate.rotateAround(pivot: MapCoordinate, degrees: Double, aspectRatio: Double): MapCoordinate {
    val radians = Math.toRadians(degrees)
    val dx = (x - pivot.x) * aspectRatio
    val dy = y - pivot.y
    return MapCoordinate(pivot.x + (dx * cos(radians) - dy * sin(radians)) / aspectRatio, pivot.y + dx * sin(radians) + dy * cos(radians))
}

private fun OperationSelectionBounds.toFrame(projection: OperationSelectionProjection) = MapSelectionFrame(
    projection.toScreen(MapCoordinate(left, top)), projection.toScreen(MapCoordinate(right, top)),
    projection.toScreen(MapCoordinate(right, bottom)), projection.toScreen(MapCoordinate(left, bottom)),
)

private fun List<ViewerPoint>.toFrame() = MapSelectionFrame(this[0], this[1], this[2], this[3])
private fun segmentFrame(start: ViewerPoint, end: ViewerPoint, padding: Double): MapSelectionFrame {
    val length = hypot(end.x - start.x, end.y - start.y)
    val normal = if (length == 0.0) ViewerPoint(0.0, padding) else ViewerPoint(-(end.y - start.y) / length * padding, (end.x - start.x) / length * padding)
    return MapSelectionFrame(start + normal, end + normal, end - normal, start - normal)
}

private fun List<MapSelectionScreenBounds>.combined() = MapSelectionScreenBounds(minOf { it.left }, minOf { it.top }, maxOf { it.right }, maxOf { it.bottom })
private fun MapSelectionScreenBounds.expanded(amount: Double) = MapSelectionScreenBounds(left - amount, top - amount, right + amount, bottom + amount)
private fun MapSelectionScreenBounds.contains(point: ViewerPoint) = point.x in left..right && point.y in top..bottom
private fun OperationDrawingPoint.coordinate() = MapCoordinate(x, y)
private fun List<OperationDrawingPoint>.bounds() = takeIf { it.isNotEmpty() }?.let {
    OperationSelectionBounds(minOf { point -> point.x }, minOf { point -> point.y }, maxOf { point -> point.x }, maxOf { point -> point.y })
} ?: OperationSelectionBounds(0.0, 0.0, 0.0, 0.0)

private fun <T> List<T>.closedSegments() = takeIf { it.isNotEmpty() }?.let { (it + first()).zipWithNext() } ?: emptyList()
private fun segmentBounds(first: OperationDrawingPoint, second: OperationDrawingPoint) = OperationSelectionBounds(
    minOf(first.x, second.x), minOf(first.y, second.y), maxOf(first.x, second.x), maxOf(first.y, second.y),
)

private operator fun ViewerPoint.plus(other: ViewerPoint) = ViewerPoint(x + other.x, y + other.y)
private operator fun ViewerPoint.minus(other: ViewerPoint) = ViewerPoint(x - other.x, y - other.y)
