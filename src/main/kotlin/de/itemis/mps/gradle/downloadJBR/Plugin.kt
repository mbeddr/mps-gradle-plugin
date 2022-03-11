package de.itemis.mps.gradle.downloadJBR

import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Copy
import org.apache.tools.ant.taskdefs.condition.Os
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import java.io.File
import javax.inject.Inject

open class DownloadJbrConfiguration @Inject constructor(of: ObjectFactory) {
    @get:Input
    val jbrVersion: Property<String>  = of.property(String::class.java)

    var distributionType : String? = null

    @get:Input
    @get:Optional
    val downloadDir: RegularFileProperty = of.fileProperty()
}

open class DownloadJbrProjectPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.run {

            val extension = extensions.create("downloadJbr", DownloadJbrConfiguration::class.java)

            afterEvaluate {
                val version = extension.jbrVersion.get()
                val downloadDir = extension.downloadDir.map { it.asFile }.getOrElse(File(buildDir, "jbrDownload"))

                // from version 10 on the jbr distribution type is replaced with jbr_jcef
                // jbr_jcef is the distribution used to start a normal desktop ide and should include everything
                // required for running tests. While a little bit larger than jbr_nomod it should cause the least
                // surprises when using it as a default.
                // see https://github.com/mbeddr/build.publish.jdk/commit/10bbf7d177336179ca189fc8bb4c1262029c69da
                val distributionType = if(extension.distributionType == null &&
                Regex("""11_0_[0-9][^0-9]""").find(version) != null) {
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
                val dependency = project.dependencies.create(dependencyString)
                val configuration = configurations.detachedConfiguration(dependency)

                val extractJbr = tasks.create("extractJbr", Copy::class.java) {
                    doFirst {
                        downloadDir.delete()
                    }
                    from({configuration.resolve().map { tarTree(it) }})
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

                tasks.create("downloadJbr", DownloadJbrForPlatform::class.java) {
                    dependsOn(extractJbr)
                    group = "Build"
                    description = "Downloads the JetBrains Runtime for the current platform and extracts it."
                    val directoryProperty: DirectoryProperty = layout.buildDirectory.fileValue(jbrSubdir)
                    jbrDir.set(directoryProperty)
                    val dirFile = directoryProperty.file("bin/java")
                    javaExecutable.set(dirFile)
                }
            }
        }
    }
}