package io.hsar.mapgenerator.commandgui

import io.hsar.mapgenerator.commandgui.operation.OperationGraphic
import io.hsar.mapgenerator.commandgui.operation.OperationUnit
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.empty
import org.hamcrest.Matchers.greaterThan
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path

class LocalOperationLoadProbeTest {
    @Test
    fun `local saved operation retains placed objects in the engine neutral timeline`() {
        val path = Path.of(System.getProperty("user.home"), "Documents", "My Games", "OperationalCommandTool", "saves", "Sanguine_Shield_0e554b65.oct.zip")
        assumeTrue(Files.exists(path))

        val content = MapViewerSaveRepository().loadOperation(path).content
        println(
            content.objects.joinToString(separator = "\n", prefix = "Compose map-object diagnostic:\n") { objectValue ->
                when (objectValue) {
                    is OperationUnit -> content.unitTemplate(objectValue.unitTemplateId).let { template ->
                        "unit id=${objectValue.id.value} template=${objectValue.unitTemplateId.value} coordinate=${objectValue.coordinate} " +
                                "resolved=${template != null} image=${template?.image?.width}x${template?.image?.height} alpha=${template?.image?.maxAlpha()}"
                    }

                    is OperationGraphic -> content.graphicTemplate(objectValue.graphicTemplateId).let { template ->
                        "graphic id=${objectValue.id.value} template=${objectValue.graphicTemplateId.value} coordinate=${objectValue.coordinate} " +
                                "resolved=${template != null} image=${template?.image?.width}x${template?.image?.height} alpha=${template?.image?.maxAlpha()}"
                    }

                    else -> "drawing id=${objectValue.id.value}"
                }
            },
        )

        assertThat(content.objects.size, greaterThan(0))
        assertThat(content.objects.filterIsInstance<OperationUnit>().size, greaterThan(0))
        assertThat(content.objects.filterIsInstance<OperationGraphic>().size, greaterThan(0))
        assertThat(content.unitTemplates.size, greaterThan(0))
        assertThat(
            content.objects.filterIsInstance<OperationUnit>().all { unit -> content.unitTemplate(unit.unitTemplateId) != null },
            org.hamcrest.Matchers.equalTo(true),
        )
        assertThat(
            content.objects.filterIsInstance<OperationGraphic>().map(OperationGraphic::graphicTemplateId)
                .filter { content.graphicTemplate(it) == null },
            empty(),
        )
        assertThat(
            content.unitTemplates.maxOf { template ->
                (0 until template.image.height).maxOf { y ->
                    (0 until template.image.width).maxOf { x -> template.image.getRGB(x, y) ushr 24 and 0xff }
                }
            },
            greaterThan(0),
        )
    }
}

private fun java.awt.image.BufferedImage.maxAlpha() =
    (0 until height).maxOf { y -> (0 until width).maxOf { x -> getRGB(x, y) ushr 24 and 0xff } }
