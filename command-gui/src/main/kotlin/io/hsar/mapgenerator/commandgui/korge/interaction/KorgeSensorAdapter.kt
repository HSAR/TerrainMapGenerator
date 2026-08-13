package io.hsar.mapgenerator.commandgui.korge.interaction

import io.hsar.mapgenerator.commandgui.korge.adapter.toOperationUnit
import io.hsar.mapgenerator.commandgui.korge.adapter.toOperationUnitContent
import io.hsar.mapgenerator.commandgui.operation.*
import io.hsar.mapgenerator.grid.GridLayer

internal data class SensorContact(val unit: UnitSnapshot, val distanceUnits: Double, val detectionRangeUnits: Double, val baseDetectionRangeUnits: Double)
internal enum class SensorReportMode { SCIREP, NAVREP }
internal enum class EcmSensorContact { NONE, FAINT_TRACK, SENSOR_GHOST }
internal data class SensorObserverProfile(val unitClass: String?, val activeSensors: Boolean, val advancedSensorsBonus: Double) {
    companion object { val LOWEST_FIGHTER_SEEKER = SensorObserverProfile("F", false, 0.0) }
}
internal data class SensorVisibilityCheck(
    val unit: UnitSnapshot,
    val visible: Boolean,
    val detected: Boolean,
    val distanceUnits: Double,
    val identificationRangeUnits: Double,
    val detectionRangeUnits: Double,
    val baseDetectionRangeUnits: Double,
    val notes: List<String>,
    val naturalVisible: Boolean = visible,
    val naturalDetected: Boolean = detected,
    val ecmIntensity: Int = 0,
    val ecmTransitionIntensity: Int? = null,
    val ecmContact: EcmSensorContact = EcmSensorContact.NONE,
    val legacyEcmLevel: Int = 0,
)
internal data class SitrepVisibilityReport(
    val displayText: String,
    val copyText: String,
    val visibleChecks: List<SensorVisibilityCheck>,
    val detectedChecks: List<SensorVisibilityCheck>,
    val unidentifiedChecks: List<SensorVisibilityCheck> = emptyList(),
    val hiddenChecks: List<SensorVisibilityCheck>,
    val ecmGhostChecks: List<SensorVisibilityCheck> = emptyList(),
)
internal data class SitrepContactState(
    val sensorContactIds: Set<MapPlacedObjectId> = emptySet(),
    val identifiedIds: Set<MapPlacedObjectId> = emptySet(),
    val sierraContactNumbers: Map<MapPlacedObjectId, Int> = emptyMap(),
    val usedSierraContactNumbers: Set<Int> = emptySet(),
    val lastSeenPositions: Map<MapPlacedObjectId, MapCoordinate> = emptyMap(),
    val lastSeenContacts: Map<MapPlacedObjectId, SitrepLostContact> = emptyMap(),
    val lostContacts: List<SitrepLostContact> = emptyList(),
)
internal data class SitrepLostContact(
    val contactId: MapPlacedObjectId,
    val sierraNumber: Int,
    val lastSeenPosition: MapCoordinate,
    val lastSeenRangeUnits: Double,
    val wasIdentified: Boolean,
    val unitTypeName: String? = null,
    val unitClassCode: String? = null,
    val wasFriendly: Boolean = false,
    val lostDueToEnemyAction: Boolean = false,
    val destroyedByFriendlyAction: Boolean = false,
    val identifiedAffiliation: String? = null,
    val unitInstanceName: String? = null,
)
internal data class ScirepContactSegment(val contactId: MapPlacedObjectId, val sierraNumber: Int, val opening: String, val clauses: List<String>)

internal fun UnitSnapshot.isNavrepUnit() = neutralPresentation(OperationUnitPresentation::isNavrepUnit)
internal fun UnitSnapshot.sitrepValidationError() = neutralPresentation(OperationUnitPresentation::sitrepValidationError)
internal fun UnitSnapshot.displayName() = neutralPresentation(OperationUnitPresentation::displayName)
internal fun UnitSnapshot.navrepMenuLabel() = neutralPresentation(OperationUnitPresentation::navrepMenuLabel)
internal fun UnitSnapshot.navrepReportLabel() = neutralPresentation(OperationUnitPresentation::navrepReportLabel)
internal fun UnitSnapshot.unitClassLabel() = neutralPresentation(OperationUnitPresentation::unitClass)
private fun <T> UnitSnapshot.neutralPresentation(block: (OperationUnit, OperationContent) -> T): T =
    listOf(this).toOperationUnitContent().let { block(toOperationUnit(), it) }

