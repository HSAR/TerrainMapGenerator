import java.io.DataOutputStream

plugins {
    id("terrain.kotlin-jvm")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

dependencies {
    implementation(project(":library"))
    implementation(project(":contour-generator"))
    implementation(project(":facility-generator"))
    implementation(project(":ship-generator"))
    implementation(project(":command-gui"))
    runtimeOnly(project(":command-gui-compose"))
    implementation(libs.compose.runtime)
    implementation(libs.jcommander)
    implementation(libs.log4j.api)
    runtimeOnly(libs.log4j.core)
}

tasks.withType<JavaExec>().configureEach {
    providers.systemProperty("commandgui.compose.debugMapObjects").orNull?.let { value ->
        systemProperty("commandgui.compose.debugMapObjects", value)
    }
}

tasks.processResources {
    from(rootProject.file("LICENSE")) {
        into("licenses")
    }
}

compose.desktop {
    application {
        mainClass = "io.hsar.mapgenerator.commandline.MainKt"
        nativeDistributions {
            modules("java.desktop", "java.logging", "java.management", "jdk.accessibility")
            packageName = "TerrainMapGenerator"
            packageVersion = "0.0.1"
            windows {
                iconFile.set(layout.buildDirectory.file("generated/app-icon.ico"))
            }
        }
    }
}

val appIconSources = listOf(64, 128, 256).map { size ->
    project(":command-gui-compose").layout.projectDirectory.file("src/main/resources/logo${size.toString().padStart(3, '0')}.png")
}

val generateWindowsIcon by tasks.registering {
    val output = layout.buildDirectory.file("generated/app-icon.ico")
    inputs.files(appIconSources)
    outputs.file(output)

    doLast {
        val images = inputs.files.files
            .sortedBy { file -> file.name }
            .map { file -> file.readBytes() }
        val iconFile = outputs.files.singleFile
        iconFile.parentFile.mkdirs()
        DataOutputStream(iconFile.outputStream().buffered()).use { stream ->
            fun writeShort(value: Int) {
                stream.write(value and 0xFF)
                stream.write(value ushr 8 and 0xFF)
            }
            fun writeInt(value: Int) {
                stream.write(value and 0xFF)
                stream.write(value ushr 8 and 0xFF)
                stream.write(value ushr 16 and 0xFF)
                stream.write(value ushr 24 and 0xFF)
            }

            writeShort(0)
            writeShort(1)
            writeShort(images.size)
            var offset = 6 + images.size * 16
            images.zip(listOf(64, 128, 256)).forEach { (image, size) ->
                stream.write(if (size == 256) 0 else size)
                stream.write(if (size == 256) 0 else size)
                stream.write(0)
                stream.write(0)
                writeShort(1)
                writeShort(32)
                writeInt(image.size)
                writeInt(offset)
                offset += image.size
            }
            images.forEach(stream::write)
        }
    }
}

tasks.configureEach {
    if (name == "createDistributable" || name.startsWith("package")) {
        dependsOn(generateWindowsIcon)
    }
}

tasks.register<Zip>("portableZip") {
    group = "distribution"
    description = "Builds a self-contained, extract-and-run Windows distribution."
    dependsOn("createDistributable")
    from(layout.buildDirectory.dir("compose/binaries/main/app"))
    from(rootProject.file("LICENSE")) {
        into("TerrainMapGenerator/licenses")
    }
    archiveBaseName.set("terrain-map-generator-windows")
    archiveVersion.set(project.version.toString())
    destinationDirectory.set(layout.buildDirectory.dir("distributions"))
}
