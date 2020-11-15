package de.itemis.mps.gradle

import org.apache.log4j.Logger
import org.gradle.api.GradleException
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.tasks.Copy
import org.w3c.dom.Document
import org.w3c.dom.Node
import java.io.File
import java.util.zip.ZipFile
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.xpath.XPath
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathFactory

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
    var mpsPluginConfig: Configuration? = null
    var mpsLocation: File? = null
    var plugins: List<Plugin> = emptyList()
    var pluginLocation: File? = null
    var macros: List<Macro> = emptyList()
    var projectLocation: File? = null
    var debug = false
    var javaExec: File? = null
}

fun validateDefaultJvm() {
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

fun Project.initializeMpsPluginDependencies(extension: BasePluginExtensions) {
    val mpsLocation = extension.mpsLocation ?: File(buildDir, "mps")
    extension.pluginLocation = extension.pluginLocation ?: File(mpsLocation, "plugins")
    val pluginDeps = (extension.mpsPluginConfig?.dependencies?.asSequence() ?: emptySequence())
            .flatMap { extension.mpsPluginConfig!!.files(it).asSequence() }
            .map { toPlugin(it) }
            .asSequence()
    extension.plugins = sequenceOf(
            extension.plugins.asSequence(),
            pluginDeps
    ).flatten().toList()


}

fun Project.addCopyMpsPluginDepsTask(extension: BasePluginExtensions, dependencies: Any, pluginTail: String) =
        tasks.create("copyMpsPluginsFor$pluginTail", Copy::class.java) {
            dependsOn(dependencies)
            into(extension.pluginLocation!!)
            extension.mpsPluginConfig?.asFileTree?.forEach {
                from(zipTree(it))
            }
        }

private fun toPlugin(file: File): de.itemis.mps.gradle.Plugin {
    return ZipFile(file).use { zipFile ->
        zipFile.entries().asSequence()
                .first { it.name.endsWith("META-INF/plugin.xml") }
                .let { entry ->
                    val name = entry.name.substring(0, entry.name.indexOf('/'))
                    val xmlDoc: Document = DocumentBuilderFactory.newInstance()
                            .newDocumentBuilder()
                            .parse(zipFile.getInputStream(entry))
                    xmlDoc.documentElement.normalize()
                    val xPath: XPath = XPathFactory.newInstance().newXPath()
                    val evaluate: Node =
                            xPath.compile("/idea-plugin/id")
                                    .evaluate(xmlDoc, XPathConstants.NODE) as Node
                    de.itemis.mps.gradle.Plugin(evaluate.textContent, name)
                }
    }
}
