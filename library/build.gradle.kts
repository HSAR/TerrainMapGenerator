plugins {
    id("terrain.kotlin-jvm")
}

dependencies {
    api(libs.tektosyne)
    api(libs.opensimplex)
    implementation(libs.log4j.api)
    runtimeOnly(libs.log4j.core)
}
