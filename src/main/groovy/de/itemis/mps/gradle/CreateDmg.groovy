package de.itemis.mps.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
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

    def setDmgFile(Object file) {
        this.dmgFile = project.file(file)
        if (dmgFile != null && !dmgFile.name.endsWith(".dmg")) {
            throw new GradleException("Value of dmgFile must end with .dmg but was " + dmgFile)
        }
    }

    @TaskAction
    def build() {
        String[] scripts = ['mpssign.sh', 'mpsdmg.sh', 'mpsdmg.pl']
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
            InputStream resourceStream = getClass().getResourceAsStream(name)
            if (resourceStream == null) {
                throw new IllegalArgumentException("Resource ${name} was not found")
            }

            resourceStream.withStream { is -> file.newOutputStream().withStream { os -> os << is } }
            Files.setPosixFilePermissions(file.toPath(), rwxPermissions)
        }
    }
}
