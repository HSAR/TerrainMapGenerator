package io.hsar.mapgenerator.commandgui.operation

import io.hsar.mapgenerator.commandgui.GridReferenceFormatter
import io.hsar.mapgenerator.commandgui.MapCoordinate
import io.hsar.mapgenerator.commandgui.operation.OperationSensorService.formatPosition
import io.hsar.mapgenerator.grid.GridLayer
import kotlin.math.pow
import kotlin.math.roundToInt

internal data class OperationSensorContact(
    val unit: OperationUnit,
    val distanceUnits: Double,
    val detectionRangeUnits: Double,
    val baseDetectionRangeUnits: Double,
)

internal enum class OperationSensorReportMode {
    SCIREP,
    NAVREP,
}

internal enum class OperationEcmSensorContact {
    NONE,
    FAINT_TRACK,
    SENSOR_GHOST,
}

internal data class OperationSensorObserverProfile(
    val unitClass: String?,
    val activeSensors: Boolean,
    val advancedSensorsBonus: Double,
) {
    companion object {
        val LOWEST_FIGHTER_SEEKER = OperationSensorObserverProfile("F", activeSensors = false, advancedSensorsBonus = 0.0)
    }
}

internal data class OperationSensorVisibilityCheck(
    val unit: OperationUnit,
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
    val ecmContact: OperationEcmSensorContact = OperationEcmSensorContact.NONE,
    val legacyEcmLevel: Int = 0,
)

internal data class OperationSensorAssessment(
    val displayText: String,
    val copyText: String,
    val visibleChecks: List<OperationSensorVisibilityCheck>,
    val detectedChecks: List<OperationSensorVisibilityCheck>,
    val unidentifiedChecks: List<OperationSensorVisibilityCheck> = emptyList(),
    val hiddenChecks: List<OperationSensorVisibilityCheck>,
    val ecmGhostChecks: List<OperationSensorVisibilityCheck> = emptyList(),
)

internal data class OperationSitrepContactState(
    val sensorContactIds: Set<OperationObjectId> = emptySet(),
    val identifiedIds: Set<OperationObjectId> = emptySet(),
    val sierraContactNumbers: Map<OperationObjectId, Int> = emptyMap(),
    val usedSierraContactNumbers: Set<Int> = emptySet(),
    val lastSeenPositions: Map<OperationObjectId, MapCoordinate> = emptyMap(),
    val lastSeenContacts: Map<OperationObjectId, OperationLostContact> = emptyMap(),
    val lostContacts: List<OperationLostContact> = emptyList(),
)

