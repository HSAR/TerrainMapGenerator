plugins {
    id("org.jetbrains.compose") version "1.11.1" apply false
}

group = "io.hsar.mapgenerator"
version = "0.0.1-SNAPSHOT"

tasks.register("testAll") {
    group = "verification"
    description = "Runs tests in every project."
    dependsOn(subprojects.map { "${it.path}:test" })
}
