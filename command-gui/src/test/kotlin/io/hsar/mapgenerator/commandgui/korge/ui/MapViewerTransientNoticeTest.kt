package io.hsar.mapgenerator.commandgui.korge.ui

import korlibs.korge.view.Container
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test

class MapViewerTransientNoticeTest {

    @Test
    fun `transient notices keep the newest five visible and recycle the oldest`() {
        val root = Container()
        val notice = MapViewerTransientNotice(
            root = root,
            viewportHeightProvider = { 600 },
            durationMs = 1_000L,
            fadeDurationMs = 200L,
            maxVisibleNotices = 5,
        )

        (0..5).forEach { index ->
            notice.show("Notice ${index + 1}", nowMs = index.toLong())
        }

        assertThat(notice.snapshotForTesting().visible, equalTo(listOf("Notice 2", "Notice 3", "Notice 4", "Notice 5", "Notice 6")))
        assertThat(notice.snapshotForTesting().fading, equalTo(listOf("Notice 1")))

        notice.update(nowMs = 250L)

        assertThat(notice.snapshotForTesting().visible, equalTo(listOf("Notice 2", "Notice 3", "Notice 4", "Notice 5", "Notice 6")))
        assertThat(notice.snapshotForTesting().fading, equalTo(emptyList()))
    }
}
