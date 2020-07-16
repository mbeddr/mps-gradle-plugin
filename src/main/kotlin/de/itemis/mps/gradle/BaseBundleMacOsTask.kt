package de.itemis.mps.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.file.ProjectLayout
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.kotlin.dsl.property
import java.io.File

abstract class BaseBundleMacOsTask(
    objects: ObjectFactory,
    private val providers: ProviderFactory,
    private val layout: ProjectLayout
) : DefaultTask() {
    @InputFile
    val rcpArtifact = objects.fileProperty()

    @InputFile
    val jdk = objects.fileProperty()

    fun jdkDependency(dependencyNotation: String) {
        val jdkConfig = project.configurations.detachedConfiguration(
            project.dependencies.create(dependencyNotation)
        ).apply {
            description = "JDK for $name"
        }
        jdk.set(layout.file(providers.provider { jdkConfig.singleFile }))
    }

    @Input
    val cleanTemporaryDirectories = objects.property<Boolean>().convention(true)

    protected fun rmdirs(vararg directory: File) {
        // Do not use File.deleteDir() because it follows symlinks!
        // (e.g. the symlink to /Applications inside tmpDir)
        project.exec {
            commandLine("rm", "-rf", *directory)
        }
    }
}
