package io.hsar.mapgenerator.contourgenerator

import io.hsar.mapgenerator.graph.Point

/** Controls the large-scale morphology of the generated elevation field. */
enum class TerrainProfile {
    SEEDED,
    ROLLING_PLAINS,
    UPLANDS,
    ALPINE,
    RIDGED_BADLANDS;

    fun resolve(seed: Long): TerrainProfile =
        if (this == SEEDED) concreteProfiles[Math.floorMod(seed.profileHash(), concreteProfiles.size)] else this

    companion object {
        internal val concreteProfiles: List<TerrainProfile> = entries.filterNot { it == SEEDED }
    }
}

private fun Long.profileHash(): Int =
    (this + PROFILE_SEED_SALT)
        .let { value -> (value xor (value ushr 30)) * FIRST_PROFILE_MIX_MULTIPLIER }
        .let { value -> (value xor (value ushr 27)) * SECOND_PROFILE_MIX_MULTIPLIER }
        .let { value -> value xor (value ushr 31) }
        .let { value -> (value xor (value ushr 32)).toInt() }

private const val PROFILE_SEED_SALT = -7046029254386353131L
private const val FIRST_PROFILE_MIX_MULTIPLIER = -4658895280553007687L
private const val SECOND_PROFILE_MIX_MULTIPLIER = -7723592293110705685L

data class ContourTracingOptions(
    val stepPixels: Double = 2.0,
    val angularStepRadians: Double = 0.17,
    val valueTolerance: Double = 0.0035,
    val seedGridStepPixels: Int = 8,
    val maximumPointsPerDirection: Int = 50_000,
) {
    init {
        require(stepPixels > 0.0)
        require(angularStepRadians > 0.0)
        require(valueTolerance > 0.0)
        require(seedGridStepPixels > 0)
        require(maximumPointsPerDirection > 0)
    }
}

data class TopographyRequest(
    val width: Int,
    val height: Int,
    val seed: Long,
    val worldUnitsPerPixel: Double = 1.0,
    val noiseScale: Double = 425.0,
    val terrainProfile: TerrainProfile = TerrainProfile.SEEDED,
    val contourLevels: List<Double> = (3..17).map { it * 0.05 },
    val tracing: ContourTracingOptions = ContourTracingOptions(),
) {
    init {
        require(width > 0)
        require(height > 0)
        require(worldUnitsPerPixel > 0.0)
        require(noiseScale > 0.0)
        require(contourLevels.all { it in 0.0..1.0 })
    }
}

class HeightField(
    val width: Int,
    val height: Int,
    values: DoubleArray,
) {
    private val values = values.copyOf()

    init {
        require(width > 0)
        require(height > 0)
        require(values.size == width * height)
        require(values.all { it in 0.0..1.0 })
    }

    operator fun get(x: Int, y: Int): Double = values[y * width + x]

    fun values(): DoubleArray = values.copyOf()
}

data class ContourPath(
    val level: Double,
    val points: List<Point>,
    val closed: Boolean,
)

data class TopographyMap(
    val heightField: HeightField,
    val contours: List<ContourPath>,
    val resolvedTerrainProfile: TerrainProfile = TerrainProfile.UPLANDS,
)

fun interface HeightSampler {
    fun sample(worldX: Double, worldY: Double): Double
}
