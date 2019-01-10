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

val pluginVersion = "1"

version = if (project.hasProperty("forceCI") || project.hasProperty("teamcity")) {
    de.itemis.mps.gradle.GitBasedVersioning.getVersion(mpsVersion, pluginVersion)
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
}