package de.itemis.mps.gradle;

import org.gradle.api.DefaultTask
import org.gradle.api.file.FileCollection
import org.gradle.api.logging.LogLevel
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import java.util.*

open class RunAntScript : DefaultTask() {
    @Input
    lateinit var script: Any
    @Input
    var targets: List<String> = emptyList()
    @Optional @InputFiles
    var scriptClasspath: FileCollection? = null
    @Input
    var scriptArgs: List<String> = emptyList()
    @Input
    var includeDefaultArgs = true
    @Input
    var includeDefaultClasspath = true
    @Optional @Input
    var executable: Any? = null

    fun targets(vararg targets: String) {
        this.targets = targets.toList()
    }

    fun executable(executable: Any?) {
        this.executable = executable
    }

    @TaskAction
    fun build() {
        var allArgs = scriptArgs
        if (includeDefaultArgs) {
            val defaultArgs = project.findProperty("itemis.mps.gradle.ant.defaultScriptArgs") as Collection<*>?
            if (defaultArgs != null) {
                allArgs = allArgs + defaultArgs.map { it as String }
            }
        }

        if(logging.level != null && logging.level != LogLevel.LIFECYCLE && !allArgs.any { it.startsWith("-Dmps.ant.log=") }) {
            allArgs = allArgs + "-Dmps.ant.log=${logging.level.toString().toLowerCase(Locale.ENGLISH)}"
        }

        project.javaexec {
            if (this@RunAntScript.executable != null) {
                executable(this@RunAntScript.executable)
            } else {
                val defaultJava = project.findProperty("itemis.mps.gradle.ant.defaultJavaExecutable")
                if (defaultJava != null) {
                    executable(defaultJava)
                }
            }

            main = "org.apache.tools.ant.launch.Launcher"
            workingDir = project.rootDir

            if (includeDefaultClasspath) {
                val defaultClasspath = project.findProperty(
                        "itemis.mps.gradle.ant.defaultScriptClasspath") as FileCollection?
                if (defaultClasspath != null) {
                    classpath(defaultClasspath)
                }
            }

            if (scriptClasspath != null) {
                classpath(scriptClasspath)
            }

            args(allArgs)
            args("-buildfile", project.file(script))
            args(targets)
        }
    }
}

open class BuildLanguages : RunAntScript() {
    init {
        targets = listOf("clean", "generate", "assemble")
    }
}

open class TestLanguages : RunAntScript() {
    init {
        targets = listOf("clean", "generate", "assemble", "check")
    }
}