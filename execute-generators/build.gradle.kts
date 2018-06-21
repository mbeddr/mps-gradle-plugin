import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.net.URI

group = "de.itemis.mps"

plugins {
    kotlin("jvm")
    `maven-publish`
    `java-gradle-plugin`
}

repositories {
    mavenCentral()
    maven {
        url = URI("https://projects.itemis.de/nexus/content/repositories/mbeddr")
    }
}

val nexusUsername: String? by project
val nexusPassword: String? by project


//define directories
val artifactsDir = File(buildDir, "artifacts")
val mpsDir = File(artifactsDir, "mps")
val kotlin_argparser_version = "2.0.7"
val pluginVersion = "1"
val mpsVersion = "2017.3.5"


version = if (project.hasProperty("forceCI") || project.hasProperty("teamcity")) {
    de.itemis.mps.gradle.GitBasedVersioning.getVersion(mpsVersion, pluginVersion)
} else {
    "$mpsVersion.$pluginVersion-SNAPSHOT"
}


val mpsConfiguration = configurations.create("mps")

dependencies {
    compile(kotlin("stdlib"))
    compile("com.xenomachina:kotlin-argparser:$kotlin_argparser_version")
    compileOnly(fileTree(mpsDir).include("**/*.jar"))
    mpsConfiguration("com.jetbrains:mps:$mpsVersion")
}


tasks {
    val resolveMps by creating(Copy::class) {
        from(mpsConfiguration.resolve().map { zipTree(it) })
        into(mpsDir)
    }

    getByName("compileKotlin").dependsOn(resolveMps)
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}
