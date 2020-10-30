package de.itemis.mps.gradle.downloadJBR

import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Copy
import org.apache.tools.ant.taskdefs.condition.Os
import java.io.File

open class DownloadJBRConfiguration {
    lateinit var jbrVersion: String
    var downloadDir: File? = null
}

open class DownloadJbrProjectPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.run {

            val extension = extensions.create("JbrDownload", DownloadJBRConfiguration::class.java)

            afterEvaluate {
                val downloadDir = extension.downloadDir ?: File(buildDir, "jbrDownload")
                val version = extension.jbrVersion
                val dependencyString = when {
                    Os.isFamily(Os.FAMILY_MAC) -> {
                        "com.jetbrains.jdk:jbr:$version:osx-x64@tgz"
                    }
                    Os.isFamily(Os.FAMILY_WINDOWS) -> {
                        "com.jetbrains.jdk:jbr:$version:windows-x64@tgz"
                    }
                    Os.isFamily(Os.FAMILY_UNIX) -> {
                        "com.jetbrains.jdk:jbr:$version:linux-x64@tgz"
                    }
                    else -> {
                        throw GradleException("Unsupported platform! Please open a bug at https://github.com/mbeddr/mps-gradle-plugin with details about your operating system.")
                    }
                }

                val dependency = project.dependencies.create(dependencyString)
                val configuration = configurations.detachedConfiguration(dependency)

                val extractJbr = tasks.create("extractJbr", Copy::class.java) {
                    doFirst {
                        downloadDir.delete()
                    }
                    from(configuration.resolve().map { tarTree(it) })
                    into(downloadDir)
                }

                val jbrSubdir = when {
                    Os.isFamily(Os.FAMILY_MAC) -> {
                        File(downloadDir, "jbr/Contents/Home")
                    }
                    else -> {
                        File(downloadDir, "jbr")
                    }
                }

                tasks.create("downloadJbr", DownloadJBRForPlatform::class.java) {
                    dependsOn(extractJbr)
                    jbrDir = jbrSubdir
                    javaExecutable = File(jbrSubdir, "bin/java")
                }
            }
        }
    }
}