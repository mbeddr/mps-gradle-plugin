package de.itemis.mps.gradle;

import de.itemis.mps.gradle.launcher.MpsVersionDetection
import de.itemis.mps.gradle.tasks.MpsTask
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.logging.LogLevel
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.tasks.*
import org.gradle.jvm.toolchain.JavaLauncher
import org.gradle.process.ExecOperations
import org.gradle.work.DisableCachingByDefault
import javax.inject.Inject

@DisableCachingByDefault(because = "Runs an Ant script that builds MPS languages and has non-trivial, external outputs")
abstract class RunAntScript : DefaultTask(), MpsTask {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val script: RegularFileProperty

    @get:Input
    abstract val targets: ListProperty<String>

    @get:Classpath
    abstract val scriptClasspath: ConfigurableFileCollection

    @get:Input
    abstract val scriptArgs: ListProperty<String>

    @get:Internal("working directory has no effect on the output")
    abstract val workingDirectory: DirectoryProperty

    @get:Inject
    protected abstract val execOperations: ExecOperations

    @get:Inject
    protected abstract val providerFactory: ProviderFactory

    @Input
    @Optional
    abstract override fun getMpsVersion(): Property<String>

    @Nested
    @Optional
    abstract override fun getJavaLauncher(): Property<JavaLauncher>

    init {
        mpsVersion.convention(MpsVersionDetection.fromMpsHome(project.layout, providerFactory, mpsHome.asFile))
        scriptClasspath.convention(mpsHome.asFileTree.matching {
            include("lib/ant/lib/*.jar")
            include("lib/*.jar")
        })
        workingDirectory.convention(project.rootProject.layout.projectDirectory)
    }

    @TaskAction
    fun build() {
        val allArgs = scriptArgs.get().toMutableList()

        val mpsAntLogLevel = toMpsAntLogLevel(logLevel.get())
        if (mpsAntLogLevel != null && !allArgs.any { it.startsWith("-Dmps.ant.log=") }) {
            allArgs += "-Dmps.ant.log=$mpsAntLogLevel"
        }

        val mpsHomePath = mpsHome.get().asFile.absolutePath
        if (!allArgs.any { it.startsWith("-Dmps.home=") }) {
            allArgs += "-Dmps.home=$mpsHomePath"
        }
        if (!allArgs.any { it.startsWith("-Dmps_home=") }) {
            allArgs += "-Dmps_home=$mpsHomePath"
        }

        allArgs += "-buildfile"
        allArgs += script.get().asFile.absolutePath
        allArgs += targets.get()

        execOperations.javaexec {
            if (javaLauncher.isPresent) {
                executable(javaLauncher.get().executablePath)
            }

            mainClass.set("org.apache.tools.ant.launch.Launcher")
            workingDir = workingDirectory.get().asFile

            classpath(scriptClasspath)

            args(allArgs)
        }
    }

    /**
     * Maps a Gradle log level to a value of the `mps.ant.log` property, which MPS build scripts pass to the `logLevel`
     * attribute of MPS Ant tasks. That attribute only accepts `off`, `error`, `warn`, `warning`, `info`, `debug` and
     * `all`. Returns `null` for [LogLevel.LIFECYCLE], the default Gradle level, so that the script's own default
     * stays in effect.
     */
    private fun toMpsAntLogLevel(level: LogLevel): String? = when (level) {
        LogLevel.DEBUG -> "debug"
        LogLevel.INFO -> "info"
        LogLevel.LIFECYCLE -> null
        LogLevel.WARN -> "warn"
        LogLevel.QUIET, LogLevel.ERROR -> "error"
    }
}

@DisableCachingByDefault(because = "Runs an Ant script that builds MPS languages and has non-trivial, external outputs")
abstract class BuildLanguages : RunAntScript() {
    init {
        targets.convention(listOf("clean", "generate", "assemble"))
    }
}

@DisableCachingByDefault(because = "Runs an Ant script that tests MPS languages and has non-trivial, external outputs")
abstract class TestLanguages : RunAntScript() {
    init {
        targets.convention(listOf("clean", "generate", "assemble", "check"))
    }
}
