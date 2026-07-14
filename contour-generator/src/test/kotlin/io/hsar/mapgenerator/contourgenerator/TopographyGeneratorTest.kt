package io.hsar.mapgenerator.contourgenerator

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.greaterThan
import org.hamcrest.Matchers.hasSize
import org.hamcrest.Matchers.lessThanOrEqualTo
import org.junit.jupiter.api.Test
import kotlin.math.hypot

class TopographyGeneratorTest {
    @Test
    fun `fast noise sampling is deterministic and normalized`() {
        val request = request(seed = 42L)

        val first = TopographyGenerator.generate(request)
        val second = TopographyGenerator.generate(request)

        assertThat(first.heightField.values().toList(), equalTo(second.heightField.values().toList()))
        assertThat(first.heightField.values().all { it in 0.0..1.0 }, equalTo(true))
    }

    @Test
    fun `different seeds produce different terrain`() {
        val first = TopographyGenerator.generate(request(seed = 42L))
        val second = TopographyGenerator.generate(request(seed = 43L))

        assertThat(first.heightField.values().contentEquals(second.heightField.values()), equalTo(false))
    }

    @Test
    fun `seeded profile resolution is deterministic and reaches every morphology`() {
        val resolutions = (0L..200L).map { seed -> TerrainProfile.SEEDED.resolve(seed) }

        assertThat(TerrainProfile.SEEDED.resolve(42L), equalTo(TerrainProfile.SEEDED.resolve(42L)))
        assertThat(resolutions.toSet(), equalTo(TerrainProfile.concreteProfiles.toSet()))
    }

    @Test
    fun `explicit profiles are reported and produce distinct height fields`() {
        val maps = TerrainProfile.concreteProfiles.map { profile ->
            TopographyGenerator.generate(
                request(width = 64, height = 64, seed = 42L, contourLevels = emptyList(), terrainProfile = profile),
            )
        }

        assertThat(maps.map(TopographyMap::resolvedTerrainProfile), equalTo(TerrainProfile.concreteProfiles))
        assertThat(maps.map { it.heightField.values().contentHashCode() }.toSet().size, equalTo(TerrainProfile.concreteProfiles.size))
    }

    @Test
    fun `all profiles remain deterministic normalized and stable in world coordinates`() {
        TerrainProfile.concreteProfiles.forEach { profile ->
            val coarseRequest = request(
                width = 32,
                height = 32,
                seed = 91L,
                worldUnitsPerPixel = 2.0,
                contourLevels = emptyList(),
                terrainProfile = profile,
            )
            val fineRequest = coarseRequest.copy(width = 64, height = 64, worldUnitsPerPixel = 1.0)
            val coarse = TopographyGenerator.generate(coarseRequest)
            val repeated = TopographyGenerator.generate(coarseRequest)
            val fine = TopographyGenerator.generate(fineRequest)

            assertThat(coarse.heightField.values().contentEquals(repeated.heightField.values()), equalTo(true))
            assertThat(coarse.heightField.values().all { it in 0.0..1.0 }, equalTo(true))
            (0 until 32).forEach { x ->
                (0 until 32).forEach { y ->
                    assertThat(fine.heightField[x * 2, y * 2], equalTo(coarse.heightField[x, y]))
                }
            }
        }
    }

    @Test
    fun `terrain profiles form a spectrum from broad valleys to rugged ridges`() {
        val maps = TerrainProfile.concreteProfiles.associateWith { profile ->
            TopographyGenerator.generate(
                request(
                    width = 128,
                    height = 128,
                    seed = 718L,
                    contourLevels = emptyList(),
                    terrainProfile = profile,
                    noiseScale = 48.0,
                ),
            ).heightField
        }
        val plains = maps.getValue(TerrainProfile.ROLLING_PLAINS)
        val alpine = maps.getValue(TerrainProfile.ALPINE)
        val badlands = maps.getValue(TerrainProfile.RIDGED_BADLANDS)
        val plainsRoughness = meanNeighbourDifference(plains)
        val alpineRoughness = meanNeighbourDifference(alpine)
        val plainsHighGradients = highGradientCount(plains)
        val badlandsHighGradients = highGradientCount(badlands)

        assertThat(
            "Alpine roughness $alpineRoughness should exceed plains roughness $plainsRoughness",
            alpineRoughness > plainsRoughness,
            equalTo(true),
        )
        assertThat(
            "Badlands high-gradient count $badlandsHighGradients should exceed plains count $plainsHighGradients",
            badlandsHighGradients > plainsHighGradients,
            equalTo(true),
        )
        assertThat(plains.values().count { it < 0.4 } > alpine.values().count { it < 0.4 }, equalTo(true))
    }

