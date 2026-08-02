package io.hsar.mapgenerator.commandgui.operation

import io.hsar.mapgenerator.commandgui.MapCoordinate
import io.hsar.mapgenerator.commandgui.MapCoordinateBounds
import io.hsar.mapgenerator.commandgui.MapCoordinateDelta
import io.hsar.mapgenerator.commandgui.MapSelectionCorner
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin

data class OperationSelectionBounds(
    val left: Double,
    val top: Double,
    val right: Double,
    val bottom: Double,
) {
    val width get() = right - left
    val height get() = bottom - top
    val centre get() = MapCoordinate((left + right) * 0.5, (top + bottom) * 0.5)

    fun contains(point: MapCoordinate, tolerance: Double = 0.0) =
        point.x in left - tolerance..right + tolerance && point.y in top - tolerance..bottom + tolerance

    fun overlaps(other: OperationSelectionBounds) =
        left <= other.right && right >= other.left && top <= other.bottom && bottom >= other.top

    fun expanded(amount: Double) = OperationSelectionBounds(left - amount, top - amount, right + amount, bottom + amount)

    fun corner(corner: MapSelectionCorner) = when (corner) {
        MapSelectionCorner.TOP_LEFT -> MapCoordinate(left, top)
        MapSelectionCorner.TOP_RIGHT -> MapCoordinate(right, top)
        MapSelectionCorner.BOTTOM_RIGHT -> MapCoordinate(right, bottom)
        MapSelectionCorner.BOTTOM_LEFT -> MapCoordinate(left, bottom)
    }

    companion object {
        fun fromPoints(first: MapCoordinate, second: MapCoordinate) = OperationSelectionBounds(
            minOf(first.x, second.x), minOf(first.y, second.y), maxOf(first.x, second.x), maxOf(first.y, second.y),
        )
    }
}

fun OperationContent.selectionBounds(ids: Set<OperationObjectId>, aspectRatio: Double): OperationSelectionBounds? =
    objects.filter { it.id in ids }.map { it.selectionBounds(aspectRatio) }.takeIf(List<*>::isNotEmpty)?.let { bounds ->
        OperationSelectionBounds(
            bounds.minOf(OperationSelectionBounds::left),
            bounds.minOf(OperationSelectionBounds::top),
            bounds.maxOf(OperationSelectionBounds::right),
            bounds.maxOf(OperationSelectionBounds::bottom),
        )
    }

fun OperationObject.selectionBounds(aspectRatio: Double): OperationSelectionBounds = when (this) {
    is OperationUnit -> OperationSelectionBounds(
        coordinate.x - normalizedWidth * 0.5,
        coordinate.y - normalizedHeight * 0.5,
        coordinate.x + normalizedWidth * 0.5,
        coordinate.y + normalizedHeight * 0.5,
    )

    is OperationGraphic -> OperationSelectionBounds(
        coordinate.x - normalizedWidth * 0.5,
        coordinate.y - normalizedHeight * 0.5,
        coordinate.x + normalizedWidth * 0.5,
        coordinate.y + normalizedHeight * 0.5,
    )

    is OperationDrawingObject -> drawing.selectionBounds(aspectRatio)
}

fun OperationContent.objectAt(
    point: MapCoordinate,
    tolerance: Double,
    aspectRatio: Double,
) = objects.asReversed().firstOrNull { it.contains(point, tolerance, aspectRatio) }

fun OperationObject.contains(point: MapCoordinate, tolerance: Double, aspectRatio: Double): Boolean = when (this) {
    is OperationUnit, is OperationGraphic -> selectionBounds(aspectRatio).contains(point, tolerance)
    is OperationDrawingObject -> drawing.contains(point, tolerance, aspectRatio)
}

internal fun OperationContent.move(
    ids: Set<OperationObjectId>,
    requestedDelta: MapCoordinateDelta,
    bounds: MapCoordinateBounds,
    aspectRatio: Double,
): OperationContent {
    val selectedBounds = selectionBounds(ids, aspectRatio) ?: return this
    val delta = MapCoordinateDelta(
        requestedDelta.x.coerceIn(bounds.left - selectedBounds.left, bounds.right - selectedBounds.right),
        requestedDelta.y.coerceIn(bounds.top - selectedBounds.top, bounds.bottom - selectedBounds.bottom),
    )
    return copy(objects = objects.map { value -> if (value.id in ids) value.translate(delta) else value })
}

