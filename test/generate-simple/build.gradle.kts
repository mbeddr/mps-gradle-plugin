group = "test.de.itemis.mps.gradle.generate"
version = "1.3-SNAPSHOT"

plugins {
    id("generate-models") version "1.5-SNAPSHOT"
}

repositories {
    mavenLocal()
    mavenCentral()
    maven {
        url = java.net.URI("https://projects.itemis.de/nexus/content/repositories/mbeddr")
    }
}

val mps = configurations.create("mps")
val mpsVersion = "2021.1.3"

dependencies {
    mps("com.jetbrains:mps:$mpsVersion")
}

generate {
    projectLocation = file("$rootDir/mps-prj")
    mpsConfig = mps
}
