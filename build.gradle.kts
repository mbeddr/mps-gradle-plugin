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

val kotlinApiVersion by extra { "1.5" }
val kotlinVersion by extra { "$kotlinApiVersion.31" }


plugins {
    groovy
    `java-gradle-plugin`
    `kotlin-dsl`
    `maven-publish`
    kotlin("jvm") version "1.5.31"
}

val versionMajor = 1
val versionMinor = 16

group = "de.itemis.mps"

var currentBranch:String? = ""
currentBranch = de.itemis.mps.gradle.GitBasedVersioning.getGitBranch()

version = if (!project.hasProperty("useSnapshot") &&
    (project.hasProperty("forceCI") || project.hasProperty("teamcity"))
) {
    de.itemis.mps.gradle.GitBasedVersioning.getVersion(
        // Publish releases from v1.x branch without v1.x prefix
        if (currentBranch == "v1.x") "HEAD" else currentBranch,
        versionMajor.toString(), versionMinor.toString())
} else {
    "$versionMajor.$versionMinor-SNAPSHOT"
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
    testImplementation("junit:junit:4.13.2")
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

java {
    targetCompatibility = JavaVersion.VERSION_1_8
    withSourcesJar()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
    kotlinOptions.apiVersion = kotlinApiVersion
    kotlinOptions.allWarningsAsErrors = true
}