internal fun OperationContent.moveUnbounded(
    ids: Set<OperationObjectId>,
    delta: MapCoordinateDelta,
    constrainUnit: (OperationUnit, Boolean) -> OperationUnit,
    bypassUnitConstraint: Boolean,
) = copy(objects = objects.map { value ->
    if (value.id !in ids) value else value.translate(delta).let { moved ->
        if (moved is OperationUnit) constrainUnit(moved, bypassUnitConstraint) else moved
    }
})

internal fun OperationContent.resize(
    ids: Set<OperationObjectId>,
    grabbedCorner: MapSelectionCorner,
    draggedCorner: MapCoordinate,
    aspectRatio: Double,
    fromCentre: Boolean = false,
    preserveAspect: Boolean = false,
    originalFixedCorner: MapCoordinate? = null,
): OperationContent {
    val selected = objects.filter { it.id in ids }
    val bounds = selectionBounds(ids, aspectRatio) ?: return this
    val singleDrawing = selected.singleOrNull() as? OperationDrawingObject
    val anchor = singleDrawing?.drawing?.resizeAnchor(grabbedCorner, fromCentre, aspectRatio)
        ?: if (selected.size == 1 && fromCentre || selected.size > 1 && !fromCentre) bounds.centre else bounds.corner(grabbedCorner.opposite())
    val original = bounds.corner(grabbedCorner)
    val effectiveDraggedCorner = singleDrawing?.drawing?.constrainedDraggedCorner(
        anchor, draggedCorner, originalFixedCorner, preserveAspect, aspectRatio,
    ) ?: draggedCorner
    val scaleX = scale(effectiveDraggedCorner.x, anchor.x, original.x)
    val scaleY = scale(effectiveDraggedCorner.y, anchor.y, original.y)
    val resizedSelected = selected.map {
        it.resize(
            bounds, grabbedCorner, anchor, effectiveDraggedCorner, originalFixedCorner,
            scaleX, scaleY, selected.size, aspectRatio,
        )
    }
    val graphicSizes = resizedSelected.filterIsInstance<OperationGraphic>()
        .associate { it.graphicTemplateId to (it.normalizedWidth to it.normalizedHeight) }
    val resizedById = resizedSelected.associateBy(OperationObject::id)
    return copy(
        objects = objects.map { originalObject ->
            (originalObject as? OperationGraphic)?.takeIf { it.graphicTemplateId in graphicSizes }?.let { graphic ->
                    graphicSizes.getValue(graphic.graphicTemplateId).let { size ->
                        graphic.copy(normalizedWidth = size.first, normalizedHeight = size.second)
                    }
                }
                ?: resizedById[originalObject.id]
                ?: originalObject
        },
        graphicTemplates = graphicTemplates.map { template ->
            graphicSizes[template.id]?.let { size ->
                template.copy(normalizedWidth = size.first, normalizedHeight = size.second)
            } ?: template
        },
    ).takeIf { selected.isNotEmpty() } ?: this
}

internal fun OperationContent.rotate(
    ids: Set<OperationObjectId>,
    deltaRadians: Double,
    aspectRatio: Double,
    snap: Boolean = false,
): OperationContent {
    val pivot = selectionBounds(ids, aspectRatio)?.centre ?: return this
    return copy(objects = objects.map { value -> if (value.id in ids) value.rotate(pivot, deltaRadians, aspectRatio, snap) else value })
}

fun OperationContent.delete(ids: Set<OperationObjectId>) = copy(objects = objects.filterNot { it.id in ids })

fun OperationContent.changesFrom(before: OperationContent): OperationChangeSet = OperationChangeSet(
    objects = changes(before.objects, objects, OperationObject::id),
    unitTemplates = changes(before.unitTemplates, unitTemplates, OperationUnitTemplate::id),
    graphicTemplates = changes(before.graphicTemplates, graphicTemplates, OperationGraphicTemplate::id),
    notes = changes(before.notes, notes, OperationNote::id),
    noteOrderBefore = before.notes.map(OperationNote::id).takeIf { it != notes.map(OperationNote::id) },
    noteOrderAfter = notes.map(OperationNote::id).takeIf { it != before.notes.map(OperationNote::id) },
)

