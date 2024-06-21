package de.itemis.mps.gradle.tasks

import de.itemis.mps.gradle.BackendConfigurations
import de.itemis.mps.gradle.TaskGroups
import de.itemis.mps.gradle.launcher.MpsBackendBuilder
import org.gradle.api.Incubating
import org.gradle.api.file.*
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.kotlin.dsl.*
import org.gradle.process.CommandLineArgumentProvider

@CacheableTask
@Incubating
abstract class MpsCheck : JavaExec(), MpsTask, VerificationTask {
    @get:Internal("only modules and models matter, covered by #sources")
    val projectLocation: DirectoryProperty =
        objectFactory.directoryProperty().convention(project.layout.projectDirectory)

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
    val warningAsError: Property<Boolean> = objectFactory.property<Boolean>().convention(false)

    @get:OutputFile
    val junitFile: RegularFileProperty = objectFactory.fileProperty()
        .convention(project.layout.buildDirectory.map { it.file("TEST-${this@MpsCheck.name}.xml") })

    @get:Input
    val junitFormat: Property<String> = objectFactory.property<String>().convention("module-and-model")

    @get:Input
    val parallel: Property<Boolean> = objectFactory.property<Boolean>().convention(false)

    @get:Internal("covered by classpath")
    val additionalModelcheckBackendClasspath: ConfigurableFileCollection =
        objectFactory.fileCollection().from(initialModelcheckBackendClasspath())

    @Suppress("unused")
    @get:InputFiles
    @get:SkipWhenEmpty
    @get:PathSensitive(PathSensitivity.NONE)
    protected val sources: FileTree = projectLocation.asFileTree.matching {
        exclude(project.layout.buildDirectory.get().asFile.toRelativeString(projectLocation.get().asFile))
        exclude("**/*_gen*")
        exclude("**/*_gen*/**")

        include("**/*.msd")
        include("**/*.mpsr")
        include("**/*.mps")
    }

    @Suppress("unused")
    @get:Classpath
    protected val compiledClasses: FileTree = projectLocation.asFileTree.matching {
        exclude(project.layout.buildDirectory.get().asFile.toRelativeString(projectLocation.get().asFile))
        include("**/classes_gen/**")
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

            // Only a limited subset of checkers is registered in MPS environment, IDEA environment is necessary for
            // proper checking.
            result.add("--environment=IDEA")

            result.addAll(models.get().map { "--model=$it" })
            result.addAll(modules.get().map { "--module=$it" })
            result.addAll(excludeModels.get().map { "--exclude-model=$it" })
            result.addAll(excludeModules.get().map { "--exclude-module=$it" })

            if (warningAsError.get()) {
                result.add("--warning-as-error")
            }

            if (ignoreFailures) {
                result.add("--error-no-fail")
            }

            if (junitFile.isPresent) {
                result.add("--result-file=${junitFile.get().asFile}")
            }

            if (junitFormat.isPresent) {
                result.add("--result-format=${junitFormat.get()}")
            }

            if (parallel.get()) {
                result.add("--parallel")
            }

            result
        })

        group = TaskGroups.VERIFICATION

        classpath(project.configurations.named(BackendConfigurations.MODELCHECK_BACKEND_CONFIGURATION_NAME))
        classpath(additionalModelcheckBackendClasspath)

        mainClass.set("de.itemis.mps.gradle.modelcheck.MainKt")
    }

    override fun exec() {
        checkProjectLocation(projectLocation)
        super.exec()
    }

    private fun initialModelcheckBackendClasspath() = mpsHome.asFileTree.matching {
        include("lib/**/*.jar")

        // add only minimal number of plugins jars that are required by the modelcheck code
        // (to avoid conflicts with plugin classloader if custom configured plugins are loaded)
        // mps-httpsupport: we need it to print the node url to the console.
        // mps-modelchecker: contains used UnresolvedReferencesChecker
        // git4idea: has to be on classpath as bundled plugin to be loaded (since 2019.3)
        include("plugins/mps-modelchecker/**/*.jar")
        include("plugins/mps-httpsupport/**/*.jar")
        include("plugins/git4idea/**/*.jar")
    }
}
