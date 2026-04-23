pluginManagement {
    includeBuild("git-based-versioning")
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version ("1.0.0")
}

rootProject.name = "mps-gradle-plugin"

includeBuild("git-based-versioning")
includeBuild("mps-gradle-plugin-api")
includeBuild("mps-gradle-plugin")
