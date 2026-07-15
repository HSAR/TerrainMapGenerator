package io.hsar.mapgenerator.commandgui.korge.interaction

import io.hsar.mapgenerator.grid.GridLayer
import kotlin.math.pow
import kotlin.math.roundToInt

internal data class SensorContact(
    val unit: UnitSnapshot,
    val distanceUnits: Double,
    val detectionRangeUnits: Double,
    val baseDetectionRangeUnits: Double,
)

internal enum class SensorReportMode {
    SCIREP,
    NAVREP,
}

internal enum class EcmSensorContact {
    NONE,
    FAINT_TRACK,
    SENSOR_GHOST,
}

internal data class SensorObserverProfile(
    val unitClass: String?,
    val activeSensors: Boolean,
    val advancedSensorsBonus: Double,
) {
    companion object {
        val LOWEST_FIGHTER_SEEKER = SensorObserverProfile("F", activeSensors = false, advancedSensorsBonus = 0.0)
    }
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
)

internal data class ScirepContactSegment(
    val contactId: MapPlacedObjectId,
    val sierraNumber: Int,
    val opening: String,
    val clauses: List<String>,
)

internal fun UnitSnapshot.isNavrepUnit(): Boolean =
    unitClassLabel() in NAVREP_ELIGIBLE_CLASSES

internal fun UnitSnapshot.sitrepValidationError(): String? {
    val unitClass = unitClassLabel() ?: return "This unit has no unitClass setting."
    return unitClass.takeUnless { it in SENSOR_UNIT_CLASS_NAMES }
        ?.let { "The unitClass value \"$it\" is not recognised." }
}

internal fun UnitSnapshot.displayName(): String =
    name.ifBlank { template.name }

internal fun UnitSnapshot.navrepMenuLabel(): String =
    buildString {
        append(displayName())
        unitClassLabel()?.let { append(" [").append(it).append(']') }
    }

internal fun UnitSnapshot.navrepReportLabel(): String =
    buildString {
        unitClassLabel()?.let { append(it).append(' ') }
        append(displayName())
    }

internal fun UnitSnapshot.unitClassLabel(): String? =
    (customProperties.stringProperty(UNIT_CLASS_KEY)
        ?: template.customProperties.stringProperty(UNIT_CLASS_KEY))
        ?.uppercase()

private const val UNIT_CLASS_KEY = "unitClass"
internal val NAVREP_ELIGIBLE_CLASSES = setOf("FF", "F", "DD", "CL", "CA", "CVE", "CVL", "BC")
internal val SENSOR_UNIT_CLASS_NAMES = mapOf(
    "FF" to "frigate",
    "F" to "fighter",
    "DD" to "destroyer",
    "CL" to "light cruiser",
    "CA" to "heavy cruiser",
    "CVE" to "escort carrier",
    "CVL" to "light carrier",
    "CV" to "carrier",
    "BC" to "battlecruiser",
    "AK" to "Cargo Ship",
)
internal val SENSOR_UNIT_CLASS_BRACKETS = mapOf(
    "F" to "lightweight",
    "FF" to "lightweight",
    "DD" to "midweight",
    "CL" to "midweight",
    "CA" to "heavyweight",
    "CVE" to "heavyweight",
    "CVL" to "heavyweight",
    "CV" to "heavyweight",
    "BC" to "heavyweight",
)
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

internal object SensorDetection {
    fun visibleContacts(
        observer: UnitSnapshot,
        candidates: List<UnitSnapshot>,
        mapWidth: Int,
        mapHeight: Int,
        mapUnitsPerPixel: Double,
        gridLayer: GridLayer? = null,
        candidateFilter: (UnitSnapshot) -> Boolean = { it.template.affiliation == observer.template.affiliation },
        sensorReportMode: SensorReportMode = SensorReportMode.SCIREP,
        ecmIntensity: Int = 0,
    ): List<SensorContact> =
        visibleContacts(observer, UnitQueryService(candidates, mapWidth, mapHeight, mapUnitsPerPixel), gridLayer, candidateFilter, sensorReportMode, ecmIntensity)

