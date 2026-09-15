plugins {
    `java-library`
    `maven-publish`
}

group = "de.itemis.mps"

version = "1.0.0"

repositories {
    mavenCentral()
}

dependencyLocking {
    lockAllConfigurations()
}

dependencies {
    compileOnly(gradleApi())
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
    withSourcesJar()
}

tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.addAll(listOf("-Xlint:all", "-Werror"))
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

    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
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
}

tasks.register("setTeamCityBuildNumber") {
    doLast {
        println("##teamcity[buildNumber '$version']")
    }
}
