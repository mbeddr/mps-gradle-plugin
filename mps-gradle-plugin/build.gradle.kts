plugins {
    groovy
    `java-gradle-plugin`
    `kotlin-dsl`
    `maven-publish`
    alias(libs.plugins.kotlin.compatibility.validator)
}

group = "de.itemis.mps"

version = "3.0.0-SNAPSHOT"

repositories {
    mavenCentral()
    // For mps-build-backends, during tests
    maven(url = "https://artifacts.itemis.cloud/repository/maven-mps")
}

dependencyLocking {
    lockAllConfigurations()
}

dependencies {
    api(libs.itemis.gradle.git.based.versioning)
    api("de.itemis.mps:mps-gradle-plugin-api")
    implementation(libs.swiftzer.semver)
    implementation(libs.itemis.gradle.build.backends.launcher)
    testImplementation(libs.junit)
}

tasks.test {
    useJUnit()
}

gradlePlugin {
    plugins {
        // Plugins are provided by the common.gradle.kts precompiled script plugin.
        // Task types are used directly by consumers.
    }
}

tasks.register("setTeamCityBuildNumber") {
    doLast {
        println("##teamcity[buildNumber '$version']")
    }
}

publishing {
    repositories {
        val isSnapshot = project.version.toString().endsWith("SNAPSHOT")
        if (project.hasProperty("artifacts.itemis.cloud.user") && project.hasProperty("artifacts.itemis.cloud.pw")) {
            maven {
                name = "itemisCloud"
                if (isSnapshot) {
                    url = uri("https://artifacts.itemis.cloud/repository/maven-mps-snapshots/")
                } else {
                    url = uri("https://artifacts.itemis.cloud/repository/maven-mps-releases/")
                }
                credentials {
                    username = project.findProperty("artifacts.itemis.cloud.user") as String?
                    password = project.findProperty("artifacts.itemis.cloud.pw") as String?
                }
            }
        }

        if (!isSnapshot && project.hasProperty("gpr.token")) {
            maven {
                name = "GitHubPackages"
                url = uri("https://maven.pkg.github.com/mbeddr/mps-gradle-plugin")
                credentials {
                    username = project.findProperty("gpr.user") as String?
                    password = project.findProperty("gpr.token") as String?
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
            url = "https://github.com/mbeddr/mps-gradle-plugin"
            licenses {
                license {
                    name = "The Apache License, Version 2.0"
                    url = "https://www.apache.org/licenses/LICENSE-2.0.txt"
                }
            }
            scm {
                connection = "scm:git:git://github.com/mbeddr/mps-gradle-plugin.git"
                developerConnection = "scm:git:ssh://github.com/mbeddr/mps-gradle-plugin.git"
                url = "https://github.com/mbeddr/mps-gradle-plugin"
            }
        }
    }
}

java {
    targetCompatibility = JavaVersion.VERSION_17
    withSourcesJar()
}

kotlin {
    compilerOptions {
        allWarningsAsErrors = true
    }
}

apiValidation {
    ignoredClasses.add("de.itemis.mps.gradle.Common_gradle")
}

tasks.test {
    maxParallelForks = (Runtime.getRuntime().availableProcessors() * 2 / 3).coerceAtLeast(1)
}
