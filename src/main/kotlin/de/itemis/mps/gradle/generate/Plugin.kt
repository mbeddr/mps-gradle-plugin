package de.itemis.mps.gradle.generate

import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.JavaExec

import org.gradle.kotlin.dsl.*
import java.io.File


data class Plugin(
        var id: String,
        var path: String
)

data class Macro(
        var name: String,
        var value: String
)


open class GeneratePluginExtensions {
    lateinit var mpsConfig: Configuration
    var mpsLocation: File? = null
    var plugins: List<de.itemis.mps.gradle.generate.Plugin> = emptyList()
    var pluginLocation: File? = null
    var models: List<String> = emptyList()
    var macros: List<Macro> = emptyList()
    var projectLocation: File? = File("./mps-prj")
    var debug = false
}

open class GenerateMpsProjectPlugin : Plugin<Project> {

    override fun apply(project: Project) {

        project.run {

            val extension = extensions.create("generate", GeneratePluginExtensions::class.java)
            val mpsLocation = extension.mpsLocation ?: File(project.buildDir, "mps")

            val genConfig = configurations.maybeCreate("genRuntime")
            genConfig.isVisible = false

            afterEvaluate {
                val mpsVersion = extension
                        .mpsConfig
                        .resolvedConfiguration
                        .firstLevelModuleDependencies.find { it.moduleGroup == "com.jetbrains" && it.moduleName == "mps" }
                        ?.moduleVersion ?: throw GradleException("MPS configuration doesn't contain MPS")

                dependencies.add("genRuntime", "de.itemis.mps:execute-generators:$mpsVersion+")

                val pluginLocation = if (extension.pluginLocation != null) {
                    sequenceOf("--plugin-location=${extension.pluginLocation!!.absolutePath}")
                } else {
                    emptySequence()
                }


                val projectLocation = extension.projectLocation ?: throw GradleException("No project path set")
                val prj = sequenceOf(projectLocation.absolutePath)


                val args = sequenceOf(pluginLocation,
                        extension.plugins.map { "--plugin=${it.id}:${it.path}" }.asSequence(),
                        extension.models.map { "--model=$it" }.asSequence(),
                        extension.macros.map { "--macro=${it.name}:${it.value}" }.asSequence(),
                        prj).flatten().toList()

                val resolveMps = tasks.create("resolveMpsForGeneration", Copy::class.java) {
                    from(extension.mpsConfig.resolve().map { zipTree(it) })
                    into(mpsLocation)
                }

                val fake = tasks.create("fakeBuildNumber", FakeBuildNumberTask::class.java) {
                    mpsDir = mpsLocation
                    dependsOn(resolveMps)
                }

                val generate = tasks.create("generate", JavaExec::class.java) {
                    dependsOn(fake)
                    args(args)
                    classpath(fileTree(File(mpsLocation, "/lib")).include("**/*.jar"))
                    classpath(fileTree(File(mpsLocation, "/plugins")).include("**/lib/**/*.jar"))
                    classpath(file(File(mpsLocation, "/plugins/modelchecker.jar")))
                    classpath(genConfig)
                    debug = extension.debug
                    main = "de.itemis.mps.gradle.generate.ProjectKt"
                }
            }


        }
    }
}