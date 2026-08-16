package io.hsar.mapgenerator.commandgui.operation

data class OperationFireArcWeaponDefinition(
    val arcs: List<String>,
    val beamClass: Int? = null,
    val magazine: Boolean = false,
    val pointDefense: Boolean = false,
    val submunition: Boolean = false,
    val salvoRack: Boolean = false,
)

sealed interface OperationFireArcPrimitive {
    val radiusUnits: Double
    val strokeWidthPx: Double
    val outerBoundary: Boolean

    data class Circle(
        override val radiusUnits: Double,
        override val strokeWidthPx: Double,
        override val outerBoundary: Boolean,
    ) : OperationFireArcPrimitive

    data class SectorArc(
        override val radiusUnits: Double,
        val startDegrees: Double,
        val endDegrees: Double,
        override val strokeWidthPx: Double,
        override val outerBoundary: Boolean,
    ) : OperationFireArcPrimitive

    data class Radial(
        override val radiusUnits: Double,
        val degrees: Double,
        override val strokeWidthPx: Double,
        override val outerBoundary: Boolean = true,
    ) : OperationFireArcPrimitive
}

object OperationFireArcOverlayBuilder {
    const val RANGE_BAND_SIZE_UNITS = 6.0
    private const val ARC_WIDTH_DEGREES = 60.0
    private const val BORDER_WIDTH_PX = 1.5
    private const val RANGE_WIDTH_PX = 1.0
    private val ARC_ORDER = listOf("F", "FS", "AS", "A", "AP", "FP")

    fun build(facingDegrees: Int, weapons: List<OperationFireArcWeaponDefinition>): List<OperationFireArcPrimitive> {
        data class ArcRange(val arcIndex: Int, val band: Int)
        val beamWeapons = weapons.mapNotNull { weapon ->
            val beamClass = weapon.beamClass ?: when {
                weapon.magazine || weapon.salvoRack -> 2
                weapon.pointDefense || weapon.submunition -> 1
                else -> return@mapNotNull null
            }
            val indices = weapon.arcs.mapNotNull { ARC_ORDER.indexOf(it).takeIf { index -> index >= 0 } }.distinct()
            indices.takeIf(List<Int>::isNotEmpty)?.let { beamClass to it }
        }
        val coverage = mutableMapOf<ArcRange, Int>()
        beamWeapons.forEach { (beamClass, arcIndices) -> arcIndices.forEach { arc ->
            (1..beamClass).forEach { band -> coverage[ArcRange(arc, band)] = coverage.getOrDefault(ArcRange(arc, band), 0) + 1 }
        } }
        val maximumBand = beamWeapons.maxOfOrNull { it.first } ?: return emptyList()
        return buildList {
            (1..maximumBand).forEach { band ->
                val counts = ARC_ORDER.indices.map { coverage[ArcRange(it, band)] ?: 0 }
                val outerCounts = ARC_ORDER.indices.map { arc -> beamWeapons.count { (beamClass, arcs) -> beamClass == band && arc in arcs } }
                val common = counts.minOrNull() ?: 0
                val uniformBoundary = outerCounts.distinct().size <= 1
                fun width(count: Int, outer: Boolean) = (if (outer) BORDER_WIDTH_PX else RANGE_WIDTH_PX) * if (count > 1) 2.0 else 1.0
                if (common > 0 && uniformBoundary) {
                    val outer = outerCounts.firstOrNull()?.let { it > 0 } == true
                    add(OperationFireArcPrimitive.Circle(band * RANGE_BAND_SIZE_UNITS, width(common, outer), outer))
                }
                counts.forEachIndexed { arc, count ->
                    if (count > 0 && (count > common || !uniformBoundary)) {
                        val outer = outerCounts[arc] > 0
                        val start = facingDegrees + arc * ARC_WIDTH_DEGREES - ARC_WIDTH_DEGREES * 0.5
                        add(OperationFireArcPrimitive.SectorArc(band * RANGE_BAND_SIZE_UNITS, start, start + ARC_WIDTH_DEGREES, width(count, outer), outer))
                    }
                }
            }
            beamWeapons.forEach { (beamClass, arcs) ->
                if (arcs.size < ARC_ORDER.size) {
                    val start = facingDegrees + arcs.first() * ARC_WIDTH_DEGREES - ARC_WIDTH_DEGREES * 0.5
                    val radius = beamClass * RANGE_BAND_SIZE_UNITS
                    add(OperationFireArcPrimitive.Radial(radius, start, BORDER_WIDTH_PX))
                    add(OperationFireArcPrimitive.Radial(radius, start + arcs.size * ARC_WIDTH_DEGREES, BORDER_WIDTH_PX))
                }
            }
        }
    }
}

object OperationFireArcOverlayService {
    fun weaponsFor(unit: OperationUnit, content: OperationContent): List<OperationFireArcWeaponDefinition> {
        val definition = OlympiadDawnShip.definitionFor(unit, content)
        val weapons = definition?.let { it.weapons + it.fighters.flatMap { fighter -> fighter.weapons } }
            ?: OlympiadDawnShip.squadronStatus(unit, content)?.weapons
            ?: return emptyList()
        return weapons.map { weapon -> OperationFireArcWeaponDefinition(
            arcs = weapon.arcs,
            beamClass = weapon.beamClass,
            magazine = weapon.magazineId != null,
            pointDefense = weapon.pointDefense,
            submunition = weapon.submunition,
            salvoRack = weapon.salvoRack,
        ) }
    }
}
