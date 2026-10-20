plugins {
    groovy
    `maven-publish`
}

group = "de.itemis.mps.gradle"
tasks.register("checkReleaseVersion") {
    val releaseTag = providers.gradleProperty("releaseTag")
    doLast {
        require(version.toString().matches(Regex("[0-9]+\\.[0-9]+\\.[0-9]+"))) {
            "Release version must be MAJOR.MINOR.PATCH, got $version"
        }
        require(releaseTag.get() == "${project.name}-$version") {
            "Release tag ${releaseTag.get()} does not match ${project.name}-$version"
        }
    }
}

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
            url = uri(if (version.toString().endsWith("-SNAPSHOT")) {
                "https://artifacts.itemis.cloud/repository/maven-mps-snapshots/"
            } else {
                "https://artifacts.itemis.cloud/repository/maven-mps-releases/"
            })
            credentials {
                username = providers.gradleProperty("artifacts.itemis.cloud.user")
                    .orElse(providers.environmentVariable("ARTIFACTS_ITEMIS_CLOUD_USER")).orNull
                password = providers.gradleProperty("artifacts.itemis.cloud.pw")
                    .orElse(providers.environmentVariable("ARTIFACTS_ITEMIS_CLOUD_PW")).orNull
            }
        }
        if (!version.toString().endsWith("-SNAPSHOT")) {
            maven {
                name = "GitHubPackages"
                url = uri("https://maven.pkg.github.com/mbeddr/mps-gradle-plugin")
                credentials {
                    username = providers.gradleProperty("gpr.user")
                        .orElse(providers.environmentVariable("GITHUB_ACTOR")).orNull
                    password = providers.gradleProperty("gpr.token")
                        .orElse(providers.environmentVariable("GITHUB_TOKEN")).orNull
                }
            }
        }
    }

}
