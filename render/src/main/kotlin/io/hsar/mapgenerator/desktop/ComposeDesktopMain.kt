package io.hsar.mapgenerator.desktop

fun main(arguments: Array<String>) {
    configureComposeDesktopRendering()
    if (arguments.isEmpty()) {
        io.hsar.mapgenerator.commandline.main(arrayOf("show-gui", "--viewer=compose"))
    } else {
        io.hsar.mapgenerator.commandline.main(arguments)
        kotlin.system.exitProcess(0)
    }
}

internal fun configureComposeDesktopRendering(properties: java.util.Properties = System.getProperties()) {
    if (properties.getProperty("skiko.gpu.resourceCacheLimit").isNullOrBlank()) {
        properties.setProperty("skiko.gpu.resourceCacheLimit", "512M")
    }
}
