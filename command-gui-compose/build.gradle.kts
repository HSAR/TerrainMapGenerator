import jdk.tools.jlink.resources.plugins

plugins {
    id("terrain.kotlin-jvm")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

dependencies {
    implementation(project(":command-gui"))
    implementation(compose.desktop.currentOs)
    implementation(libs.compose.material3)
    implementation(libs.log4j.api)
    runtimeOnly(libs.log4j.core)
}
