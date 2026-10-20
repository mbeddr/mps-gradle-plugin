import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

buildscript {
    configurations.classpath {
        resolutionStrategy.activateDependencyLocking()
    }
}

plugins {
    groovy
    `java-gradle-plugin`
    `kotlin-dsl`
    `maven-publish`
    kotlin("jvm") version libs.versions.kotlin
    alias(libs.plugins.kotlin.compatibility.validator)
}

group = "de.itemis.mps"

tasks.register("checkReleaseVersion") {
    val releaseTag = providers.gradleProperty("releaseTag")
    doLast {
        require(version.toString().matches(Regex("[0-9]+\\.[0-9]+\\.[0-9]+"))) {
            "Release version must be MAJOR.MINOR.PATCH, got $version"
        }
        require(releaseTag.get() == "${project.name}-$version") {
            "Release tag ${releaseTag.get()} does not match ${project.name}-$version"
        }
    }
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
    api(libs.itemis.gradle.git.based.versioning)
    implementation(libs.kotlin.stdlib)
    implementation(libs.swiftzer.semver)
    implementation(libs.itemis.gradle.build.backends.launcher)
    testImplementation(libs.junit)
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

publishing {
    repositories {
        maven {
            name = "itemisCloud"
            url = uri(if (version.toString().endsWith("-SNAPSHOT")) {
                "https://artifacts.itemis.cloud/repository/maven-mps-snapshots/"
            } else {
                "https://artifacts.itemis.cloud/repository/maven-mps-releases/"
            })
            credentials {
                username = providers.gradleProperty("artifacts.itemis.cloud.user")
                    .orElse(providers.environmentVariable("ARTIFACTS_ITEMIS_CLOUD_USER")).orNull
                password = providers.gradleProperty("artifacts.itemis.cloud.pw")
                    .orElse(providers.environmentVariable("ARTIFACTS_ITEMIS_CLOUD_PW")).orNull
            }
        }
        if (!version.toString().endsWith("-SNAPSHOT")) {
            maven {
                name = "GitHubPackages"
                url = uri("https://maven.pkg.github.com/mbeddr/mps-gradle-plugin")
                credentials {
                    username = providers.gradleProperty("gpr.user")
                        .orElse(providers.environmentVariable("GITHUB_ACTOR")).orNull
                    password = providers.gradleProperty("gpr.token")
                        .orElse(providers.environmentVariable("GITHUB_TOKEN")).orNull
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
    targetCompatibility = JavaVersion.VERSION_11
    withSourcesJar()
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.fromTarget(libs.versions.kotlinJvmTarget.get())
        apiVersion = KotlinVersion.fromVersion(libs.versions.kotlinApi.get())
        allWarningsAsErrors = true
    }
}

apiValidation {
    ignoredClasses.add("de.itemis.mps.gradle.Common_gradle")
}

tasks.test {
    maxParallelForks = (Runtime.getRuntime().availableProcessors() * 2 / 3).coerceAtLeast(1)
}
