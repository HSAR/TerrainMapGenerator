package io.hsar.mapgenerator.contourgenerator

import io.hsar.mapgenerator.graph.Point
import personthecat.fastnoise.FastNoise
import personthecat.fastnoise.data.NoiseType
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.pow
import kotlin.math.sin

object TopographyGenerator {
    fun generate(request: TopographyRequest): TopographyMap {
        val resolvedProfile = request.terrainProfile.resolve(request.seed)
        return generate(
            request = request,
            sampler = TerrainHeightSampler.create(request, resolvedProfile),
            resolvedProfile = resolvedProfile,
        )
    }

    fun generate(request: TopographyRequest, sampler: HeightSampler): TopographyMap =
        generate(request, sampler, request.terrainProfile.resolve(request.seed))

    private fun generate(
        request: TopographyRequest,
        sampler: HeightSampler,
        resolvedProfile: TerrainProfile,
    ): TopographyMap {
        val sampleAtPixel = { x: Double, y: Double ->
            sampler.sample(
                request.worldOriginX + x * request.worldUnitsPerPixel,
                request.worldOriginY + y * request.worldUnitsPerPixel,
            ).coerceIn(0.0, 1.0)
        }
        val field = HeightField(
            width = request.width,
            height = request.height,
            values = DoubleArray(request.width * request.height) { index ->
                sampleAtPixel((index % request.width).toDouble(), (index / request.width).toDouble())
            },
        )
        return TopographyMap(field, DirectionalContourTracer(request, field, sampleAtPixel).trace(), resolvedProfile)
    }
}

private object TerrainHeightSampler {
    fun create(request: TopographyRequest, profile: TerrainProfile): HeightSampler {
        val definition = profile.definition()
        val frequency = definition.baseFrequencyMultiplier / request.noiseScale
        val octaves = definition.amplitudes.mapIndexed { octave, amplitude ->
            TerrainOctave(
                noise = FastNoise.builder()
                    .type(NoiseType.SIMPLEX2)
                    .frequency((frequency * definition.frequencyGrowth.pow(octave)).toFloat())
                    .seed(request.seed.octaveSeed(octave))
                    .build(),
                amplitude = amplitude,
            )
        }
        val amplitudeTotal = definition.amplitudes.sum()
        return HeightSampler { x, y ->
            var fractalSum = 0.0
            var ridgeSum = 0.0
            var cumulativeAmplitude = 0.0
            var ridgeInfluence = 1.0
            octaves.forEach { octave ->
                val value = octave.noise.getNoise(x.toFloat(), y.toFloat()).toDouble()
                fractalSum += ((value + 1.0) * 0.5) * octave.amplitude
                ridgeSum += octave.amplitude * (1.0 - kotlin.math.abs(value)) * ridgeInfluence
                cumulativeAmplitude += octave.amplitude
                ridgeInfluence = (ridgeSum / cumulativeAmplitude).coerceIn(0.0, 1.0)
            }
            val fractal = fractalSum / amplitudeTotal
            val ridged = ridgeSum / amplitudeTotal
            (fractal + (ridged - fractal) * definition.ridgeBlend)
                .times(definition.redistributionFactor)
                .coerceIn(0.0, 1.0)
                .pow(definition.redistributionExponent)
        }
    }
}

private data class TerrainOctave(
    val noise: FastNoise,
    val amplitude: Double,
)

private data class TerrainProfileDefinition(
    val amplitudes: List<Double>,
    val baseFrequencyMultiplier: Double,
    val frequencyGrowth: Double,
    val redistributionExponent: Double,
    val redistributionFactor: Double,
    val ridgeBlend: Double,
)