internal data class OperationLostContact(
    val contactId: OperationObjectId,
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

internal data class OperationScirepContactSegment(
    val contactId: OperationObjectId,
    val sierraNumber: Int,
    val opening: String,
    val clauses: List<String>,
)

internal val OPERATION_NAVREP_ELIGIBLE_CLASSES = OperationUnitPresentation.navrepEligibleClasses
internal val OPERATION_SENSOR_UNIT_CLASS_NAMES = OperationUnitPresentation.classNames
internal val OPERATION_SENSOR_UNIT_CLASS_BRACKETS = OperationUnitPresentation.classBrackets
private const val DETECTION_RANGE_BAND_MULTIPLIER = 2.0

private fun Map<String, String>.warpBonus(): Int? {
    val warpText = stringProperty("warp") ?: return null
    return when (warpText.lowercase()) {
        "max", "maximum", "full" -> 2
        "0", "false", "off" -> 0
        else -> warpText.toIntOrNull()?.takeIf { it > 0 }?.let { 1 }
    }
}

private fun Map<String, String>.booleanProperty(key: String): Boolean =
    stringProperty(key)?.lowercase() in setOf("true", "yes", "1", "on")

private fun Map<String, String>.intProperty(key: String): Int? =
    stringProperty(key)?.toIntOrNull()

private fun Map<String, String>.stringProperty(key: String): String? =
    entries.firstOrNull { it.key.equals(key, ignoreCase = true) }?.value?.trim()?.takeIf { it.isNotBlank() }

internal object OperationSensorService {
    fun visibleContacts(
        observer: OperationUnit,
        unitQueryService: OperationUnitQueryService,
        gridLayer: GridLayer? = null,
        candidateFilter: (OperationUnit) -> Boolean = { OperationUnitPresentation.affiliation(it, unitQueryService.content) == OperationUnitPresentation.affiliation(observer, unitQueryService.content) },
        sensorReportMode: OperationSensorReportMode = OperationSensorReportMode.SCIREP,
        ecmIntensity: Int = 0,
    ): List<OperationSensorContact> =
        visibilityChecks(observer, unitQueryService, gridLayer, candidateFilter, sensorReportMode, ecmIntensity)
            .filter { it.detected }
            .map {
                OperationSensorContact(
                    unit = it.unit,
                    distanceUnits = it.distanceUnits,
                    detectionRangeUnits = it.detectionRangeUnits,
                    baseDetectionRangeUnits = it.baseDetectionRangeUnits,
                )
            }

    fun visibilityChecks(
        observer: OperationUnit,
        unitQueryService: OperationUnitQueryService,
        gridLayer: GridLayer? = null,
        candidateFilter: (OperationUnit) -> Boolean = { OperationUnitPresentation.affiliation(it, unitQueryService.content) == OperationUnitPresentation.affiliation(observer, unitQueryService.content) },
        sensorReportMode: OperationSensorReportMode = OperationSensorReportMode.SCIREP,
        ecmIntensity: Int = 0,
    ): List<OperationSensorVisibilityCheck> =
        unitQueryService.allUnits().asSequence()
            .filter { it.id != observer.id }
            .filter(candidateFilter)
            .mapNotNull { candidate ->
                visibilityCheck(
                    observer, candidate, unitQueryService.distanceBetween(observer, candidate), unitQueryService.content, sensorReportMode, ecmIntensity,
                )
            }
            .sortedWith(
                compareBy<OperationSensorVisibilityCheck> { it.distanceUnits }
                    .thenBy(String.CASE_INSENSITIVE_ORDER) { OperationUnitPresentation.displayName(it.unit, unitQueryService.content) }
                    .thenBy { OperationUnitPresentation.unitClass(it.unit, unitQueryService.content) }
                    .thenBy { it.unit.id.value },
            )
            .toList()

    fun visibilityCheck(
        observer: OperationUnit,
        candidate: OperationUnit,
        distanceUnits: Double,
        content: OperationContent,
        sensorReportMode: OperationSensorReportMode = OperationSensorReportMode.SCIREP,
        ecmIntensity: Int = 0,
        observerProfile: OperationSensorObserverProfile? = null,
    ): OperationSensorVisibilityCheck? {
        val intensity = ecmIntensity.coerceIn(0, MAXIMUM_ECM_INTENSITY)
        val natural = candidate.sensorRanges(observer, content, sensorReportMode, 0, observerProfile) ?: return null
        val degraded = candidate.sensorRanges(observer, content, sensorReportMode, intensity, observerProfile) ?: return null
        val naturalVisible = candidate.transmissionInScope() || distanceUnits <= natural.identification
        val naturalDetected = candidate.transmissionInScope() || distanceUnits <= natural.detection
        val visible = candidate.transmissionInScope() || distanceUnits <= degraded.identification
        val detected = candidate.transmissionInScope() || distanceUnits <= degraded.detection
        val contact = when {
            naturalDetected && !detected -> OperationEcmSensorContact.SENSOR_GHOST
            naturalVisible && !visible && detected -> OperationEcmSensorContact.FAINT_TRACK
            else -> OperationEcmSensorContact.NONE
        }
        val transitionIntensity = when (contact) {
            OperationEcmSensorContact.FAINT_TRACK -> (1..intensity).firstOrNull { level ->
                candidate.sensorRanges(observer, content, sensorReportMode, level, observerProfile)
                    ?.let { ranges -> distanceUnits > ranges.identification && distanceUnits <= ranges.detection } == true
            }

            OperationEcmSensorContact.SENSOR_GHOST -> (1..intensity).firstOrNull { level ->
                candidate.sensorRanges(observer, content, sensorReportMode, level, observerProfile)
                    ?.let { ranges -> distanceUnits > ranges.detection } == true
            }

            OperationEcmSensorContact.NONE -> null
        }
        val legacyLevel = transitionIntensity?.let { transition ->
            ((if (contact == OperationEcmSensorContact.SENSOR_GHOST) 2 else 1) + intensity - transition)
                .coerceAtMost(MAXIMUM_ECM_INTENSITY)
        } ?: 0
        val notes = candidate.visibilityNotes(content, sensorReportMode) + buildList {
            if (intensity > 0) add("Electronic warfare: -${ecmBandPenalty(intensity, observerProfile?.activeSensors ?: observer.activeSensors())} detection bands.")
            add(if (detected) "Detected within range." else "Outside detection range.")
        }
        return OperationSensorVisibilityCheck(
            unit = candidate,
            visible = visible,
            detected = detected,
            distanceUnits = distanceUnits,
            identificationRangeUnits = degraded.identification,
            detectionRangeUnits = degraded.detection,
            baseDetectionRangeUnits = candidate.effectiveSensorBand(content).baseDetectionRangeUnits,
            notes = if (candidate.transmissionInScope()) notes + "Transmitting in scope: automatically detected." else notes,
            naturalVisible = naturalVisible,
            naturalDetected = naturalDetected,
            ecmIntensity = intensity,
            ecmTransitionIntensity = transitionIntensity,
            ecmContact = contact,
            legacyEcmLevel = legacyLevel,
        )
    }

    fun visibilityReport(
        observer: OperationUnit,
        unitQueryService: OperationUnitQueryService,
        gridLayer: GridLayer? = null,
        includedIds: Set<OperationObjectId>? = null,
        unidentifiedIds: Set<OperationObjectId> = emptySet(),
        candidateFilter: (OperationUnit) -> Boolean = { OperationUnitPresentation.affiliation(it, unitQueryService.content) == OperationUnitPresentation.affiliation(observer, unitQueryService.content) },
        emptySummaryText: String = "No allied contacts to report.",
        sensorReportMode: OperationSensorReportMode = OperationSensorReportMode.SCIREP,
        ecmIntensity: Int = 0,
    ): OperationSensorAssessment {
        val checks = visibilityChecks(observer, unitQueryService, gridLayer, candidateFilter, sensorReportMode, ecmIntensity)
        val effectiveIncludedIds = includedIds ?: checks.filter { it.visible }.mapTo(mutableSetOf()) { it.unit.id }
        val visibleChecks = checks.filter { it.unit.id in effectiveIncludedIds }
        val unidentifiedChecks = checks.filter { it.unit.id in unidentifiedIds && it.unit.id !in effectiveIncludedIds }
        val hiddenChecks = checks.filterNot { it.unit.id in effectiveIncludedIds || it.unit.id in unidentifiedIds }
        val lines = buildList {
            add(buildPositionSummary(observer, visibleChecks, unitQueryService.content, unitQueryService.mapWidth, unitQueryService.mapHeight, gridLayer, emptySummaryText))
        }
        val reportText = lines.joinToString("\n")
        val navrepConditions = if (sensorReportMode == OperationSensorReportMode.NAVREP) buildList {
            if (observer.customProperties.booleanProperty("inNebula")) add(NEBULAR_INTERFERENCE_CONDITIONS)
            if (ecmIntensity > 0) add(ELECTRONIC_WARFARE_CONDITIONS)
        } else emptyList()
        val displayText = (navrepConditions + reportText).joinToString("\n")
        return OperationSensorAssessment(
            displayText = displayText,
            copyText = displayText,
            visibleChecks = visibleChecks,
            detectedChecks = checks.filter { it.detected },
            unidentifiedChecks = unidentifiedChecks,
            hiddenChecks = hiddenChecks,
            ecmGhostChecks = checks.filter { it.ecmContact == OperationEcmSensorContact.SENSOR_GHOST },
        )
    }

    fun hiddenContactLabel(
        observer: OperationUnit,
        check: OperationSensorVisibilityCheck,
        content: OperationContent,
        mapWidth: Int,
        mapHeight: Int,
        gridLayer: GridLayer? = null,
    ): String =
        buildHiddenLine(observer, check, content, mapWidth, mapHeight, gridLayer)

    fun scirepText(
        checks: List<OperationSensorVisibilityCheck>,
        identifiedIds: Set<OperationObjectId>,
        contactNumbers: Map<OperationObjectId, Int>,
        content: OperationContent,
        mapWidth: Int,
        mapHeight: Int,
        gridLayer: GridLayer? = null,
        emptySummaryText: String,
        observerAffiliation: OperationUnitAffiliation? = null,
        identificationAnnouncements: List<OperationSensorVisibilityCheck> = emptyList(),
        previouslyDetectedIds: Set<OperationObjectId> = emptySet(),
        lostContacts: List<OperationLostContact> = emptyList(),
        lowConfidenceContacts: Map<OperationObjectId, OperationLostContact> = emptyMap(),
        sensorConditionsPoor: Boolean = false,
        electronicWarfare: Boolean = false,
    ): String {
        val report = if (checks.isEmpty() && lostContacts.isEmpty()) {
            emptySummaryText
        } else {
            val newlyIdentifiedIds = identificationAnnouncements.map { it.unit.id }.toSet()
            combineScirepContactSegments(
                checks.map { check ->
                    val lowConfidence = lowConfidenceContacts[check.unit.id]
                    val designation = if (check.unit.id in identifiedIds) "Master" else "Sierra"
                    val newlyIdentified = check.unit.id in newlyIdentifiedIds
                    val newlyDetected = check.unit.id !in previouslyDetectedIds
                    val rangeDescriptor = if (check.distanceUnits > check.identificationRangeUnits) " at extreme range" else ""
                    val opening = if (lowConfidence != null) {
                        "New contact tentatively designated $designation ${contactNumbers.getValue(check.unit.id)} from last known position"
                    } else if (newlyIdentified && newlyDetected && check.unit.id in identifiedIds) {
                        "Contact detected$rangeDescriptor as Master ${contactNumbers.getValue(check.unit.id)}"
                    } else if (check.unit.id !in identifiedIds) {
                        val contactPrefix = if (check.unit.customProperties.booleanProperty("activeSensors")) {
                            "Contact actively radiating"
                        } else {
                            "Contact detected"
                        }
                        "$contactPrefix$rangeDescriptor as Sierra ${contactNumbers.getValue(check.unit.id)}"
                    } else {
                        "Contact Master ${contactNumbers.getValue(check.unit.id)}"
                    }
                    val classification = when {
                        check.ecmContact == OperationEcmSensorContact.FAINT_TRACK -> "is faint sensor track"
                        check.ecmContact == OperationEcmSensorContact.SENSOR_GHOST -> "is sensor ghost"
                        check.unit.id in identifiedIds && lowConfidence != null ->
                            check.unit.identificationClause(content, "was", observerAffiliation)

                        newlyIdentified -> check.unit.identificationClause(content, observerAffiliation = observerAffiliation)

                        check.unit.id in identifiedIds ->
                            check.unit.identificationClause(content, "was previously", observerAffiliation)

                        else -> "has unknown ${check.unit.unknownClassificationBracket(content)} classification"
                    }
                    OperationScirepContactSegment(
                        contactId = check.unit.id,
                        sierraNumber = contactNumbers.getValue(check.unit.id),
                        opening = opening,
                        clauses = listOf(
                            "at ${
                                lowConfidence?.let { formatPosition(it.lastSeenPosition, mapWidth, mapHeight, gridLayer) } ?: formatPosition(
                                    check.unit.coordinate,
                                    mapWidth,
                                    mapHeight,
                                    gridLayer
                                )
                            }, " +
                                    "range ${formatDistance(lowConfidence?.lastSeenRangeUnits ?: check.distanceUnits)}U",
                            classification,
                        ),
                    )
                } + lostContacts.map { lost ->
                    OperationScirepContactSegment(
                        contactId = lost.contactId,
                        sierraNumber = lost.sierraNumber,
                        opening = lostContactText(lost),
                        clauses = emptyList(),
                    )
                },
            )
        }
        val conditions = buildList {
            if (sensorConditionsPoor) add(NEBULAR_INTERFERENCE_CONDITIONS)
            if (electronicWarfare) add(ELECTRONIC_WARFARE_CONDITIONS)
        }
        return (conditions + report).joinToString("\n")
    }

    fun lostContactText(lost: OperationLostContact): String =
        buildString {
            append("Contact Sierra ${lost.sierraNumber} lost")
            if (lost.lostDueToEnemyAction) append(" due to enemy action")
            else if (lost.destroyedByFriendlyAction) append(", destroyed by friendly action")
            if (lost.wasIdentified) {
                append(" was previously identified as ")
                append(lost.identifiedAffiliation ?: if (lost.wasFriendly) "allied" else "unknown")
                append(' ')
                append(lost.unitTypeName?.ifBlank { "Unknown" } ?: "Unknown")
                append("-class ")
                append(lost.unitClassCode ?: "UKN")
                lost.unitInstanceName?.takeIf { it.isNotBlank() }?.let { append(' ').append(it) }
            }
        }

    fun navrepObserverPositionText(
        observer: OperationUnit,
        mapWidth: Int,
        mapHeight: Int,
        gridLayer: GridLayer? = null,
    ): String =
        "Position ${formatPosition(observer.coordinate, mapWidth, mapHeight, gridLayer)} bearing ${formatBearing(observer.facingDegrees)}."

    fun navrepIdentificationText(
        observer: OperationUnit,
        checks: List<OperationSensorVisibilityCheck>,
        contactNumbers: Map<OperationObjectId, Int>,
        content: OperationContent,
        mapWidth: Int,
        mapHeight: Int,
        gridLayer: GridLayer? = null,
        previouslyDetectedIds: Set<OperationObjectId> = emptySet(),
        previouslyIdentifiedIds: Set<OperationObjectId> = emptySet(),
    ): String =
        checks
            .sortedBy { contactNumbers.getValue(it.unit.id) }
            .joinToString("\n") { check ->
                val position = formatPosition(check.unit.coordinate, mapWidth, mapHeight, gridLayer)
                if (check.unit.id in previouslyDetectedIds) {
                    "Contact Sierra ${contactNumbers.getValue(check.unit.id)} at $position bearing ${formatBearing(check.unit.facingDegrees)} ${
                        check.unit.navrepIdentificationClause(
                            observer,
                            content,
                            wasPreviouslyIdentified = check.unit.id in previouslyIdentifiedIds,
                        )
                    }."
                } else {
                    "Contact detected at $position ${check.unit.navrepIdentificationClause(observer, content)}."
                }
            }

    fun combineScirepContactSegments(segments: List<OperationScirepContactSegment>): String =
        segments
            .groupBy { it.contactId }
            .values
            .sortedBy { contactSegments -> contactSegments.minOf { it.sierraNumber } }
            .joinToString("\n") { contactSegments ->
                val first = contactSegments.first()
                val clauses = contactSegments.flatMap { it.clauses }
                buildString {
                    append(first.opening)
                    clauses.forEach { clause ->
                        append(' ')
                        append(clause.trim().trimEnd('.', '!', '?'))
                    }
                    append('.')
                }
            }

    private fun OperationUnit.identificationClause(
        content: OperationContent,
        verb: String = "is",
        observerAffiliation: OperationUnitAffiliation? = null,
    ): String =
        "$verb identified as ${affiliationDescription(content, observerAffiliation)} ${identificationDescription(content)}"

    private fun OperationUnit.navrepIdentificationClause(
        observer: OperationUnit,
        content: OperationContent,
        wasPreviouslyIdentified: Boolean = false,
    ): String =
        "${if (wasPreviouslyIdentified) "was previously" else "is"} identified as ${
            if (OperationUnitPresentation.affiliation(this, content) == OperationUnitPresentation.affiliation(observer, content)) "allied" else affiliationDescription(content)
        } ${identificationDescription(content)}"

    private fun OperationUnit.affiliationDescription(content: OperationContent, observerAffiliation: OperationUnitAffiliation? = null): String =
        when (OperationUnitPresentation.affiliation(this, content)) {
            OperationUnitAffiliation.FRIEND -> if (observerAffiliation == OperationUnitAffiliation.HOSTILE) "enemy" else "allied"
            OperationUnitAffiliation.HOSTILE -> "enemy"
            OperationUnitAffiliation.NEUTRAL -> "neutral"
            OperationUnitAffiliation.UNKNOWN -> "unknown"
            null -> "unaffiliated"
        }

    private fun OperationUnit.identificationDescription(content: OperationContent): String =
        buildString {
            append(content.unitTemplate(unitTemplateId)?.name?.ifBlank { "Unknown" } ?: "Unknown")
            append("-class ")
            append(OperationUnitPresentation.unitClass(this@identificationDescription, content) ?: "UKN")
            if (instanceName.isNotBlank() && instanceName != content.unitTemplate(unitTemplateId)?.name) {
                append(' ')
                append(OperationUnitPresentation.displayName(this@identificationDescription, content))
            }
        }

    private fun OperationUnit.unknownClassificationBracket(content: OperationContent): String = effectiveSensorBand(content).label

    private enum class SensorBand(
        val baseDetectionRangeUnits: Double,
        val observerModifier: Double,
        val label: String,
    ) {
        LIGHTWEIGHT(6.0, 2.0 / 3.0, "lightweight"),
        MIDWEIGHT(12.0, 1.0, "midweight"),
        HEAVYWEIGHT(24.0, 1.5, "heavyweight"),
    }

    private fun OperationUnit.effectiveSensorBand(content: OperationContent): SensorBand {
        val classBand = when (OperationUnitPresentation.unitClass(this, content)) {
            "FF", "F" -> SensorBand.LIGHTWEIGHT
            "DD", "CL" -> SensorBand.MIDWEIGHT
            "CA", "CVE", "CVL", "CV", "BC" -> SensorBand.HEAVYWEIGHT
            else -> null
        }
        return classBand ?: when (OperationUnitPresentation.mass(this, content)) {
            in 0..29 -> SensorBand.LIGHTWEIGHT
            in 30..59 -> SensorBand.MIDWEIGHT
            in 60..Int.MAX_VALUE -> SensorBand.HEAVYWEIGHT
            else -> SensorBand.LIGHTWEIGHT
        }
    }

    private data class SensorRanges(val identification: Double, val detection: Double)

    private fun OperationUnit.sensorRanges(
        observer: OperationUnit,
        content: OperationContent,
        sensorReportMode: OperationSensorReportMode,
        ecmIntensity: Int,
        observerProfile: OperationSensorObserverProfile?,
    ): SensorRanges? {
        val base = effectiveSensorBand(content).baseDetectionRangeUnits
        val observerModifier = observerProfile?.unitClass
            ?.let { observerProfile.unitClassSensorBand().observerModifier }
            ?: observer.effectiveSensorBand(content).observerModifier
        val activeSensors = observerProfile?.activeSensors ?: observer.activeSensors()

        val bandOffset = buildList {
            customProperties.warpBonus()?.let(::add)
            if (sensorReportMode == OperationSensorReportMode.NAVREP && customProperties.booleanProperty("activeWarp")) add(2)
            if (customProperties.booleanProperty("transmitting") && !customProperties.booleanProperty("transmissionInScope")) {
                add(1)
            }
            repeat(customProperties.intProperty("nebula") ?: 0) {
                add(-1)
            }
            if (sensorReportMode == OperationSensorReportMode.NAVREP && customProperties.booleanProperty("inNebula")) add(-1)
            if (sensorReportMode == OperationSensorReportMode.SCIREP && customProperties.booleanProperty("silentRunning")) add(-1)
            if (sensorReportMode == OperationSensorReportMode.SCIREP && customProperties.booleanProperty("activeSensors")) add(1)
        }.sum()

        if (customProperties.booleanProperty("transmissionInScope")) {
            return SensorRanges(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY)
        }

        val observerSensorModifier = buildList {
            if (sensorReportMode == OperationSensorReportMode.SCIREP && activeSensors) add(1)
            if (sensorReportMode == OperationSensorReportMode.NAVREP && observer.customProperties.booleanProperty("inNebula")) add(-1)
            if (ecmIntensity > 0) add(-ecmBandPenalty(ecmIntensity, activeSensors))
        }.sum()
        val advancedSensors = observerProfile?.advancedSensorsBonus ?: observer.advancedSensorsBonus(content)
        val identification = (base * observerModifier) * 2.0.pow(bandOffset + observerSensorModifier) + advancedSensors
        return SensorRanges(
            identification = identification,
            detection = if (customProperties.booleanProperty("maximumWarp")) Double.POSITIVE_INFINITY else identification * DETECTION_RANGE_BAND_MULTIPLIER,
        )
    }

    private fun OperationUnit.activeSensors(): Boolean = customProperties.booleanProperty("activeSensors")

    private fun ecmBandPenalty(intensity: Int, activeSensors: Boolean): Int =
        intensity.coerceIn(1, MAXIMUM_ECM_INTENSITY) + if (activeSensors) 0 else 1

    private fun OperationUnit.transmissionInScope(): Boolean =
        customProperties.booleanProperty("transmissionInScope") || customProperties.booleanProperty("transmittingInScope")

    private fun OperationUnit.advancedSensorsBonus(content: OperationContent): Double {
        val definition = OlympiadDawnShip.definitionFor(this, content) ?: return 0.0
        val damage = OlympiadDawnShip.instanceDamage(customProperties, definition)
        return if (definition.systems.any { it.name == "Advanced Sensors" && it.id !in damage.destroyedSystems }) 1.0 else 0.0
    }

    private fun OperationUnit.visibilityNotes(content: OperationContent, sensorReportMode: OperationSensorReportMode): List<String> =
        buildList {
            OperationUnitPresentation.unitClass(this@visibilityNotes, content)?.let { add("Class $it baseline.") }
            if (sensorReportMode == OperationSensorReportMode.SCIREP && customProperties.booleanProperty("activeSensors")) {
                add("Active sensors: +100% range, +1 signature.")
            }
            customProperties.warpBonus()?.let {
                add(
                    when (it) {
                        2 -> "Warp: +2 signature, extreme at maximum warp."
                        1 -> "Warp: +1 signature."
                        else -> "Warp engaged."
                    },
                )
            }
            if (customProperties.booleanProperty("transmitting")) {
                add("Transmitting: automatically detected if in cone and range.")
            }
            if (sensorReportMode == OperationSensorReportMode.SCIREP && customProperties.booleanProperty("silentRunning")) {
                add("Silent running: -1 signature, drive rating halved.")
            }
            customProperties.intProperty("nebula")?.takeIf { it > 0 }?.let {
                add("Nebula: -$it signature.")
            }
            if (sensorReportMode == OperationSensorReportMode.NAVREP && customProperties.booleanProperty("activeWarp")) {
                add("Active warp: +2 detection bands.")
            }
            if (customProperties.booleanProperty("maximumWarp")) {
                add("Maximum warp: automatically detected at any range.")
            }
            if (sensorReportMode == OperationSensorReportMode.NAVREP && customProperties.booleanProperty("inNebula")) {
                add("In nebula: -1 detection band.")
            }
        }

    private fun formatDistance(distanceUnits: Double): String =
        if (distanceUnits.isInfinite()) "∞" else ((distanceUnits * 10.0).roundToInt() / 10.0).toString().trimEnd('0').trimEnd('.')

    private fun buildPositionSummary(
        observer: OperationUnit,
        visibleChecks: List<OperationSensorVisibilityCheck>,
        content: OperationContent,
        mapWidth: Int,
        mapHeight: Int,
        gridLayer: GridLayer?,
        emptySummaryText: String,
    ): String =
        buildString {
            append(navrepObserverPositionText(observer, mapWidth, mapHeight, gridLayer).trimEnd('.'))
            if (visibleChecks.isEmpty()) {
                append('.')
                if (emptySummaryText.isNotBlank()) {
                    append('\n')
                    append(emptySummaryText)
                }
            } else {
                append(".\n")
                visibleChecks.forEachIndexed { index, check ->
                    if (index > 0) {
                        append('\n')
                    }
                    append(OperationUnitPresentation.navrepReportLabel(check.unit, content))
                    append(" has position ")
                    append(formatPosition(check.unit.coordinate, mapWidth, mapHeight, gridLayer))
                    append(" bearing ")
                    append(formatBearing(check.unit.facingDegrees))
                    append('.')
                }
            }
        }

    private fun buildHiddenLine(
        observer: OperationUnit,
        check: OperationSensorVisibilityCheck,
        content: OperationContent,
        mapWidth: Int,
        mapHeight: Int,
        gridLayer: GridLayer?,
    ): String =
        buildString {
            append(OperationUnitPresentation.navrepReportLabel(check.unit, content))
            append(" contact ranges are ")
            append(formatDistance(check.detectionRangeUnits))
            append(" / ")
            append(formatDistance(check.identificationRangeUnits))
            append(", actual range is ")
            append(formatDistance(check.distanceUnits))
        }

    private fun formatPosition(
        position: MapCoordinate,
        mapWidth: Int,
        mapHeight: Int,
        gridLayer: GridLayer?,
    ): String =
        gridLayer
            ?.let { layer ->
                GridReferenceFormatter.format(
                    layer = layer,
                    mapWidth = mapWidth,
                    mapHeight = mapHeight,
                    mapX = position.x * mapWidth,
                    mapY = position.y * mapHeight,
                )
            }
            ?: "${formatDistance(position.x * mapWidth)} ${formatDistance(position.y * mapHeight)}"

    private fun formatBearing(facingDegrees: Int?): String =
        facingDegrees?.toString() ?: "unknown"

    private fun OperationSensorObserverProfile.unitClassSensorBand(): SensorBand =
        when (unitClass) {
            "FF", "F" -> SensorBand.LIGHTWEIGHT
            "DD", "CL" -> SensorBand.MIDWEIGHT
            "CA", "CVE", "CVL", "CV", "BC" -> SensorBand.HEAVYWEIGHT
            else -> SensorBand.MIDWEIGHT
        }

    private fun String?.sensorRangeSummary(): String =
        when (this) {
            "FF", "F" -> "-33%"
            "CA", "CVE", "CVL", "BC" -> "+50%"
            else -> "baseline"
        }

    const val NEBULAR_INTERFERENCE_CONDITIONS = "Poor sensor conditions due to nebular interference."
    const val ELECTRONIC_WARFARE_CONDITIONS = "Poor sensor conditions due to electronic warfare."
    private const val MAXIMUM_ECM_INTENSITY = 4

}
