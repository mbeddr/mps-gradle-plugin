package de.itemis.mps.gradle.tasks

import de.itemis.mps.gradle.BackendConfigurations
import de.itemis.mps.gradle.TaskGroups
import de.itemis.mps.gradle.launcher.MpsBackendBuilder
import org.gradle.api.Incubating
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.tasks.*
import org.gradle.kotlin.dsl.newInstance
import org.gradle.process.CommandLineArgumentProvider
import javax.inject.Inject

@Incubating
@UntrackedTask(because = "Operates 'in place'")
abstract class Remigrate @Inject constructor(
    objectFactory: ObjectFactory,
    providerFactory: ProviderFactory
) : MpsTask, JavaExec() {

    @get:Internal("covered by allProjectFiles")
    val projectDirectories: ConfigurableFileCollection = objectFactory.fileCollection()

    @get:InputFiles
    @get:SkipWhenEmpty
    @get:IgnoreEmptyDirectories
    protected val allProjectFiles = providerFactory.provider { projectDirectories.flatMap { objectFactory.fileTree().from(it) } }

    @get:Internal
    val additionalClasspath: ConfigurableFileCollection =
        objectFactory.fileCollection().from(initialBackendClasspath())


    init {
        val backendBuilder: MpsBackendBuilder = project.objects.newInstance(MpsBackendBuilder::class)
        backendBuilder.withMpsHomeDirectory(mpsHome).withMpsVersion(mpsVersion).configure(this)

        val backendConfig = project.configurations.named(BackendConfigurations.REMIGRATE_BACKEND_CONFIGURATION_NAME)
        dependsOn(backendConfig)

        argumentProviders.add(backendArguments())
        argumentProviders.add(CommandLineArgumentProvider {
            val result = mutableListOf<String>()

            for (dir in projectDirectories) {
                result.add("--project=$dir")
            }

            result.add("--plugin=de.itemis.mps.buildbackends.remigrate::" +
                    backendConfig.get().files(backendConfig.get().dependencies.first()).first())

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
        for (dir in projectDirectories) {
            checkProjectLocation(dir)
        }
        super.exec()
    }

    private fun initialBackendClasspath() = mpsHome.asFileTree.matching {
        include("lib/**/*.jar")
    }
}
