package de.itemis.mps.gradle.generate

import de.itemis.mps.gradle.*
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.JavaExec
import org.gradle.kotlin.dsl.support.zipTo
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipInputStream


open class GeneratePluginExtensions: BasePluginExtensions() {
    var models: List<String> = emptyList()
}

open class GenerateMpsProjectPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        project.run {
            val extension = extensions.create("generate", GeneratePluginExtensions::class.java)

            afterEvaluate {
                val mpsLocation = extension.mpsLocation ?: File(project.buildDir, "mps")
                val mpsVersion = extension.getMPSVersion()

                val dep = project.dependencies.create("de.itemis.mps:execute-generators:$mpsVersion+")
                val genConfig = configurations.detachedConfiguration(dep)

                if(mpsVersion.substring(0..3).toInt() < 2020) {
                    throw GradleException(MPS_SUPPORT_MSG)
                }

                val args = argsFromBaseExtension(extension)
                args.addAll(extension.models.map { "--model=$it" }.asSequence())

                val resolveMps: Task = if(extension.mpsConfig != null) {
                    tasks.create("resolveMpsForGeneration", Copy::class.java) {
                        from({extension.mpsConfig!!.resolve().map { zipTree(it) }})
                        into(mpsLocation)
                    }
                } else {
                    tasks.create("resolveMpsForGeneration")
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

                tasks.create("generate", JavaExec::class.java) {
                    dependsOn(fake)
                    args(args)
                    if (extension.javaExec != null)
                        executable(extension.javaExec!!)
                    else
                        validateDefaultJvm()
                    group = "build"
                    description = "Generates models in the project"
                    classpath(fileTree(File(mpsLocation, "/lib")).include("**/*.jar"))
                    // add only minimal number of plugins jars that are required by the generate code
                    // (to avoid conflicts with plugin classloader if custom configured plugins are loaded)
                    // git4idea: has to be on classpath as bundled plugin to be loaded (since 2019.3)
                    classpath(fileTree(File(mpsLocation, "/plugins")).include("git4idea/**/*.jar"))
                    classpath(genConfig)
                    debug = extension.debug
                    mainClass.set("de.itemis.mps.gradle.generate.MainKt")
                }
            }
        }
    }
}