internal val NAVREP_ELIGIBLE_CLASSES = OperationUnitPresentation.navrepEligibleClasses
internal val SENSOR_UNIT_CLASS_NAMES = OperationUnitPresentation.classNames
internal val SENSOR_UNIT_CLASS_BRACKETS = OperationUnitPresentation.classBrackets

internal object SensorDetection {
    fun visibleContacts(observer: UnitSnapshot, candidates: List<UnitSnapshot>, mapWidth: Int, mapHeight: Int, mapUnitsPerPixel: Double,
        gridLayer: GridLayer? = null, candidateFilter: (UnitSnapshot) -> Boolean = { it.template.affiliation == observer.template.affiliation },
        sensorReportMode: SensorReportMode = SensorReportMode.SCIREP, ecmIntensity: Int = 0) =
        visibleContacts(observer, KorgeUnitQueryAdapter(candidates, mapWidth, mapHeight, mapUnitsPerPixel), gridLayer, candidateFilter, sensorReportMode, ecmIntensity)

    fun visibleContacts(observer: UnitSnapshot, unitQueryService: KorgeUnitQueryAdapter, gridLayer: GridLayer? = null,
        candidateFilter: (UnitSnapshot) -> Boolean = { it.template.affiliation == observer.template.affiliation },
        sensorReportMode: SensorReportMode = SensorReportMode.SCIREP, ecmIntensity: Int = 0): List<SensorContact> =
        visibilityChecks(observer, unitQueryService, gridLayer, candidateFilter, sensorReportMode, ecmIntensity).filter { it.detected }
            .map { SensorContact(it.unit, it.distanceUnits, it.detectionRangeUnits, it.baseDetectionRangeUnits) }

    fun visibilityChecks(observer: UnitSnapshot, candidates: List<UnitSnapshot>, mapWidth: Int, mapHeight: Int, mapUnitsPerPixel: Double,
        gridLayer: GridLayer? = null, candidateFilter: (UnitSnapshot) -> Boolean = { it.template.affiliation == observer.template.affiliation },
        sensorReportMode: SensorReportMode = SensorReportMode.SCIREP, ecmIntensity: Int = 0) =
        visibilityChecks(observer, KorgeUnitQueryAdapter(candidates, mapWidth, mapHeight, mapUnitsPerPixel), gridLayer, candidateFilter, sensorReportMode, ecmIntensity)

    fun visibilityChecks(observer: UnitSnapshot, unitQueryService: KorgeUnitQueryAdapter, gridLayer: GridLayer? = null,
        candidateFilter: (UnitSnapshot) -> Boolean = { it.template.affiliation == observer.template.affiliation },
        sensorReportMode: SensorReportMode = SensorReportMode.SCIREP, ecmIntensity: Int = 0): List<SensorVisibilityCheck> {
        val included = unitQueryService.allUnits().filter(candidateFilter).mapTo(hashSetOf()) { it.id.value }
        return OperationSensorService.visibilityChecks(observer.toOperationUnit(), unitQueryService.operation, gridLayer,
            { it.id.value in included }, sensorReportMode.operation(), ecmIntensity).mapNotNull(unitQueryService::toKorge)
    }

    fun visibilityCheck(observer: UnitSnapshot, candidate: UnitSnapshot, distanceUnits: Double,
        sensorReportMode: SensorReportMode = SensorReportMode.SCIREP, ecmIntensity: Int = 0,
        observerProfile: SensorObserverProfile? = null): SensorVisibilityCheck? {
        val snapshots = listOf(observer, candidate)
        val content = snapshots.toOperationUnitContent()
        return OperationSensorService.visibilityCheck(observer.toOperationUnit(), candidate.toOperationUnit(), distanceUnits, content,
            sensorReportMode.operation(), ecmIntensity, observerProfile?.operation())?.toKorge(candidate)
    }

