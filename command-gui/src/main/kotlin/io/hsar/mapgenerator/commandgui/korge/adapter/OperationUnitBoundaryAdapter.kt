package io.hsar.mapgenerator.commandgui.korge.adapter

import io.hsar.mapgenerator.commandgui.korge.interaction.MapViewerUnitTemplate
import io.hsar.mapgenerator.commandgui.korge.interaction.UnitAffiliation
import io.hsar.mapgenerator.commandgui.korge.interaction.UnitSnapshot
import io.hsar.mapgenerator.commandgui.operation.OperationContent
import io.hsar.mapgenerator.commandgui.operation.OperationObjectId
import io.hsar.mapgenerator.commandgui.operation.OperationUnit
import io.hsar.mapgenerator.commandgui.operation.OperationUnitAffiliation
import io.hsar.mapgenerator.commandgui.operation.OperationUnitTemplate
import io.hsar.mapgenerator.commandgui.operation.OperationUnitTemplateId
import java.awt.image.BufferedImage

private val EMPTY_OPERATION_IMAGE = BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB)

internal fun UnitSnapshot.toOperationUnit() = OperationUnit(
    id = OperationObjectId(id.value),
    unitTemplateId = OperationUnitTemplateId(unitTemplateId.value),
    instanceName = instanceName,
    destroyed = destroyed,
    facingDegrees = facingDegrees,
    coordinate = mapCoordinate,
    normalizedWidth = normalizedWidth,
    normalizedHeight = normalizedHeight,
    customProperties = customProperties,
    affiliation = template.affiliation?.toOperation(),
)

internal fun MapViewerUnitTemplate.toOperationTemplate(index: Int = 0) = OperationUnitTemplate(
    id = OperationUnitTemplateId(id.value),
    name = name,
    image = EMPTY_OPERATION_IMAGE,
    index = index,
    assetSource = assetSource,
    affiliation = affiliation?.toOperation(),
    customProperties = customProperties,
)

internal fun Iterable<UnitSnapshot>.toOperationUnitContent(): OperationContent {
    val snapshots = toList()
    return OperationContent(
        objects = snapshots.map(UnitSnapshot::toOperationUnit),
        unitTemplates = snapshots.distinctBy(UnitSnapshot::unitTemplateId)
            .mapIndexed { index, snapshot -> snapshot.template.toOperationTemplate(index) },
    )
}

private fun UnitAffiliation.toOperation() = OperationUnitAffiliation.valueOf(name.uppercase())
