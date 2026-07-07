package io.hsar.mapgenerator.commandgui.korge.ui

import korlibs.math.geom.Vector2D

internal fun focusShadowPolygon(
    bounds: MapViewerDialogBounds,
    targetBounds: ScreenBounds?,
): List<Vector2D>? {
    targetBounds ?: return null
    if (bounds.width <= 0.0 || bounds.height <= 0.0 || targetBounds.width <= 0.0 || targetBounds.height <= 0.0) {
        return null
    }

    val dialogCenterX = bounds.left + bounds.width * 0.5
    val dialogCenterY = bounds.top + bounds.height * 0.5
    val targetCenterX = targetBounds.left + targetBounds.width * 0.5
    val targetCenterY = targetBounds.top + targetBounds.height * 0.5
    val isVerticallyNested = targetBounds.top >= bounds.top && targetBounds.bottom <= bounds.bottom
    val isHorizontallyNested = targetBounds.left >= bounds.left && targetBounds.right <= bounds.right

    return when {
        targetCenterX < dialogCenterX && isVerticallyNested ->
            leftCutoutPolygon(bounds, targetBounds)

        targetCenterX > dialogCenterX && isVerticallyNested ->
            rightCutoutPolygon(bounds, targetBounds)

        targetCenterY < dialogCenterY && isHorizontallyNested ->
            topCutoutPolygon(bounds, targetBounds)

        targetCenterY > dialogCenterY && isHorizontallyNested ->
            bottomCutoutPolygon(bounds, targetBounds)

        else ->
            diagonalShadowPolygon(bounds, targetBounds, targetCenterX, targetCenterY, dialogCenterX, dialogCenterY)
    }
}

private fun diagonalShadowPolygon(
    dialogBounds: MapViewerDialogBounds,
    targetBounds: ScreenBounds,
    targetCenterX: Double,
    targetCenterY: Double,
    dialogCenterX: Double,
    dialogCenterY: Double,
): List<Vector2D> =
    when {
        targetCenterX > dialogCenterX && targetCenterY < dialogCenterY -> listOf(
            dialogBounds.topLeft(),
            targetBounds.topLeft(),
            targetBounds.topRight(),
            targetBounds.bottomRight(),
            dialogBounds.bottomRight(),
            dialogBounds.topRight(),
        )

        targetCenterX < dialogCenterX && targetCenterY < dialogCenterY -> listOf(
            dialogBounds.topRight(),
            targetBounds.topRight(),
            targetBounds.bottomRight(),
            targetBounds.bottomLeft(),
            dialogBounds.bottomLeft(),
            dialogBounds.topLeft(),
        )

        targetCenterX > dialogCenterX && targetCenterY > dialogCenterY -> listOf(
            dialogBounds.bottomLeft(),
            targetBounds.bottomLeft(),
            targetBounds.bottomRight(),
            targetBounds.topRight(),
            dialogBounds.topRight(),
            dialogBounds.bottomRight(),
        )

        else -> listOf(
            dialogBounds.bottomRight(),
            targetBounds.bottomRight(),
            targetBounds.topRight(),
            targetBounds.topLeft(),
            dialogBounds.topLeft(),
            dialogBounds.bottomLeft(),
        )
    }

private fun leftCutoutPolygon(
    dialogBounds: MapViewerDialogBounds,
    targetBounds: ScreenBounds,
): List<Vector2D> =
    listOf(
        dialogBounds.topLeft(),
        targetBounds.topLeft(),
        targetBounds.topRight(),
        targetBounds.bottomRight(),
        targetBounds.bottomLeft(),
        dialogBounds.bottomLeft(),
    )

private fun rightCutoutPolygon(
    dialogBounds: MapViewerDialogBounds,
    targetBounds: ScreenBounds,
): List<Vector2D> =
    listOf(
        dialogBounds.topRight(),
        targetBounds.topRight(),
        targetBounds.bottomRight(),
        targetBounds.bottomLeft(),
        targetBounds.topLeft(),
        dialogBounds.bottomRight(),
    )

private fun topCutoutPolygon(
    dialogBounds: MapViewerDialogBounds,
    targetBounds: ScreenBounds,
): List<Vector2D> =
    listOf(
        dialogBounds.topLeft(),
        targetBounds.bottomLeft(),
        targetBounds.topLeft(),
        targetBounds.topRight(),
        targetBounds.bottomRight(),
        dialogBounds.topRight(),
    )

private fun bottomCutoutPolygon(
    dialogBounds: MapViewerDialogBounds,
    targetBounds: ScreenBounds,
): List<Vector2D> =
    listOf(
        dialogBounds.bottomLeft(),
        targetBounds.bottomLeft(),
        targetBounds.topLeft(),
        targetBounds.topRight(),
        targetBounds.bottomRight(),
        dialogBounds.bottomRight(),
    )

private fun MapViewerDialogBounds.topLeft(): Vector2D = Vector2D(left, top)

private fun MapViewerDialogBounds.topRight(): Vector2D = Vector2D(right, top)

private fun MapViewerDialogBounds.bottomLeft(): Vector2D = Vector2D(left, bottom)

private fun MapViewerDialogBounds.bottomRight(): Vector2D = Vector2D(right, bottom)

private fun ScreenBounds.topLeft(): Vector2D = Vector2D(left, top)

private fun ScreenBounds.topRight(): Vector2D = Vector2D(right, top)

private fun ScreenBounds.bottomLeft(): Vector2D = Vector2D(left, bottom)

private fun ScreenBounds.bottomRight(): Vector2D = Vector2D(right, bottom)
