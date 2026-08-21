package io.hsar.mapgenerator.commandgui.compose

import kotlinx.coroutines.runBlocking
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import java.awt.image.BufferedImage
import java.util.UUID

class ComposeTemplateBitmapLoaderTest {
    @Test
    fun `loader retains only active template textures and bounds unit resolution`() = runBlocking {
        // Arrange
        val loader = ComposeTemplateBitmapLoader(this)
        val unit = request(ComposeTemplateBitmapKind.UNIT, 256, 64)
        val graphic = request(ComposeTemplateBitmapKind.GRAPHIC, 16, 8)

        // Act
        loader.request(listOf(unit))
        eventually { unit.key in loader.ready }
        val unitBitmap = loader.ready.getValue(unit.key)
        loader.request(listOf(graphic))
        eventually { graphic.key in loader.ready }

        // Assert
        assertThat(unitBitmap.width, equalTo(128))
        assertThat(unitBitmap.height, equalTo(32))
        assertThat(loader.ready.keys, equalTo(setOf(graphic.key)))
        loader.dispose()
    }

    private fun request(kind: ComposeTemplateBitmapKind, width: Int, height: Int) = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB).let { image ->
        ComposeTemplateBitmapRequest(
            ComposeTemplateBitmapKey(kind, UUID.randomUUID(), image),
        )
    }

    private suspend fun eventually(condition: () -> Boolean) {
        repeat(200) {
            if (condition()) return
            kotlinx.coroutines.delay(5)
        }
        error("Condition was not met")
    }
}
