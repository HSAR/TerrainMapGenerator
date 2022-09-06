package io.hsar.mapgenerator.image

import io.hsar.mapgenerator.image.ImageUtils.toBufferedImage
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Test

import java.nio.file.Path
import kotlin.io.path.readBytes

internal class ImageWriterTest {

    @Test
    fun `writes images as specified`() {
        // Arrange
        val testData = listOf(
            listOf(0.5, 0.0, 0.0, 0.0, 1.0, 0.0),
            listOf(0.7, 0.0, 1.0, 0.0, 0.0, 0.0)
        )

        val imagePath = Path.of("test.png").toAbsolutePath()

        // Act
        ImageWriter.writeGreyScaleImage(testData.toBufferedImage(), imagePath)

        // Assert
        val expectedImage = ImageWriterTest::class.java.classLoader.getResource("data/test.png").readBytes()
        val actualImage = imagePath.readBytes()

        assertThat(actualImage, equalTo(expectedImage))
    }
}