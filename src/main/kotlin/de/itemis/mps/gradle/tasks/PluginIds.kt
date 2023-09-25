package de.itemis.mps.gradle.tasks

import de.itemis.mps.gradle.Plugin
import org.slf4j.LoggerFactory
import org.w3c.dom.Document
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import java.util.jar.JarFile
import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory

internal val logger = LoggerFactory.getLogger("de.itemis.mps.gradle.tasks.PluginIds")!!

internal fun findPluginsRecursively(root: File): List<Plugin> = mutableListOf<Plugin>().apply {
    Files.walkFileTree(root.toPath(), object : SimpleFileVisitor<Path>() {
        override fun preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult {
            val dirAsFile = dir.toFile()
            val id = readPluginId(dirAsFile)
            if (id != null) {
                this@apply.add(Plugin(id, dir.toString()))
                return FileVisitResult.SKIP_SUBTREE
            }
            return FileVisitResult.CONTINUE
        }
    })
}.toList()

internal fun readPluginId(pluginDirectory: File): String? {
    logger.debug("Reading plugin ID for {}", pluginDirectory)
    val pluginXml = findPluginDescriptor(pluginDirectory) ?: return null
    val ids = pluginXml.documentElement.getElementsByTagName("id")
    if (ids.length != 1) {
        logger.debug("Expected a single 'id' element, found {}", ids.length)
        return null
    }

    val result = ids.item(0).textContent
    logger.debug("Found ID: {}", result)
    return result.ifBlank { null }
}

private fun findPluginDescriptor(pluginDirectory: File): Document? {
    logger.debug("Looking for plugin descriptor in {}", pluginDirectory)
    val libDir = pluginDirectory.resolve("lib")

    if (libDir.isDirectory) {
        val jarsInLib = libDir.listFiles { file -> file.name.endsWith(".jar") && file.isFile }

        if (jarsInLib != null) {
            for (jar in jarsInLib) {
                val descriptor = readDescriptorFromJarFile(jar)
                if (descriptor != null) {
                    logger.debug("Found plugin descriptor inside {}", jar)
                    return descriptor
                }
            }
        }
    }

    val pluginXmlFile = pluginDirectory.resolve("META-INF/plugin.xml")
    if (pluginXmlFile.isFile) {
        logger.debug("Found plugin descriptor in {}", pluginXmlFile)
        return readXmlFile(pluginXmlFile)
    }

    logger.debug("Plugin descriptor not found in {}", pluginDirectory)
    return null
}

private fun readDescriptorFromJarFile(file: File): Document? {
    try {
        JarFile(file).use { jarFile ->
            val jarEntry = jarFile.getJarEntry("META-INF/plugin.xml") ?: return null
            jarFile.getInputStream(jarEntry).use {
                return readXmlFile(it, "${file}!${jarEntry.name}")
            }
        }
    } catch (ex: IOException) {
        logger.warn("Error reading JAR file $file", ex)
        return null
    }
}

private fun readXmlFile(file: File): Document? {
    return try {
        newDocumentBuilder().parse(file)
    } catch (e: Exception) {
        logger.warn("Error reading $file", e)
        null
    }
}

private fun readXmlFile(stream: InputStream, name: String): Document? {
    return try {
        newDocumentBuilder().parse(stream, name)
    } catch (e: Exception) {
        logger.warn("Error reading $name", e)
        null
    }
}

private fun newDocumentBuilder(): DocumentBuilder {
    val dbf = DocumentBuilderFactory.newInstance()
    disableDTD(dbf)
    return dbf.newDocumentBuilder()
}

private fun disableDTD(dbf: DocumentBuilderFactory) {
    dbf.isValidating = false;
    dbf.isNamespaceAware = true;
    dbf.setFeature("http://xml.org/sax/features/namespaces", false);
    dbf.setFeature("http://xml.org/sax/features/validation", false);
    dbf.setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false);
    dbf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
}