fun rotationDelta(origin: MapCoordinate, start: MapCoordinate, current: MapCoordinate, aspectRatio: Double) =
    bearing(origin, current, aspectRatio) - bearing(origin, start, aspectRatio)

private fun bearing(origin: MapCoordinate, target: MapCoordinate, aspectRatio: Double) =
    atan2((target.x - origin.x) * aspectRatio, origin.y - target.y)

private fun OperationObject.translate(delta: MapCoordinateDelta): OperationObject = when (this) {
    is OperationUnit -> copy(coordinate = coordinate + delta)
    is OperationGraphic -> copy(coordinate = coordinate + delta)
    is OperationDrawingObject -> copy(drawing = drawing.translate(delta))
}

private fun OperationObject.resize(
    bounds: OperationSelectionBounds,
    grabbedCorner: MapSelectionCorner,
    anchor: MapCoordinate,
    draggedCorner: MapCoordinate,
    originalFixedCorner: MapCoordinate?,
    scaleX: Double,
    scaleY: Double,
    selectionSize: Int,
    aspectRatio: Double,
): OperationObject = when (this) {
    is OperationUnit -> this
    is OperationGraphic -> {
        val uniform = if (abs(scaleX - 1.0) >= abs(scaleY - 1.0)) scaleX else scaleY
        copy(
            coordinate = coordinate.scaleFrom(anchor, uniform, uniform),
            normalizedWidth = (normalizedWidth * abs(uniform)).coerceAtLeast(MIN_SIZE),
            normalizedHeight = (normalizedHeight * abs(uniform)).coerceAtLeast(MIN_SIZE),
        )
    }

    is OperationDrawingObject -> copy(
        drawing = drawing.resize(
            bounds, grabbedCorner, anchor, draggedCorner, originalFixedCorner,
            scaleX, scaleY, selectionSize, aspectRatio,
        ),
    )
}

private fun OperationObject.rotate(
    pivot: MapCoordinate,
    radians: Double,
    aspectRatio: Double,
    snap: Boolean,
): OperationObject = when (this) {
    is OperationUnit -> copy(
        coordinate = coordinate.rotateAround(pivot, radians, aspectRatio),
        facingDegrees = if (snap) snapDegrees((facingDegrees ?: 0) + Math.toDegrees(radians))
        else normalizeDegrees((facingDegrees ?: 0) + Math.toDegrees(radians).roundToInt()),
    )

    is OperationGraphic -> copy(coordinate = coordinate.rotateAround(pivot, radians, aspectRatio))
    is OperationDrawingObject -> copy(
        drawing = drawing.rotate(
            pivot,
            if (snap) Math.toRadians(snapDegrees(Math.toDegrees(radians)).toDouble()) else radians,
            aspectRatio,
            snap,
        ),
    )
}

private fun OperationDrawing.translate(delta: MapCoordinateDelta): OperationDrawing = when (this) {
    is OperationFreeformDrawing -> copy(points = points.map { it + delta })
    is OperationLineDrawing -> copy(start = start + delta, end = end + delta)
    is OperationRectangleDrawing -> copy(start = start + delta, end = end + delta)
    is OperationOvalDrawing -> copy(centre = centre + delta)
}

