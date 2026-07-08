package io.hsar.mapgenerator.commandgui.korge.interaction

internal fun contextMenuSubmenuHoldBounds(
    parentBounds: ScreenBounds,
    parentIndex: Int,
    submenuBounds: ScreenBounds,
): ScreenBounds {
    val rowBounds = ScreenBounds(
        left = parentBounds.left,
        top = parentBounds.top + (parentIndex * ContextMenuLayout.ACTION_HEIGHT),
        width = parentBounds.width,
        height = ContextMenuLayout.ACTION_HEIGHT,
    )
    return rowBounds.union(submenuBounds).expanded(ContextMenuLayout.SUBMENU_HOVER_PADDING)
}

private fun ScreenBounds.union(other: ScreenBounds): ScreenBounds {
    val left = minOf(left, other.left)
    val top = minOf(top, other.top)
    val right = maxOf(right, other.right)
    val bottom = maxOf(bottom, other.bottom)
    return ScreenBounds(
        left = left,
        top = top,
        width = right - left,
        height = bottom - top,
    )
}

private fun ScreenBounds.expanded(amount: Double): ScreenBounds =
    ScreenBounds(
        left = left - amount,
        top = top - amount,
        width = width + amount * 2.0,
        height = height + amount * 2.0,
    )
