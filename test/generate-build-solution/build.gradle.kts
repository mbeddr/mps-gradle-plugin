group = "test.de.itemis.mps.gradle.generate"
version = "1.3-SNAPSHOT"

plugins {
    id("generate-models") version "1.4-SNAPSHOT"
}

repositories {
    mavenLocal()
    mavenCentral()
    maven {
        url = java.net.URI("https://artifacts.itemis.cloud/repository/maven-mps")
    }
}

val mps = configurations.create("mps")
val mpsVersion = "2020.2.3"

dependencies {
    mps("com.jetbrains:mps:$mpsVersion")
}

generate {
    projectLocation = file("$rootDir/mps-prj")
    mpsConfig = mps
    models = listOf("my.build.script")
}
