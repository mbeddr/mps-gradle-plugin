package de.itemis.mps.gradle.tasks

import de.itemis.mps.gradle.BackendConfigurations
import de.itemis.mps.gradle.EnvironmentKind
import de.itemis.mps.gradle.TaskGroups
import de.itemis.mps.gradle.launcher.MpsBackendBuilder
import de.itemis.mps.gradle.launcher.MpsVersionDetection
import org.gradle.api.Incubating
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileTree
import org.gradle.api.logging.LogLevel
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.kotlin.dsl.newInstance
import org.gradle.process.CommandLineArgumentProvider

@CacheableTask
@Incubating
abstract class MpsGenerate : JavaExec(), MpsProjectTask {

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

    @get:Input
    abstract val environmentKind: Property<EnvironmentKind>

    @get:Input
    abstract val models: ListProperty<String>

    @get:Input
    abstract val modules: ListProperty<String>

    @get:Input
    abstract val excludeModels: ListProperty<String>

    @get:Input
    abstract val excludeModules: ListProperty<String>

    @get:Input
    abstract val strictMode: Property<Boolean>

    @get:Input
    abstract val parallelGenerationThreads: Property<Int>

    @get:Internal("covered by classpath")
    abstract val additionalGenerateBackendClasspath: ConfigurableFileCollection

    init {
        mpsVersion.convention(MpsVersionDetection.fromMpsHome(project.layout, providerFactory, mpsHome.asFile))
        projectLocation.convention(project.layout.projectDirectory)
        environmentKind.convention(EnvironmentKind.MPS)
        strictMode.convention(true)
        parallelGenerationThreads.convention(0)
    }

    @Suppress("unused")
    @get:InputFiles
    @get:SkipWhenEmpty
    @get:PathSensitive(PathSensitivity.NONE)
    protected val sources: FileTree = projectLocation.asFileTree.matching {
        exclude(project.layout.buildDirectory.get().asFile.relativeTo(projectLocation.get().asFile).path + "/**")
        exclude("**/*_gen*/**")

        include("**/*.msd")
        include("**/*.mpsr")
        include("**/*.mps")
    }

    @Suppress("unused")
    @get:Classpath
    protected val compiledClasses: FileTree = projectLocation.asFileTree.matching {
        exclude(project.layout.buildDirectory.get().asFile.relativeTo(projectLocation.get().asFile).path + "/**")
        include("**/classes_gen/*")
    }

    init {
        val backendBuilder: MpsBackendBuilder = project.objects.newInstance(MpsBackendBuilder::class)
        backendBuilder.withMpsHomeDirectory(mpsHome).withMpsVersion(mpsVersion).configure(this)

        argumentProviders.add(CommandLineArgumentProvider {
            val result = mutableListOf<String>()

            result.add("--project=${projectLocation.get().asFile}")

            addPluginRoots(result, pluginRoots)
            addFolderMacros(result, folderMacros)

            result.add("--environment=${environmentKind.get()}")

            result.addAll(models.get().map { "--model=$it" })
            result.addAll(modules.get().map { "--module=$it" })
            result.addAll(excludeModels.get().map { "--exclude-model=$it" })
            result.addAll(excludeModules.get().map { "--exclude-module=$it" })

            if (logLevel.get() <= LogLevel.INFO) {
                result.add("--log-level=${logLevel.get()}")
            }

            if (!strictMode.get()) {
                result.add("--no-strict-mode")
            }

            if (parallelGenerationThreads.get() != 0) {
                result.add("--parallel-generation-threads=${parallelGenerationThreads.get()}")
            }

            result
        })

        group = TaskGroups.GENERATION

        classpath(mpsJars())
        classpath(additionalGenerateBackendClasspath)
        classpath(project.configurations.named(BackendConfigurations.GENERATE_BACKEND_CONFIGURATION_NAME))

        mainClass.set("de.itemis.mps.gradle.generate.MainKt")
    }

    override fun exec() {
        checkProjectLocation(projectLocation)

        super.exec()
    }

    private fun mpsJars() = mpsHome.asFileTree.matching {
        include("lib/**/*.jar")
    }
}
