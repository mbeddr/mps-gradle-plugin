package de.itemis.mps.gradle

import de.itemis.mps.gradle.generate.GeneratePluginExtensions
import org.apache.log4j.Logger
import org.gradle.api.GradleException
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskContainer
import java.util.Collections.emptyList

import java.io.File
import javax.inject.Inject

private val logger = Logger.getLogger("de.itemis.mps.gradle.common")

const val MPS_SUPPORT_MSG =
    "Version 1.5 doesn't only support MPS 2020.1+, please use versions 1.4 or below with older versions of MPS."

data class Plugin(
    val id: String,
    val path: String
)

data class Macro(
    val name: String,
    val value: String
)

open class BasePluginExtensions @Inject constructor(of: ObjectFactory, project: Project) {

    val mpsConfig: Property<Configuration> = of.property(Configuration::class.java)

    val mpsLocation: RegularFileProperty = of.fileProperty().convention { File(project.buildDir, "mps") }

    val mpsVersion: Property<String> = of.property(String::class.java)

    val plugins: ListProperty<Plugin> = of.listProperty(Plugin::class.java).convention(emptyList())

    val pluginLocation: RegularFileProperty = of.fileProperty()

    val macros: ListProperty<Macro> = of.listProperty(Macro::class.java).convention(emptyList())

    val projectLocation: RegularFileProperty = of.fileProperty()

    val debug: Property<Boolean> = of.property(Boolean::class.java).convention(false)

    val javaExec: RegularFileProperty = of.fileProperty()
}

fun validateDefaultJvm() {
    if (JavaVersion.current() != JavaVersion.VERSION_11) logger.error("MPS requires Java 11 but current JVM uses ${JavaVersion.current()}, starting MPS will most probably fail!")
}

fun argsFromBaseExtension(extensions: BasePluginExtensions): MutableList<String> {
    val pluginLocation =
        extensions.pluginLocation.map { sequenceOf("--plugin-location=${it.asFile.absolutePath}") }.getOrElse(
            emptySequence()
        )

    val projectLocation = extensions.projectLocation.getOrElse { throw GradleException("No project path set") }.asFile
    val prj = sequenceOf("--project=${projectLocation.absolutePath}")

    return sequenceOf(
        pluginLocation,
        extensions.plugins.get().map { "--plugin=${it.id}::${it.path}" }.asSequence(),
        extensions.macros.get().map { "--macro=${it.name}::${it.value}" }.asSequence(),
        prj
    ).flatten().toMutableList()
}

fun BasePluginExtensions.getMPSVersion(): String {
    /*
    If the user supplies a MPS config we use this one to resolve MPS and get the version. For other scenarios the user
    can supply mpsLocation and mpsVersion then we do not resolve anything and the users build script is responsible for
    resolving a compatible MPS into th mpsLocation before the
     */
    if (mpsConfig.isPresent) {
        return mpsConfig
            .get()
            .resolvedConfiguration
            .firstLevelModuleDependencies.find { it.moduleGroup == "com.jetbrains" && it.moduleName == "mps" }
            ?.moduleVersion ?: throw GradleException("MPS configuration doesn't contain MPS")
    }

    if (mpsVersion.isPresent) {
        if (!mpsLocation.isPresent) {
            throw GradleException("Setting an MPS version but no MPS location is not supported!")
        }
        return mpsVersion.get()
    }

    throw GradleException("Either mpsConfig or mpsVersion needs to specified!")

}