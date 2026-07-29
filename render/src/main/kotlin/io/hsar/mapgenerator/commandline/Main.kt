package io.hsar.mapgenerator.commandline

import com.beust.jcommander.JCommander
import com.beust.jcommander.Parameter
import com.beust.jcommander.Parameters
import io.hsar.mapgenerator.TerrainMapGenerator
import io.hsar.mapgenerator.commandgui.GridColourSelector
import io.hsar.mapgenerator.commandgui.MapLayer
import io.hsar.mapgenerator.commandgui.MapViewerBackend
import io.hsar.mapgenerator.commandgui.MapViewerFeatureFlag
import io.hsar.mapgenerator.commandgui.MapViewerFrame
import io.hsar.mapgenerator.commandgui.MapViewerLayer
import io.hsar.mapgenerator.commandgui.MapViewerRenderData
import io.hsar.mapgenerator.commandgui.MapViewerSettings
import io.hsar.mapgenerator.commandgui.MapViewerSetupInputs
import io.hsar.mapgenerator.commandgui.StaticImageLayer
import io.hsar.mapgenerator.grid.Coord
import io.hsar.mapgenerator.grid.GridLayerBuilder
import io.hsar.mapgenerator.grid.ReferenceSystem
import io.hsar.mapgenerator.gui.ImageFrame
import io.hsar.mapgenerator.image.ImageUtils.compose
import io.hsar.mapgenerator.image.ImageWriter
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.nio.file.Path
import java.util.*
import java.util.concurrent.CancellationException
import java.util.concurrent.ConcurrentHashMap
import javax.imageio.ImageIO
import kotlin.math.roundToInt
import kotlin.system.exitProcess

@Parameters(commandDescription = "Generates terrain maps and opens the KorGE map viewer.")
object LaunchGuiCommand : Command("show-gui") {
    @Parameter(
        names = ["--viewer"],
        description = "Viewer backend to launch: korge or compose.",
        required = false,
    )
    private var viewer: String = MapViewerBackend.KORGE.cliName

    @Parameter(
        names = ["--map"],
        description = "Path to a map image to prefill the create new operation screen.",
        required = false,
    )
    private var mapPath: String? = null

    @Parameter(
        names = ["--feature-flag"],
        description = "Enable an experimental command GUI feature. Repeat to enable multiple flags.",
        required = false,
    )
    private var featureFlags = mutableListOf<MapViewerFeatureFlag>()

    override fun run() {
        val staticLayers = loadStaticImageLayers(staticImagePaths)
        featureFlags.startupLogMessage()?.let(logger::info)

        MapViewerFrame(
            renderMap = ::renderViewerImage,
            initialSettings = viewerSettings(staticLayers),
            initialSetupInputs = initialSetupInputs(mapPath, staticImagePaths),
            backend = MapViewerBackend.fromCliName(viewer),
        ).showWindow()
    }

    private fun viewerSettings(staticLayers: List<LoadedStaticImageLayer>) = MapViewerSettings(
        availableLayers = buildViewerLayers(staticLayers),
        enabledLayerIds = defaultEnabledLayerIds(selectedLayers, staticLayers),
        staticLayerImages = staticLayers.associate { it.layer.id to it.image },
        featureFlags = featureFlags.toSet(),
        seed = seed,
        unitsPerPixel = metresPerPixel,
        metresPerContour = metresPerContour,
        width = width,
        height = height,
    )

    private fun List<MapViewerFeatureFlag>.startupLogMessage(): String? =
        takeIf { it.isNotEmpty() }?.let { flags ->
            "Enabled experimental GUI feature flag(s): ${flags.joinToString(", ")}"
        }
}

@Parameters(commandDescription = "Generates a random terrain map and shows it in a GUI window.")
object ShowImageCommand : Command("show-map") {
    override fun run() {
        val terrainMapGenerator = TerrainMapGenerator(
            metresPerPixel = metresPerPixel,
            metresPerContour = metresPerContour,
            width = width,
            height = height,
            seed = seed,
        )
        val staticLayers = loadStaticImageLayers(staticImagePaths)
        val selectedViewerLayers = selectedViewerLayers(selectedLayers, staticLayers)

        renderSelectedLayers(
            selectedLayers = selectedViewerLayers,
            renderers = builtInRenderers(terrainMapGenerator) + staticRenderers(staticLayers, width, height),
        )
            .forEach { image -> ImageFrame().showImage(image) }

    }
}

