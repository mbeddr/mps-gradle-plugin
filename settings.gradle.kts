pluginManagement {
    includeBuild("git-based-versioning")
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version ("0.8.0")
}

rootProject.name = "mps-gradle-plugin"

includeBuild("git-based-versioning")
