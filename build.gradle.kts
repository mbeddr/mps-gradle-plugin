import de.itemis.mps.gradle.GitBasedVersioning
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
    configurations.classpath {
        resolutionStrategy.activateDependencyLocking()
    }

    dependencies {
        classpath("de.itemis.mps.gradle:git-based-versioning")
    }
}

val kotlinApiVersion: String by project
val kotlinVersion: String by project

plugins {
    groovy
    `java-gradle-plugin`
    `kotlin-dsl` version "4.5.0"
    `maven-publish`
    kotlin("jvm")
    id("org.jetbrains.kotlinx.binary-compatibility-validator") version "0.13.2"
}

val baseVersion = "1.28.0"

group = "de.itemis.mps"

val currentBranch : String? = GitBasedVersioning.getGitBranch()

version = if (!project.hasProperty("useSnapshot") &&
    (project.hasProperty("forceCI") || project.hasProperty("teamcity"))
) {
    val prefix = when (currentBranch) {
        null, "", "v1.x", "HEAD", "master", "main" -> ""
        else -> "$currentBranch."
    }

    val suffix = ".${GitBasedVersioning.getGitCommitCount()}.${GitBasedVersioning.getGitShortCommitHash()}"

    prefix + baseVersion + suffix
} else {
    "$baseVersion-SNAPSHOT"
}

val mpsConfiguration = configurations.create("mps")

repositories {
    mavenCentral()
    // For mps-build-backends, during tests
    maven(url = "https://artifacts.itemis.cloud/repository/maven-mps")
}

dependencyLocking {
    lockAllConfigurations()
}

dependencies {
    api("de.itemis.mps.gradle:git-based-versioning")
    implementation(kotlin("stdlib", version = kotlinVersion))
    implementation("net.swiftzer.semver:semver:1.1.2")
    implementation("de.itemis.mps.build-backends:launcher:2.4.0.+")
    testImplementation("junit:junit:4.13.2")
}

tasks.test {
    useJUnit()
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
        register("migrations-executor") {
            id = "run-migrations"
            implementationClass = "de.itemis.mps.gradle.runmigrations.RunMigrationsMpsProjectPlugin"
        }
        register("download-jbr") {
            id = "download-jbr"
            implementationClass = "de.itemis.mps.gradle.downloadJBR.DownloadJbrProjectPlugin"
        }
    }
}

tasks.register("setTeamCityBuildNumber") {
    doLast {
        println("##teamcity[buildNumber '$version']")
    }
}

publishing {
    repositories {
        maven {
            name = "itemisCloud"
            url = uri("https://artifacts.itemis.cloud/repository/maven-mps-releases/")
            if (project.hasProperty("artifacts.itemis.cloud.user") && project.hasProperty("artifacts.itemis.cloud.pw")) {
                credentials {
                    username = project.findProperty("artifacts.itemis.cloud.user") as String?
                    password = project.findProperty("artifacts.itemis.cloud.pw") as String?
                }
            }
        }
        if (currentBranch == "master" || currentBranch == "v1.x") {
            maven {
                name = "GitHubPackages"
                url = uri("https://maven.pkg.github.com/mbeddr/mps-gradle-plugin")
                if(project.hasProperty("gpr.token")) {
                    credentials {
                        username = project.findProperty("gpr.user") as String?
                        password = project.findProperty("gpr.token") as String?
                    }
                }
            }
        }
    }

    publications.withType<MavenPublication>().configureEach {
        versionMapping {
            allVariants {
                fromResolutionResult()
            }
        }
        pom {
            url.set("https://github.com/mbeddr/mps-gradle-plugin")
            licenses {
                license {
                    name.set("The Apache License, Version 2.0")
                    url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                }
            }
            scm {
                connection.set("scm:git:git://github.com/mbeddr/mps-gradle-plugin.git")
                developerConnection.set("scm:git:ssh://github.com/mbeddr/mps-gradle-plugin.git")
                url.set("https://github.com/mbeddr/mps-gradle-plugin")
            }
        }
    }
}

java {
    targetCompatibility = JavaVersion.VERSION_1_8
    withSourcesJar()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
    kotlinOptions.apiVersion = kotlinApiVersion
    kotlinOptions.allWarningsAsErrors = true
}

apiValidation {
    ignoredClasses.add("de.itemis.mps.gradle.Common_gradle")
}

tasks.test {
    maxParallelForks = (Runtime.getRuntime().availableProcessors() * 2 / 3).coerceAtLeast(1)
}
