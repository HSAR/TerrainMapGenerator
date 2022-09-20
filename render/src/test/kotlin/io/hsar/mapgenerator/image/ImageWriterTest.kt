package io.hsar.mapgenerator.image

import io.hsar.mapgenerator.image.ImageUtils.toBufferedImage
import java.nio.file.Path
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.junit.jupiter.api.Test
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
        io.hsar.mapgenerator.image.ImageWriter.writeGreyScaleImage(testData.toBufferedImage(), imagePath)

        // Assert
        val expectedImage = ImageWriterTest::class.java.classLoader.getResource("data/test.png").readBytes()
        val actualImage = imagePath.readBytes()

        MatcherAssert.assertThat(actualImage, CoreMatchers.equalTo(expectedImage))
    }
}