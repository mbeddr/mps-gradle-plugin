package de.itemis.mps.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.TaskAction

class RunAntScript extends DefaultTask {
    Object script
    List<String> targets = Collections.emptyList()
    FileCollection scriptClasspath = project.files()
    List<String> scriptArgs

    def targets(String... targets) {
        this.targets = Arrays.asList(targets)
    }

    @TaskAction
    def build() {
        project.javaexec {
            main 'org.apache.tools.ant.launch.Launcher'
            workingDir project.rootDir

            classpath scriptClasspath

            args(*scriptArgs, '-buildfile', project.file(script), *targets)
        }
    }
}
