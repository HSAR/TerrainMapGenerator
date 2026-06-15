package io.hsar.mapgenerator.commandline

import com.beust.jcommander.JCommander
import com.beust.jcommander.Parameter
import com.beust.jcommander.Parameters
import io.hsar.mapgenerator.TerrainMapGenerator
import io.hsar.mapgenerator.commandgui.MapLayer
import io.hsar.mapgenerator.commandgui.MapViewerRenderData
import io.hsar.mapgenerator.commandgui.MapViewerFrame
import io.hsar.mapgenerator.commandgui.MapViewerSettings
import io.hsar.mapgenerator.gui.ImageFrame
import io.hsar.mapgenerator.image.ImageUtils.compose
import io.hsar.mapgenerator.image.ImageWriter
import java.nio.file.Path
import java.awt.image.BufferedImage
import javax.swing.SwingUtilities
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import kotlin.math.roundToInt
import java.util.concurrent.CancellationException
import java.util.concurrent.ConcurrentHashMap
import kotlin.system.exitProcess

@Parameters(commandDescription = "Generates terrain maps and opens the Swing GUI viewer.")
object LaunchGuiCommand : Command("show-gui") {
    override fun run() {
        val sessionSeed = seed
        val settings = MapViewerSettings(
            enabledLayers = selectedLayers,
            seed = sessionSeed,
            metresPerPixel = metresPerPixel,
            metresPerContour = metresPerContour,
            width = width,
            height = height,
        )

        SwingUtilities.invokeLater {
            MapViewerFrame(
                renderMap = { renderViewerImage(it, sessionSeed) },
                initialSettings = settings,
            ).showWindow()
        }
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

        renderSelectedLayers(
            selectedLayers = selectedLayers,
            renderers = mapOf(
                MapLayer.GRAPH to { terrainMapGenerator.generateGraphImage() },
                MapLayer.HEIGHT to { terrainMapGenerator.generateHeightImage() },
                MapLayer.CONTOUR to { terrainMapGenerator.generateContourImage() },
                MapLayer.GRID to { terrainMapGenerator.generateGridImage() },
            ),
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

        renderSelectedLayers(
            selectedLayers = selectedLayers,
            renderers = mapOf(
                MapLayer.GRAPH to { terrainMapGenerator.generateGraphImage() },
                MapLayer.HEIGHT to { terrainMapGenerator.generateHeightImage() },
                MapLayer.CONTOUR to { terrainMapGenerator.generateContourImage() },
                MapLayer.GRID to { terrainMapGenerator.generateGridImage() },
            ),
        )
            .zip(selectedLayers.sortedBy { it.ordinal })
            .forEach { (image, layer) ->
                val filePath = "${layer.name.lowercase()}-$path"
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
        names = ["--metresPerPixel"],
        description = "The scale of the map to generate, in metres per pixel. Defaults to 2, so a 1080x720px image would cover 2.2x1.4km.",
        required = false
    )
    protected var metresPerPixel = 2.0

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

    companion object {
        @JvmStatic
        protected val logger: Logger = LogManager.getLogger(Command::class.java)
    }

    protected val selectedLayers: Set<MapLayer>
        get() = layers.toSet()

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
    val renderMetresPerPixel = settings.metresPerPixel / settings.zoom
    val graphMapWidthMetres = settings.width * settings.metresPerPixel
    val graphMapHeightMetres = settings.height * settings.metresPerPixel

    val baseLayers = settings.enabledLayers - MapLayer.GRID
    val baseKey = BaseRenderKey(
        seed = seed,
        width = renderWidth,
        height = renderHeight,
        metresPerPixel = renderMetresPerPixel,
        metresPerContour = settings.metresPerContour,
        layers = baseLayers,
    )
    val gridKey = GridRenderKey(
        seed = seed,
        width = renderWidth,
        height = renderHeight,
        metresPerPixel = renderMetresPerPixel,
        metresPerContour = settings.metresPerContour,
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
            val layers = baseLayers
                .sortedBy { it.ordinal }
                .map { layer ->
                    ensureNotCancelled()
                    when (layer) {
                        MapLayer.GRAPH -> terrainMapGenerator.generateGraphImage()
                        MapLayer.HEIGHT -> terrainMapGenerator.generateHeightImage()
                        MapLayer.CONTOUR -> terrainMapGenerator.generateContourImage()
                        MapLayer.GRID -> error("Grid layer is rendered separately")
                    }
                }

            ensureNotCancelled()
            layers.compose()
        }
    }

    val gridLayer = if (MapLayer.GRID in settings.enabledLayers) {
        RenderCache.gridLayers.getOrPut(gridKey) {
            terrainMapGenerator.generateGridLayer()
        }
    } else {
        null
    }

    return MapViewerRenderData(
        baseImage = baseImage,
        gridLayer = gridLayer,
    )
}

internal fun renderSelectedLayers(
    selectedLayers: Set<MapLayer>,
    renderers: Map<MapLayer, () -> BufferedImage>,
): List<BufferedImage> =
    selectedLayers
        .sortedBy { it.ordinal }
        .map { layer -> renderers.getValue(layer).invoke() }

private data class BaseRenderKey(
    val seed: Long,
    val width: Int,
    val height: Int,
    val metresPerPixel: Double,
    val metresPerContour: Double,
    val layers: Set<MapLayer>,
)

private data class GridRenderKey(
    val seed: Long,
    val width: Int,
    val height: Int,
    val metresPerPixel: Double,
    val metresPerContour: Double,
)

private object RenderCache {
    val baseImages = ConcurrentHashMap<BaseRenderKey, BufferedImage>()
    val gridLayers = ConcurrentHashMap<GridRenderKey, io.hsar.mapgenerator.grid.GridLayer>()
}
