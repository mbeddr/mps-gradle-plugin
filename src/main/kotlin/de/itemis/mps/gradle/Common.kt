package de.itemis.mps.gradle

import org.apache.log4j.Logger
import org.gradle.api.GradleException
import org.gradle.api.JavaVersion
import org.gradle.api.artifacts.Configuration
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.kotlin.dsl.property
import org.gradle.process.CommandLineArgumentProvider
import java.io.File
import javax.inject.Inject

private val logger = Logger.getLogger("de.itemis.mps.gradle.common")

const val MPS_SUPPORT_MSG = "Version 1.8 doesn't only support MPS 2020.1+, please use versions 1.4 or below with older versions of MPS."

const val MPS_BUILD_BACKENDS_VERSION = "[1.6,2.0)"

data class Plugin(
        var id: String,
        var path: String
)

data class Macro(
        var name: String,
        var value: String
)

enum class EnvironmentKind { MPS, IDEA }

open class BasePluginExtensions @Inject constructor(objectFactory: ObjectFactory) {
    var mpsConfig: Configuration? = null
    var mpsLocation: File? = null
    var mpsVersion: String? = null

    /**
     * The plugins to load. Backed by [pluginsProperty] which should be used instead of this property.
     */
    @Deprecated("Use pluginsProperty")
    var plugins: List<Plugin>
        get() = pluginsProperty.get()
        set(value) { pluginsProperty.value(value) }

    /**
     * The plugins to load.
     */
    val pluginsProperty: ListProperty<Plugin> = objectFactory.listProperty(Plugin::class.java)

    var pluginLocation: File? = null
    var macros: List<Macro> = emptyList()
    var projectLocation: File? = null
    var debug = false
    var javaExec: File? = null
    var backendConfig: Configuration? = null

    /**
     * The environment to set up, IDEA or MPS. Default is IDEA for backwards compatibility reasons.
     */
    val environmentKind = objectFactory.property(EnvironmentKind::class).convention(EnvironmentKind.IDEA)

    /**
     * Maximum heap size, passed as the argument to the `-Xmx` JVM option. Example: `4G`, `512m`.
     */
    var maxHeap: String? = null
}

fun validateDefaultJvm(){
    if (JavaVersion.current() != JavaVersion.VERSION_11) logger.error("MPS requires Java 11 but current JVM uses ${JavaVersion.current()}, starting MPS will most probably fail!")
}

fun argsFromBaseExtension(extensions: BasePluginExtensions): CommandLineArgumentProvider =
    CommandLineArgumentProvider {
        val result = mutableListOf<String>()

        if (extensions.pluginLocation != null) {
            result.add("--plugin-location=${extensions.pluginLocation!!.absolutePath}")
        }

        val projectLocation = extensions.projectLocation ?: throw GradleException("No project path set")
        result.add("--project=${projectLocation.absolutePath}")

        extensions.pluginsProperty.get().mapTo(result) { "--plugin=${it.id}::${it.path}" }
        extensions.macros.mapTo(result) { "--macro=${it.name}::${it.value}" }

        // --environment is supported by backend 1.2 and above
        result.add("--environment=${extensions.environmentKind.get().name}")

        result
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