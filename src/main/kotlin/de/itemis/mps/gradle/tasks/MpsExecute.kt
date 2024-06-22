package de.itemis.mps.gradle.tasks

import de.itemis.mps.gradle.BackendConfigurations
import de.itemis.mps.gradle.launcher.MpsBackendBuilder
import de.itemis.mps.gradle.launcher.MpsVersionDetection
import org.gradle.api.Incubating
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.kotlin.dsl.newInstance
import org.gradle.work.DisableCachingByDefault


@DisableCachingByDefault(because = "calls arbitrary user code")
@Incubating
abstract class MpsExecute : MpsTask, JavaExec() {
    @get:Internal
    abstract val projectLocation: DirectoryProperty

    @Deprecated("Use [folderMacros].")
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

        argumentProviders.add(backendArguments())
        argumentProviders.add {
            mutableListOf<String>().apply {
                @Suppress("DEPRECATION")
                addVarMacros(this, macros, this@MpsExecute, "macros")

                add("--project=${projectLocation.get().asFile}")

                add("--module=${module.get()}")
                add("--class=${className.get()}")
                add("--method=${method.get()}")
                methodArguments.get().forEach { add("--arg=$it") }
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
        checkProjectLocation(projectLocation)

        super.exec()
    }

    private fun initialExecuteBackendClasspath() = mpsHome.asFileTree.matching {
        include("lib/**/*.jar")
    }
}