private fun OperationDrawing.resize(
    bounds: OperationSelectionBounds,
    grabbedCorner: MapSelectionCorner,
    anchor: MapCoordinate,
    draggedCorner: MapCoordinate,
    originalFixedCorner: MapCoordinate?,
    scaleX: Double,
    scaleY: Double,
    selectionSize: Int,
    aspectRatio: Double,
): OperationDrawing = when (this) {
    is OperationFreeformDrawing -> copy(points = points.map { it.scaleFrom(anchor, scaleX, scaleY) })
    is OperationLineDrawing -> resizeFromEndpoint(grabbedCorner, anchor, draggedCorner, selectionSize == 1 && anchor.isCloseTo(centre()))
    is OperationRectangleDrawing -> if (selectionSize == 1 && rotationDegrees != 0.0 && originalFixedCorner != null) {
        if (anchor.isCloseTo(centre())) resizeRotatedFromCentre(draggedCorner, aspectRatio)
        else resizeRotatedFromFixedCorner(originalFixedCorner, draggedCorner, aspectRatio)
    } else copy(start = start.scaleFrom(anchor, scaleX, scaleY), end = end.scaleFrom(anchor, scaleX, scaleY))

    is OperationOvalDrawing -> if (selectionSize == 1 && rotationDegrees != 0.0 && originalFixedCorner != null) {
        if (anchor.isCloseTo(centre.coordinate())) resizeRotatedFromCentre(draggedCorner, aspectRatio)
        else resizeRotatedFromFixedCorner(originalFixedCorner, draggedCorner, aspectRatio)
    } else copy(
        centre = centre.scaleFrom(anchor, scaleX, scaleY),
        xRadius = (xRadius * abs(scaleX)).coerceAtLeast(MIN_SIZE),
        yRadius = (yRadius * abs(scaleY)).coerceAtLeast(MIN_SIZE),
    )
}

private fun OperationDrawing.rotate(pivot: MapCoordinate, radians: Double, aspectRatio: Double, snap: Boolean): OperationDrawing = when (this) {
    is OperationFreeformDrawing -> copy(points = points.map { it.rotateAround(pivot, radians, aspectRatio) })
    is OperationLineDrawing -> copy(start = start.rotateAround(pivot, radians, aspectRatio), end = end.rotateAround(pivot, radians, aspectRatio))
    is OperationRectangleDrawing -> centre().let { originalCentre ->
        originalCentre.rotateAround(pivot, radians, aspectRatio).let { rotatedCentre ->
            copy(
                start = start + (rotatedCentre - originalCentre),
                end = end + (rotatedCentre - originalCentre),
                rotationDegrees = rotatedDegrees(rotationDegrees, radians, snap),
            )
        }
    }

    is OperationOvalDrawing -> copy(
        centre = centre.rotateAround(pivot, radians, aspectRatio),
        rotationDegrees = rotatedDegrees(rotationDegrees, radians, snap),
    )
}

private fun OperationDrawing.resizeAnchor(
    grabbedCorner: MapSelectionCorner,
    fromCentre: Boolean,
    aspectRatio: Double,
): MapCoordinate = when (this) {
    is OperationRectangleDrawing -> listOf(start, end).bounds().let { if (fromCentre) it.centre else it.corner(grabbedCorner.opposite()) }
    is OperationOvalDrawing -> OperationSelectionBounds(
        centre.x - xRadius, centre.y - yRadius, centre.x + xRadius, centre.y + yRadius,
    ).let { if (fromCentre) it.centre else it.corner(grabbedCorner.opposite()) }

    is OperationFreeformDrawing -> selectionBounds(aspectRatio).let { if (fromCentre) it.centre else it.corner(grabbedCorner.opposite()) }
    is OperationLineDrawing -> if (fromCentre) centre() else endpointFor(grabbedCorner.opposite())
}

private fun OperationDrawing.constrainedDraggedCorner(
    anchor: MapCoordinate,
    draggedCorner: MapCoordinate,
    originalFixedCorner: MapCoordinate?,
    preserveAspect: Boolean,
    aspectRatio: Double,
): MapCoordinate = when {
    !preserveAspect -> draggedCorner
    this is OperationRectangleDrawing && rotationDegrees != 0.0 ->
        constrainedRotatedCorner(anchor, draggedCorner, originalFixedCorner, centre(), rotationDegrees, aspectRatio)

    this is OperationOvalDrawing && rotationDegrees != 0.0 ->
        constrainedRotatedCorner(anchor, draggedCorner, originalFixedCorner, centre.coordinate(), rotationDegrees, aspectRatio)

    this is OperationRectangleDrawing || this is OperationOvalDrawing -> squareEnd(anchor, draggedCorner, aspectRatio)
    else -> draggedCorner
}

private fun constrainedRotatedCorner(
    anchor: MapCoordinate,
    draggedCorner: MapCoordinate,
    originalFixedCorner: MapCoordinate?,
    centre: MapCoordinate,
    rotationDegrees: Double,
    aspectRatio: Double,
) = when {
    anchor.isCloseTo(centre) -> anchor.constrainedRotatedDelta(draggedCorner, rotationDegrees, aspectRatio)
    originalFixedCorner != null -> originalFixedCorner.constrainedRotatedDelta(draggedCorner, rotationDegrees, aspectRatio)
    else -> squareEnd(anchor, draggedCorner, aspectRatio)
}

