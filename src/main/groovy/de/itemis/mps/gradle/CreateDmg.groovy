package de.itemis.mps.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.artifacts.Dependency
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermissions

class CreateDmg extends DefaultTask {
    @InputFile
    File rcpArtifact

    @InputFile
    File backgroundImage

    @InputFile
    File jdk

    @OutputFile
    File dmgFile

    def setRcpArtifact(Object file) {
        this.rcpArtifact = project.file(file)
    }

    def setBackgroundImage(Object file) {
        this.backgroundImage = project.file(file)
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

    def setDmgFile(Object file) {
        this.dmgFile = project.file(file)
        if (dmgFile != null && !dmgFile.name.endsWith(".dmg")) {
            throw new GradleException("Value of dmgFile must end with .dmg but was $dmgFile")
        }
    }

    @TaskAction
    def build() {
        String[] scripts = ['mpssign.sh', 'mpsdmg.sh', 'mpsdmg.pl',
                            'Mac/Finder/DSStore/BuddyAllocator.pm', 'Mac/Finder/DSStore.pm']
        File scriptsDir = File.createTempDir()
        File dmgDir = File.createTempDir()
        try {
            extractScriptsToDir(scriptsDir, scripts)
            project.exec {
                executable new File(scriptsDir, 'mpssign.sh')
                args rcpArtifact, dmgDir, jdk
                workingDir scriptsDir
            }
            project.exec {
                executable new File(scriptsDir, 'mpsdmg.sh')
                args dmgDir, dmgFile, backgroundImage
                workingDir scriptsDir
            }
        } finally {
            // Do not use File.deleteDir() because it follows symlinks!
            // (e.g. the symlink to /Applications inside dmgDir)
            project.exec {
                commandLine 'rm', '-rf', scriptsDir, dmgDir
            }
        }
    }

    private void extractScriptsToDir(File dir, String... scriptNames) {
        def rwxPermissions = PosixFilePermissions.fromString("rwx------")

        for (name in scriptNames) {
            File file = new File(dir, name)
            if (!file.parentFile.isDirectory() && ! file.parentFile.mkdirs()) {
                throw new GradleException("Could not create directory " + file.parentFile)
            }
            InputStream resourceStream = getClass().getResourceAsStream(name)
            if (resourceStream == null) {
                throw new IllegalArgumentException("Resource ${name} was not found")
            }

            resourceStream.withStream { is -> file.newOutputStream().withStream { os -> os << is } }
            try {
                Files.setPosixFilePermissions(file.toPath(), rwxPermissions)
            } catch (UnsupportedOperationException ex) {
                // workaround in case the script is executed on Win platform
                file.setReadable(true)
                file.setWritable(true)
                file.setExecutable(true)
            }
        }
    }
}
