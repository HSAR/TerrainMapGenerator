package io.hsar.mapgenerator.commandgui.korge.interaction

import io.hsar.mapgenerator.grid.GridLayer
import kotlin.math.hypot
import kotlin.math.roundToInt

internal data class NavrepContact(
    val unit: UnitSnapshot,
    val distanceUnits: Double,
    val detectionRangeUnits: Double,
    val baseDetectionRangeUnits: Double,
)

internal data class NavrepVisibilityCheck(
    val unit: UnitSnapshot,
    val visible: Boolean,
    val distanceUnits: Double,
    val detectionRangeUnits: Double,
    val baseDetectionRangeUnits: Double,
    val notes: List<String>,
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

internal object NavrepDetection {
    fun visibleContacts(
        observer: UnitSnapshot,
        candidates: List<UnitSnapshot>,
        mapWidth: Int,
        mapHeight: Int,
        mapUnitsPerPixel: Double,
        gridLayer: GridLayer? = null,
    ): List<NavrepContact> =
        visibilityChecks(observer, candidates, mapWidth, mapHeight, mapUnitsPerPixel, gridLayer)
            .filter { it.visible }
            .map {
                NavrepContact(
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
    ): List<NavrepVisibilityCheck> =
        candidates.asSequence()
            .filter { it.id != observer.id }
            .filter { it.template.affiliation == observer.template.affiliation }
            .mapNotNull { candidate ->
                val distanceUnits = observer.distanceTo(candidate, mapWidth, mapHeight, mapUnitsPerPixel)
                val detection = candidate.detectionRangeUnits(observer)
                val baseDetectionRangeUnits = candidate.unitClassLabel().baseDetectionRangeUnits()
                val notes = candidate.visibilityNotes()
                when {
                    candidate.transmissionInScope() -> NavrepVisibilityCheck(
                        unit = candidate,
                        visible = true,
                        distanceUnits = distanceUnits,
                        detectionRangeUnits = Double.POSITIVE_INFINITY,
                        baseDetectionRangeUnits = baseDetectionRangeUnits,
                        notes = notes + "Transmitting in scope: automatically detected.",
                    )

                    detection == null -> null
                    distanceUnits <= detection -> NavrepVisibilityCheck(
                        unit = candidate,
                        visible = true,
                        distanceUnits = distanceUnits,
                        detectionRangeUnits = detection,
                        baseDetectionRangeUnits = baseDetectionRangeUnits,
                        notes = notes + "Detected within range.",
                    )

                    else -> NavrepVisibilityCheck(
                        unit = candidate,
                        visible = false,
                        distanceUnits = distanceUnits,
                        detectionRangeUnits = detection,
                        baseDetectionRangeUnits = baseDetectionRangeUnits,
                        notes = notes + "Outside detection range.",
                    )
                }
            }
            .sortedWith(
                compareBy<NavrepVisibilityCheck> { it.distanceUnits }
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
    ): String =
        buildList {
            val checks = visibilityChecks(observer, candidates, mapWidth, mapHeight, mapUnitsPerPixel, gridLayer)
            val visibleChecks = checks.filter { it.visible }
            val hiddenChecks = checks.filterNot { it.visible }
            add(buildPositionSummary(observer, visibleChecks, mapWidth, mapHeight, gridLayer))
            if (hiddenChecks.isNotEmpty()) {
                add("")
                add("[HIDDEN]")
                hiddenChecks.forEach { check ->
                    add(buildHiddenLine(observer, check, mapWidth, mapHeight, gridLayer))
                }
            } else {
                // keep the report compact when nothing is hidden
            }
        }.joinToString("\n")

    private fun UnitSnapshot.detectionRangeUnits(observer: UnitSnapshot): Double? {
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

        return when ((base * observerModifier).toRangeBandIndex() + bandOffset) {
            in Int.MIN_VALUE..0 -> 3.0
            1 -> 6.0
            2 -> 12.0
            3 -> 24.0
            4 -> 48.0
            5 -> 96.0
            else -> 192.0
        }
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

    private fun Double.toRangeBandIndex(): Int =
        when {
            this <= 3.0 -> 0
            this <= 6.0 -> 1
            this <= 12.0 -> 2
            this <= 24.0 -> 3
            this <= 48.0 -> 4
            this <= 96.0 -> 5
            else -> 6
        }

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
        visibleChecks: List<NavrepVisibilityCheck>,
        mapWidth: Int,
        mapHeight: Int,
        gridLayer: GridLayer?,
    ): String =
        buildString {
            append("Position ")
            append(formatPosition(observer, mapWidth, mapHeight, gridLayer))
            append(" bearing ")
            append(formatBearing(observer.facingDegrees))
            if (visibleChecks.isEmpty()) {
                append(". No allied contacts to report.")
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
        check: NavrepVisibilityCheck,
        mapWidth: Int,
        mapHeight: Int,
        gridLayer: GridLayer?,
    ): String =
        buildString {
            append("- ")
            append(check.unit.navrepReportLabel())
            append(" detection range is ")
            append(formatDistance(check.detectionRangeUnits))
            append(" [base ")
            append(formatDistance(check.baseDetectionRangeUnits))
            append(", observer ")
            append(observer.unitClassLabel() ?: "unknown")
            append(' ')
            append(observer.unitClassLabel().sensorRangeSummary())
            append("] but actual range was ")
            append(formatDistance(check.distanceUnits))
        }

    private fun formatPosition(
        unit: UnitSnapshot,
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
                    mapX = unit.mapCoordinate.x * mapWidth,
                    mapY = unit.mapCoordinate.y * mapHeight,
                )
            }
            ?: "${formatDistance(unit.mapCoordinate.x * mapWidth)} ${formatDistance(unit.mapCoordinate.y * mapHeight)}"

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