    fun visibilityReport(observer: UnitSnapshot, candidates: List<UnitSnapshot>, mapWidth: Int, mapHeight: Int, mapUnitsPerPixel: Double,
        gridLayer: GridLayer? = null, includedIds: Set<MapPlacedObjectId>? = null, unidentifiedIds: Set<MapPlacedObjectId> = emptySet(),
        candidateFilter: (UnitSnapshot) -> Boolean = { it.template.affiliation == observer.template.affiliation }, emptySummaryText: String = "No allied contacts to report.",
        sensorReportMode: SensorReportMode = SensorReportMode.SCIREP, ecmIntensity: Int = 0) =
        visibilityReport(observer, KorgeUnitQueryAdapter(candidates, mapWidth, mapHeight, mapUnitsPerPixel), gridLayer, includedIds, unidentifiedIds,
            candidateFilter, emptySummaryText, sensorReportMode, ecmIntensity)

    fun visibilityReport(observer: UnitSnapshot, unitQueryService: KorgeUnitQueryAdapter, gridLayer: GridLayer? = null,
        includedIds: Set<MapPlacedObjectId>? = null, unidentifiedIds: Set<MapPlacedObjectId> = emptySet(),
        candidateFilter: (UnitSnapshot) -> Boolean = { it.template.affiliation == observer.template.affiliation }, emptySummaryText: String = "No allied contacts to report.",
        sensorReportMode: SensorReportMode = SensorReportMode.SCIREP, ecmIntensity: Int = 0): SitrepVisibilityReport {
        val includedCandidates = unitQueryService.allUnits().filter(candidateFilter).mapTo(hashSetOf()) { it.id.value }
        val report = OperationSensorService.visibilityReport(observer.toOperationUnit(), unitQueryService.operation, gridLayer,
            includedIds?.mapTo(hashSetOf()) { OperationObjectId(it.value) }, unidentifiedIds.mapTo(hashSetOf()) { OperationObjectId(it.value) },
            { it.id.value in includedCandidates }, emptySummaryText, sensorReportMode.operation(), ecmIntensity)
        return report.korge(unitQueryService)
    }

    fun hiddenContactLabel(observer: UnitSnapshot, check: SensorVisibilityCheck, mapWidth: Int, mapHeight: Int, gridLayer: GridLayer? = null) =
        OperationSensorService.hiddenContactLabel(observer.toOperationUnit(), check.operation(), listOf(observer, check.unit).toOperationUnitContent(), mapWidth, mapHeight, gridLayer)

    fun scirepText(checks: List<SensorVisibilityCheck>, identifiedIds: Set<MapPlacedObjectId>, contactNumbers: Map<MapPlacedObjectId, Int>,
        mapWidth: Int, mapHeight: Int, gridLayer: GridLayer? = null, emptySummaryText: String, observerAffiliation: UnitAffiliation? = null,
        identificationAnnouncements: List<SensorVisibilityCheck> = emptyList(), previouslyDetectedIds: Set<MapPlacedObjectId> = emptySet(),
        lostContacts: List<SitrepLostContact> = emptyList(), lowConfidenceContacts: Map<MapPlacedObjectId, SitrepLostContact> = emptyMap(),
        sensorConditionsPoor: Boolean = false, electronicWarfare: Boolean = false): String {
        val content = checks.map { it.unit }.toOperationUnitContent()
        return OperationSensorService.scirepText(checks.map { it.operation() }, identifiedIds.operation(), contactNumbers.mapKeys { OperationObjectId(it.key.value) }, content,
            mapWidth, mapHeight, gridLayer, emptySummaryText, observerAffiliation?.operation(), identificationAnnouncements.map { it.operation() },
            previouslyDetectedIds.operation(), lostContacts.map { it.operation() }, lowConfidenceContacts.mapKeys { OperationObjectId(it.key.value) }.mapValues { it.value.operation() },
            sensorConditionsPoor, electronicWarfare)
    }

    fun lostContactText(lost: SitrepLostContact) = OperationSensorService.lostContactText(lost.operation())
    fun navrepObserverPositionText(observer: UnitSnapshot, mapWidth: Int, mapHeight: Int, gridLayer: GridLayer? = null) =
        OperationSensorService.navrepObserverPositionText(observer.toOperationUnit(), mapWidth, mapHeight, gridLayer)
    fun navrepIdentificationText(observer: UnitSnapshot, checks: List<SensorVisibilityCheck>, contactNumbers: Map<MapPlacedObjectId, Int>,
        mapWidth: Int, mapHeight: Int, gridLayer: GridLayer? = null, previouslyDetectedIds: Set<MapPlacedObjectId> = emptySet(),
        previouslyIdentifiedIds: Set<MapPlacedObjectId> = emptySet()): String {
        val content = (checks.map { it.unit } + observer).toOperationUnitContent()
        return OperationSensorService.navrepIdentificationText(observer.toOperationUnit(), checks.map { it.operation() },
            contactNumbers.mapKeys { OperationObjectId(it.key.value) }, content, mapWidth, mapHeight, gridLayer,
            previouslyDetectedIds.operation(), previouslyIdentifiedIds.operation())
    }
    fun combineScirepContactSegments(segments: List<ScirepContactSegment>) = OperationSensorService.combineScirepContactSegments(
        segments.map { OperationScirepContactSegment(OperationObjectId(it.contactId.value), it.sierraNumber, it.opening, it.clauses) })
}