private fun TerrainProfile.definition(): TerrainProfileDefinition = when (this) {
    TerrainProfile.ROLLING_PLAINS -> TerrainProfileDefinition(
        amplitudes = listOf(1.0, 0.42, 0.18, 0.075),
        baseFrequencyMultiplier = 0.85,
        frequencyGrowth = 1.75,
        redistributionExponent = 1.45,
        redistributionFactor = 1.12,
        ridgeBlend = 0.0,
    )

    TerrainProfile.UPLANDS -> TerrainProfileDefinition(
        amplitudes = listOf(1.0, 0.46, 0.25, 0.14, 0.08),
        baseFrequencyMultiplier = 0.95,
        frequencyGrowth = 1.80,
        redistributionExponent = 1.25,
        redistributionFactor = 1.08,
        ridgeBlend = 0.08,
    )

    TerrainProfile.ALPINE -> TerrainProfileDefinition(
        amplitudes = listOf(1.0, 0.62, 0.40, 0.28, 0.18),
        baseFrequencyMultiplier = 1.0,
        frequencyGrowth = 1.85,
        redistributionExponent = 1.10,
        redistributionFactor = 1.05,
        ridgeBlend = 0.26,
    )

    TerrainProfile.RIDGED_BADLANDS -> TerrainProfileDefinition(
        amplitudes = listOf(1.0, 0.48, 0.27, 0.16, 0.09),
        baseFrequencyMultiplier = 0.95,
        frequencyGrowth = 1.85,
        redistributionExponent = 1.20,
        redistributionFactor = 1.08,
        ridgeBlend = 0.50,
    )

    TerrainProfile.SEEDED -> error("A seeded terrain profile must be resolved before sampling")
}

private class DirectionalContourTracer(
    private val request: TopographyRequest,
    private val field: HeightField,
    private val sample: (Double, Double) -> Double,
) {
    private val options = request.tracing
    private val maximumX = (request.width - 1).toDouble()
    private val maximumY = (request.height - 1).toDouble()

    fun trace(): List<ContourPath> = request.contourLevels
        .distinct()
        .sorted()
        .flatMap(::traceLevel)

    private fun traceLevel(level: Double): List<ContourPath> {
        val occupied = SpatialPointIndex(options.stepPixels * 1.5)
        return discoverSeeds(level).fold(emptyList()) { paths, seed ->
            if (occupied.contains(seed)) {
                paths
            } else {
                traceFromSeed(seed, level)
                    ?.takeIf { it.points.size >= MINIMUM_PATH_POINTS }
                    ?.also { path -> path.points.forEach(occupied::add) }
                    ?.let { paths + it }
                    ?: paths
            }
        }
    }

    private fun discoverSeeds(level: Double): List<Point> {
        val step = options.seedGridStepPixels
        val xs = steppedCoordinates(request.width, step)
        val ys = steppedCoordinates(request.height, step)
        return ys.zipWithNext().flatMap { (top, bottom) ->
            xs.zipWithNext().flatMap { (left, right) ->
                listOf(
                    crossing(left, top, right, top, level),
                    crossing(right, top, right, bottom, level),
                    crossing(right, bottom, left, bottom, level),
                    crossing(left, bottom, left, top, level),
                ).filterNotNull()
            }
        }
    }

    private fun crossing(x1: Int, y1: Int, x2: Int, y2: Int, level: Double): Point? {
        val first = field[x1, y1]
        val second = field[x2, y2]
        if (first == second || level < minOf(first, second) || level > maxOf(first, second)) return null
        val fraction = (level - first) / (second - first)
        return Point(x1 + (x2 - x1) * fraction, y1 + (y2 - y1) * fraction)
    }

    private fun traceFromSeed(seed: Point, level: Double): ContourPath? {
        val heading = tangentHeading(seed)
        val forward = traceDirection(seed, heading, level)
        if (forward.closed) return ContourPath(level, forward.points, closed = true)
        val backward = traceDirection(seed, heading + PI, level)
        val combined = backward.points.asReversed().dropLast(1) + forward.points
        return combined.takeIf { it.size >= 2 }?.let { ContourPath(level, it, closed = false) }
    }

    private fun tangentHeading(point: Point): Double {
        val epsilon = 1.0
        val dx = sampleClamped(point.x + epsilon, point.y) - sampleClamped(point.x - epsilon, point.y)
        val dy = sampleClamped(point.x, point.y + epsilon) - sampleClamped(point.x, point.y - epsilon)
        return atan2(dx, -dy)
    }

    private fun traceDirection(start: Point, initialHeading: Double, level: Double): TraceResult {
        val points = mutableListOf(start)
        val visited = SpatialPointIndex(options.stepPixels * 0.25)
        visited.add(start)
        var current = start
        var heading = initialHeading
        repeat(options.maximumPointsPerDirection) {
            val candidate = nextPoint(current, heading, level) ?: return TraceResult(points, closed = false)
            heading = atan2(candidate.y - current.y, candidate.x - current.x)
            if (!inBounds(candidate)) {
                points += candidate.clamped(maximumX, maximumY)
                return TraceResult(points, closed = false)
            }
            if (points.size > MINIMUM_PATH_POINTS && candidate.distanceTo(start) <= options.stepPixels) {
                points += start
                return TraceResult(points, closed = true)
            }
            if (visited.contains(candidate)) return TraceResult(points, closed = false)
            points += candidate
            visited.add(candidate)
            current = candidate
        }
        return TraceResult(points, closed = false)
    }

    private fun nextPoint(origin: Point, heading: Double, level: Double): Point? {
        val candidates = generateSequence(heading + PI / 2.0) { angle ->
            (angle - options.angularStepRadians).takeIf { it >= heading - PI / 2.0 }
        }.map { angle ->
            Point(
                x = origin.x + options.stepPixels * cos(angle),
                y = origin.y + options.stepPixels * sin(angle),
            )
        }.toList()
        val best = candidates.minByOrNull { candidate -> kotlin.math.abs(sample(candidate.x, candidate.y) - level) } ?: return null
        return best.takeIf { kotlin.math.abs(sample(it.x, it.y) - level) <= options.valueTolerance * 4.0 }
    }

    private fun sampleClamped(x: Double, y: Double): Double = sample(x.coerceIn(0.0, maximumX), y.coerceIn(0.0, maximumY))

    private fun inBounds(point: Point): Boolean = point.x in 0.0..maximumX && point.y in 0.0..maximumY

    private data class TraceResult(val points: List<Point>, val closed: Boolean)

    private companion object {
        const val MINIMUM_PATH_POINTS = 4
    }
}

