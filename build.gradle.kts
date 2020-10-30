import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.net.URI

buildscript {
    configurations.classpath {
        resolutionStrategy.activateDependencyLocking()
    }
}

val kotlinApiVersion by extra {"1.3"}
val kotlinVersion by extra {"$kotlinApiVersion.11"}


plugins {
    groovy
    `java-gradle-plugin`
    `kotlin-dsl`
    `maven-publish`
    kotlin("jvm") version "1.3.11"
}

val versionMajor = 1
val versionMinor = 4

group = "de.itemis.mps"


val nexusUsername: String? by project
val nexusPassword: String? by project

val kotlinArgParserVersion by extra { "2.0.7" }
val mpsVersion by extra { "2020.1.6" }
//this version needs to align with the version shiped with MPS found in the /lib folder otherwise, runtime problems will
//surface because mismatching jars on the classpath.
val fastXmlJacksonVersion by extra { "2.10.+" }


version = if (!project.hasProperty("useSnapshot") &&
        (project.hasProperty("forceCI") || project.hasProperty("teamcity"))) {
    de.itemis.mps.gradle.GitBasedVersioning.getVersion(versionMajor, versionMinor)
} else {
    "$versionMajor.$versionMinor-SNAPSHOT"
}


val mpsConfiguration = configurations.create("mps")


repositories {
    mavenCentral()
    maven {
        url = URI("https://projects.itemis.de/nexus/content/repositories/mbeddr")
    }
}

dependencyLocking {
    lockAllConfigurations()
}

dependencies {
    implementation(localGroovy())
    implementation(kotlin("stdlib", version = kotlinVersion))
    testImplementation("junit:junit:4.12")
}


gradlePlugin {
    plugins {
        register("generate-models") {
            id = "generate-models"
            implementationClass = "de.itemis.mps.gradle.generate.GenerateMpsProjectPlugin"
        }
        register("modelcheck") {
            id = "modelcheck"
            implementationClass = "de.itemis.mps.gradle.modelcheck.ModelcheckMpsProjectPlugin"
        }
        register("JbrDownload") {
            id = "JbrDownload"
            implementationClass = "de.itemis.mps.gradle.downloadJBR.DownloadJbrProjectPlugin"
        }
    }
}

tasks {
    wrapper {
        gradleVersion = "6.2.2"
        distributionType = Wrapper.DistributionType.ALL
    }

    register("setTeamCityBuildNumber") {
        doLast {
            println("##teamcity[buildNumber '$version']")
        }
    }
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

subprojects {
    dependencyLocking {
        lockAllConfigurations()
    }
}

tasks.register("createClasspathManifest") {
    val outputDir = file("$buildDir/$name")

    inputs.files(sourceSets.main.get().runtimeClasspath)
            .withPropertyName("runtimeClasspath")
            .withNormalizer(ClasspathNormalizer::class)
    outputs.dir(outputDir)
            .withPropertyName("outputDir")

    doLast {
        outputDir.mkdirs()
        file("$outputDir/plugin-classpath.txt").writeText(sourceSets.main.get().runtimeClasspath.joinToString("\n"))
    }
}

dependencies {
    testRuntimeOnly(files(tasks["createClasspathManifest"]))
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
    kotlinOptions.apiVersion = kotlinApiVersion
    kotlinOptions.allWarningsAsErrors = true
}



