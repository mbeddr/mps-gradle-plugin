package de.itemis.mps.gradle.tasks

import de.itemis.mps.gradle.BackendConfigurations
import de.itemis.mps.gradle.launcher.MpsBackendBuilder
import de.itemis.mps.gradle.launcher.MpsVersionDetection
import org.gradle.api.Incubating
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.logging.LogLevel
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.kotlin.dsl.newInstance
import org.gradle.work.DisableCachingByDefault


@DisableCachingByDefault(because = "calls arbitrary user code")
@Incubating
abstract class MpsExecute : JavaExec(), MpsProjectTask {

    @Input
    abstract override fun getLogLevel(): Property<LogLevel>

    @Internal("covered by mpsVersion and classpath")
    abstract override fun getMpsHome(): DirectoryProperty

    @Input
    @Optional
    abstract override fun getMpsVersion(): Property<String>

    @Internal("covered by sources")
    abstract override fun getProjectLocation(): DirectoryProperty

    @Classpath
    abstract override fun getPluginRoots(): ConfigurableFileCollection

    @Internal("Folder macros are ignored for the purposes of up-to-date checks and caching")
    abstract override fun getFolderMacros(): MapProperty<String, Directory>

    @get:Internal
    abstract val module: Property<String>

    @get:Internal
    abstract val className: Property<String>

    @get:Internal
    abstract val method: Property<String>

    @get:Internal
    abstract val methodArguments: ListProperty<String>

    @get:Internal
    abstract val additionalExecuteBackendClasspath: ConfigurableFileCollection

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

                addPluginRoots(this, pluginRoots)
                addFolderMacros(this, folderMacros)

                add("--module=${module.get()}")
                add("--class=${className.get()}")
                add("--method=${method.get()}")
                methodArguments.get().forEach { add("--arg=$it") }

                if (logLevel.get() <= LogLevel.INFO) {
                    add("--log-level=${logLevel.get()}")
                }
            }
        }

        description = "Execute specified method from a generated class to modify the MPS project"
        group = "execute"

        classpath(mpsJars())
        classpath(project.configurations.named(BackendConfigurations.EXECUTE_BACKEND_CONFIGURATION_NAME))
        classpath(additionalExecuteBackendClasspath)

        mainClass.set("de.itemis.mps.gradle.execute.MainKt")
    }

    @TaskAction
    override fun exec() {
        checkProjectLocation(projectLocation)

        super.exec()
    }

    private fun mpsJars() = mpsHome.asFileTree.matching {
        include("lib/**/*.jar")
    }
}
