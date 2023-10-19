package de.itemis.mps.gradle.modelcheck

import de.itemis.mps.gradle.*
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.JavaExec
import org.gradle.process.CommandLineArgumentProvider
import java.io.File

open class ModelCheckPluginExtensions(objectFactory: ObjectFactory) : BasePluginExtensions(objectFactory) {
    var models: List<String> = emptyList()
    var modules: List<String> = emptyList()
    var excludeModels: List<String> = emptyList()
    var excludeModules: List<String> = emptyList()
    var warningAsError = false
    var errorNoFail = false
    var junitFile: File? = null
    var junitFormat: String? = null
    var junitReportAllErrors: Boolean = false
}

open class ModelcheckMpsProjectPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.run {
            val extension = extensions.create("modelcheck", ModelCheckPluginExtensions::class.java)
            val checkmodels = tasks.register("checkmodels", JavaExec::class.java)

            afterEvaluate {
                val mpsLocation = extension.mpsLocation ?: File(project.buildDir, "mps")

                val mpsVersion = extension.getMPSVersion()

                val genConfig = extension.backendConfig ?: createDetachedBackendConfig(project)

                if(mpsVersion.substring(0..3).toInt() < 2020) {
                    throw GradleException(MPS_SUPPORT_MSG)
                }

                val resolveMps = if (extension.mpsConfig != null) {
                    tasks.register("resolveMpsForModelcheck", Copy::class.java) {
                        from({ extension.mpsConfig!!.resolve().map(::zipTree) })
                        into(mpsLocation)
                    }
                } else {
                    tasks.register("resolveMpsForModelcheck")
                }

                checkmodels.configure {
                    dependsOn(resolveMps)

                    argumentProviders.add(argsFromBaseExtension(extension))
                    argumentProviders.add(CommandLineArgumentProvider {
                        val args = mutableListOf<String>()
                        args.addAll(extension.models.map { "--model=$it" })
                        args.addAll(extension.modules.map { "--module=$it" })
                        args.addAll(extension.excludeModels.map { "--exclude-model=$it" })
                        args.addAll(extension.excludeModules.map { "--exclude-module=$it" })

                        if (extension.warningAsError) {
                            args.add("--warning-as-error")
                        }

                        if (extension.errorNoFail) {
                            args.add("--error-no-fail")
                        }

                        if (extension.junitFile != null) {
                            args.add("--result-file=${extension.junitFile!!.absolutePath}")
                        }

                        if (extension.junitFormat != null) {
                            args.add("--result-format=${extension.junitFormat}")
                        }

                        if (extension.junitReportAllErrors) {
                            args.add("--result-all-errors")
                        }
                        args
                    })

                    if (extension.javaExec != null)
                        executable(extension.javaExec!!)
                    else
                        validateDefaultJvm()

                    group = "test"
                    description = "Check models in the project"
                    if (extension.maxHeap != null) {
                        maxHeapSize = extension.maxHeap!!
                    }
                    classpath(fileTree(File(mpsLocation, "/lib")).include("**/*.jar"))
                    // add only minimal number of plugins jars that are required by the modelcheck code
                    // (to avoid conflicts with plugin classloader if custom configured plugins are loaded)
                    // mps-httpsupport: we need it to print the node url to the console.
                    // mps-modelchecker: contains used UnresolvedReferencesChecker
                    // git4idea: has to be on classpath as bundled plugin to be loaded (since 2019.3)
                    classpath(
                        fileTree(File(mpsLocation, "/plugins")).include(
                            "mps-modelchecker/**/*.jar",
                            "mps-httpsupport/**/*.jar",
                            "git4idea/**/*.jar"
                        )
                    )
                    classpath(genConfig)
                    debug = extension.debug
                    mainClass.set("de.itemis.mps.gradle.modelcheck.MainKt")
                }
            }

        }
    }

    private fun createDetachedBackendConfig(project: Project): Configuration {
        val dep = project.dependencies.create("de.itemis.mps.build-backends:modelcheck:${MPS_BUILD_BACKENDS_VERSION}")
        val genConfig = project.configurations.detachedConfiguration(dep)
        return genConfig
    }

}
