import jdk.tools.jlink.resources.plugins

plugins {
    id("terrain.kotlin-jvm")
}

dependencies {
    implementation(project(":library"))
    implementation(libs.fastnoise)
    implementation(libs.log4j.api)
    runtimeOnly(libs.log4j.core)
}
