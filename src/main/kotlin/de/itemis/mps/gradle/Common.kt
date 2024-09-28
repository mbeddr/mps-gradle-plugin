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

class MPSVersion(
    var major: String,
    var minor: String,
    var releaseType: String? = null) {
    
    companion object {
        val MPS_2017_1 =   MPSVersion("2017","1")
        val MPS_2017_1_1 = MPSVersion("2017","1.1")
        val MPS_2017_1_2 = MPSVersion("2017","1.2")
        val MPS_2017_1_3 = MPSVersion("2017","1.3")
        val MPS_2017_2 =   MPSVersion("2017","2")
        val MPS_2017_2_1 = MPSVersion("2017","2.1")
        val MPS_2017_2_2 = MPSVersion("2017","2.2")
        val MPS_2017_2_3 = MPSVersion("2017","2.3")
        val MPS_2017_3_ =  MPSVersion("2017","3")
        val MPS_2017_3_1 = MPSVersion("2017","3.1")
        val MPS_2017_3_2 = MPSVersion("2017","3.2")
        val MPS_2017_3_3 = MPSVersion("2017","3.3")
        val MPS_2017_3_4 = MPSVersion("2017","3.4")
        val MPS_2017_3_5 = MPSVersion("2017","3.5")
        val MPS_2017_3_6 = MPSVersion("2017","3.6")

        val MPS_2018_1_ =  MPSVersion("2018","1")
        val MPS_2018_1_1 = MPSVersion("2018","1.1")
        val MPS_2018_1_2 = MPSVersion("2018","1.2")
        val MPS_2018_1_3 = MPSVersion("2018","1.3")
        val MPS_2018_1_4 = MPSVersion("2018","1.4")
        val MPS_2018_1_5 = MPSVersion("2018","1.5")
        val MPS_2018_1_6 = MPSVersion("2018","1.6")
        val MPS_2018_2_ =  MPSVersion("2018","2")
        val MPS_2018_2_1 = MPSVersion("2018","2.1")
        val MPS_2018_2_2 = MPSVersion("2018","2.2")
        val MPS_2018_2_3 = MPSVersion("2018","2.3")
        val MPS_2018_2_4 = MPSVersion("2018","2.4")
        val MPS_2018_2_5 = MPSVersion("2018","2.5")
        val MPS_2018_2_6 = MPSVersion("2018","2.6")
        val MPS_2018_3_ =  MPSVersion("2018","3")
        val MPS_2018_3_1 = MPSVersion("2018","3.1")
        val MPS_2018_3_2 = MPSVersion("2018","3.2")
        val MPS_2018_3_3 = MPSVersion("2018","3.3")
        val MPS_2018_3_4 = MPSVersion("2018","3.4")
        val MPS_2018_3_5 = MPSVersion("2018","3.5")
        val MPS_2018_3_6 = MPSVersion("2018","3.6")
        val MPS_2018_3_7 = MPSVersion("2018","3.7")

        val MPS_2019_1_ =  MPSVersion("2019","1")
        val MPS_2019_1_1 = MPSVersion("2019","1.1")
        val MPS_2019_1_2 = MPSVersion("2019","1.2")
        val MPS_2019_1_3 = MPSVersion("2019","1.3")
        val MPS_2019_1_4 = MPSVersion("2019","1.4")
        val MPS_2019_1_5 = MPSVersion("2019","1.5")
        val MPS_2019_1_6 = MPSVersion("2019","1.6")
        val MPS_2019_2_ =  MPSVersion("2019","2")
        val MPS_2019_2_1 = MPSVersion("2019","2.1")
        val MPS_2019_2_2 = MPSVersion("2019","2.2")
        val MPS_2019_2_3 = MPSVersion("2019","2.3")
        val MPS_2019_2_4 = MPSVersion("2019","2.4")

        val MPS_2020_1 =   MPSVersion("2020","1")
        val MPS_2020_1_1 = MPSVersion("2020","1.1")
        val MPS_2020_1_2 = MPSVersion("2020","1.2")
        val MPS_2020_1_3 = MPSVersion("2020","1.3")
        val MPS_2020_1_4 = MPSVersion("2020","1.4")
        val MPS_2020_1_5 = MPSVersion("2020","1.5")
        val MPS_2020_1_6 = MPSVersion("2020","1.6")
        val MPS_2020_1_7 = MPSVersion("2020","1.7")
        val MPS_2020_2 =   MPSVersion("2020","2")
        val MPS_2020_2_1 = MPSVersion("2020","2.1")
        val MPS_2020_2_2 = MPSVersion("2020","2.2")
        val MPS_2020_2_3 = MPSVersion("2020","2.3")
        val MPS_2020_3 =   MPSVersion("2020","3")
        val MPS_2020_3_1 = MPSVersion("2020","3.1")
        val MPS_2020_3_2 = MPSVersion("2020","3.2")
        val MPS_2020_3_3 = MPSVersion("2020","3.3")
        val MPS_2020_3_4 = MPSVersion("2020","3.4")
        val MPS_2020_3_5 = MPSVersion("2020","3.5")
        val MPS_2020_3_6 = MPSVersion("2020","3.6")

        val MPS_2021_1 =   MPSVersion("2021","1")
        val MPS_2021_1_1 = MPSVersion("2021","1.1")
        val MPS_2021_1_2 = MPSVersion("2021","1.2")
        val MPS_2021_1_3 = MPSVersion("2021","1.3")
        val MPS_2021_1_4 = MPSVersion("2021","1.4")
        val MPS_2021_2 =   MPSVersion("2021","2")
        val MPS_2021_2_1 = MPSVersion("2021","2.1")
        val MPS_2021_2_2 = MPSVersion("2021","2.2")
        val MPS_2021_2_3 = MPSVersion("2021","2.3")
        val MPS_2021_2_4 = MPSVersion("2021","2.4")
        val MPS_2021_2_5 = MPSVersion("2021","2.5")
        val MPS_2021_2_6 = MPSVersion("2021","2.6")
        val MPS_2021_3 =   MPSVersion("2021","3")
        val MPS_2021_3_1 = MPSVersion("2021","3.1")
        val MPS_2021_3_2 = MPSVersion("2021","3.2")
        val MPS_2021_3_3 = MPSVersion("2021","3.3")
        val MPS_2021_3_4 = MPSVersion("2021","3.4")
        val MPS_2021_3_5 = MPSVersion("2021","3.5")

        val MPS_2022_2 =   MPSVersion("2022","2")
        val MPS_2022_2_1 = MPSVersion("2022","2.1")
        val MPS_2022_2_2 = MPSVersion("2022","2.2")
        val MPS_2022_2_3 = MPSVersion("2022","2.3")
        val MPS_2022_3 =   MPSVersion("2022","3")
        val MPS_2022_3_1 = MPSVersion("2022","3.1")
        val MPS_2022_3_2 = MPSVersion("2022","3.2")

        val MPS_2023_2 =   MPSVersion("2023","2")
        val MPS_2023_2_1 = MPSVersion("2023","2.1")
        val MPS_2023_2_2 = MPSVersion("2023","2.2")
        val MPS_2023_3 =   MPSVersion("2023","3")
        val MPS_2023_3_1 = MPSVersion("2023","3.1")

        val MPS_2024_1 =   MPSVersion("2024","1")
    }

    private fun appendOpt(str: String?, pre: String): String {
        return if (!str.isNullOrEmpty()) "${pre}${str}" else ""
    }
    
    private fun substringBefore(text: String, substring: String):String? {
        val i: Int = text.indexOf(substring)
        if (i == -1) return null
        return text.substring(0, i)
    }
    
    fun minorMainPart(): String {
        val value = substringBefore(minor, ".")
        return value ?: minor
    }

    override fun toString(): String {
        return major + appendOpt(minor, ".") + appendOpt(releaseType, "-")
    }
    
    fun toMavenSnapshot() = "$major.${minorMainPart()}-SNAPSHOT"
}
