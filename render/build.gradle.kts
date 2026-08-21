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
        mainClass = "io.hsar.mapgenerator.desktop.ComposeDesktopMainKt"
        nativeDistributions {
            modules("java.desktop", "java.logging", "java.management", "java.naming", "jdk.accessibility")
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

val portableZip by tasks.registering(Zip::class) {
    group = "distribution"
    description = "Builds a self-contained, extract-and-run Windows distribution."
    dependsOn("createDistributable")
    from(layout.buildDirectory.dir("compose/binaries/main/app"))
    from(rootProject.file("LICENSE")) {
        into("TerrainMapGenerator/licenses")
        rename { "PROJECT_LICENSE.txt" }
    }
    from(project(":command-gui-compose").layout.projectDirectory.file("src/main/resources/licenses/THIRD_PARTY_NOTICES.txt")) {
        into("TerrainMapGenerator/licenses")
    }
    archiveBaseName.set("terrain-map-generator-windows")
    archiveVersion.set(project.version.toString())
    destinationDirectory.set(layout.buildDirectory.dir("distributions"))
}

val portableSmokeRoot = layout.buildDirectory.dir("portable smoke é")
val preparePortableSmoke by tasks.registering(Sync::class) {
    dependsOn(portableZip)
    from(portableZip.map { archive -> zipTree(archive.archiveFile.get().asFile) })
    into(portableSmokeRoot)
}

tasks.register<Exec>("portableSmoke") {
    group = "verification"
    description = "Runs create/save/close/reopen through the packaged Windows executable from Unicode paths."
    dependsOn(preparePortableSmoke)
    onlyIf { System.getProperty("os.name").startsWith("Windows", ignoreCase = true) }
    val executable = portableSmokeRoot.get().file("TerrainMapGenerator/TerrainMapGenerator.exe").asFile
    val smokeData = portableSmokeRoot.get().dir("données with spaces").asFile
    workingDir(executable.parentFile)
    commandLine(executable.absolutePath, "portable-smoke", "--target", smokeData.absolutePath)
}

val portableRendererReportRoot = layout.buildDirectory.dir("reports/parity/slice6")

val portableRendererSmokeDirect3d by tasks.registering(Exec::class) {
    group = "verification"
    description = "Presents two frames through the packaged Direct3D Compose renderer."
    dependsOn(preparePortableSmoke)
    onlyIf { System.getProperty("os.name").startsWith("Windows", ignoreCase = true) }
    val executable = portableSmokeRoot.get().file("TerrainMapGenerator/TerrainMapGenerator.exe").asFile
    val report = portableRendererReportRoot.get().file("direct3d.txt").asFile
    workingDir(executable.parentFile)
    environment("SKIKO_RENDER_API", "DIRECT3D")
    commandLine(executable.absolutePath, "portable-renderer-smoke", "--report", report.absolutePath)
    outputs.file(report)
}

val portableRendererSmokeSoftware by tasks.registering(Exec::class) {
    group = "verification"
    description = "Presents two frames through the packaged software Compose renderer."
    dependsOn(preparePortableSmoke)
    onlyIf { System.getProperty("os.name").startsWith("Windows", ignoreCase = true) }
    val executable = portableSmokeRoot.get().file("TerrainMapGenerator/TerrainMapGenerator.exe").asFile
    val report = portableRendererReportRoot.get().file("software.txt").asFile
    workingDir(executable.parentFile)
    environment("SKIKO_RENDER_API", "SOFTWARE")
    commandLine(executable.absolutePath, "portable-renderer-smoke", "--report", report.absolutePath)
    outputs.file(report)
}

portableRendererSmokeSoftware.configure {
    mustRunAfter(portableRendererSmokeDirect3d)
}

tasks.register("portableRendererSmoke") {
    group = "verification"
    description = "Runs packaged Direct3D and software renderer frame probes."
    dependsOn(portableRendererSmokeDirect3d, portableRendererSmokeSoftware)
}