    fun visibleContacts(
        observer: UnitSnapshot,
        unitQueryService: UnitQueryService,
        gridLayer: GridLayer? = null,
        candidateFilter: (UnitSnapshot) -> Boolean = { it.template.affiliation == observer.template.affiliation },
        sensorReportMode: SensorReportMode = SensorReportMode.SCIREP,
        ecmIntensity: Int = 0,
    ): List<SensorContact> =
        visibilityChecks(observer, unitQueryService, gridLayer, candidateFilter, sensorReportMode, ecmIntensity)
            .filter { it.detected }
            .map {
                SensorContact(
                    unit = it.unit,
                    distanceUnits = it.distanceUnits,
                    detectionRangeUnits = it.detectionRangeUnits,
                    baseDetectionRangeUnits = it.baseDetectionRangeUnits,
                )
            }

    fun visibilityChecks(
        observer: UnitSnapshot,
        unitQueryService: UnitQueryService,
        gridLayer: GridLayer? = null,
        candidateFilter: (UnitSnapshot) -> Boolean = { it.template.affiliation == observer.template.affiliation },
        sensorReportMode: SensorReportMode = SensorReportMode.SCIREP,
        ecmIntensity: Int = 0,
    ): List<SensorVisibilityCheck> =
        unitQueryService.allUnits().asSequence()
            .filter { it.id != observer.id }
            .filter(candidateFilter)
            .mapNotNull { candidate ->
                visibilityCheck(
                    observer, candidate, unitQueryService.distanceBetween(observer, candidate), sensorReportMode, ecmIntensity,
                )
            }
            .sortedWith(
                compareBy<SensorVisibilityCheck> { it.distanceUnits }
                    .thenBy { it.unit.instanceName.lowercase() }
                    .thenBy { it.unit.unitClassLabel() },
            )
            .toList()

    fun visibilityChecks(
        observer: UnitSnapshot,
        candidates: List<UnitSnapshot>,
        mapWidth: Int,
        mapHeight: Int,
        mapUnitsPerPixel: Double,
        gridLayer: GridLayer? = null,
        candidateFilter: (UnitSnapshot) -> Boolean = { it.template.affiliation == observer.template.affiliation },
        sensorReportMode: SensorReportMode = SensorReportMode.SCIREP,
        ecmIntensity: Int = 0,
    ): List<SensorVisibilityCheck> =
        visibilityChecks(observer, UnitQueryService(candidates, mapWidth, mapHeight, mapUnitsPerPixel), gridLayer, candidateFilter, sensorReportMode, ecmIntensity)

