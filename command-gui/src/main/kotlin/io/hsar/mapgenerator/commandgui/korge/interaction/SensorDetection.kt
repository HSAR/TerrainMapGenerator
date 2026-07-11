package io.hsar.mapgenerator.commandgui.korge.interaction

import io.hsar.mapgenerator.grid.GridLayer
import kotlin.math.hypot
import kotlin.math.pow
import kotlin.math.roundToInt

internal data class SensorContact(
    val unit: UnitSnapshot,
    val distanceUnits: Double,
    val detectionRangeUnits: Double,
    val baseDetectionRangeUnits: Double,
)

internal data class SensorVisibilityCheck(
    val unit: UnitSnapshot,
    val visible: Boolean,
    val detected: Boolean,
    val distanceUnits: Double,
    val identificationRangeUnits: Double,
    val detectionRangeUnits: Double,
    val baseDetectionRangeUnits: Double,
    val notes: List<String>,
)

internal data class SitrepVisibilityReport(
    val displayText: String,
    val copyText: String,
    val visibleChecks: List<SensorVisibilityCheck>,
    val detectedChecks: List<SensorVisibilityCheck>,
    val unidentifiedChecks: List<SensorVisibilityCheck> = emptyList(),
    val hiddenChecks: List<SensorVisibilityCheck>,
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
    "CVE" to "escort aircraft carrier",
    "CVL" to "light aircraft carrier",
    "BC" to "battlecruiser",
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
    ): List<SensorContact> =
        visibilityChecks(observer, candidates, mapWidth, mapHeight, mapUnitsPerPixel, gridLayer, candidateFilter)
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
        candidates: List<UnitSnapshot>,
        mapWidth: Int,
        mapHeight: Int,
        mapUnitsPerPixel: Double,
        gridLayer: GridLayer? = null,
        candidateFilter: (UnitSnapshot) -> Boolean = { it.template.affiliation == observer.template.affiliation },
    ): List<SensorVisibilityCheck> =
        candidates.asSequence()
            .filter { it.id != observer.id }
            .filter(candidateFilter)
            .mapNotNull { candidate ->
                val distanceUnits = observer.distanceTo(candidate, mapWidth, mapHeight, mapUnitsPerPixel)
                val identification = candidate.identificationRangeUnits(observer)
                val detection = identification?.let { it * DETECTION_RANGE_BAND_MULTIPLIER }
                val baseDetectionRangeUnits = candidate.unitClassLabel().baseDetectionRangeUnits()
                val notes = candidate.visibilityNotes()
                when {
                    candidate.transmissionInScope() -> SensorVisibilityCheck(
                        unit = candidate,
                        visible = true,
                        detected = true,
                        distanceUnits = distanceUnits,
                        identificationRangeUnits = Double.POSITIVE_INFINITY,
                        detectionRangeUnits = Double.POSITIVE_INFINITY,
                        baseDetectionRangeUnits = baseDetectionRangeUnits,
                        notes = notes + "Transmitting in scope: automatically detected.",
                    )

                    identification == null -> null
                    else -> SensorVisibilityCheck(
                        unit = candidate,
                        visible = distanceUnits <= identification,
                        detected = distanceUnits <= detection!!,
                        distanceUnits = distanceUnits,
                        identificationRangeUnits = identification,
                        detectionRangeUnits = detection,
                        baseDetectionRangeUnits = baseDetectionRangeUnits,
                        notes = notes + if (distanceUnits <= detection) {
                            "Detected within range."
                        } else {
                            "Outside detection range."
                        },
                    )
                }
            }
            .sortedWith(
                compareBy<SensorVisibilityCheck> { it.distanceUnits }
                    .thenBy { it.unit.instanceName.lowercase() }
                    .thenBy { it.unit.unitClassLabel() },
            )
            .toList()

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
    ): SitrepVisibilityReport {
        val checks = visibilityChecks(observer, candidates, mapWidth, mapHeight, mapUnitsPerPixel, gridLayer, candidateFilter)
        val effectiveIncludedIds = includedIds ?: checks.filter { it.visible }.mapTo(mutableSetOf()) { it.unit.id }
        val visibleChecks = checks.filter { it.unit.id in effectiveIncludedIds }
        val unidentifiedChecks = checks.filter { it.unit.id in unidentifiedIds && it.unit.id !in effectiveIncludedIds }
        val hiddenChecks = checks.filterNot { it.unit.id in effectiveIncludedIds || it.unit.id in unidentifiedIds }
        val lines = buildList {
            add(buildPositionSummary(observer, visibleChecks, mapWidth, mapHeight, gridLayer, emptySummaryText))
        }
        val displayText = lines.joinToString("\n")
        return SitrepVisibilityReport(
            displayText = displayText,
            copyText = displayText,
            visibleChecks = visibleChecks,
            detectedChecks = checks.filter { it.detected },
            unidentifiedChecks = unidentifiedChecks,
            hiddenChecks = hiddenChecks,
        )
    }

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
    ): String =
        if (checks.isEmpty() && lostContacts.isEmpty()) {
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
                        "Contact detected at extreme range as Sierra ${contactNumbers.getValue(check.unit.id)}"
                    } else {
                        "Contact Master ${contactNumbers.getValue(check.unit.id)}"
                    }
                    val classification = when {
                        check.unit.id in identifiedIds && lowConfidence != null ->
                            "was identified as ${check.unit.identificationDescription()}"

                        check.unit.id in newlyIdentifiedIds ->
                            (if (lowConfidence != null) "was identified as " else "is identified as ") +
                                    check.unit.identificationDescription()

                        check.unit.id in identifiedIds ->
                            "is ${check.unit.identificationDescription()}"

                        else -> "has unknown classification"
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

    private fun UnitSnapshot.identificationRangeUnits(observer: UnitSnapshot): Double? {
        val base = unitClassLabel().baseDetectionRangeUnits()
        val observerModifier = observer.unitClassLabel().sensorRangeModifier()

        val bandOffset = buildList {
            if (customProperties.booleanProperty("activeSensors")) add(1)
            customProperties.warpBonus()?.let(::add)
            if (customProperties.booleanProperty("transmitting") && !customProperties.booleanProperty("transmissionInScope")) {
                add(1)
            }
            repeat(customProperties.intProperty("nebula") ?: 0) {
                add(-1)
            }
            if (customProperties.booleanProperty("silentRunning")) add(-1)
        }.sum()

        if (customProperties.booleanProperty("transmissionInScope")) {
            return Double.POSITIVE_INFINITY
        }

        val observerSensorModifier = if (observer.customProperties.booleanProperty("activeSensors")) 1 else 0
        return (base * observerModifier) * 2.0.pow(bandOffset + observerSensorModifier)
    }

    private fun UnitSnapshot.transmissionInScope(): Boolean =
        customProperties.booleanProperty("transmissionInScope") || customProperties.booleanProperty("transmittingInScope")

    private fun UnitSnapshot.distanceTo(
        other: UnitSnapshot,
        mapWidth: Int,
        mapHeight: Int,
        mapUnitsPerPixel: Double,
    ): Double =
        hypot(
            (mapCoordinate.x - other.mapCoordinate.x) * mapWidth,
            (mapCoordinate.y - other.mapCoordinate.y) * mapHeight,
        ) * mapUnitsPerPixel

    private fun UnitSnapshot.visibilityNotes(): List<String> =
        buildList {
            unitClassLabel()?.let { add("Class $it baseline.") }
            if (customProperties.booleanProperty("activeSensors")) {
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
            if (customProperties.booleanProperty("silentRunning")) {
                add("Silent running: -1 signature, drive rating halved.")
            }
            customProperties.intProperty("nebula")?.takeIf { it > 0 }?.let {
                add("Nebula: -$it signature.")
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

}