private class SpatialPointIndex(private val cellSize: Double) {
    private val cells = mutableMapOf<Pair<Int, Int>, MutableList<Point>>()

    fun add(point: Point) {
        cells.getOrPut(point.cell()) { mutableListOf() } += point
    }

    fun contains(point: Point): Boolean {
        val (x, y) = point.cell()
        return (-1..1).any { dx ->
            (-1..1).any { dy ->
                cells[x + dx to y + dy].orEmpty().any { indexed -> indexed.distanceTo(point) <= cellSize }
            }
        }
    }

    private fun Point.cell(): Pair<Int, Int> = floor(x / cellSize).toInt() to floor(y / cellSize).toInt()
}

private fun steppedCoordinates(size: Int, step: Int): List<Int> =
    generateSequence(0) { (it + step).takeIf { next -> next < size - 1 } }
        .toList()
        .let { coordinates -> if (coordinates.lastOrNull() == size - 1) coordinates else coordinates + (size - 1) }

private fun Point.distanceTo(other: Point): Double = hypot(x - other.x, y - other.y)

private fun Point.clamped(maximumX: Double, maximumY: Double): Point = Point(x.coerceIn(0.0, maximumX), y.coerceIn(0.0, maximumY))

private fun Long.octaveSeed(octave: Int): Int =
    (this + GOLDEN_RATIO_INCREMENT * (octave + 1))
        .let { value -> (value xor (value ushr 30)) * FIRST_MIX_MULTIPLIER }
        .let { value -> (value xor (value ushr 27)) * SECOND_MIX_MULTIPLIER }
        .let { value -> value xor (value ushr 31) }
        .let { value -> (value xor (value ushr 32)).toInt() }

private const val GOLDEN_RATIO_INCREMENT = -7046029254386353131L
private const val FIRST_MIX_MULTIPLIER = -4658895280553007687L
private const val SECOND_MIX_MULTIPLIER = -7723592293110705685L
