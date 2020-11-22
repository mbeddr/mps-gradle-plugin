import java.net.URI

group = "test.de.itemis.mps.gradle.modelcheck"
version = "1.3-SNAPSHOT"

plugins {
    id("modelcheck") version "1.4-SNAPSHOT"
}

repositories {
    maven {
        url = URI("https://projects.itemis.de/nexus/content/repositories/mbeddr")
    }
    mavenLocal()
    mavenCentral()
}

var mps = configurations.create("mps")
val mpsVersion = "2020.1.6"

dependencies{
    mps("com.jetbrains:mps:$mpsVersion")
}



modelcheck {
    mpsConfig = mps
    projectLocation = file("$rootDir/mps-prj")
    //modules = listOf("my.solution")

    modules = listOf("my.solution.with.errors")
    junitFile = file("$buildDir/TEST-modelcheck-results.xml")
    //errorNoFail = true
    //debug = true
}
