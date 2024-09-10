plugins {
    groovy
    `maven-publish`
}

group = "de.itemis.mps.gradle"
version = "1.1.0"

dependencies {
    implementation(gradleApi())
}

java {
    targetCompatibility = JavaVersion.VERSION_1_8
    withSourcesJar()
}

publishing {
    publications {
        create<MavenPublication>("gitBasedVersioning") {
            from(components["java"])
        }
    }

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
