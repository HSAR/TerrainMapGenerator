package io.hsar.mapgenerator.commandline

import com.beust.jcommander.JCommander
import com.beust.jcommander.Parameter
import com.beust.jcommander.Parameters
import io.hsar.mapgenerator.terrain.TerrainMapGenerator
import java.nio.file.Path
import kotlin.system.exitProcess

abstract class Command(val name: String) : Runnable

@Parameters(commandDescription = "Generates a random terrain map.")
class TerrainMapGeneratorCommand : Command("generate-map") {

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
    private var height = 1080

    @Parameter(
        names = ["--width"],
        description = "The width of the map to generate, in pixels. Defaults to 720.",
        required = false
    )
    private var width = 720

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
            height = height,
            width = width,
        ).generateImage(path = Path.of(path))
    }
}

fun main(args: Array<String>) {
    val commandsByName =
            listOf(
                    TerrainMapGeneratorCommand()
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