    fun visibilityCheck(
        observer: UnitSnapshot,
        candidate: UnitSnapshot,
        distanceUnits: Double,
        sensorReportMode: SensorReportMode = SensorReportMode.SCIREP,
        ecmIntensity: Int = 0,
        observerProfile: SensorObserverProfile? = null,
    ): SensorVisibilityCheck? {
        val intensity = ecmIntensity.coerceIn(0, MAXIMUM_ECM_INTENSITY)
        val natural = candidate.sensorRanges(observer, sensorReportMode, 0, observerProfile) ?: return null
        val degraded = candidate.sensorRanges(observer, sensorReportMode, intensity, observerProfile) ?: return null
        val naturalVisible = candidate.transmissionInScope() || distanceUnits <= natural.identification
        val naturalDetected = candidate.transmissionInScope() || distanceUnits <= natural.detection
        val visible = candidate.transmissionInScope() || distanceUnits <= degraded.identification
        val detected = candidate.transmissionInScope() || distanceUnits <= degraded.detection
        val contact = when {
            naturalDetected && !detected -> EcmSensorContact.SENSOR_GHOST
            naturalVisible && !visible && detected -> EcmSensorContact.FAINT_TRACK
            else -> EcmSensorContact.NONE
        }
        val transitionIntensity = when (contact) {
            EcmSensorContact.FAINT_TRACK -> (1..intensity).firstOrNull { level ->
                candidate.sensorRanges(observer, sensorReportMode, level, observerProfile)
                    ?.let { ranges -> distanceUnits > ranges.identification && distanceUnits <= ranges.detection } == true
            }

            EcmSensorContact.SENSOR_GHOST -> (1..intensity).firstOrNull { level ->
                candidate.sensorRanges(observer, sensorReportMode, level, observerProfile)
                    ?.let { ranges -> distanceUnits > ranges.detection } == true
            }

            EcmSensorContact.NONE -> null
        }
        val legacyLevel = transitionIntensity?.let { transition ->
            ((if (contact == EcmSensorContact.SENSOR_GHOST) 2 else 1) + intensity - transition)
                .coerceAtMost(MAXIMUM_ECM_INTENSITY)
        } ?: 0
        val notes = candidate.visibilityNotes(sensorReportMode) + buildList {
            if (intensity > 0) add("Electronic warfare: -${ecmBandPenalty(intensity, observerProfile?.activeSensors ?: observer.activeSensors())} detection bands.")
            add(if (detected) "Detected within range." else "Outside detection range.")
        }
        return SensorVisibilityCheck(
            unit = candidate,
            visible = visible,
            detected = detected,
            distanceUnits = distanceUnits,
            identificationRangeUnits = degraded.identification,
            detectionRangeUnits = degraded.detection,
            baseDetectionRangeUnits = candidate.unitClassLabel().baseDetectionRangeUnits(),
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
        observer: UnitSnapshot,
        unitQueryService: UnitQueryService,
        gridLayer: GridLayer? = null,
        includedIds: Set<MapPlacedObjectId>? = null,
        unidentifiedIds: Set<MapPlacedObjectId> = emptySet(),
        candidateFilter: (UnitSnapshot) -> Boolean = { it.template.affiliation == observer.template.affiliation },
        emptySummaryText: String = "No allied contacts to report.",
        sensorReportMode: SensorReportMode = SensorReportMode.SCIREP,
        ecmIntensity: Int = 0,
    ): SitrepVisibilityReport {
        val checks = visibilityChecks(observer, unitQueryService, gridLayer, candidateFilter, sensorReportMode, ecmIntensity)
        val effectiveIncludedIds = includedIds ?: checks.filter { it.visible }.mapTo(mutableSetOf()) { it.unit.id }
        val visibleChecks = checks.filter { it.unit.id in effectiveIncludedIds }
        val unidentifiedChecks = checks.filter { it.unit.id in unidentifiedIds && it.unit.id !in effectiveIncludedIds }
        val hiddenChecks = checks.filterNot { it.unit.id in effectiveIncludedIds || it.unit.id in unidentifiedIds }
        val lines = buildList {
            add(buildPositionSummary(observer, visibleChecks, unitQueryService.mapWidth, unitQueryService.mapHeight, gridLayer, emptySummaryText))
        }
        val reportText = lines.joinToString("\n")
        val navrepConditions = if (sensorReportMode == SensorReportMode.NAVREP) buildList {
            if (observer.customProperties.booleanProperty("inNebula")) add(NEBULAR_INTERFERENCE_CONDITIONS)
            if (ecmIntensity > 0) add(ELECTRONIC_WARFARE_CONDITIONS)
        } else emptyList()
        val displayText = (navrepConditions + reportText).joinToString(" ")
        return SitrepVisibilityReport(
            displayText = displayText,
            copyText = displayText,
            visibleChecks = visibleChecks,
            detectedChecks = checks.filter { it.detected },
            unidentifiedChecks = unidentifiedChecks,
            hiddenChecks = hiddenChecks,
            ecmGhostChecks = checks.filter { it.ecmContact == EcmSensorContact.SENSOR_GHOST },
        )
    }

    fun visibilityReport(
        observer: UnitSnapshot,
        candidates: List<UnitSnapshot>,
        mapWidth: Int,
        mapHeight: Int,
        mapUnitsPerPixel: Double,
        gridLayer: GridLayer? = null,
        includedIds: Set<MapPlacedObjectId>? = null,
        unidentifiedIds: Set<MapPlacedObjectId> = emptySet(),
        candidateFilter: (UnitSnapshot) -> Boolean = { it.template.affiliation == observer.template.affiliation },
        emptySummaryText: String = "No allied contacts to report.",
        sensorReportMode: SensorReportMode = SensorReportMode.SCIREP,
        ecmIntensity: Int = 0,
    ): SitrepVisibilityReport =
        visibilityReport(
            observer,
            UnitQueryService(candidates, mapWidth, mapHeight, mapUnitsPerPixel),
            gridLayer,
            includedIds,
            unidentifiedIds,
            candidateFilter,
            emptySummaryText,
            sensorReportMode,
            ecmIntensity,
        )

    fun hiddenContactLabel(
        observer: UnitSnapshot,
        check: SensorVisibilityCheck,
        mapWidth: Int,
        mapHeight: Int,
        gridLayer: GridLayer? = null,
    ): String =
        buildHiddenLine(observer, check, mapWidth, mapHeight, gridLayer)

    fun scirepText(
        checks: List<SensorVisibilityCheck>,
        identifiedIds: Set<MapPlacedObjectId>,
        contactNumbers: Map<MapPlacedObjectId, Int>,
        mapWidth: Int,
        mapHeight: Int,
        gridLayer: GridLayer? = null,
        emptySummaryText: String,
        identificationAnnouncements: List<SensorVisibilityCheck> = emptyList(),
        lostContacts: List<SitrepLostContact> = emptyList(),
        lowConfidenceContacts: Map<MapPlacedObjectId, SitrepLostContact> = emptyMap(),
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
                    val opening = if (lowConfidence != null) {
                        "New contact tentatively designated $designation ${contactNumbers.getValue(check.unit.id)} from last known position"
                    } else if (check.unit.id !in identifiedIds) {
                        val contactPrefix = if (check.unit.customProperties.booleanProperty("activeSensors")) {
                            "Contact actively radiating"
                        } else {
                            "Contact detected"
                        }
                        "$contactPrefix at extreme range as Sierra ${contactNumbers.getValue(check.unit.id)}"
                    } else {
                        "Contact Master ${contactNumbers.getValue(check.unit.id)}"
                    }
                    val classification = when {
                        check.ecmContact == EcmSensorContact.FAINT_TRACK -> "is faint sensor track"
                        check.ecmContact == EcmSensorContact.SENSOR_GHOST -> "is sensor ghost"
                        check.unit.id in identifiedIds && lowConfidence != null ->
                            "was identified as ${check.unit.identificationDescription()}"

                        check.unit.id in newlyIdentifiedIds ->
                            (if (lowConfidence != null) "was identified as " else "is identified as ") +
                                    check.unit.identificationDescription()

                        check.unit.id in identifiedIds ->
                            "is ${check.unit.identificationDescription()}"

                        else -> "has unknown ${check.unit.unknownClassificationBracket()} classification"
                    }
                    ScirepContactSegment(
                        contactId = check.unit.id,
                        sierraNumber = contactNumbers.getValue(check.unit.id),
                        opening = opening,
                        clauses = listOf(
                            "at ${
                                lowConfidence?.let { formatPosition(it.lastSeenPosition, mapWidth, mapHeight, gridLayer) } ?: formatPosition(
                                    check.unit,
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
                    ScirepContactSegment(
                        contactId = lost.contactId,
                        sierraNumber = lost.sierraNumber,
                        opening = "Contact Sierra ${lost.sierraNumber} lost",
                        clauses = emptyList(),
                    )
                },
            )
        }
        val conditions = buildList {
            if (sensorConditionsPoor) add(NEBULAR_INTERFERENCE_CONDITIONS)
            if (electronicWarfare) add(ELECTRONIC_WARFARE_CONDITIONS)
        }
        return (conditions + report).joinToString(" ")
    }

    fun combineScirepContactSegments(segments: List<ScirepContactSegment>): String =
        segments
            .groupBy { it.contactId }
            .values
            .sortedBy { contactSegments -> contactSegments.minOf { it.sierraNumber } }
            .joinToString(" ") { contactSegments ->
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

    private fun UnitSnapshot.identificationDescription(): String =
        buildString {
            append(template.name.ifBlank { "Unknown" })
            append("-class ")
            append(SENSOR_UNIT_CLASS_NAMES[unitClassLabel()] ?: "unknown")
            if (name.isNotBlank() && name != template.name) {
                append(' ')
                append(displayName())
            }
        }

    private fun UnitSnapshot.unknownClassificationBracket(): String =
        mass()?.let {
            when {
                it in 0..29 -> "lightweight"
                it in 30..59 -> "midweight"
                it >= 60 -> "heavyweight"
                else -> "unknown"
            }
        } ?: SENSOR_UNIT_CLASS_BRACKETS[unitClassLabel()] ?: "unknown"

    private fun UnitSnapshot.mass(): Int? =
        (customProperties.stringProperty(OlympiadDawnShip.MASS_KEY)
            ?: template.customProperties.stringProperty(OlympiadDawnShip.MASS_KEY))
            ?.toIntOrNull()

    private data class SensorRanges(val identification: Double, val detection: Double)

    private fun UnitSnapshot.sensorRanges(
        observer: UnitSnapshot,
        sensorReportMode: SensorReportMode,
        ecmIntensity: Int,
        observerProfile: SensorObserverProfile?,
    ): SensorRanges? {
        val base = unitClassLabel().baseDetectionRangeUnits()
        val observerModifier = (observerProfile?.unitClass ?: observer.unitClassLabel()).sensorRangeModifier()
        val activeSensors = observerProfile?.activeSensors ?: observer.activeSensors()

        val bandOffset = buildList {
            customProperties.warpBonus()?.let(::add)
            if (sensorReportMode == SensorReportMode.NAVREP && customProperties.booleanProperty("activeWarp")) add(2)
            if (customProperties.booleanProperty("transmitting") && !customProperties.booleanProperty("transmissionInScope")) {
                add(1)
            }
            repeat(customProperties.intProperty("nebula") ?: 0) {
                add(-1)
            }
            if (sensorReportMode == SensorReportMode.NAVREP && customProperties.booleanProperty("inNebula")) add(-1)
            if (sensorReportMode == SensorReportMode.SCIREP && customProperties.booleanProperty("silentRunning")) add(-1)
            if (sensorReportMode == SensorReportMode.SCIREP && customProperties.booleanProperty("activeSensors")) add(1)
        }.sum()

        if (customProperties.booleanProperty("transmissionInScope")) {
            return SensorRanges(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY)
        }

        val observerSensorModifier = buildList {
            if (sensorReportMode == SensorReportMode.SCIREP && activeSensors) add(1)
            if (sensorReportMode == SensorReportMode.NAVREP && observer.customProperties.booleanProperty("activeWarp")) add(2)
            if (sensorReportMode == SensorReportMode.NAVREP && observer.customProperties.booleanProperty("inNebula")) add(-1)
            if (ecmIntensity > 0) add(-ecmBandPenalty(ecmIntensity, activeSensors))
        }.sum()
        val advancedSensors = observerProfile?.advancedSensorsBonus ?: observer.advancedSensorsBonus()
        val identification = (base * observerModifier) * 2.0.pow(bandOffset + observerSensorModifier) + advancedSensors
        return SensorRanges(identification, identification * DETECTION_RANGE_BAND_MULTIPLIER)
    }

    private fun UnitSnapshot.activeSensors(): Boolean = customProperties.booleanProperty("activeSensors")

    private fun ecmBandPenalty(intensity: Int, activeSensors: Boolean): Int =
        intensity.coerceIn(1, MAXIMUM_ECM_INTENSITY) + if (activeSensors) 0 else 1

    private fun UnitSnapshot.transmissionInScope(): Boolean =
        customProperties.booleanProperty("transmissionInScope") || customProperties.booleanProperty("transmittingInScope")

    private fun UnitSnapshot.advancedSensorsBonus(): Double {
        val definition = OlympiadDawnShip.definition(customProperties) ?: OlympiadDawnShip.definition(template.customProperties) ?: return 0.0
        val damage = OlympiadDawnShip.instanceDamage(customProperties, definition)
        return if (definition.systems.any { it.name == "Advanced Sensors" && it.id !in damage.destroyedSystems }) 1.0 else 0.0
    }

    private fun UnitSnapshot.visibilityNotes(sensorReportMode: SensorReportMode): List<String> =
        buildList {
            unitClassLabel()?.let { add("Class $it baseline.") }
            if (sensorReportMode == SensorReportMode.SCIREP && customProperties.booleanProperty("activeSensors")) {
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
            if (sensorReportMode == SensorReportMode.SCIREP && customProperties.booleanProperty("silentRunning")) {
                add("Silent running: -1 signature, drive rating halved.")
            }
            customProperties.intProperty("nebula")?.takeIf { it > 0 }?.let {
                add("Nebula: -$it signature.")
            }
            if (sensorReportMode == SensorReportMode.NAVREP && customProperties.booleanProperty("activeWarp")) {
                add("Active warp: +2 detection bands.")
            }
            if (sensorReportMode == SensorReportMode.NAVREP && customProperties.booleanProperty("inNebula")) {
                add("In nebula: -1 detection band.")
            }
        }

    private fun formatDistance(distanceUnits: Double): String =
        ((distanceUnits * 10.0).roundToInt() / 10.0).toString().trimEnd('0').trimEnd('.')

    private fun buildPositionSummary(
        observer: UnitSnapshot,
        visibleChecks: List<SensorVisibilityCheck>,
        mapWidth: Int,
        mapHeight: Int,
        gridLayer: GridLayer?,
        emptySummaryText: String,
    ): String =
        buildString {
            append("Position ")
            append(formatPosition(observer, mapWidth, mapHeight, gridLayer))
            append(" bearing ")
            append(formatBearing(observer.facingDegrees))
            if (visibleChecks.isEmpty()) {
                append(". ")
                append(emptySummaryText)
            } else {
                append(". ")
                visibleChecks.forEachIndexed { index, check ->
                    if (index > 0) {
                        append(' ')
                    }
                    append(check.unit.navrepReportLabel())
                    append(" has position ")
                    append(formatPosition(check.unit, mapWidth, mapHeight, gridLayer))
                    append(" bearing ")
                    append(formatBearing(check.unit.facingDegrees))
                    append('.')
                }
            }
        }

    private fun buildHiddenLine(
        observer: UnitSnapshot,
        check: SensorVisibilityCheck,
        mapWidth: Int,
        mapHeight: Int,
        gridLayer: GridLayer?,
    ): String =
        buildString {
            append(check.unit.navrepReportLabel())
            append(" contact ranges are ")
            append(formatDistance(check.detectionRangeUnits))
            append(" / ")
            append(formatDistance(check.identificationRangeUnits))
            append(", actual range is ")
            append(formatDistance(check.distanceUnits))
        }

    private fun formatPosition(
        unit: UnitSnapshot,
        mapWidth: Int,
        mapHeight: Int,
        gridLayer: GridLayer?,
    ): String =
        formatPosition(unit.mapCoordinate, mapWidth, mapHeight, gridLayer)

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

    private fun String?.baseDetectionRangeUnits(): Double =
        when (this) {
            "FF", "F" -> 6.0
            "DD", "CL" -> 12.0
            "CA", "CVE", "CVL", "BC" -> 24.0
            else -> 6.0
        }

    private fun String?.sensorRangeModifier(): Double =
        when (this) {
            "FF", "F" -> 2.0 / 3.0
            "CA", "CVE", "CVL", "BC" -> 1.5
            else -> 1.0
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
