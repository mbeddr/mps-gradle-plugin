import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.net.URI

buildscript {
    configurations.classpath {
        resolutionStrategy.activateDependencyLocking()
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
val versionMinor = 9

group = "de.itemis.mps"


val nexusUsername: String? by project
val nexusPassword: String? by project

val kotlinArgParserVersion by extra { "2.0.7" }
val mpsVersion by extra { "2020.3.4" }
//this version needs to align with the version shiped with MPS found in the /lib folder otherwise, runtime problems will
//surface because mismatching jars on the classpath.
val fastXmlJacksonVersion by extra { "2.11.+" }


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
        register("download-jbr") {
            id = "download-jbr"
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

allprojects {
    apply<MavenPublishPlugin>()
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

java {
    targetCompatibility = JavaVersion.VERSION_1_8
    withSourcesJar()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
    kotlinOptions.apiVersion = kotlinApiVersion
    kotlinOptions.allWarningsAsErrors = true
}



tasks.register("resolveAndLockAll") {
    doFirst {
        require(gradle.startParameter.isWriteDependencyLocks)
    }
    doLast {
        configurations.filter { it.isCanBeResolved }.forEach { it.resolve() }
        subprojects.forEach { project ->
            project.configurations.filter { it.isCanBeResolved }.forEach { it.resolve() }
        }
    }
}


