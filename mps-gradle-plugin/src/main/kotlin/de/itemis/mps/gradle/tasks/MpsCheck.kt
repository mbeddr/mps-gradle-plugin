package de.itemis.mps.gradle.tasks

import de.itemis.mps.gradle.BackendConfigurations
import de.itemis.mps.gradle.TaskGroups
import de.itemis.mps.gradle.launcher.MpsBackendBuilder
import de.itemis.mps.gradle.launcher.MpsVersionDetection
import org.gradle.api.Incubating
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileTree
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.logging.LogLevel
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.kotlin.dsl.newInstance
import org.gradle.process.CommandLineArgumentProvider

@CacheableTask
@Incubating
abstract class MpsCheck : JavaExec(), VerificationTask, MpsProjectTask {

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
    abstract val models: ListProperty<String>

    @get:Input
    abstract val modules: ListProperty<String>

    @get:Input
    abstract val excludeModels: ListProperty<String>

    @get:Input
    abstract val excludeModules: ListProperty<String>

    @get:Input
    abstract val warningAsError: Property<Boolean>

    @get:OutputFile
    abstract val junitFile: RegularFileProperty

    @get:Input
    abstract val junitFormat: Property<String>

    @get:Input
    abstract val parallel: Property<Boolean>

    @get:Internal("covered by classpath")
    abstract val additionalModelcheckBackendClasspath: ConfigurableFileCollection

    init {
        mpsVersion.convention(MpsVersionDetection.fromMpsHome(project.layout, providerFactory, mpsHome.asFile))
        projectLocation.convention(project.layout.projectDirectory)
        warningAsError.convention(false)
        junitFile.convention(project.layout.buildDirectory.map { it.file("TEST-${this@MpsCheck.name}.xml") })
        junitFormat.convention("module-and-model")
        parallel.convention(false)
    }

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

        argumentProviders.add(CommandLineArgumentProvider {
            val result = mutableListOf<String>()

            result.add("--project=${projectLocation.get().asFile}")

            addPluginRoots(result, pluginRoots)
            addFolderMacros(result, folderMacros)

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

            if (logLevel.get() <= LogLevel.INFO) {
                result.add("--log-level=${logLevel.get()}")
            }

            result
        })

        group = TaskGroups.VERIFICATION

        classpath(mpsAndPluginJars())
        classpath(project.configurations.named(BackendConfigurations.MODELCHECK_BACKEND_CONFIGURATION_NAME))
        classpath(additionalModelcheckBackendClasspath)

        mainClass.set("de.itemis.mps.gradle.modelcheck.MainKt")
    }

    override fun exec() {
        checkProjectLocation(projectLocation)
        super.exec()
    }

    private fun mpsAndPluginJars() = mpsHome.asFileTree.matching {
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
