package de.itemis.mps.gradle.tasks

import de.itemis.mps.gradle.BackendConfigurations
import de.itemis.mps.gradle.ErrorMessages
import de.itemis.mps.gradle.launcher.MpsBackendBuilder
import de.itemis.mps.gradle.launcher.MpsVersionDetection
import org.gradle.api.GradleException
import org.gradle.api.Incubating
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.logging.LogLevel
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.*
import org.gradle.kotlin.dsl.newInstance
import org.gradle.work.DisableCachingByDefault


@DisableCachingByDefault(because = "calls arbitrary user code")
@Incubating
abstract class MpsExecute : JavaExec() {

    @get:Internal
    abstract val mpsHome: DirectoryProperty

    @get:Internal
    abstract val mpsVersion: Property<String>

    @get:Internal
    abstract val projectLocation: DirectoryProperty

    @get:Classpath
    abstract val pluginRoots: SetProperty<Directory>

    @get:Internal
    abstract val macros: MapProperty<String, String>

    @get:Internal
    abstract val module: Property<String>

    @get:Internal
    abstract val className: Property<String>

    @get:Internal
    abstract val method: Property<String>

    @get:Internal
    abstract val methodArguments: ListProperty<String>

    @get:Internal
    val additionalExecuteBackendClasspath: ConfigurableFileCollection =
        objectFactory.fileCollection().from(initialExecuteBackendClasspath())

    init {
        mpsVersion.convention(MpsVersionDetection.fromMpsHome(project.layout, providerFactory, mpsHome.asFile))
        projectLocation.convention(project.layout.projectDirectory)

        objectFactory.newInstance(MpsBackendBuilder::class)
            .withMpsHomeDirectory(mpsHome)
            .withMpsVersion(mpsVersion)
            .configure(this)

        argumentProviders.add {
            mutableListOf<String>().apply {
                add("--project=${projectLocation.get().asFile}")

                pluginRoots.get().forEach {
                    findPluginsRecursively(it.asFile).forEach {
                        add("--plugin=${it.id}::${it.path}")
                    }
                }
                macros.get().forEach { add("--macro=${it.key}::${it.value}") }

                add("--module=${module.get()}")
                add("--class=${className.get()}")
                add("--method=${method.get()}")
                methodArguments.get().forEach { add("--arg=$it") }

                val effectiveLogLevel = logging.level ?: project.logging.level ?: project.gradle.startParameter.logLevel
                if (effectiveLogLevel <= LogLevel.INFO) {
                    add("--log-level=info")
                }
            }
        }

        description = "Execute specified method from a generated class to modify the MPS project"
        group = "execute"

        classpath(project.configurations.named(BackendConfigurations.EXECUTE_BACKEND_CONFIGURATION_NAME))
        classpath(additionalExecuteBackendClasspath)

        mainClass.set("de.itemis.mps.gradle.execute.MainKt")
    }

    @TaskAction
    override fun exec() {
        val projectLocationAsFile = projectLocation.get().asFile
        if (!projectLocationAsFile.resolve(".mps").isDirectory) {
            throw GradleException(ErrorMessages.noMpsProjectIn(projectLocationAsFile))
        }

        super.exec()
    }

    private fun initialExecuteBackendClasspath() = mpsHome.asFileTree.matching {
        include("lib/**/*.jar")
    }
}
