package de.itemis.mps.gradle.modelcheck

import de.itemis.mps.gradle.BasePluginExtensions
import de.itemis.mps.gradle.argsFromBaseExtension
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.JavaExec
import java.io.File

open class ModelCheckPluginExtensions: BasePluginExtensions() {
    var models: List<String> = emptyList()
    var modules: List<String> = emptyList()
    var warningAsError = false
    var errorNoFail = false
    var junitFile: File? = null
    var maxHeap: String? = null
}

open class ModelcheckMpsProjectPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.run {
            val extension = extensions.create("modelcheck", ModelCheckPluginExtensions::class.java)

            afterEvaluate {
                val mpsLocation = extension.mpsLocation ?: File(project.buildDir, "mps")

                val mpsVersion = extension
                        .mpsConfig
                        .resolvedConfiguration
                        .firstLevelModuleDependencies.find { it.moduleGroup == "com.jetbrains" && it.moduleName == "mps" }
                        ?.moduleVersion ?: throw GradleException("MPS configuration doesn't contain MPS")

                // this dependency will never resolve against SNAPSHOT version, if there is a previously released version for $mpsVersion
                // hence if testing SNAPSHOT version locally, replace '+' with '.[pluginVersion]-SNAPSHOT', e.g. '.2-SNAPSHOT'
                // to make sure that your local SNAPSHOT version will be resolved here
                val dep = project.dependencies.create("de.itemis.mps:modelcheck:$mpsVersion+")
                val genConfig = configurations.detachedConfiguration(dep)

                val args = argsFromBaseExtension(extension)

                args.addAll(extension.models.map { "--model=$it" }.asSequence())
                args.addAll(extension.modules.map { "--module=$it" }.asSequence())

                if(extension.warningAsError) {
                    args.add("--warning-as-error")
                }

                if (extension.errorNoFail) {
                    args.add("--error-no-fail")
                }

                if (extension.junitFile != null) {
                    args.add("--result-file=${extension.junitFile!!.absolutePath}")
                }

                val resolveMps = tasks.create("resolveMpsForModelcheck", Copy::class.java) {
                    from(extension.mpsConfig.resolve().map { zipTree(it) })
                    into(mpsLocation)
                }
                tasks.create("checkmodels", JavaExec::class.java) {
                    dependsOn(resolveMps)
                    args(args)
                    group = "test"
                    description = "Check models in the project"
                    if (extension.maxHeap != null) {
                        maxHeapSize = extension.maxHeap!!
                    }
                    classpath(fileTree(File(mpsLocation, "/lib")).include("**/*.jar"))
                    // http support runtime is not located in lib folder, as all other plugin jars,
                    // we need it to print the node url to the console.
                    classpath(fileTree(File(mpsLocation, "/plugins")).include("**/lib/**/*.jar", "mps-httpsupport/**/jetbrains.mps.ide.httpsupport.runtime.jar"))
                    classpath(genConfig)
                    debug = extension.debug
                    main = "de.itemis.mps.gradle.modelcheck.MainKt"
                }
            }

        }
    }
}
