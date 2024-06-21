package de.itemis.mps.gradle.tasks

import de.itemis.mps.gradle.BackendConfigurations
import de.itemis.mps.gradle.EnvironmentKind
import de.itemis.mps.gradle.TaskGroups
import de.itemis.mps.gradle.launcher.MpsBackendBuilder
import org.gradle.api.Incubating
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileTree
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.kotlin.dsl.listProperty
import org.gradle.kotlin.dsl.mapProperty
import org.gradle.kotlin.dsl.newInstance
import org.gradle.kotlin.dsl.property
import org.gradle.process.CommandLineArgumentProvider

@CacheableTask
@Incubating
abstract class MpsGenerate : MpsTask, JavaExec() {

    @get:Internal("only modules and models matter, covered by #sources")
    val projectLocation: DirectoryProperty =
        objectFactory.directoryProperty().convention(project.layout.projectDirectory)

    @get:Input
    val environmentKind: Property<EnvironmentKind> = objectFactory.property<EnvironmentKind>()
        .convention(EnvironmentKind.MPS)

    @Deprecated("All macros should point to directories, property will be removed in a later release")
    @get:Input
    val varMacros: MapProperty<String, String> = objectFactory.mapProperty()

    @get:Input
    val models: ListProperty<String> = objectFactory.listProperty()

    @get:Input
    val modules: ListProperty<String> = objectFactory.listProperty()

    @get:Input
    val excludeModels: ListProperty<String> = objectFactory.listProperty()

    @get:Input
    val excludeModules: ListProperty<String> = objectFactory.listProperty()

    @get:Input
    val strictMode: Property<Boolean> = objectFactory.property<Boolean>().convention(true)

    @get:Input
    val parallelGenerationThreads: Property<Int> = objectFactory.property<Int>().convention(0)

    @get:Internal("covered by classpath")
    val additionalGenerateBackendClasspath: ConfigurableFileCollection =
        objectFactory.fileCollection().from(initialGenerateBackendClasspath())

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

        argumentProviders.add(backendArguments())
        argumentProviders.add(CommandLineArgumentProvider {
            val result = mutableListOf<String>()
            @Suppress("DEPRECATION")
            addVarMacros(result, varMacros, this)

            result.add("--project=${projectLocation.get().asFile}")
            result.add("--environment=${environmentKind.get()}")

            result.addAll(models.get().map { "--model=$it" })
            result.addAll(modules.get().map { "--module=$it" })
            result.addAll(excludeModels.get().map { "--exclude-model=$it" })
            result.addAll(excludeModules.get().map { "--exclude-module=$it" })

            if (!strictMode.get()) {
                result.add("--no-strict-mode")
            }

            if (parallelGenerationThreads.get() != 0) {
                result.add("--parallel-generation-threads=${parallelGenerationThreads.get()}")
            }

            result
        })

        group = TaskGroups.GENERATION

        classpath(project.configurations.named(BackendConfigurations.GENERATE_BACKEND_CONFIGURATION_NAME))
        classpath(additionalGenerateBackendClasspath)

        mainClass.set("de.itemis.mps.gradle.generate.MainKt")
    }

    override fun exec() {
        checkProjectLocation(projectLocation)

        super.exec()
    }

    private fun initialGenerateBackendClasspath() = mpsHome.asFileTree.matching {
        include("lib/**/*.jar")
    }
}
