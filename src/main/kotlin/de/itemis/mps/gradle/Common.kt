package de.itemis.mps.gradle

import net.swiftzer.semver.SemVer
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

const val MPS_SUPPORT_MSG = ErrorMessages.MPS_VERSION_NOT_SUPPORTED

const val MPS_BUILD_BACKENDS_VERSION = "[1.15,2.0)" // 1.15 required for --plugin-root support.

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
    if (JavaVersion.current() < JavaVersion.VERSION_11) logger.error("MPS requires at least Java 11 but current JVM uses ${JavaVersion.current()}, starting MPS will most probably fail!")
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

@Deprecated("Use getMPSVersion(extensionName)", replaceWith = ReplaceWith("getMPSVersion(this.javaClass.name)"))
fun BasePluginExtensions.getMPSVersion(): String = getMPSVersion(this.javaClass.name)

/**
 * [extensionName]: extension name, for diagnostics.
 */
fun BasePluginExtensions.getMPSVersion(extensionName: String): String {
    // If the user supplies explicit mpsVersion, we use it.
    if (mpsVersion != null) return mpsVersion!!

    val mpsConfig = mpsConfig
    if (mpsConfig != null) {
        // If the user supplies a configuration, we use it to detect MPS version.
        return mpsConfig.resolvedConfiguration.firstLevelModuleDependencies
            .find { it.moduleGroup == "com.jetbrains" && it.moduleName == "mps" }
            ?.moduleVersion
            ?: throw GradleException(ErrorMessages.couldNotDetermineMpsVersionFromConfiguration(mpsConfig)
        )
    }

    //  Otherwise, the version has to be provided explicitly.
    throw GradleException(ErrorMessages.mustSetVersionWhenNoMpsConfiguration(extensionName))
}

class MPSVersion private constructor(val version: SemVer) {
    
    companion object {
        
        fun parse(str: String):MPSVersion {
            var semVersion = SemVer.parseOrNull(str)
            if (semVersion == null) {
                val insertIndex = str.indexOf("-")
                val newStr =  if(insertIndex != -1) str.substring(0,insertIndex)+ ".0" + str.substring(insertIndex) else "$str.0"
                semVersion = SemVer.parse(newStr)
            }
            return MPSVersion(semVersion)
        }
    }

    private fun appendOpt(str: String?, pre: String): String {
        return if (!str.isNullOrEmpty()) "${pre}${str}" else ""
    }
    
    private fun substringBefore(text: String, substring: String):String? {
        val i: Int = text.indexOf(substring)
        if (i == -1) return null
        return text.substring(0, i)
    }

    override fun toString(): String {
        return version.toString()
    }
    
    fun toMavenSnapshot() = "${version.major}.${version.minor}-SNAPSHOT"
}
