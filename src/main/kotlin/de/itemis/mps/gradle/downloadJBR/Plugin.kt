package de.itemis.mps.gradle.downloadJBR

import org.apache.tools.ant.taskdefs.condition.Os
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.model.ObjectFactory
import java.io.File
import javax.inject.Inject

open class DownloadJbrConfiguration @Inject constructor(objects: ObjectFactory) {
    lateinit var jbrVersion: String
    var defaultJavaExecutable = false
    var distributionType : String? = null
    internal val downloadDirProperty: DirectoryProperty = objects.directoryProperty()

    var downloadDir: File?
        get() = downloadDirProperty.get().asFile
        set(value) {
            downloadDirProperty.set(value)
        }
}

open class DownloadJbrProjectPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.run {

            val extension = extensions.create("downloadJbr", DownloadJbrConfiguration::class.java)
            extension.downloadDirProperty.convention(layout.buildDirectory.dir("jbrDownload"))

            val configuration = configurations.detachedConfiguration()
            configuration.dependencies.addLater(provider { project.dependencies.create(dependencyString(extension)) })

            val extractJbr = tasks.register("extractJbr") {
                inputs.files(configuration).skipWhenEmpty()
                outputs.dir(extension.downloadDirProperty)

                doLast {
                    if (Os.isFamily(Os.FAMILY_UNIX)) {
                        // Use Unix utilities to properly deal with symlinks
                        delete(extension.downloadDirProperty)
                        val downloadDir = mkdir(extension.downloadDirProperty)

                        exec {
                            commandLine("tar", "-xzf", configuration.singleFile.absolutePath)
                            workingDir = downloadDir
                        }

                        if (downloadDir.listFiles { _, name -> name.startsWith("jbr_") || name.startsWith("jbr-") }!!.any()) {
                            exec {
                                commandLine("sh", "-c", "mv jbr* jbr")
                                workingDir = downloadDir
                            }
                        }

                        exec {
                            commandLine("chmod", "-R", "u+w", ".")
                            workingDir = downloadDir
                        }
                    } else {
                        // On Windows we don't worry about symlinks nor file modes.
                        sync {
                            from({ tarTree(configuration.singleFile) })
                            into(extension.downloadDirProperty)
                            includeEmptyDirs = false
                            eachFile {
                                permissions { user { write = true } }
                            }
                            filesMatching("jbr_*/**") {
                                path = path.replace("jbr_(.*?)/(.*)".toRegex(), "jbr/$2")
                            }
                        }
                    }
                }
            }

            tasks.register("downloadJbr", DownloadJbrForPlatform::class.java) {
                dependsOn(extractJbr)
                group = "Build"
                description = "Downloads the JetBrains Runtime for the current platform and extracts it."

                jbrDirProperty.set(extension.downloadDirProperty.dir(
                    if (Os.isFamily(Os.FAMILY_MAC)) "jbr/Contents/Home"
                    else "jbr"
                ))
                javaExecutableProperty.set(jbrDirProperty.file(if (Os.isFamily(Os.FAMILY_WINDOWS)) "bin/java.exe" else "bin/java"))
                if(extension.defaultJavaExecutable) {
                    project.setProperty("itemis.mps.gradle.ant.defaultJavaExecutable",javaExecutableProperty.asFile)
                }
            }
        }
    }

    private fun dependencyString(extension: DownloadJbrConfiguration): String {
        val version = extension.jbrVersion
        // from version 10 on the jbr distribution type is replaced with jbr_jcef
        // jbr_jcef is the distribution used to start a normal desktop ide and should include everything
        // required for running tests. While a little bit larger than jbr_nomod it should cause the least
        // surprises when using it as a default.
        // see https://github.com/mbeddr/build.publish.jdk/commit/10bbf7d177336179ca189fc8bb4c1262029c69da
        val distributionType =
                if (extension.distributionType != null) {
                    extension.distributionType
                } else if (Regex("""11_0_[0-9][^0-9]""").find(version) != null) {
                    "jbr"
                } else {
                    "jbr_jcef"
                }

        val cpuArch = when(System.getProperty ("os.arch")) {
            "aarch64" -> "aarch64"
            "amd64" -> "x64"
            "x86_64" -> "x64"
            else -> throw GradleException("Unsupported CPU Architecture: ${System.getProperty ("os.arch")}! Please open a bug at https://github.com/mbeddr/mps-gradle-plugin with details about your operating system and CPU.")

        }

        val dependencyString = when {
            Os.isFamily(Os.FAMILY_MAC) -> {
                "com.jetbrains.jdk:$distributionType:$version:osx-$cpuArch@tgz"
            }
            Os.isFamily(Os.FAMILY_WINDOWS) -> {
                "com.jetbrains.jdk:$distributionType:$version:windows-$cpuArch@tgz"
            }
            Os.isFamily(Os.FAMILY_UNIX) -> {
                "com.jetbrains.jdk:$distributionType:$version:linux-$cpuArch@tgz"
            }
            else -> {
                throw GradleException("Unsupported platform! Please open a bug at https://github.com/mbeddr/mps-gradle-plugin with details about your operating system.")
            }
        }

        return dependencyString
    }
}
