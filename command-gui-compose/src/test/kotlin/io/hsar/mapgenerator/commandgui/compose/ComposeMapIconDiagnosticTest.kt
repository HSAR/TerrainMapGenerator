package io.hsar.mapgenerator.commandgui.compose

import androidx.compose.ui.graphics.toPixelMap
import io.hsar.mapgenerator.commandgui.MapViewerSaveRepository
import io.hsar.mapgenerator.commandgui.operation.OperationGraphic
import io.hsar.mapgenerator.commandgui.operation.OperationUnit
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path

class ComposeMapIconDiagnosticTest {

    @Test
    fun `reports the Compose bitmap conversion for each placed icon`() {
        val path = Path.of(System.getProperty("user.home"), "Documents", "My Games", "OperationalCommandTool", "saves", "Sanguine_Shield_0e554b65.oct.zip")
        assumeTrue(Files.exists(path))
        val content = MapViewerSaveRepository().loadOperation(path).content

        println(
            content.objects.joinToString(separator = "\n", prefix = "Compose image-conversion diagnostic:\n") { objectValue ->
                when (objectValue) {
                    is OperationUnit -> content.unitTemplate(objectValue.unitTemplateId)?.image?.toImageBitmap()
                        .let { bitmap -> "unit id=${objectValue.id.value} bitmap=${bitmap?.width}x${bitmap?.height} alpha=${bitmap?.toPixelMap()?.maxAlpha()}" }

                    is OperationGraphic -> content.graphicTemplate(objectValue.graphicTemplateId)?.image?.toImageBitmap()
                        .let { bitmap -> "graphic id=${objectValue.id.value} bitmap=${bitmap?.width}x${bitmap?.height} alpha=${bitmap?.toPixelMap()?.maxAlpha()}" }

                    else -> "drawing id=${objectValue.id.value}"
                }
            },
        )
    }
}

private fun androidx.compose.ui.graphics.PixelMap.maxAlpha() =
    (0 until height).maxOf { y -> (0 until width).maxOf { x -> this[x, y].alpha } }
