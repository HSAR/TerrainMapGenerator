package io.hsar.mapgenerator.commandgui.korge.launch

import io.hsar.mapgenerator.commandgui.korge.interaction.BatchLedgerEntry
import io.hsar.mapgenerator.commandgui.korge.interaction.CreateUnitTemplateAction
import io.hsar.mapgenerator.commandgui.korge.interaction.DeleteUnitTemplateAction
import io.hsar.mapgenerator.commandgui.korge.interaction.EditUnitTemplateAction
import io.hsar.mapgenerator.commandgui.korge.interaction.MapViewerUnitTemplate
import io.hsar.mapgenerator.commandgui.korge.interaction.UnitAffiliation
import io.hsar.mapgenerator.commandgui.korge.interaction.UnitTemplateId
import io.hsar.mapgenerator.commandgui.korge.interaction.UnitTemplateSnapshot
import korlibs.image.bitmap.Bitmap32
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import java.util.*

class MapViewerNewOperationUnitTemplateImportTest {
    @Test
    fun `unit type identities retain same named friendly and hostile types`() {
        val friendlyInfantry = unitTypeIdentity("Infantry", UnitAffiliation.Friend)
        val hostileInfantry = unitTypeIdentity("Infantry", UnitAffiliation.Hostile)

        assertThat(friendlyInfantry == hostileInfantry, equalTo(false))
    }

    @Test
    fun `unit type identities match same named types with the same affiliation`() {
        val existingInfantry = unitTypeIdentity("Infantry", UnitAffiliation.Friend)
        val importedInfantry = unitTypeIdentity("Infantry", UnitAffiliation.Friend)

        assertThat(existingInfantry, equalTo(importedInfantry))
    }

    @Test
    fun `latest unit templates applies batch edits and deletes in save order`() {
        val alpha = snapshot("Alpha", index = 0)
        val bravo = snapshot("Bravo", index = 1)
        val editedAlpha = alpha.copy(template = alpha.template.copy(name = "Alpha Prime"))

        val templates = listOf(
            BatchLedgerEntry(
                listOf(
                    CreateUnitTemplateAction(alpha),
                    CreateUnitTemplateAction(bravo),
                ),
            ),
            EditUnitTemplateAction(
                id = alpha.id,
                forward = emptyMap(),
                backward = emptyMap(),
                afterSnapshot = editedAlpha,
            ),
            DeleteUnitTemplateAction(bravo),
        ).latestUnitTemplates()

        assertThat(templates.map { it.template.name }, equalTo(listOf("Alpha Prime")))
    }

    private fun snapshot(name: String, index: Int): UnitTemplateSnapshot =
        UnitTemplateSnapshot(
            id = UnitTemplateId(UUID.nameUUIDFromBytes(name.toByteArray())),
            template = MapViewerUnitTemplate(
                id = UnitTemplateId(UUID.nameUUIDFromBytes("$name-template".toByteArray())),
                name = name,
                bitmap = Bitmap32(1, 1),
            ),
            index = index,
        )
}