private fun OperationLineDrawing.resizeFromEndpoint(
    grabbedCorner: MapSelectionCorner,
    anchor: MapCoordinate,
    draggedCorner: MapCoordinate,
    fromCentre: Boolean,
): OperationLineDrawing {
    val dragged = OperationDrawingPoint(draggedCorner.x, draggedCorner.y)
    val anchored = OperationDrawingPoint(anchor.x, anchor.y)
    val opposite = OperationDrawingPoint(anchor.x * 2.0 - draggedCorner.x, anchor.y * 2.0 - draggedCorner.y)
    return when {
        fromCentre && grabbedCorner.selectsStartEndpoint() -> copy(start = dragged, end = opposite)
        fromCentre -> copy(start = opposite, end = dragged)
        grabbedCorner.selectsStartEndpoint() -> copy(start = dragged, end = anchored)
        else -> copy(start = anchored, end = dragged)
    }
}

private fun OperationRectangleDrawing.resizeRotatedFromFixedCorner(
    fixedCorner: MapCoordinate,
    draggedCorner: MapCoordinate,
    aspectRatio: Double,
): OperationRectangleDrawing {
    val newCentre = fixedCorner.midpoint(draggedCorner)
    val local = listOf(
        fixedCorner.rotateAround(newCentre, Math.toRadians(-rotationDegrees), aspectRatio),
        draggedCorner.rotateAround(newCentre, Math.toRadians(-rotationDegrees), aspectRatio),
    ).map { OperationDrawingPoint(it.x, it.y) }.bounds()
    return copy(start = OperationDrawingPoint(local.left, local.top), end = OperationDrawingPoint(local.right, local.bottom))
}

private fun OperationRectangleDrawing.resizeRotatedFromCentre(
    draggedCorner: MapCoordinate,
    aspectRatio: Double,
): OperationRectangleDrawing {
    val centre = centre()
    val local = draggedCorner.rotateAround(centre, Math.toRadians(-rotationDegrees), aspectRatio)
    val xRadius = abs(local.x - centre.x).coerceAtLeast(MIN_SIZE)
    val yRadius = abs(local.y - centre.y).coerceAtLeast(MIN_SIZE)
    return copy(
        start = OperationDrawingPoint(centre.x - xRadius, centre.y - yRadius),
        end = OperationDrawingPoint(centre.x + xRadius, centre.y + yRadius),
    )
}

private fun OperationOvalDrawing.resizeRotatedFromFixedCorner(
    fixedCorner: MapCoordinate,
    draggedCorner: MapCoordinate,
    aspectRatio: Double,
): OperationOvalDrawing {
    val newCentre = fixedCorner.midpoint(draggedCorner)
    val local = listOf(
        fixedCorner.rotateAround(newCentre, Math.toRadians(-rotationDegrees), aspectRatio),
        draggedCorner.rotateAround(newCentre, Math.toRadians(-rotationDegrees), aspectRatio),
    ).map { OperationDrawingPoint(it.x, it.y) }.bounds()
    return copy(
        centre = OperationDrawingPoint(local.centre.x, local.centre.y),
        xRadius = ((local.right - local.left) * 0.5).coerceAtLeast(MIN_SIZE),
        yRadius = ((local.bottom - local.top) * 0.5).coerceAtLeast(MIN_SIZE),
    )
}

private fun OperationOvalDrawing.resizeRotatedFromCentre(
    draggedCorner: MapCoordinate,
    aspectRatio: Double,
): OperationOvalDrawing {
    val local = draggedCorner.rotateAround(centre.coordinate(), Math.toRadians(-rotationDegrees), aspectRatio)
    return copy(
        xRadius = abs(local.x - centre.x).coerceAtLeast(MIN_SIZE),
        yRadius = abs(local.y - centre.y).coerceAtLeast(MIN_SIZE),
    )
}

