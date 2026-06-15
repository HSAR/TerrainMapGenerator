package io.hsar.mapgenerator.commandline

import com.beust.jcommander.JCommander
import com.beust.jcommander.Parameter
import com.beust.jcommander.Parameters
import io.hsar.mapgenerator.TerrainMapGenerator
import io.hsar.mapgenerator.commandgui.MapLayer
import io.hsar.mapgenerator.commandgui.MapViewerFrame
import io.hsar.mapgenerator.commandgui.MapViewerSettings
import io.hsar.mapgenerator.gui.ImageFrame
import io.hsar.mapgenerator.image.ImageWriter
import java.nio.file.Path
import javax.swing.SwingUtilities
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import kotlin.system.exitProcess

@Parameters(commandDescription = "Generates terrain maps and opens the Swing GUI viewer.")
object LaunchGuiCommand : Command("show-gui") {
    override fun run() {
        val settings = MapViewerSettings(
            metresPerPixel = metresPerPixel,
            metresPerContour = metresPerContour,
            width = width,
            height = height,
        )
        val terrainMapGenerator = TerrainMapGenerator(
            metresPerPixel = metresPerPixel,
            metresPerContour = metresPerContour,
            width = width,
            height = height,
        )
        val imagesByLayer = mapOf(
            MapLayer.GRAPH to terrainMapGenerator.generateGraphImage(),
            MapLayer.HEIGHT to terrainMapGenerator.generateHeightImage(),
            MapLayer.CONTOUR to terrainMapGenerator.generateContourImage(),
            MapLayer.GRID to terrainMapGenerator.generateGridImage(),
        )

        SwingUtilities.invokeLater {
            MapViewerFrame(imagesByLayer, settings).showWindow()
        }
    }
}

@Parameters(commandDescription = "Generates a random terrain map and shows it in a GUI window.")
object ShowImageCommand : Command("show-map") {
    override fun run() {
        TerrainMapGenerator(
            metresPerPixel = metresPerPixel,
            metresPerContour = metresPerContour,
            width = width,
            height = height,
        ).also {
            it.generateGraphImage()
                .let { image ->
                    ImageFrame().showImage(image)
                }
//            it.generateHeightImage()
//                .let { image ->
//                    ImageFrame().showImage(image)
//                }
            it.generateContourImage()
                .let { image ->
                    ImageFrame().showImage(image)
                }
            it.generateGridImage()
                .let { image ->
                    ImageFrame().showImage(image)
                }
        }

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
        TerrainMapGenerator(
            metresPerPixel = metresPerPixel,
            metresPerContour = metresPerContour,
            width = width,
            height = height,
        ).also {
            it.generateGraphImage()
                .let { image ->
                    val filePath = "graph-$path"
                    ImageWriter.writeGreyScaleImage(image, Path.of(filePath))
                    logger.info("Saved image to $filePath")
                }
//            it.generateHeightImage()
//                .let { image ->
//                    val filePath = "height-$path"
//                    ImageWriter.writeGreyScaleImage(image, Path.of(filePath))
//                    logger.info("Saved image to $filePath")
//                }
//            it.generateContourImage()
//                .let { image ->
//                    val filePath = "contour-$path"
//                    ImageWriter.writeGreyScaleImage(image, Path.of(filePath))
//                    logger.info("Saved image to $filePath")
//                }
//            it.generateGridImage()
//                .let { image ->
//                    val filePath = "grid-$path"
//                    ImageWriter.writeGreyScaleImage(image, Path.of(filePath))
//                    logger.info("Saved image to $filePath")
//                }
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
        TerrainMapGenerator(
            metresPerPixel = metresPerPixel,
            metresPerContour = metresPerContour,
            width = width,
            height = height,
        ).also {
            it.generateGridImage()
                .let { image ->
                    val filePath = "grid-$path"
                    ImageWriter.writeGreyScaleImage(image, Path.of(filePath))
                    logger.info("Saved image to $filePath")
                }
        }
    }
}

abstract class Command(val name: String) : Runnable {

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
