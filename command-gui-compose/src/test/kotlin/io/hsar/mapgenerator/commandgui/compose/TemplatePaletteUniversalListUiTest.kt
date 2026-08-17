package io.hsar.mapgenerator.commandgui.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.rightClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import io.hsar.mapgenerator.commandgui.operation.OperationGraphicTemplate
import io.hsar.mapgenerator.commandgui.operation.OperationGraphicTemplateId
import io.hsar.mapgenerator.commandgui.operation.OperationUnitAffiliation
import io.hsar.mapgenerator.commandgui.operation.OperationUnitTemplate
import io.hsar.mapgenerator.commandgui.operation.OperationUnitTemplateId
import org.junit.jupiter.api.Test
import java.awt.image.BufferedImage
import java.util.UUID

class TemplatePaletteUniversalListUiTest {
    @Test
    @OptIn(ExperimentalTestApi::class)
    fun `unit palette keeps edit and delete in the compact row context menu`() = runComposeUiTest {
        val id = OperationUnitTemplateId(UUID(0, 81))
        val template = OperationUnitTemplate(
            id, "Scout", BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB), 0, affiliation = OperationUnitAffiliation.FRIEND,
        )
        setContent {
            NordTheme {
                Box(Modifier.size(360.dp, 300.dp)) {
                    UnitTypesPalette(
                        templates = listOf(template),
                        expanded = true,
                        editable = true,
                        activePlacement = null,
                        onToggle = {},
                        onCreate = {},
                        onEdit = {},
                        onPlace = {},
                        onDelete = {},
                        showToggle = false,
                    )
                }
            }
        }

        onNodeWithTag("unit-type-row-${id.value}").assertHeightIsEqualTo(44.dp)
        onNodeWithText("PLACE").fetchSemanticsNode()
        onAllNodesWithText("Edit Unit Type").assertCountEquals(0)
        onNodeWithTag("unit-type-row-${id.value}").performMouseInput { rightClick() }
        onNodeWithText("Edit Unit Type").fetchSemanticsNode()
        onNodeWithText("Delete Unit Type").fetchSemanticsNode()
    }

    @Test
    @OptIn(ExperimentalTestApi::class)
    fun `graphic palette uses the same compact universal row`() = runComposeUiTest {
        val id = OperationGraphicTemplateId(UUID(0, 82))
        val template = OperationGraphicTemplate(id, "Waypoint", BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB), 0)
        setContent {
            NordTheme {
                Box(Modifier.size(360.dp, 300.dp)) {
                    GraphicTypesPalette(
                        templates = listOf(template),
                        expanded = true,
                        editable = true,
                        activePlacement = null,
                        onToggle = {},
                        onCreate = {},
                        onEdit = {},
                        onPlace = {},
                        onDelete = {},
                        showToggle = false,
                    )
                }
            }
        }

        onNodeWithTag("graphic-type-row-${id.value}").assertHeightIsEqualTo(44.dp)
    }
}
