package de.itemis.mps.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.TaskAction

class RunAntScript extends DefaultTask {
    Object script
    List<String> targets = Collections.emptyList()
    FileCollection scriptClasspath = project.files()
    List<String> scriptArgs = []
    boolean includeDefaultArgs = true
    boolean includeDefaultClasspath = true

    def targets(String... targets) {
        this.targets = Arrays.asList(targets)
    }

    @TaskAction
    def build() {
        List<String> allArgs = scriptArgs
        List<String> defaultArgs = project.findProperty("itemis.mps.gradle.ant.defaultScriptArgs")
        if(defaultArgs != null && includeDefaultArgs) {
            allArgs.addAll(defaultArgs)
        }

        FileCollection defaultClasspath = project.findProperty("itemis.mps.gradle.ant.defaultScriptClasspath")


        project.javaexec {
            main 'org.apache.tools.ant.launch.Launcher'
            workingDir project.rootDir

            if (defaultClasspath != null && includeDefaultClasspath) {
                classpath defaultClasspath
            }

            classpath scriptClasspath

            args(*allArgs, '-buildfile', project.file(script), *targets)
        }
    }
}
