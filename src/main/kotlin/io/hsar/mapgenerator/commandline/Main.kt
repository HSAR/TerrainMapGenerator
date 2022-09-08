package io.hsar.mapgenerator.commandline

import com.beust.jcommander.JCommander
import com.beust.jcommander.Parameter
import com.beust.jcommander.Parameters
import io.hsar.mapgenerator.gui.ImageFrame
import io.hsar.mapgenerator.image.ImageWriter
import io.hsar.mapgenerator.terrain.TerrainMapGenerator
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import java.nio.file.Path
import kotlin.system.exitProcess

@Parameters(commandDescription = "Generates a random terrain map and shows it in a GUI window.")
object ShowImageCommand : Command("show-map") {
    override fun run() {
        generator
            .generateImage()
            .let { image ->
                ImageFrame.showImage(image)
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
        generator
            .generateImage()
            .let { image ->
                ImageWriter.writeGreyScaleImage(image, Path.of(path))
                logger.info("Saved image to $path")
            }
    }
}

abstract class Command(val name: String) : Runnable {

    @Parameter(
        names = ["--metresPerPixel"],
        description = "The scale of the map to generate, in metres per pixel. Defaults to 10, so a 1080x720px image would cover 10.8x7.2km.",
        required = false
    )
    private var metresPerPixel = 10.0

    @Parameter(
        names = ["--metresPerContour"],
        description = "The distance represented by each contour line, in metres. Defaults to 10.",
        required = false
    )
    private var metresPerContour = 10.0

    @Parameter(
        names = ["--height"],
        description = "The height of the map to generate, in pixels. Defaults to 1080.",
        required = false
    )
    private var height = 720

    @Parameter(
        names = ["--width"],
        description = "The width of the map to generate, in pixels. Defaults to 720.",
        required = false
    )
    private var width = 1080

    protected val generator = TerrainMapGenerator(
        metresPerPixel = metresPerPixel,
        metresPerContour = metresPerContour,
        width = width,
        height = height,
    )

    companion object {
        @JvmStatic
        protected val logger: Logger = LogManager.getLogger(Command::class.java)
    }

}

fun main(args: Array<String>) {
    val commandsByName =
        listOf(
            ShowImageCommand,
            SaveImageCommand,
        )
            .associateBy { it.name }

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
