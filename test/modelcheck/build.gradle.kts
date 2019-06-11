import java.net.URI

group = "test.de.itemis.mps.gradle.modelcheck"
version = "1.0-SNAPSHOT"


plugins {
    id("modelcheck") version "1.0-SNAPSHOT"
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
    mps("com.jetbrains:mps:2018.2.5")
}

modelcheck {
    mpsConfig = mps
    projectLocation = File("./mps-prj")
    //debug = true
}
