package de.itemis.mps.gradle.modelcheck

import de.itemis.mps.gradle.BasePluginExtensions
import de.itemis.mps.gradle.argsFromBaseExtension
import de.itemis.mps.gradle.validateDefaultJvm
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.JavaExec
import org.gradle.kotlin.dsl.support.zipTo
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipInputStream

open class ModelCheckPluginExtensions: BasePluginExtensions() {
    var models: List<String> = emptyList()
    var modules: List<String> = emptyList()
    var warningAsError = false
    var errorNoFail = false
    var junitFile: File? = null
    var junitFormat: String? = null
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

                if (extension.junitFormat != null) {
                    args.add("--result-format=${extension.junitFormat}")
                }

                val resolveMps = tasks.create("resolveMpsForModelcheck", Copy::class.java) {
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
                tasks.create("checkmodels", JavaExec::class.java) {
                    dependsOn(resolveMps)
                    args(args)
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
                    classpath(fileTree(File(mpsLocation, "/plugins")).include("mps-modelchecker/**/*.jar", "mps-httpsupport/**/*.jar", "git4idea/**/*.jar"))
                    classpath(genConfig)
                    debug = extension.debug
                    main = "de.itemis.mps.gradle.modelcheck.MainKt"
                }
            }

        }
    }
}