@Parameters(commandDescription = "Generates a random terrain map and saves it to disk.")
object SaveImageCommand : Command("save-map") {

    @Parameter(
        names = ["--target"],
        description = "Path to save the image to.",
        required = true
    )
    private lateinit var path: String

    override fun run() {
        val terrainMapGenerator = TerrainMapGenerator(
            metresPerPixel = metresPerPixel,
            metresPerContour = metresPerContour,
            width = width,
            height = height,
            seed = seed,
        )
        val staticLayers = loadStaticImageLayers(staticImagePaths)
        val selectedViewerLayers = selectedViewerLayers(selectedLayers, staticLayers)

        renderSelectedLayers(
            selectedLayers = selectedViewerLayers,
            renderers = builtInRenderers(terrainMapGenerator) + staticRenderers(staticLayers, width, height),
        )
            .zip(selectedViewerLayers)
            .forEach { (image, layer) ->
                val filePath = "${outputLayerName(layer)}-$path"
                ImageWriter.writeGreyScaleImage(image, Path.of(filePath))
                logger.info("Saved image to $filePath")
            }
    }
}

@Parameters(commandDescription = "Generates grid squares onto the given terrain map and saves it to disk next to the input file.")
object ApplyGridCommand : Command("apply-grid") {

    @Parameter(
        names = ["--target"],
        description = "Path to the originating image to.",
        required = true
    )
    private lateinit var path: String

    override fun run() {
        val terrainMapGenerator = TerrainMapGenerator(
            metresPerPixel = metresPerPixel,
            metresPerContour = metresPerContour,
            width = width,
            height = height,
            seed = seed,
        )

        if (MapLayer.GRID !in selectedLayers) {
            logger.warn("Grid layer is not enabled, so no grid image will be generated.")
            return
        }

        terrainMapGenerator.generateGridImage()
            .let { image ->
                val filePath = "grid-$path"
                ImageWriter.writeGreyScaleImage(image, Path.of(filePath))
                logger.info("Saved image to $filePath")
            }
    }
}

abstract class Command(val name: String) : Runnable {

    @Parameter(
        names = ["--layer"],
        description = "Map layer to enable. Repeat to enable more layers.",
        required = false
    )
    protected var layers = mutableListOf<MapLayer>()

    @Parameter(
        names = ["--seed"],
        description = "The seed used for deterministic map generation. Defaults to the current time.",
        required = false
    )
    protected var seed = System.currentTimeMillis()

    @Parameter(
        names = ["--metres-per-pixel", "--metresPerPixel"],
        description = "The scale of the map to generate, in metres per pixel. Defaults to 20, so a 1920x1080px image covers 38.4x21.6km.",
        required = false
    )
    protected var metresPerPixel = DEFAULT_METRES_PER_PIXEL

    @Parameter(
        names = ["--metresPerContour"],
        description = "The distance represented by each contour line, in metres. Defaults to 10.",
        required = false
    )
    protected var metresPerContour = 10.0

    @Parameter(
        names = ["--width"],
        description = "The width of the map to generate, in pixels. Defaults to 720.",
        required = false
    )
    protected var width = 1920

    @Parameter(
        names = ["--height"],
        description = "The height of the map to generate, in pixels. Defaults to 1080.",
        required = false
    )
    protected var height = 1080

    @Parameter(
        names = ["--image"],
        description = "Path to a static image layer to load. Repeat to add more layers.",
        required = false
    )
    protected var staticImagePaths = mutableListOf<String>()

    companion object {
        @JvmStatic
        protected val logger: Logger = LogManager.getLogger(Command::class.java)

        const val DEFAULT_METRES_PER_PIXEL = 20.0
    }

    protected val selectedLayers: Set<MapLayer>
        get() = layers.toSet()

    internal val configuredMetresPerPixel: Double
        get() = metresPerPixel

}

