package de.itemis.mps.gradle.generate

import de.itemis.mps.gradle.BasePluginExtensions
import de.itemis.mps.gradle.argsFromBaseExtension
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
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
            val mpsLocation = extension.mpsLocation ?: File(project.buildDir, "mps")

            afterEvaluate {
                val mpsVersion = extension
                        .mpsConfig
                        .resolvedConfiguration
                        .firstLevelModuleDependencies.find { it.moduleGroup == "com.jetbrains" && it.moduleName == "mps" }
                        ?.moduleVersion ?: throw GradleException("MPS configuration doesn't contain MPS")

                val dep = project.dependencies.create("de.itemis.mps:execute-generators:$mpsVersion+")
                val genConfig = configurations.detachedConfiguration(dep)


                val args = argsFromBaseExtension(extension)
                args.addAll(extension.models.map { "--model=$it" }.asSequence())

                val resolveMps = tasks.create("resolveMpsForGeneration", Copy::class.java) {
                    from(extension.mpsConfig.resolve().map { zipTree(it) })
                    /*
                    Some 2019.2 src jars cause MPS startup to fail with an exception during indexing
                    because of the wrong specification in module.xml -> we remove those broken parts as a workaround.
                    In MPS 2019.3 those src jars are already removed.
                     */
                    val modelsTagRegEx = Regex("<models>.*</models>", RegexOption.DOT_MATCHES_ALL)
                    val replaceTag = "<models/>"
                    filesMatching(listOf("**/runtimes/*.feedback.*-src.jar", "**/runtimes/*.messages.api-src.jar")) {
                        ZipInputStream(open()).use { zis ->
                            val resultEntries = ArrayList<Pair<String, ByteArray>>()
                            do {
                                val nextEntry = zis.nextEntry ?: break
                                val outputStream = ByteArrayOutputStream()
                                zis.copyTo(outputStream)
                                var byteArray : ByteArray
                                if (nextEntry.name.contains(".msd")) {
                                    byteArray = outputStream.toString().replace(modelsTagRegEx, replaceTag).toByteArray()
                                } else {
                                    byteArray = outputStream.toByteArray()
                                }
                                resultEntries.add(Pair(nextEntry.name, byteArray))
                            } while (true)
                            zipTo(File(mpsLocation, this.path), resultEntries.asSequence())
                        }
                        exclude()
                    }
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

                tasks.create("generate", JavaExec::class.java) {
                    dependsOn(fake)
                    args(args)
                    group = "build"
                    description = "Generates models in the project"
                    classpath(fileTree(File(mpsLocation, "/lib")).include("**/*.jar"))
                    // add only minimal number of plugins jars that are required by the generate code
                    // (to avoid conflicts with plugin classloader if custom configured plugins are loaded)
                    //classpath(fileTree(File(mpsLocation, "/plugins")).include("<plugin-folder>/**/*.jar"))
                    classpath(genConfig)
                    debug = extension.debug
                    main = "de.itemis.mps.gradle.generate.MainKt"
                }
            }
        }
    }
}