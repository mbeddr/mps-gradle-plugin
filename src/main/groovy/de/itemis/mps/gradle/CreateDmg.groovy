package de.itemis.mps.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermissions

class CreateDmg extends DefaultTask {
    @Input
    def workingDir

    @InputFile
    def rcpArtifact

    @Input
    def baseName

    @InputFile
    def jdk

    @OutputFile
    File getOutput() {
        new File(project.file(workingDir), baseName.toString() + '.dmg')
    }

    @TaskAction
    def build() {
        String[] scripts = ['mpssign.sh', 'mpsdmg.sh', 'mpsdmg.pl']
        def workingDirPath = project.file(workingDir).toPath()
        extractScriptsToDir(workingDirPath, scripts)
        try {
            project.exec {
                executable scriptsFolder.resolve('mpssign.sh')
                args rcpArtifact, baseName, jdk
                workingDir scriptsFolder
            }
            project.exec {
                executable scriptsFolder.resolve('mpsdmg.sh')
                args rcpArtifact, baseName
                workingDir scriptsFolder
            }
        } finally {
            scripts.each { Files.deleteIfExists(workingDirPath.resolve(it)) }
        }
    }

    private void extractScriptsToDir(Path dir, String... scriptNames) {
        def rwxPermissions = PosixFilePermissions.fromString("rwx------")

        for (name in scriptNames) {
            Path file = dir.resolve(name)
            InputStream resourceStream = getClass().getResourceAsStream(resourcePath)
            if (resourceStream == null) {
                throw new IllegalArgumentException("Resource ${resourcePath} was not found")
            }

            resourceStream.withStream { is -> file.newOutputStream().withStream { os -> os << is } }
            Files.setPosixFilePermissions(file, rwxPermissions)
        }
    }
}
