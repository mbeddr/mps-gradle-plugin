pluginManagement {
    includeBuild("git-based-versioning")

    val kotlinVersion: String by settings
    val kotlinDSLVersion: String by settings
    val binaryCompatibilityValidator: String by settings
    
    plugins {
        kotlin("jvm") version kotlinVersion
        `kotlin-dsl` version kotlinDSLVersion
        id("org.jetbrains.kotlinx.binary-compatibility-validator") version binaryCompatibilityValidator
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version ("0.8.0")
}

rootProject.name = "mps-gradle-plugin"

includeBuild("git-based-versioning")
