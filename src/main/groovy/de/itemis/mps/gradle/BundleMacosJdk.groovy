package de.itemis.mps.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.artifacts.Dependency
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

class BundleMacosJdk extends DefaultTask {
    @InputFile
    File rcpArtifact

    @InputFile
    File jdk

    @OutputFile
    File outputFile

    def setRcpArtifact(Object file) {
        this.rcpArtifact = project.file(file)
    }

    def setJdk(Object file) {
        this.jdk = project.file(file)
    }

    /**
     * Sets the {@link #jdk} property from a dependency, given as either a {@link Dependency} object or in dependency
     * notation.
     */
    def setJdkDependency(Object jdkDependency) {
        Dependency dep = project.dependencies.create(jdkDependency)
        def files = project.configurations.detachedConfiguration(dep).resolve()
        if (files.size() != 1) {
            throw new GradleException(
                    "Expected a single file for jdkDependency '$jdkDependency', got ${files.size()} files")
        }
        this.jdk = files.first()
    }

    def setOutputFile(Object file) {
        this.outputFile = project.file(file)
    }

    @TaskAction
    def build() {
        File scriptsDir = File.createTempDir()
        File tmpDir = File.createTempDir()
        try {
            String scriptName = 'bundle_macos_jdk.sh'
            BundledScripts.extractScriptsToDir(scriptsDir, scriptName)
            project.exec {
                executable new File(scriptsDir, scriptName)
                args rcpArtifact, tmpDir, jdk, outputFile
                workingDir scriptsDir
            }
        } finally {
            // Do not use File.deleteDir() because it follows symlinks!
            // (e.g. the symlink to /Applications inside tmpDir)
            project.exec {
                commandLine 'rm', '-rf', scriptsDir, tmpDir
            }
        }
    }
}
