package de.itemis.mps.gradle.generate

import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.JavaExec
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
    var projectLocation: File? = null
    var debug = false
}

open class GenerateMpsProjectPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        project.run {

            val extension = extensions.create("generate", GeneratePluginExtensions::class.java)
            val mpsLocation = extension.mpsLocation ?: File(project.buildDir, "mps")

            afterEvaluate {
                val mpsVersion = extension
                        .mpsConfig
                        .resolvedConfiguration
                        .firstLevelModuleDependencies.find { it.moduleGroup == "com.jetbrains" && it.moduleName == "mps" }
                        ?.moduleVersion ?: throw GradleException("MPS configuration doesn't contain MPS")

                val dep = project.dependencies.create("de.itemis.mps:execute-generators:$mpsVersion+")
                val genConfig = configurations.detachedConfiguration(dep)


                val pluginLocation = if (extension.pluginLocation != null) {
                    sequenceOf("--plugin-location=${extension.pluginLocation!!.absolutePath}")
                } else {
                    emptySequence()
                }


                val projectLocation = extension.projectLocation ?: throw GradleException("No project path set")
                val prj = sequenceOf("--project=${projectLocation.absolutePath}")

                val args = sequenceOf(pluginLocation,
                        extension.plugins.map { "--plugin=${it.id}:${it.path}" }.asSequence(),
                        extension.models.map { "--model=$it" }.asSequence(),
                        extension.macros.map { "--macro=${it.name}:${it.value}" }.asSequence(),
                        prj).flatten().toList()

                val resolveMps = tasks.create("resolveMpsForGeneration", Copy::class.java) {
                    from(extension.mpsConfig.resolve().map { zipTree(it) })
                    into(mpsLocation)
                }

                /*
                * The problem her is is that for some reason the ApplicationInfo isn't initialised properly.
                * That causes PluginManagerCore.BUILD_NUMBER to be null.
                * In this case the PluginManagerCore resorts to BuildNumber.currentVersion() which finally
                * calls into BuildNumber.fromFile().
                *
                * This behaviour allows us to place a build.txt in the root of the home path (see PathManager.getHomePath()).
                * The file is then used to load the build number.
                *
                * TODO: Since MPS 2018.2 a newer version of the platform allows to get a similar behaviour via setting idea.plugins.compatible.build property.
                *
                */
                val fake = tasks.create("fakeBuildNumber", FakeBuildNumberTask::class.java) {
                    mpsDir = mpsLocation
                    dependsOn(resolveMps)
                }

                val generate = tasks.create("generate", JavaExec::class.java) {
                    dependsOn(fake)
                    args(args)
                    group = "build"
                    description = "Generates models in the project"
                    classpath(fileTree(File(mpsLocation, "/lib")).include("**/*.jar"))
                    classpath(fileTree(File(mpsLocation, "/plugins")).include("**/lib/**/*.jar"))
                    classpath(file(File(mpsLocation, "/plugins/modelchecker.jar")))
                    classpath(genConfig)
                    debug = extension.debug
                    main = "de.itemis.mps.gradle.generate.MainKt"
                }
            }
        }
    }
}