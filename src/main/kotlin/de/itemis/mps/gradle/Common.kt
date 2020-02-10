package de.itemis.mps.gradle

import org.apache.log4j.Logger
import org.gradle.api.GradleException
import org.gradle.api.JavaVersion
import org.gradle.api.artifacts.Configuration
import java.io.File

private val logger = Logger.getLogger("de.itemis.mps.gradle.common")

data class Plugin(
        var id: String,
        var path: String
)

data class Macro(
        var name: String,
        var value: String
)

open class BasePluginExtensions {
    lateinit var mpsConfig: Configuration
    var mpsLocation: File? = null
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