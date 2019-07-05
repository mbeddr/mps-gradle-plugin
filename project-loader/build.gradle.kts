import de.itemis.mps.gradle.GitBasedVersioning
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    `maven-publish`
    `java-gradle-plugin`
}

group = "de.itemis.mps"

val mpsVersion: String by project
val kotlinArgParserVersion: String by project
val kotlinApiVersion: String by project
val kotlinVersion: String by project

val nexusUsername: String? by project
val nexusPassword: String? by project

val pluginVersion = "1"

version = if (project.hasProperty("forceCI") || project.hasProperty("teamcity")) {
    val fullVersion = GitBasedVersioning.getVersion(mpsVersion, pluginVersion)
    // maintenance builds for specific MPS versions should be published without branch prefix, so that they can be
    // resolved as dependency from the gradle plugin using version spec "de.itemis.mps:modelcheck:$mpsVersion+"
    GitBasedVersioning.stripMaintenancePrefix(fullVersion)
} else {
    "$mpsVersion.$pluginVersion-SNAPSHOT"
}

repositories {
    mavenCentral()
    maven {
        url = uri("https://projects.itemis.de/nexus/content/repositories/mbeddr")
    }
}

val mpsConfiguration = configurations.create("mps")

dependencies {
    implementation(kotlin("stdlib-jdk8", version = kotlinVersion))
    mpsConfiguration("com.jetbrains:mps:$mpsVersion")
    implementation("com.xenomachina:kotlin-argparser:$kotlinArgParserVersion")
    compileOnly(mpsConfiguration.resolve().map { zipTree(it)  }.first().matching { include("lib/*.jar")})
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
    kotlinOptions.apiVersion = kotlinApiVersion
    kotlinOptions.allWarningsAsErrors = true
}

publishing {
    repositories {
        maven {
            name = "itemis"
            url = uri("https://projects.itemis.de/nexus/content/repositories/mbeddr")
            credentials {
                username = nexusUsername
                password = nexusPassword
            }
        }
    }
}