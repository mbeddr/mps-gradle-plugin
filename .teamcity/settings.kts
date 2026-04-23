import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildFeatures.commitStatusPublisher
import jetbrains.buildServer.configs.kotlin.buildSteps.gradle
import jetbrains.buildServer.configs.kotlin.triggers.vcs

version = "2025.11"

project {
    buildType(BuildPlugin)
    buildType(BuildGitBasedVersioning)
}

object BuildPlugin : BuildType({
    id("MpsGradlePlugin")
    name = "mps-gradle-plugin"

    vcs {
        root(AbsoluteId("Mbeddr_Tooling_MpsGradlePlugin"))
    }

    params {
        param("env.BUILD_COUNTER", "%build.counter%")
    }

    steps {
        gradle {
            name = "Build"
            tasks = "setTeamCityBuildNumber build"
            jdkHome = "%env.JDK_17_0_x64%"
            useGradleWrapper = true
        }
        gradle {
            name = "Publish"
            tasks = "publish"
            jdkHome = "%env.JDK_17_0_x64%"
            useGradleWrapper = true
        }
    }

    triggers {
        vcs {
            branchFilter = """
                +:*
                -:pull/*
            """.trimIndent()
        }
    }

    features {
        commitStatusPublisher {
            vcsRootExtId = "Mbeddr_Tooling_MpsGradlePlugin"
            publisher = github {
                githubUrl = "https://api.github.com"
                authType = personalToken {
                    token = "%system.github.token%"
                }
            }
        }
    }

    requirements {
        doesNotContain("teamcity.agent.hostname", "nonspot")
    }
})

object BuildGitBasedVersioning : BuildType({
    id("GitBasedVersioning")
    name = "git-based-versioning"
    description = "Part of mps-gradle-plugin, published separately because it rarely changes and uses a separate, explicitly set version number."

    templates(AbsoluteId("RequiresMpsExtions"))

    vcs {
        root(AbsoluteId("Mbeddr_Tooling_MpsGradlePlugin"))
    }

    steps {
        gradle {
            name = "Build and publish git-based-versioning"
            tasks = ":git-based-versioning:build :git-based-versioning:publish"
            jdkHome = "%env.JDK_17_0_x64%"
            useGradleWrapper = true
            param("teamcity.coverage.emma.include.source", "true")
            param("teamcity.coverage.emma.instr.parameters", "-ix -*Test*")
            param("teamcity.coverage.idea.includePatterns", "*")
            param("teamcity.coverage.jacoco.patterns", "+:*")
            param("teamcity.tool.jacoco", "%teamcity.tool.jacoco.DEFAULT%")
        }
    }

    requirements {
        doesNotContain("teamcity.agent.hostname", "nonspot")
    }
})