fun main(args: Array<String>) {
    val commandsByName = availableCommands().associateBy { it.name }

    JCommander()
        .also { commander ->
            commandsByName.forEach { (name, command) ->
                commander.addCommand(name, command)
            }
        }
        .let { commander ->
            if (args.isEmpty()) {
                commander.usage()
                System.err.println("Expected some arguments")
                exitProcess(1)
            }

            try {
                commander.parse(*args)
                commandsByName
                    .getValue(commander.parsedCommand)
                    .run()
            } catch (e: Exception) {
                e.printStackTrace()
                exitProcess(1)
            }
        }
}

internal fun availableCommands(): List<Command> =
    listOf(
        LaunchGuiCommand,
        ShowImageCommand,
        SaveImageCommand,
        ApplyGridCommand,
    )

private fun renderViewerImage(settings: MapViewerSettings, seed: Long): MapViewerRenderData {
    fun ensureNotCancelled() {
        if (Thread.currentThread().isInterrupted) {
            throw CancellationException("Render cancelled")
        }
    }

    val renderWidth = (settings.width * settings.zoom).roundToInt().coerceAtLeast(1)
    val renderHeight = (settings.height * settings.zoom).roundToInt().coerceAtLeast(1)
    val renderMetresPerPixel = settings.unitsPerPixel / settings.zoom
    val graphMapWidthMetres = settings.width * settings.unitsPerPixel
    val graphMapHeightMetres = settings.height * settings.unitsPerPixel

    val selectedLayers = settings.availableLayers.filter { it.id in settings.enabledLayerIds }
    val baseLayers = selectedLayers.filterNot { it.id == MapLayer.GRID.id }
    val baseKey = BaseRenderKey(
        seed = seed,
        width = renderWidth,
        height = renderHeight,
        metresPerPixel = renderMetresPerPixel,
        metresPerContour = settings.metresPerContour,
        layers = baseLayers.map { it.id }.toSet(),
    )
    val terrainMapGenerator by lazy {
        TerrainMapGenerator(
            metresPerPixel = renderMetresPerPixel,
            metresPerContour = settings.metresPerContour,
            width = renderWidth,
            height = renderHeight,
            graphMapWidthMetres = graphMapWidthMetres,
            graphMapHeightMetres = graphMapHeightMetres,
            seed = seed,
        )
    }

    val baseImage = RenderCache.baseImages.getOrPut(baseKey) {
        if (baseLayers.isEmpty()) {
            io.hsar.mapgenerator.image.ImageBuilder(renderWidth, renderHeight)
                .fillTransparent()
                .build()
        } else {
            val layers: List<BufferedImage> = baseLayers
                .map { layer ->
                    ensureNotCancelled()
                    when (layer) {
                        is MapLayer -> when (layer) {
                            MapLayer.GRAPH -> terrainMapGenerator.generateGraphImage()
                            MapLayer.HEIGHT -> terrainMapGenerator.generateHeightImage()
                            MapLayer.CONTOUR -> terrainMapGenerator.generateContourImage()
                            MapLayer.GRID -> error("Grid layer is rendered separately")
                        }
                        is StaticImageLayer -> settings.staticLayerImages.getValue(layer.id).resizeTo(renderWidth, renderHeight)
                        else -> error("Unsupported layer type: ${layer::class.simpleName}")
                    }
                }

            ensureNotCancelled()
            layers.compose()
        }
    }

    val gridConfiguration = resolveGridConfiguration(settings, baseImage, seed)
    val gridColour = settings.gridColour ?: GridColourSelector.bestPresetFor(baseImage)

    val gridLayer = if (MapLayer.GRID.id in settings.enabledLayerIds) {
        val gridKey = GridRenderKey(
            seed = seed,
            width = renderWidth,
            height = renderHeight,
            metresPerPixel = renderMetresPerPixel,
            metresPerContour = settings.metresPerContour,
            mapId = gridConfiguration.mapId,
            referenceSystem = gridConfiguration.referenceSystem,
            gridStartX = gridConfiguration.startX,
            gridStartY = gridConfiguration.startY,
            gridColour = GridColourSelector.toHex(gridColour),
        )
        RenderCache.gridLayers.getOrPut(gridKey) {
            GridLayerBuilder.build(
                width = renderWidth,
                height = renderHeight,
                gridScale = gridConfiguration.referenceSystem.gridScale,
                unitsPerPixel = renderMetresPerPixel,
                random = Random(gridSeed(seed)),
                mapId = gridConfiguration.mapId,
                referenceSystem = gridConfiguration.referenceSystem,
                verticalLineNumberStart = gridConfiguration.startX,
                horizontalLineNumberStart = gridConfiguration.startY,
                colour = gridColour,
            )
        }
    } else {
        null
    }

    return MapViewerRenderData(
        baseImage = baseImage,
        gridLayer = gridLayer,
    )
}

