package de.itemis.mps.gradle.generate

import de.itemis.mps.gradle.*
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.JavaExec
import org.gradle.internal.file.impl.DefaultFileMetadata.file
import org.gradle.kotlin.dsl.support.zipTo
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipInputStream
import javax.inject.Inject


open class GeneratePluginExtensions @Inject constructor(of: ObjectFactory, project: Project) : BasePluginExtensions(of, project) {
    val models: ListProperty<String> = of.listProperty(String::class.java).convention(emptyList())
}

open class GenerateMpsProjectPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.run {
            val extension = extensions.create("generate", GeneratePluginExtensions::class.java)

            afterEvaluate {
                val mpsLocation = extension.mpsLocation.map { it.asFile }.get()
                val mpsVersion = extension.getMPSVersion()

                val dep = project.dependencies.create("de.itemis.mps:execute-generators:$mpsVersion+")
                val genConfig = configurations.detachedConfiguration(dep)

                if (mpsVersion.substring(0..3).toInt() < 2020) {
                    throw GradleException(MPS_SUPPORT_MSG)
                }

                val args = argsFromBaseExtension(extension)
                args.addAll(extension.models.get().map { "--model=$it" }.asSequence())

                val resolveMps: Task = if (extension.mpsConfig.isPresent) {
                    tasks.create("resolveMpsForGeneration", Copy::class.java) {
                        from({ extension.mpsConfig.get().resolve().map { zipTree(it) } })
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
                    mpsDir.set(mpsLocation)
                    dependsOn(resolveMps)
                }

                tasks.create("generate", JavaExec::class.java) {
                    dependsOn(fake)
                    args(args)
                    if (extension.javaExec.isPresent)
                        executable(extension.javaExec.get())
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
                    debug = extension.debug.get()
                    mainClass.set("de.itemis.mps.gradle.generate.MainKt")
                }
            }
        }
    }
}