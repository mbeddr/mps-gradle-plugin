import java.net.URI

plugins {
    groovy
    `java-gradle-plugin`
    `kotlin-dsl`
    `maven-publish`
}

val versionMajor = 1
val versionMinor = 1

group = "de.itemis.mps"


val nexusUsername: String? by project
val nexusPassword: String? by project

val kotlinArgParserVersion by extra { "2.0.7" }
val mpsVersion by extra { "2018.2.4" }


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


dependencies {
    compile(localGroovy())
    compile(kotlin("stdlib"))

}

gradlePlugin {
    plugins {
        register("generate-models") {
            id = "generate-models"
            implementationClass = "de.itemis.mps.gradle.generate.GenerateMpsProjectPlugin"
        }
    }
}

tasks {
    wrapper {
        gradleVersion = "5.0"
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



