package de.itemis.mps.gradle


import org.gradle.api.DefaultTask
import org.gradle.api.file.FileCollection
import org.gradle.api.logging.LogLevel
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.property
import java.util.*
import javax.inject.Inject

open class RunAntScript @Inject constructor(of: ObjectFactory) : DefaultTask() {
    @Input
    val script: Property<Any> = of.property(Any::class.java)

    @Input
    var targets: List<String> = emptyList()

    @Optional
    @InputFiles
    var scriptClasspath: FileCollection? = null

    @Input
    var scriptArgs: List<String> = emptyList()

    @Optional
    @Input
    val includeDefaultArgs: Property<Boolean> = of.property(Boolean::class.java)

    @Optional
    @Input
    val includeDefaultClasspath: Property<Boolean> = of.property(Boolean::class.java)

    @Optional
    @Input
    val executable: Property<Any> = of.property(Any::class.java)

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
            this.executable.set(executable)
        }

        @TaskAction
        fun build() {
            val allArgs = scriptArgs.toMutableList()
            if (includeDefaultArgs.getOrElse(true)) {
                val defaultArgs = project.findProperty("itemis.mps.gradle.ant.defaultScriptArgs") as Collection<*>?
                if (defaultArgs != null) {
                    allArgs += defaultArgs.map { it as String }
                }
            }

            if (logging.level != null && logging.level != LogLevel.LIFECYCLE && !allArgs.any { it.startsWith("-Dmps.ant.log=") }) {
                allArgs += "-Dmps.ant.log=${logging.level.toString().toLowerCase(Locale.ENGLISH)}"
            }

            if (incremental) {
                allArgs += "-Dmps.generator.skipUnmodifiedModels=true"
            }

            val targets = if (incremental) {
                targets - "clean"
            } else {
                targets
            }

            project.javaexec {
                if (this@RunAntScript.executable.isPresent) {
                    executable(this@RunAntScript.executable.get())
                } else {
                    val defaultJava = project.findProperty("itemis.mps.gradle.ant.defaultJavaExecutable")
                    if (defaultJava != null) {
                        executable(defaultJava)
                    }
                }

                mainClass.set("org.apache.tools.ant.launch.Launcher")
                workingDir = project.rootDir

                if (includeDefaultClasspath.getOrElse(true)) {
                    val defaultClasspath = project.findProperty(
                        "itemis.mps.gradle.ant.defaultScriptClasspath"
                    ) as FileCollection?
                    if (defaultClasspath != null) {
                        classpath(defaultClasspath)
                    }
                }

                if (scriptClasspath != null) {
                    classpath(scriptClasspath)
                }

                args(allArgs)
                args("-buildfile", project.file(script.get()))
                args(targets)
            }
        }
}

open class BuildLanguages @Inject constructor(of: ObjectFactory) : RunAntScript(of) {
    init {
        targets = listOf("clean", "generate", "assemble")
    }
}

open class TestLanguages @Inject constructor(of: ObjectFactory) : RunAntScript(of) {
    init {
        targets = listOf("clean", "generate", "assemble", "check")
    }
}