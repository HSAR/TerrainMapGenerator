pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        google()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        google()
    }
}

rootProject.name = "terrain-map-generator"

include(
    "library",
    "facility-generator",
    "ship-generator",
    "contour-generator",
    "command-gui",
    "command-gui-compose",
    "render",
)
