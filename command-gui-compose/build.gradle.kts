import org.gradle.api.tasks.WriteProperties

plugins {
    id("terrain.kotlin-jvm")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

dependencies {
    implementation(project(":command-gui"))
    implementation(compose.desktop.currentOs)
    implementation(libs.compose.resources)
    implementation(libs.compose.material3)
    implementation(libs.log4j.api)
    testImplementation("org.jetbrains.compose.ui:ui-test:1.11.1")
    runtimeOnly(libs.log4j.core)
}

val generatedVersionResources = layout.buildDirectory.dir("generated/versionResources")
val generateVersionResource by tasks.registering(WriteProperties::class) {
    destinationFile = generatedVersionResources.get().file("command-gui-compose-version.properties").asFile
    property("version", project.version.toString())
}

sourceSets.main {
    resources.srcDir(generatedVersionResources)
}

tasks.named("processResources") {
    dependsOn(generateVersionResource)
}
