package de.itemis.mps.gradle;

import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.logging.LogLevel
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.process.JavaExecSpec
import java.io.File
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

    /**
     * Whether to build incrementally.
     *
     * Possible values:
     * * `true` - perform an incremental build. If the [targets] list includes `clean` target it will be removed, and
     *   `-Dmps.generator.skipUnmodifiedModels=true` will be passed to Ant.
     * * `false` - The backwards compatible default. The [targets] list will not be modified and no properties will be
     *   passed to Ant. Any outside customizations made to targets and Ant arguments are left intact so the build may
     *   in fact be incremental.
     */
    @Input
    var incremental: Boolean = false

    fun targets(vararg targets: String) {
        this.targets = targets.toList()
    }

    fun executable(executable: Any?) {
        this.executable = executable
    }

    @TaskAction
    fun build() {
        val allArgs = scriptArgs.toMutableList()
        if (includeDefaultArgs) {
            val defaultArgs = project.findProperty("itemis.mps.gradle.ant.defaultScriptArgs") as Collection<*>?
            if (defaultArgs != null) {
                allArgs += defaultArgs.map { it as String }
            }
        }

        if(logging.level != null && logging.level != LogLevel.LIFECYCLE && !allArgs.any { it.startsWith("-Dmps.ant.log=") }) {
            allArgs += "-Dmps.ant.log=${logging.level.toString().toLowerCase(Locale.ENGLISH)}"
        }

        if (incremental) {
            allArgs += "-Dmps.generator.skipUnmodifiedModels=true"
        }

        val targets = if (incremental) { targets - "clean" } else { targets }

        project.runAnt(executable, project.rootDir, allArgs + "-buildfile" + project.file(script).toString() + targets,
            includeDefaultClasspath, scriptClasspath)
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

internal fun Project.runAnt(executable: Any?, workingDir: File, args: List<String>,
                           includeDefaultClasspath: Boolean = true,
                           scriptClasspath: Any? = null
) {
    val effectiveExecutable = executable ?: project.findProperty("itemis.mps.gradle.ant.defaultJavaExecutable")

    project.javaexec {
        if (effectiveExecutable != null) {
            executable(effectiveExecutable)
        }

        mainClass.set("org.apache.tools.ant.launch.Launcher")
        this@javaexec.workingDir = workingDir

        if (includeDefaultClasspath) {
            val defaultClasspath = project.findProperty("itemis.mps.gradle.ant.defaultScriptClasspath")
            if (defaultClasspath != null) {
                classpath(defaultClasspath)
            }
        }

        if (scriptClasspath != null) {
            classpath(scriptClasspath)
        }

        args(args)
    }
}