private fun squareEnd(anchor: MapCoordinate, dragged: MapCoordinate, aspectRatio: Double): MapCoordinate {
    val dx = dragged.x - anchor.x
    val dy = dragged.y - anchor.y
    if (dx == 0.0 || dy == 0.0) return dragged
    val size = maxOf(abs(dx) * aspectRatio, abs(dy))
    return MapCoordinate(anchor.x + sign(dx) * size / aspectRatio, anchor.y + sign(dy) * size)
}

private fun MapCoordinate.constrainedRotatedDelta(dragged: MapCoordinate, degrees: Double, aspectRatio: Double): MapCoordinate {
    val origin = MapCoordinate(0.0, 0.0)
    val local = MapCoordinate(dragged.x - x, dragged.y - y).rotateAround(origin, Math.toRadians(-degrees), aspectRatio)
    val constrained = squareEnd(origin, local, aspectRatio).rotateAround(origin, Math.toRadians(degrees), aspectRatio)
    return MapCoordinate(x + constrained.x, y + constrained.y)
}

private fun OperationDrawing.selectionBounds(aspectRatio: Double): OperationSelectionBounds = when (this) {
    is OperationFreeformDrawing -> points.bounds()
    is OperationLineDrawing -> listOf(start, end).bounds()
    is OperationRectangleDrawing -> rectangleCorners().map { it.rotateAround(centre(), Math.toRadians(rotationDegrees), aspectRatio) }.bounds()
    is OperationOvalDrawing -> {
        val radians = Math.toRadians(rotationDegrees)
        val xExtent = hypot(xRadius * cos(radians), yRadius / aspectRatio * sin(radians))
        val yExtent = hypot(xRadius * aspectRatio * sin(radians), yRadius * cos(radians))
        OperationSelectionBounds(centre.x - xExtent, centre.y - yExtent, centre.x + xExtent, centre.y + yExtent)
    }
}

private fun OperationDrawing.contains(point: MapCoordinate, tolerance: Double, aspectRatio: Double): Boolean = when (this) {
    is OperationFreeformDrawing -> points.zipWithNext().any { (a, b) -> point.distanceToSegment(a, b, aspectRatio) <= tolerance }
    is OperationLineDrawing -> point.distanceToSegment(start, end, aspectRatio) <= tolerance
    is OperationRectangleDrawing -> point.rotateAround(centre(), Math.toRadians(-rotationDegrees), aspectRatio).let { local ->
        val bounds = listOf(start, end).bounds()
        bounds.contains(local, tolerance) && minOf(
            abs(local.x - bounds.left) * aspectRatio,
            abs(local.x - bounds.right) * aspectRatio,
            abs(local.y - bounds.top),
            abs(local.y - bounds.bottom),
        ) <= tolerance
    }

    is OperationOvalDrawing -> point.rotateAround(MapCoordinate(centre.x, centre.y), Math.toRadians(-rotationDegrees), aspectRatio).let { local ->
        val normalized = hypot((local.x - centre.x) / xRadius, (local.y - centre.y) / yRadius)
        abs(normalized - 1.0) <= tolerance / minOf(xRadius * aspectRatio, yRadius).coerceAtLeast(MIN_SIZE)
    }
}

private fun OperationRectangleDrawing.centre() = MapCoordinate((start.x + end.x) * 0.5, (start.y + end.y) * 0.5)
private fun OperationLineDrawing.centre() = MapCoordinate((start.x + end.x) * 0.5, (start.y + end.y) * 0.5)
private fun OperationRectangleDrawing.rectangleCorners() = listOf(
    OperationDrawingPoint(start.x, start.y), OperationDrawingPoint(end.x, start.y),
    OperationDrawingPoint(end.x, end.y), OperationDrawingPoint(start.x, end.y),
)

private fun List<OperationDrawingPoint>.bounds(): OperationSelectionBounds = takeIf(List<*>::isNotEmpty)?.let {
    OperationSelectionBounds(minOf(OperationDrawingPoint::x), minOf(OperationDrawingPoint::y), maxOf(OperationDrawingPoint::x), maxOf(OperationDrawingPoint::y))
} ?: OperationSelectionBounds(0.0, 0.0, 0.0, 0.0)

