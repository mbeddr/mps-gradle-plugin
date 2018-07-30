import java.net.URI

plugins {
    kotlin("jvm") version "1.2.51"
    `maven-publish`
    groovy
    `java-gradle-plugin`
    `kotlin-dsl`
}

val versionMajor = 1
val versionMinor = 0

group = "de.itemis.mps"
version = "1.0.0"




val nexusUsername: String? by project
val nexusPassword: String? by project

val kotlinArgParserVersion by extra {"2.0.7"}
val mpsVersion by extra {"2018.1.5"}



if (project.hasProperty("forceCI") || project.hasProperty("teamcity")) {
    version = de.itemis.mps.gradle.GitBasedVersioning.getVersion(versionMajor, versionMinor)
} else {
    version = "$versionMajor.$versionMinor-SNAPSHOT"
}


val mpsConfiguration = configurations.create("mps")


repositories {
    mavenCentral()
    maven {
        url = URI("https://projects.itemis.de/nexus/content/repositories/mbeddr")
    }
}


dependencies {
    compile(localGroovy())
    compile(kotlin("stdlib"))

}

gradlePlugin {
    (plugins) {
        "generate-models" {
            id = "generate-models"
            implementationClass = "de.itemis.mps.gradle.generate.GenerateMpsProjectPlugin"
        }
    }
}

tasks {
    "wrapper"(Wrapper::class) {
        gradleVersion = "4.8"
        distributionType = Wrapper.DistributionType.ALL
    }

    "setTeamCityBuildNumber" {
        doLast {
            println("##teamcity[buildNumber '$version']")
        }
    }
}

publishing {
    repositories {
        maven {
            url = uri("https://projects.itemis.de/nexus/content/repositories/mbeddr")
            credentials {
                username = nexusUsername
                password = nexusPassword
            }
        }
    }
}