private fun SensorReportMode.operation() = OperationSensorReportMode.valueOf(name)
private fun SensorObserverProfile.operation() = OperationSensorObserverProfile(unitClass, activeSensors, advancedSensorsBonus)
private fun UnitAffiliation.operation() = OperationUnitAffiliation.valueOf(name.uppercase())
private fun Set<MapPlacedObjectId>.operation() = mapTo(hashSetOf()) { OperationObjectId(it.value) }
internal fun SitrepLostContact.operation() = OperationLostContact(OperationObjectId(contactId.value), sierraNumber, lastSeenPosition, lastSeenRangeUnits,
    wasIdentified, unitTypeName, unitClassCode, wasFriendly, lostDueToEnemyAction, destroyedByFriendlyAction, identifiedAffiliation, unitInstanceName)
internal fun OperationLostContact.korge() = SitrepLostContact(MapPlacedObjectId(contactId.value), sierraNumber, lastSeenPosition, lastSeenRangeUnits,
    wasIdentified, unitTypeName, unitClassCode, wasFriendly, lostDueToEnemyAction, destroyedByFriendlyAction, identifiedAffiliation, unitInstanceName)
internal fun SitrepContactState.operation() = OperationSitrepContactState(
    sensorContactIds.operation(), identifiedIds.operation(), sierraContactNumbers.mapKeys { OperationObjectId(it.key.value) }, usedSierraContactNumbers,
    lastSeenPositions.mapKeys { OperationObjectId(it.key.value) }, lastSeenContacts.mapKeys { OperationObjectId(it.key.value) }.mapValues { it.value.operation() },
    lostContacts.map { it.operation() },
)
internal fun OperationSitrepContactState.korge() = SitrepContactState(
    sensorContactIds.mapTo(hashSetOf()) { MapPlacedObjectId(it.value) }, identifiedIds.mapTo(hashSetOf()) { MapPlacedObjectId(it.value) },
    sierraContactNumbers.mapKeys { MapPlacedObjectId(it.key.value) }, usedSierraContactNumbers,
    lastSeenPositions.mapKeys { MapPlacedObjectId(it.key.value) }, lastSeenContacts.mapKeys { MapPlacedObjectId(it.key.value) }.mapValues { it.value.korge() },
    lostContacts.map { it.korge() },
)
private fun SensorVisibilityCheck.operation() = OperationSensorVisibilityCheck(unit.toOperationUnit(), visible, detected, distanceUnits, identificationRangeUnits,
    detectionRangeUnits, baseDetectionRangeUnits, notes, naturalVisible, naturalDetected, ecmIntensity, ecmTransitionIntensity,
    OperationEcmSensorContact.valueOf(ecmContact.name), legacyEcmLevel)
private fun OperationSensorVisibilityCheck.toKorge(unit: UnitSnapshot) = SensorVisibilityCheck(unit, visible, detected, distanceUnits,
    identificationRangeUnits, detectionRangeUnits, baseDetectionRangeUnits, notes, naturalVisible, naturalDetected, ecmIntensity, ecmTransitionIntensity,
    EcmSensorContact.valueOf(ecmContact.name), legacyEcmLevel)
private fun KorgeUnitQueryAdapter.toKorge(check: OperationSensorVisibilityCheck) = allUnits().firstOrNull { it.id.value == check.unit.id.value }?.let(check::toKorge)
internal fun OperationSensorAssessment.korge(query: KorgeUnitQueryAdapter) = SitrepVisibilityReport(displayText, copyText,
    visibleChecks.mapNotNull(query::toKorge), detectedChecks.mapNotNull(query::toKorge), unidentifiedChecks.mapNotNull(query::toKorge),
    hiddenChecks.mapNotNull(query::toKorge), ecmGhostChecks.mapNotNull(query::toKorge))