private fun MapCoordinate.distanceToSegment(start: OperationDrawingPoint, end: OperationDrawingPoint, aspectRatio: Double): Double {
    val px = x * aspectRatio
    val ax = start.x * aspectRatio
    val bx = end.x * aspectRatio
    val dx = bx - ax
    val dy = end.y - start.y
    val lengthSquared = dx * dx + dy * dy
    val t = if (lengthSquared == 0.0) 0.0 else (((px - ax) * dx + (y - start.y) * dy) / lengthSquared).coerceIn(0.0, 1.0)
    return hypot(px - (ax + dx * t), y - (start.y + dy * t))
}

private fun MapCoordinate.rotateAround(pivot: MapCoordinate, radians: Double, aspectRatio: Double): MapCoordinate {
    val dx = (x - pivot.x) * aspectRatio
    val dy = y - pivot.y
    return MapCoordinate(
        pivot.x + (dx * cos(radians) - dy * sin(radians)) / aspectRatio,
        pivot.y + dx * sin(radians) + dy * cos(radians),
    )
}

private fun MapCoordinate.midpoint(other: MapCoordinate) = MapCoordinate((x + other.x) * 0.5, (y + other.y) * 0.5)
private fun MapCoordinate.isCloseTo(other: MapCoordinate) = abs(x - other.x) <= 1e-7 && abs(y - other.y) <= 1e-7
private fun OperationDrawingPoint.isCloseTo(other: MapCoordinate) = abs(x - other.x) <= 1e-7 && abs(y - other.y) <= 1e-7
private fun OperationDrawingPoint.coordinate() = MapCoordinate(x, y)
private fun OperationLineDrawing.endpointFor(corner: MapSelectionCorner) =
    if (corner.selectsStartEndpoint()) MapCoordinate(start.x, start.y) else MapCoordinate(end.x, end.y)

private fun MapSelectionCorner.selectsStartEndpoint() = this == MapSelectionCorner.TOP_LEFT || this == MapSelectionCorner.BOTTOM_LEFT

private fun OperationDrawingPoint.rotateAround(pivot: MapCoordinate, radians: Double, aspectRatio: Double) =
    MapCoordinate(x, y).rotateAround(pivot, radians, aspectRatio).let { OperationDrawingPoint(it.x, it.y) }

private fun MapCoordinate.scaleFrom(anchor: MapCoordinate, x: Double, y: Double) = MapCoordinate(anchor.x + (this.x - anchor.x) * x, anchor.y + (this.y - anchor.y) * y)
private fun OperationDrawingPoint.scaleFrom(anchor: MapCoordinate, x: Double, y: Double) =
    OperationDrawingPoint(anchor.x + (this.x - anchor.x) * x, anchor.y + (this.y - anchor.y) * y)

private operator fun MapCoordinate.plus(delta: MapCoordinateDelta) = MapCoordinate(x + delta.x, y + delta.y)
private operator fun MapCoordinate.minus(other: MapCoordinate) = MapCoordinateDelta(x - other.x, y - other.y)
private operator fun OperationDrawingPoint.plus(delta: MapCoordinateDelta) = OperationDrawingPoint(x + delta.x, y + delta.y)
private fun scale(current: Double, anchor: Double, original: Double) =
    (original - anchor).takeUnless { abs(it) < 1e-9 }?.let { ((current - anchor) / it).coerceAtLeast(0.01) } ?: 1.0

private fun rotatedDegrees(original: Double, radians: Double, snap: Boolean): Double =
    (original + Math.toDegrees(radians)).let { total -> if (snap) snapDegrees(total).toDouble() else normalizeDegrees(total) }

private fun normalizeDegrees(value: Int) = (value % 360 + 360) % 360
private fun normalizeDegrees(value: Double) = (value % 360.0 + 360.0) % 360.0
private fun snapDegrees(value: Double) = normalizeDegrees((value / 45.0).roundToInt() * 45)
private fun sign(value: Double) = if (value < 0.0) -1.0 else 1.0
private fun <T, I> changes(before: List<T>, after: List<T>, id: (T) -> I): List<OperationValueChange<T>> {
    val beforeById = before.associateBy(id)
    val afterById = after.associateBy(id)
    return (beforeById.keys + afterById.keys).mapNotNull { key ->
        OperationValueChange(beforeById[key], afterById[key]).takeIf { it.before != it.after }
    }
}

private const val MIN_SIZE = 0.0001
