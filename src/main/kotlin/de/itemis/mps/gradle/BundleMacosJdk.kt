package de.itemis.mps.gradle

import org.gradle.api.file.ProjectLayout
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.io.File
import javax.inject.Inject

open class BundleMacosJdk @Inject constructor(
    objects: ObjectFactory,
    providers: ProviderFactory,
    layout: ProjectLayout
) : BaseBundleMacOsTask(objects, providers, layout) {
    @OutputFile
    val outputFile = objects.fileProperty()
        .convention(layout.buildDirectory.file("bundleRcp/$name/$name.tgz"))

    @Internal
    val scriptsDir = layout.buildDirectory.dir("bundleRcp/$name/scripts")

    @Internal
    val tmpDir = layout.buildDirectory.dir("bundleRcp/$name/tmp")

    @TaskAction
    fun run() {
        val scriptsDir = scriptsDir.get().asFile
        val tmpDir = tmpDir.get().asFile
        try {
            // Cleanup temporary directory so stale files do not affect the current task execution
            rmdirs(tmpDir)
            tmpDir.mkdirs()
            val scriptName = "bundle_macos_jdk.sh"
            BundledScripts.extractScriptsToDir(scriptsDir, scriptName)
            project.exec {
                executable = "./$scriptName"
                workingDir = scriptsDir
                args(rcpArtifact.get().asFile, tmpDir, jdk.get().asFile, outputFile.get().asFile)
            }
        } finally {
            if (cleanTemporaryDirectories.get()) {
                rmdirs(scriptsDir, tmpDir)
            }
        }
    }
}
