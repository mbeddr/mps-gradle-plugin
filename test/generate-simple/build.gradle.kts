group = "test.de.itemis.mps.gradle.generate"
version = "1.2-SNAPSHOT"

plugins {
    id("generate-models") version "1.2-SNAPSHOT"
}

repositories {
    mavenLocal()
    mavenCentral()
    maven {
        url = java.net.URI("https://projects.itemis.de/nexus/content/repositories/mbeddr")
    }
}

val mps = configurations.create("mps")

val mpsVersion = "2018.2.6"

dependencies {
    mps("com.jetbrains:mps:$mpsVersion")
}

generate {
    projectLocation = File("./mps-prj")
    mpsConfig = mps
}


tasks {
    register("wrapper", Wrapper::class) {
        //make sure this version matches the version in parent build script otherwise this build will fail
        gradleVersion = "4.10.2"
        distributionType = Wrapper.DistributionType.ALL
    }
}

