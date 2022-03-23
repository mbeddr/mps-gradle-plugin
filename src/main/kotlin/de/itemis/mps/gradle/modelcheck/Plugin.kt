package de.itemis.mps.gradle.modelcheck

import de.itemis.mps.gradle.*
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.JavaExec
import java.io.File
import javax.inject.Inject

open class ModelCheckPluginExtensions @Inject constructor(of: ObjectFactory, project: Project) : BasePluginExtensions(of, project) {
    val models: ListProperty<String> = of.listProperty(String::class.java).convention(emptyList())

    val modules: ListProperty<String> = of.listProperty(String::class.java).convention(emptyList())

    val warningAsError: Property<Boolean> = of.property(Boolean::class.java).convention(false)

    val errorNoFail: Property<Boolean> = of.property(Boolean::class.java).convention(false)

    val junitFile: RegularFileProperty = of.fileProperty()

    val junitFormat: Property<String> = of.property(String::class.java)

    val maxHeap: Property<String> = of.property(String::class.java)
}

open class ModelcheckMpsProjectPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.run {
            val extension = extensions.create("modelcheck", ModelCheckPluginExtensions::class.java)
            afterEvaluate {
                val mpsLocation = extension.mpsLocation.convention{ File(project.buildDir, "mps") }.map { it.asFile }.get()

                val mpsVersion = extension.getMPSVersion()
                // this dependency will never resolve against SNAPSHOT version, if there is a previously released version for $mpsVersion
                // hence if testing SNAPSHOT version locally, replace '+' with '.[pluginVersion]-SNAPSHOT', e.g. '.2-SNAPSHOT'
                // to make sure that your local SNAPSHOT version will be resolved here
                val dep = project.dependencies.create("de.itemis.mps:modelcheck:$mpsVersion+")
                val genConfig = configurations.detachedConfiguration(dep)

                if (mpsVersion.substring(0..3).toInt() < 2020) {
                    throw GradleException(MPS_SUPPORT_MSG)
                }


                val args = argsFromBaseExtension(extension)

                args.addAll(extension.models.get().map { "--model=$it" }.asSequence())
                args.addAll(extension.modules.get().map { "--module=$it" }.asSequence())

                if (extension.warningAsError.get()) {
                    args.add("--warning-as-error")
                }

                if (extension.errorNoFail.get()) {
                    args.add("--error-no-fail")
                }

                if (extension.junitFile.isPresent) {
                    args.add("--result-file=${extension.junitFile.get().getAsFile().absolutePath}")
                }

                if (extension.junitFormat.isPresent) {
                    args.add("--result-format=${extension.junitFormat.get()}")
                }


                val resolveMps: Task = if (extension.mpsConfig.isPresent) {
                    tasks.create("resolveMpsForModelcheck", Copy::class.java) {
                        from({extension.mpsConfig.get().resolve().map { zipTree(it) }})
                        into(mpsLocation)
                    }
                } else {
                    tasks.create("resolveMpsForModelcheck")
                }


                tasks.create("checkmodels", JavaExec::class.java) {
                    dependsOn(resolveMps)
                    args(args)
                    if (extension.javaExec.isPresent)
                        executable(extension.javaExec.get())
                    else
                        validateDefaultJvm()

                    group = "test"
                    description = "Check models in the project"
                    if (extension.maxHeap.isPresent) {
                        maxHeapSize = extension.maxHeap.get()
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
                    debug = extension.debug.get()
                    mainClass.set("de.itemis.mps.gradle.modelcheck.MainKt")
                }
            }

        }
    }
}
