package io.hsar.mapgenerator.commandgui.korge.ui

import io.hsar.mapgenerator.image.Palette
import korlibs.image.color.RGBA
import korlibs.korge.view.Container
import korlibs.korge.view.container
import korlibs.korge.view.solidRect
import korlibs.korge.view.xy

internal class MapViewerTransientNotice(
    root: Container,
    private val viewportHeightProvider: () -> Int,
    private val durationMs: Long = DEFAULT_DURATION_MS,
    private val fadeDurationMs: Long = DEFAULT_FADE_DURATION_MS,
    private val maxVisibleNotices: Int = MAX_VISIBLE_NOTICES,
) {
    private val layer = root.container().also {
        it.mouseEnabled = false
        it.visible = false
        it.zIndex = Z_INDEX
    }
    private val notices = mutableListOf<NoticeView>()
    private var lastUpdateMs = System.currentTimeMillis()
    private var nextId = 0L

    fun show(message: String, nowMs: Long = System.currentTimeMillis()) {
        notices += createNotice(message, nowMs)
        enforceVisibleLimit(nowMs)
        layout(nowMs)
        lastUpdateMs = nowMs - 16L
        update(nowMs)
    }

    fun update(nowMs: Long = System.currentTimeMillis()) {
        val previousUpdateMs = lastUpdateMs
        notices
            .filter { !it.isFading && nowMs >= it.expiresAtMs }
            .forEach { beginFade(it, nowMs) }
        enforceVisibleLimit(nowMs)
        layout(nowMs)
        animate(previousUpdateMs, nowMs)
        cleanup(nowMs)
        layer.visible = notices.isNotEmpty()
        lastUpdateMs = nowMs
    }

    internal fun snapshotForTesting(): MapViewerTransientNoticeSnapshot =
        MapViewerTransientNoticeSnapshot(
            visible = notices.filterNot { it.isFading }.map { it.message },
            fading = notices.filter { it.isFading }.map { it.message },
        )

    private fun createNotice(message: String, nowMs: Long): NoticeView {
        val noticeBounds = MapViewerTextViews.bounds(message)
        val panelWidth = noticeBounds.width + HORIZONTAL_PADDING * 2
        val panelHeight = noticeBounds.height + VERTICAL_PADDING * 2
        val container = layer.container().also {
            it.alpha = 0.0
            it.xy(LEFT_PADDING, viewportHeightProvider().toDouble())
        }
        container.solidRect(
            width = panelWidth,
            height = panelHeight,
            color = RGBA(0, 0, 0, 180),
        )
        MapViewerTextViews.label(container, message, color = Palette.Colours.WHITE) {
            x = HORIZONTAL_PADDING
            y = VERTICAL_PADDING
        }
        return NoticeView(
            id = nextId++,
            message = message,
            container = container,
            panelHeight = panelHeight,
            expiresAtMs = nowMs + durationMs,
            isFresh = true,
        )
    }

    private fun enforceVisibleLimit(nowMs: Long) {
        while (notices.count { !it.isFading } > maxVisibleNotices) {
            notices.firstOrNull { !it.isFading }?.let { beginFade(it, nowMs) }
        }
    }

    private fun beginFade(notice: NoticeView, nowMs: Long) {
        if (notice.isFading) {
            return
        }

        notice.isFading = true
        notice.fadeStartedAtMs = nowMs
        notice.fadeStartY = notice.container.y
        notice.targetY = (notice.container.y - FADE_LIFT_PX).coerceAtLeast(0.0)
        notice.targetAlpha = 0.0
    }

    private fun layout(nowMs: Long) {
        val activeNotices = notices.filterNot { it.isFading }
        var bottom = viewportHeightProvider().toDouble() - BOTTOM_PADDING
        activeNotices
            .asReversed()
            .forEach { notice ->
                notice.targetY = bottom - notice.panelHeight
                notice.targetAlpha = 1.0
                if (notice.isFresh) {
                    notice.container.y = notice.targetY + ENTRY_OFFSET_PX
                    notice.isFresh = false
                }
                bottom = notice.targetY - STACK_GAP
            }

        notices
            .filter { it.isFading }
            .forEach { notice ->
                val fadeStartY = notice.fadeStartY ?: notice.container.y
                notice.targetY = (fadeStartY - FADE_LIFT_PX).coerceAtLeast(0.0)
                notice.targetAlpha = 0.0
            }
    }

    private fun animate(previousUpdateMs: Long, nowMs: Long) {
        val deltaMs = (nowMs - previousUpdateMs).coerceAtLeast(0L).coerceAtMost(50L).toDouble()
        val positionStep = (deltaMs / POSITION_ANIMATION_MS).coerceIn(0.0, 1.0)
        val alphaStep = (deltaMs / FADE_ANIMATION_MS).coerceIn(0.0, 1.0)

        notices.forEach { notice ->
            notice.container.y += (notice.targetY - notice.container.y) * positionStep
            notice.container.alpha += (notice.targetAlpha - notice.container.alpha) * alphaStep
        }
    }

    private fun cleanup(nowMs: Long) {
        notices
            .filter { it.isFading && nowMs - (it.fadeStartedAtMs ?: nowMs) >= fadeDurationMs }
            .forEach { notice ->
                notice.container.alpha = 0.0
                notice.container.removeFromParent()
            }
        notices.removeAll { it.isFading && nowMs - (it.fadeStartedAtMs ?: nowMs) >= fadeDurationMs && it.container.parent == null }
    }

    private data class NoticeView(
        val id: Long,
        val message: String,
        val container: Container,
        val panelHeight: Double,
        val expiresAtMs: Long,
        var isFading: Boolean = false,
        var fadeStartedAtMs: Long? = null,
        var fadeStartY: Double? = null,
        var targetY: Double = 0.0,
        var targetAlpha: Double = 1.0,
        var isFresh: Boolean = false,
    )

    companion object {
        const val DEFAULT_DURATION_MS = 10_000L
        const val DEFAULT_FADE_DURATION_MS = 420L
        const val MAX_VISIBLE_NOTICES = 5
    }
}

internal data class MapViewerTransientNoticeSnapshot(
    val visible: List<String>,
    val fading: List<String>,
)

private const val Z_INDEX = 50_500.0
private const val LEFT_PADDING = 16.0
private const val BOTTOM_PADDING = 16.0
private const val HORIZONTAL_PADDING = 12.0
private const val VERTICAL_PADDING = 8.0
private const val STACK_GAP = 8.0
private const val ENTRY_OFFSET_PX = 12.0
private const val FADE_LIFT_PX = 12.0
private const val POSITION_ANIMATION_MS = 160.0
private const val FADE_ANIMATION_MS = 220.0
