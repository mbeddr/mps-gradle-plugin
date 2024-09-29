package de.itemis.mps.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.artifacts.Dependency
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.io.File
import kotlin.io.path.createTempDirectory

open class BundleMacosJdk : DefaultTask() {
    @InputFile
    lateinit var rcpArtifact: File

    @Optional @Input
    var jdkDirname: String = "jre"

    @InputFile
    lateinit var jdk: File

    @OutputFile
    lateinit var outputFile: File

    /**
     * Sets the [jdk] property from a dependency, given as either a [Dependency] object or in dependency notation.
     */
    fun setJdkDependency(jdkDependency: Any) {
        val dep: Dependency = project.dependencies.create(jdkDependency)
        val files = project.configurations.detachedConfiguration(dep).resolve()
        if (files.size != 1) {
            throw GradleException(
                "Expected a single file for jdkDependency '$jdkDependency', got ${files.size} files"
            )
        }
        this.jdk = files.first()
    }

    fun setOutputFile(file: Any) {
        this.outputFile = project.file(file)
    }

    @TaskAction
    fun build() {
        project.logger.lifecycle("The jdkDirname: $jdkDirname")
        val scriptsDir = createTempDirectory().toFile()
        val tmpDir = createTempDirectory().toFile()
        try {
            val scriptName = "bundle_macos_jdk.sh"
            BundledScripts.extractScriptsToDir(scriptsDir, scriptName)
            project.exec {
                executable = File(scriptsDir, scriptName).absolutePath
                args = listOf(rcpArtifact.toString(), tmpDir.toString(), jdkDirname.toString(), jdk.toString(), outputFile.toString())
                workingDir = scriptsDir
            }
        } finally {
            // Do not use File.deleteDir() because it follows symlinks!
            // (e.g. the symlink to /Applications inside tmpDir)
            project.exec {
                commandLine("rm", "-rf", scriptsDir.absolutePath, tmpDir.absolutePath)
            }
        }
    }
}