    @Test
    fun `terrain profiles suppress excessive single pixel variation`() {
        val roughness = TerrainProfile.concreteProfiles.associateWith { profile ->
            TopographyGenerator.generate(
                request(
                    width = 128,
                    height = 128,
                    seed = 718L,
                    contourLevels = emptyList(),
                    terrainProfile = profile,
                    noiseScale = 96.0,
                ),
            ).heightField.let(::meanNeighbourDifference)
        }

        assertThat(
            "Mean neighbour differences should remain subdued: $roughness",
            roughness.values.all { it < 0.025 },
            equalTo(true),
        )
    }

    @Test
    fun `sampling uses stable world coordinates across resolutions`() {
        val coarse = TopographyGenerator.generate(request(width = 8, height = 8, worldUnitsPerPixel = 2.0, contourLevels = emptyList()))
        val fine = TopographyGenerator.generate(request(width = 16, height = 16, worldUnitsPerPixel = 1.0, contourLevels = emptyList()))

        (0 until 8).forEach { x ->
            (0 until 8).forEach { y ->
                assertThat(fine.heightField[x * 2, y * 2], equalTo(coarse.heightField[x, y]))
            }
        }
    }

    @Test
    fun `traces one closed contour around a radial height field`() {
        val size = 65
        val centre = 32.0
        val map = TopographyGenerator.generate(
            request(width = size, height = size, contourLevels = listOf(0.45)),
            HeightSampler { x, y -> (hypot(x - centre, y - centre) / 45.0).coerceIn(0.0, 1.0) },
        )

        assertThat(map.contours, hasSize(1))
        assertThat(map.contours.single().closed, equalTo(true))
        assertThat(map.contours.single().points.all { it.x in 0.0..64.0 && it.y in 0.0..64.0 }, equalTo(true))
        assertThat(
            map.contours.single().points.maxOf { kotlin.math.abs(hypot(it.x - centre, it.y - centre) / 45.0 - 0.45) },
            lessThanOrEqualTo(0.02),
        )
    }

    @Test
    fun `traces and deduplicates an open edge clipped contour`() {
        val map = TopographyGenerator.generate(
            request(width = 65, height = 65, contourLevels = listOf(0.5)),
            HeightSampler { x, _ -> x / 64.0 },
        )

        assertThat(map.contours, hasSize(1))
        assertThat(map.contours.single().closed, equalTo(false))
        assertThat(map.contours.single().points.minOf { it.y }, equalTo(0.0))
        assertThat(map.contours.single().points.maxOf { it.y }, equalTo(64.0))
    }

    @Test
    fun `maximum point limit terminates tracing`() {
        val map = TopographyGenerator.generate(
            request(
                width = 65,
                height = 65,
                contourLevels = listOf(0.5),
                tracing = ContourTracingOptions(maximumPointsPerDirection = 3),
            ),
            HeightSampler { x, _ -> x / 64.0 },
        )

        assertThat(map.contours, hasSize(greaterThan(0)))
        assertThat(map.contours.maxOf { it.points.size }, lessThanOrEqualTo(7))
    }

    private fun request(
        width: Int = 32,
        height: Int = 32,
        seed: Long = 1L,
        worldUnitsPerPixel: Double = 1.0,
        noiseScale: Double = 350.0,
        terrainProfile: TerrainProfile = TerrainProfile.SEEDED,
        contourLevels: List<Double> = listOf(0.5),
        tracing: ContourTracingOptions = ContourTracingOptions(),
    ) = TopographyRequest(
        width = width,
        height = height,
        seed = seed,
        worldUnitsPerPixel = worldUnitsPerPixel,
        noiseScale = noiseScale,
        terrainProfile = terrainProfile,
        contourLevels = contourLevels,
        tracing = tracing,
    )

    private fun meanNeighbourDifference(field: HeightField): Double =
        neighbourDifferences(field).average()

    private fun highGradientCount(field: HeightField): Int =
        neighbourDifferences(field).count { it > 0.04 }

    private fun neighbourDifferences(field: HeightField): List<Double> =
        (0 until field.height).flatMap { y ->
            (0 until field.width).flatMap { x ->
                listOfNotNull(
                    (x + 1).takeIf { it < field.width }?.let { right -> kotlin.math.abs(field[x, y] - field[right, y]) },
                    (y + 1).takeIf { it < field.height }?.let { bottom -> kotlin.math.abs(field[x, y] - field[x, bottom]) },
                )
            }
        }
}
