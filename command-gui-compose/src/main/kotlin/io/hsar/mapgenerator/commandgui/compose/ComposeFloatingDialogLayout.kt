package io.hsar.mapgenerator.commandgui.compose

/** Resolves a clamped dimension from the full drag distance, retaining pointer movement beyond the clamp. */
internal fun clampedDialogDimensionAtPointer(
    initial: Float,
    dragDistance: Float,
    minimum: Float,
) = (initial + dragDistance).coerceAtLeast(minimum)