private fun renderViewerImage(settings: MapViewerSettings): MapViewerRenderData =
    renderViewerImage(settings, settings.seed)

internal fun resolveGridConfiguration(
    settings: MapViewerSettings,
    fallbackImage: BufferedImage,
    seed: Long,
): RenderGridConfiguration =
    settings.mapId
        ?.takeIf { it.isNotBlank() }
        ?.let { mapId ->
            val startX = settings.gridStartX
            val startY = settings.gridStartY
            if (startX != null && startY != null) {
                RenderGridConfiguration(
                    mapId = mapId,
                    referenceSystem = settings.referenceSystem,
                    startX = startX,
                    startY = startY,
                )
            } else {
                null
            }
        } ?: fallbackGridConfiguration(fallbackImage, settings.unitsPerPixel, seed, settings.referenceSystem)

internal fun renderSelectedLayers(
    selectedLayers: List<MapViewerLayer>,
    renderers: Map<String, () -> BufferedImage>,
): List<BufferedImage> =
    selectedLayers
        .map { layer -> renderers.getValue(layer.id).invoke() }

private data class BaseRenderKey(
    val seed: Long,
    val width: Int,
    val height: Int,
    val metresPerPixel: Double,
    val metresPerContour: Double,
    val layers: Set<String>,
)

private data class GridRenderKey(
    val seed: Long,
    val width: Int,
    val height: Int,
    val metresPerPixel: Double,
    val metresPerContour: Double,
    val mapId: String?,
    val referenceSystem: ReferenceSystem,
    val gridStartX: Coord?,
    val gridStartY: Coord?,
    val gridColour: String,
)

internal data class RenderGridConfiguration(
    val mapId: String,
    val referenceSystem: ReferenceSystem = ReferenceSystem.MGRS,
    val startX: Coord,
    val startY: Coord,
)

private object RenderCache {
    val baseImages = ConcurrentHashMap<BaseRenderKey, BufferedImage>()
    val gridLayers = ConcurrentHashMap<GridRenderKey, io.hsar.mapgenerator.grid.GridLayer>()
}

private fun fallbackGridConfiguration(
    mapImage: BufferedImage,
    metresPerPixel: Double,
    seed: Long,
    referenceSystem: ReferenceSystem = ReferenceSystem.MGRS,
): RenderGridConfiguration =
    GridLayerBuilder.build(
        width = mapImage.width,
        height = mapImage.height,
        gridScale = referenceSystem.gridScale,
        unitsPerPixel = metresPerPixel,
        referenceSystem = referenceSystem,
        random = Random(gridSeed(seed)),
    ).let { gridLayer ->
        RenderGridConfiguration(
            mapId = gridLayer.mapId,
            referenceSystem = gridLayer.referenceSystem,
            startX = gridLayer.verticalLineNumberStart,
            startY = gridLayer.horizontalLineNumberStart,
        )
    }

private fun gridSeed(seed: Long): Long =
    Random(seed).let { random ->
        random.nextLong()
        random.nextLong()
        random.nextLong()
    }

internal data class LoadedStaticImageLayer(
    val layer: StaticImageLayer,
    val image: BufferedImage,
)

private fun buildViewerLayers(staticLayers: List<LoadedStaticImageLayer>): List<MapViewerLayer> =
    MapLayer.values().toList() + staticLayers.map { it.layer }

internal fun defaultEnabledLayerIds(
    selectedLayers: Set<MapLayer>,
    staticLayers: List<LoadedStaticImageLayer>,
): Set<String> =
    selectedBuiltInLayers(selectedLayers)
        .map { it.id }
        .toSet() + staticLayers.map { it.layer.id }

