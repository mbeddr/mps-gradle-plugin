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