import jdk.tools.jlink.resources.plugins

plugins {
    id("terrain.kotlin-jvm")
}

dependencies {
    api(project(":library"))
    implementation(project(":contour-generator"))
    implementation(libs.korge)
    implementation(libs.batik.transcoder)
    implementation(libs.jackson.databind)
    implementation(libs.jackson.kotlin)
    implementation(libs.log4j.api)
    runtimeOnly(libs.log4j.core)
}
