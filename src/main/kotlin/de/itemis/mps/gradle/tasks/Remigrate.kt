package de.itemis.mps.gradle.tasks

import de.itemis.mps.gradle.BackendConfigurations
import de.itemis.mps.gradle.TaskGroups
import de.itemis.mps.gradle.launcher.MpsBackendBuilder
import de.itemis.mps.gradle.launcher.MpsVersionDetection
import org.gradle.api.Incubating
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.tasks.*
import org.gradle.kotlin.dsl.mapProperty
import org.gradle.kotlin.dsl.newInstance
import org.gradle.kotlin.dsl.property
import org.gradle.process.CommandLineArgumentProvider
import javax.inject.Inject

@Incubating
@UntrackedTask(because = "Operates 'in place'")
open class Remigrate @Inject constructor(
    objectFactory: ObjectFactory,
    providerFactory: ProviderFactory
) : JavaExec() {

    @get:Internal
    val mpsHome: DirectoryProperty = objectFactory.directoryProperty()

    @get:Input
    @get:Optional
    val mpsVersion: Property<String> = objectFactory.property<String>()
        .convention(MpsVersionDetection.fromMpsHome(project.layout, providerFactory, mpsHome.asFile))

    @get:Internal("covered by allProjectFiles")
    val projectDirectories: ConfigurableFileCollection = objectFactory.fileCollection()

    @get:InputFiles
    @get:SkipWhenEmpty
    @get:IgnoreEmptyDirectories
    protected val allProjectFiles = providerFactory.provider { projectDirectories.flatMap { objectFactory.fileTree().from(it) } }

    @get:Internal("Folder macros are ignored for the purposes of up-to-date checks and caching")
    val folderMacros: MapProperty<String, Directory> = objectFactory.mapProperty()

    @get:Classpath
    val pluginRoots: ConfigurableFileCollection = objectFactory.fileCollection()

    @get:Internal
    val additionalClasspath: ConfigurableFileCollection =
        objectFactory.fileCollection().from(initialBackendClasspath())


    init {
        val backendBuilder: MpsBackendBuilder = project.objects.newInstance(MpsBackendBuilder::class)
        backendBuilder.withMpsHomeDirectory(mpsHome).withMpsVersion(mpsVersion).configure(this)

        val backendConfig = project.configurations.named(BackendConfigurations.REMIGRATE_BACKEND_CONFIGURATION_NAME)
        dependsOn(backendConfig)

        argumentProviders.add(CommandLineArgumentProvider {
            val result = mutableListOf<String>()

            for (dir in projectDirectories) {
                result.add("--project=$dir")
            }

            addPluginRoots(result, pluginRoots)
            addLogLevel(result)
            addFolderMacros(result, folderMacros)
            result.add("--plugin=de.itemis.mps.buildbackends.remigrate::" +
                    backendConfig.get().files(backendConfig.get().dependencies.first()).first())

            result
        })

        group = TaskGroups.MIGRATION

        classpath(backendConfig)
        classpath(additionalClasspath)

        mainClass.set("de.itemis.mps.gradle.remigrate.MainKt")
    }

    @TaskAction
    override fun exec() {
        for (dir in projectDirectories) {
            checkProjectLocation(dir)
        }
        super.exec()
    }

    private fun initialBackendClasspath() = mpsHome.asFileTree.matching {
        include("lib/**/*.jar")
    }
}
