package io.hsar.mapgenerator.desktop

fun main(arguments: Array<String>) {
    if (arguments.isEmpty()) {
        io.hsar.mapgenerator.commandline.main(arrayOf("show-gui", "--viewer=compose"))
    } else {
        io.hsar.mapgenerator.commandline.main(arguments)
        kotlin.system.exitProcess(0)
    }
}