internal fun selectedViewerLayers(
    selectedLayers: Set<MapLayer>,
    staticLayers: List<LoadedStaticImageLayer>,
): List<MapViewerLayer> {
    val selectedBuiltInIds = selectedBuiltInLayers(selectedLayers)
        .map { it.id }
        .toSet()

    return buildViewerLayers(staticLayers)
        .filter { it.id in selectedBuiltInIds || it is StaticImageLayer }
}

private fun selectedBuiltInLayers(selectedLayers: Set<MapLayer>): Set<MapLayer> =
    selectedLayers.takeIf { it.isNotEmpty() } ?: MapLayer.values().toSet()

private fun builtInRenderers(terrainMapGenerator: TerrainMapGenerator): Map<String, () -> BufferedImage> =
    mapOf(
        MapLayer.GRAPH.id to { terrainMapGenerator.generateGraphImage() },
        MapLayer.HEIGHT.id to { terrainMapGenerator.generateHeightImage() },
        MapLayer.CONTOUR.id to { terrainMapGenerator.generateContourImage() },
        MapLayer.GRID.id to { terrainMapGenerator.generateGridImage() },
    )

private fun staticRenderers(
    staticLayers: List<LoadedStaticImageLayer>,
    width: Int,
    height: Int,
): Map<String, () -> BufferedImage> =
    staticLayers.associate { asset ->
        asset.layer.id to { asset.image.resizeTo(width, height) }
    }

private fun loadStaticImageLayers(imagePaths: List<String>): List<LoadedStaticImageLayer> {
    if (imagePaths.isEmpty()) {
        return emptyList()
    }

    val loaded = imagePaths.map { path ->
        val absolutePath = Path.of(path).toAbsolutePath().normalize()
        val image = ImageIO.read(absolutePath.toFile())
            ?: error("Could not read image from $absolutePath")

        LoadedStaticImageLayer(
            layer = StaticImageLayer(
                id = absolutePath.toString(),
                displayName = absolutePath.fileName?.toString() ?: absolutePath.toString(),
            ),
            image = image,
        )
    }

    val targetWidth = loaded.maxOf { it.image.width }
    val targetHeight = loaded.maxOf { it.image.height }

    return loaded.map { asset ->
        if (asset.image.width == targetWidth && asset.image.height == targetHeight) {
            asset
        } else {
            asset.copy(image = asset.image.resizeTo(targetWidth, targetHeight))
        }
    }
}

private fun BufferedImage.resizeTo(width: Int, height: Int): BufferedImage =
    if (this.width == width && this.height == height) {
        this
    } else {
        BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB).also { image ->
            image.withGraphics { graphics ->
                graphics.setRenderingHint(
                    RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR,
                )
                graphics.drawImage(this, 0, 0, width, height, null)
            }
        }
    }

private inline fun BufferedImage.withGraphics(block: (Graphics2D) -> Unit) {
    val graphics = createGraphics()
    try {
        block(graphics)
    } finally {
        graphics.dispose()
    }
}

private fun outputLayerName(layer: MapViewerLayer): String =
    when (layer) {
        is MapLayer -> layer.name.lowercase()
        is StaticImageLayer -> Path.of(layer.id).fileName?.toString()?.substringBeforeLast('.', layer.displayName)
            ?.replace(Regex("[^A-Za-z0-9._-]"), "_")
            ?: layer.displayName.replace(Regex("[^A-Za-z0-9._-]"), "_")
        else -> layer.displayName.replace(Regex("[^A-Za-z0-9._-]"), "_")
    }

internal fun initialSetupInputs(
    mapPath: String?,
    legacyImagePaths: List<String>,
): MapViewerSetupInputs {
    val path = mapPath ?: legacyImagePaths.firstOrNull() ?: return MapViewerSetupInputs()
    val absolutePath = Path.of(path).toAbsolutePath().normalize()
    val image = ImageIO.read(absolutePath.toFile())
        ?: error("Could not read image from $absolutePath")

    return MapViewerSetupInputs(
        mapImagePath = absolutePath.toString(),
        mapImage = image,
    )
}
