import java.net.URI

group = "test.de.itemis.mps.gradle.modelcheck"
version = "1.2-SNAPSHOT"

if(JavaVersion.current() != JavaVersion.VERSION_1_8){
    throw GradleException("This build script requires java " + JavaVersion.VERSION_1_8 + ", but you are currently using " + JavaVersion.current())
}

plugins {
    id("modelcheck") version "1.2-SNAPSHOT"
}

repositories {
    maven {
        url = URI("https://projects.itemis.de/nexus/content/repositories/mbeddr")
    }
    mavenLocal()
    mavenCentral()
}

var mps = configurations.create("mps")


dependencies{
    mps("com.jetbrains:mps:2019.1.5")
}

modelcheck {
    mpsConfig = mps
    projectLocation = file("$rootDir/mps-prj")
    modules = listOf("my.solution")
    //debug = true
}
