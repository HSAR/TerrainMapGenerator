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
        }
    }
}

listOf(
    "runComposeGui" to "compose",
    "runKorgeGui" to "korge",
).forEach { (taskName, viewer) ->
    tasks.register<JavaExec>(taskName) {
        group = "application"
        description = "Launches the $viewer map viewer with its development arguments."
        classpath = sourceSets.main.get().runtimeClasspath
        mainClass.set("io.hsar.mapgenerator.commandline.MainKt")
        args("show-gui", "--viewer", viewer)
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
