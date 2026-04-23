package de.itemis.mps.gradle.tasks

import de.itemis.mps.gradle.BackendConfigurations
import de.itemis.mps.gradle.TaskGroups
import de.itemis.mps.gradle.launcher.MpsBackendBuilder
import de.itemis.mps.gradle.launcher.MpsVersionDetection
import org.gradle.api.GradleException
import org.gradle.api.Incubating
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileCollection
import org.gradle.api.logging.LogLevel
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.*
import org.gradle.kotlin.dsl.newInstance
import org.gradle.process.CommandLineArgumentProvider

@Incubating
@UntrackedTask(because = "Operates 'in place'")
abstract class Remigrate : JavaExec(), MpsProjectTask {

    @Input
    abstract override fun getLogLevel(): Property<LogLevel>

    @Internal("covered by mpsVersion and classpath")
    abstract override fun getMpsHome(): DirectoryProperty

    @Input
    @Optional
    abstract override fun getMpsVersion(): Property<String>

    @Internal("covered by allProjectFiles")
    abstract override fun getProjectLocation(): DirectoryProperty

    @get:Internal("covered by allProjectFiles")
    abstract val projectLocations: ConfigurableFileCollection

    @get:InputFiles
    @get:SkipWhenEmpty
    @get:IgnoreEmptyDirectories
    @get:PathSensitive(PathSensitivity.RELATIVE)
    protected val allProjectFiles = providerFactory.provider {
        effectiveProjectLocations().flatMap { objectFactory.fileTree().from(it) }
    }

    @Internal("Folder macros are ignored for the purposes of up-to-date checks and caching")
    abstract override fun getFolderMacros(): MapProperty<String, Directory>

    @Classpath
    abstract override fun getPluginRoots(): ConfigurableFileCollection

    @get:Internal
    abstract val additionalClasspath: ConfigurableFileCollection

    @get:Input
    abstract val excludedModuleMigrations: SetProperty<ExcludedModuleMigration>

    fun excludeModuleMigration(language: String, version: Int) {
        excludedModuleMigrations.add(ExcludedModuleMigration(language, version))
    }

    init {
        mpsVersion.convention(MpsVersionDetection.fromMpsHome(project.layout, providerFactory, mpsHome.asFile))
        additionalClasspath.from(mpsHome.asFileTree.matching { include("lib/**/*.jar") })

        val backendBuilder: MpsBackendBuilder = project.objects.newInstance(MpsBackendBuilder::class)
        backendBuilder.withMpsHomeDirectory(mpsHome).withMpsVersion(mpsVersion).configure(this)

        val backendConfig = project.configurations.named(BackendConfigurations.REMIGRATE_BACKEND_CONFIGURATION_NAME)
        dependsOn(backendConfig)

        argumentProviders.add(CommandLineArgumentProvider {
            val result = mutableListOf<String>()

            for (dir in effectiveProjectLocations()) {
                result.add("--project=$dir")
            }

            addPluginRoots(result, pluginRoots)

            if (logLevel.get() <= LogLevel.INFO) {
                result.add("--log-level=${logLevel.get()}")
            }

            addFolderMacros(result, folderMacros)

            val pluginFile = backendConfig.get().resolvedConfiguration.firstLevelModuleDependencies
                .flatMap { it.moduleArtifacts.map { it.file } }
                .single()

            result.add("--plugin=de.itemis.mps.buildbackends.remigrate::$pluginFile")

            result.addAll(
                excludedModuleMigrations.get().map { "--exclude-module-migration=${it.language}:${it.version}" })

            result
        })

        group = TaskGroups.MIGRATION

        // Additional classpath goes before backend config in order to fix problem with Kotlin version mismatch of
        // backend vs IDEA.
        classpath(additionalClasspath)
        classpath(backendConfig)

        mainClass.set("de.itemis.mps.gradle.remigrate.MainKt")
    }

    @TaskAction
    override fun exec() {
        for (dir in effectiveProjectLocations()) {
            checkProjectLocation(dir)
        }
        super.exec()
    }

    private fun effectiveProjectLocations(): FileCollection {
        val hasMultiple = !projectLocations.isEmpty
        val hasSingle = projectLocation.isPresent
        if (hasMultiple && hasSingle) {
            throw GradleException("Cannot set both projectLocation and projectLocations. Use one or the other.")
        }
        if (!hasMultiple && !hasSingle) {
            throw GradleException("Must set either projectLocation or projectLocations.")
        }
        return if (hasMultiple) projectLocations else objectFactory.fileCollection().from(projectLocation)
    }
}
