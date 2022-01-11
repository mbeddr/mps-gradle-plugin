package de.itemis.mps.gradle


import org.gradle.api.DefaultTask
import org.gradle.api.file.FileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.logging.LogLevel
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.api.tasks.Optional
import java.util.*
import javax.inject.Inject

abstract class RunAntScript @Inject constructor(of: ObjectFactory) : DefaultTask() {
    @get:Input
    val script: Property<Any> = of.property(Any::class.java)

    @get:Input
    abstract val targets: ListProperty<String>

    @get:Input
    val scriptClasspath: RegularFileProperty = of.fileProperty()

    @Input
    val scriptArgs: ListProperty<String> = of.listProperty(String::class.java)


    @get:Input
    val includeDefaultArgs: Property<Boolean> = of.property(Boolean::class.java)


    @get:Input
    val includeDefaultClasspath: Property<Boolean> = of.property(Boolean::class.java)


    @get:Input
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
    @get:Input
    val incremental: Property<Boolean> = of.property(Boolean::class.java)

        fun targets(vararg targets: String) {
            this.targets.set(targets.toList())
        }

        fun executable(executable: Any?) {
            this.executable.set(executable)
        }

        @TaskAction
        fun build() {
            val allArgs = scriptArgs.get().toMutableList()
            if (includeDefaultArgs.getOrElse(true)) {
                val defaultArgs = project.findProperty("itemis.mps.gradle.ant.defaultScriptArgs") as Collection<*>?
                if (defaultArgs != null) {
                    allArgs += defaultArgs.map { it as String }
                }
            }

            if (logging.level != null && logging.level != LogLevel.LIFECYCLE && !allArgs.any { it.startsWith("-Dmps.ant.log=") }) {
                allArgs += "-Dmps.ant.log=${logging.level.toString().toLowerCase(Locale.ENGLISH)}"
            }

            val isIncremental = incremental.getOrElse(false)
            if (isIncremental) {
                allArgs += "-Dmps.generator.skipUnmodifiedModels=true"
            }

            var targs: List<String> = targets.get().toList()
            if (isIncremental) {
                targs = targs.filter { s -> "clean" != s }
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

                if (scriptClasspath.isPresent) {
                    classpath(scriptClasspath.get())
                }

                args(allArgs)
                args("-buildfile", project.file(script.get()))
                args(targs)
            }
        }
}

open class BuildLanguages @Inject constructor(@Internal val of: ObjectFactory) : RunAntScript(of) {

    override val targets: ListProperty<String>
        get() {
            val listProperty = of.listProperty(String::class.java)
            listProperty.addAll(listOf("clean", "generate", "assemble"))
            return listProperty
        }
}

open class TestLanguages @Inject constructor(@Internal val of: ObjectFactory) : RunAntScript(of) {

    override val targets: ListProperty<String>
        get() {
            val listProperty = of.listProperty(String::class.java)
            listProperty.addAll(listOf("clean", "generate", "assemble", "check"))
            return listProperty
        }
}