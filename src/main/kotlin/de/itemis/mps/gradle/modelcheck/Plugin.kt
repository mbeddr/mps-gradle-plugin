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
    var warningsAsError = false
    var errorNoFail = false
}

open class ModelcheckMpsProjectPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.run {
            val extension = extensions.create("modelcheck", ModelCheckPluginExtensions::class.java)
            val mpsLocation = extension.mpsLocation ?: File(project.buildDir, "mps")

            afterEvaluate {
                val mpsVersion = extension
                        .mpsConfig
                        .resolvedConfiguration
                        .firstLevelModuleDependencies.find { it.moduleGroup == "com.jetbrains" && it.moduleName == "mps" }
                        ?.moduleVersion ?: throw GradleException("MPS configuration doesn't contain MPS")

                val dep = project.dependencies.create("de.itemis.mps:modelcheck:$mpsVersion+")
                val genConfig = configurations.detachedConfiguration(dep)

                val args = argsFromBaseExtension(extension)

                args.addAll(extension.models.map { "--model=$it" }.asSequence())

                if(extension.warningsAsError) {
                    args.add("--warning-as-error")
                }

                if (extension.errorNoFail) {
                    args.add("--error-no-fail")
                }

                val resolveMps = tasks.create("resolveMpsForModelcheck", Copy::class.java) {
                    from(extension.mpsConfig.resolve().map { zipTree(it) })
                    into(mpsLocation)
                }
                val modelcheck = tasks.create("checkmodels", JavaExec::class.java) {
                    dependsOn(resolveMps)
                    args(args)
                    group = "test"
                    description = "Check models in the project"
                    classpath(fileTree(File(mpsLocation, "/lib")).include("**/*.jar"))
                    // http support doesn't follow the MPS convention to with a lib folder and we need it to print the
                    // node url to the console.
                    classpath(fileTree(File(mpsLocation, "/plugins")).include("**/lib/**/*.jar","http-support/**/*.jar"))
                    classpath(file(File(mpsLocation, "/plugins/modelchecker.jar")))
                    classpath(genConfig)
                    debug = extension.debug
                    main = "de.itemis.mps.gradle.modelcheck.MainKt"
                }
            }

        }
    }
}