package de.itemis.mps.gradle

import org.apache.log4j.Logger
import org.gradle.api.GradleException
import org.gradle.api.JavaVersion
import org.gradle.api.artifacts.Configuration
import org.gradle.initialization.GradleLauncher
import java.io.File

private val logger = Logger.getLogger("de.itemis.mps.gradle.common")

const val MPS_SUPPORT_MSG = "Version 1.5 doesn't only support MPS 2020.1+, please use versions 1.4 or below with older versions of MPS."

data class Plugin(
        var id: String,
        var path: String
)

data class Macro(
        var name: String,
        var value: String
)

open class BasePluginExtensions {
    var mpsConfig: Configuration? = null
    var mpsLocation: File? = null
    var mpsVersion: String? = null
    var plugins: List<Plugin> = emptyList()
    var pluginLocation: File? = null
    var macros: List<Macro> = emptyList()
    var projectLocation: File? = null
    var debug = false
    var javaExec: File? = null
}

fun validateDefaultJvm(){
    if (JavaVersion.current() != JavaVersion.VERSION_11) logger.error("MPS requires Java 11 but current JVM uses ${JavaVersion.current()}, starting MPS will most probably fail!")
}

fun argsFromBaseExtension(extensions: BasePluginExtensions): MutableList<String> {
    val pluginLocation = if (extensions.pluginLocation != null) {
        sequenceOf("--plugin-location=${extensions.pluginLocation!!.absolutePath}")
    } else {
        emptySequence()
    }


    val projectLocation = extensions.projectLocation ?: throw GradleException("No project path set")
    val prj = sequenceOf("--project=${projectLocation.absolutePath}")

    return sequenceOf(pluginLocation,
            extensions.plugins.map { "--plugin=${it.id}::${it.path}" }.asSequence(),
            extensions.macros.map { "--macro=${it.name}::${it.value}" }.asSequence(),
            prj).flatten().toMutableList()
}

fun BasePluginExtensions.getMPSVersion(): String {
    /*
    If the user supplies a MPS config we use this one to resolve MPS and get the version. For other scenarios the user
    can supply mpsLocation and mpsVersion then we do not resolve anything and the users build script is responsible for
    resolving a compatible MPS into th mpsLocation before the
     */
    if(mpsConfig != null) {
        return mpsConfig!!
            .resolvedConfiguration
            .firstLevelModuleDependencies.find { it.moduleGroup == "com.jetbrains" && it.moduleName == "mps" }
            ?.moduleVersion ?: throw GradleException("MPS configuration doesn't contain MPS")
    }

    if(mpsVersion != null) {
        if(mpsLocation == null) {
            throw GradleException("Setting an MPS version but no MPS location is not supported!")
        }
        return mpsVersion!!
    }

    throw GradleException("Either mpsConfig or mpsVersion needs to specified!")

}