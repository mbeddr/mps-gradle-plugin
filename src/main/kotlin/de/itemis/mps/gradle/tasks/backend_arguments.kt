package de.itemis.mps.gradle.tasks

import de.itemis.mps.gradle.ErrorMessages
import org.gradle.api.GradleException
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileCollection
import org.gradle.api.file.FileSystemLocation
import org.gradle.api.file.FileTree
import org.gradle.api.logging.LogLevel
import org.gradle.api.provider.Provider
import java.io.File

internal fun checkProjectLocation(projectLocation: Provider<out FileSystemLocation>) =
    checkProjectLocation(projectLocation.get().asFile)

internal fun checkProjectLocation(projectLocation: File) {
    if (!projectLocation.resolve(".mps").isDirectory) {
        throw GradleException(ErrorMessages.noMpsProjectIn(projectLocation))
    }
}

internal fun addPluginRoots(result: MutableCollection<String>, pluginRoots: FileCollection) {
    pluginRoots.mapTo(result) { "--plugin-root=$it" }
}

internal fun addPluginRoots(result: MutableCollection<String>, pluginRoots: Iterable<FileSystemLocation>) {
    pluginRoots.mapTo(result) { "--plugin-root=$it" }
}

internal fun addFolderMacros(result: MutableCollection<String>, folderMacros: Provider<Map<String, Directory>>) {
    folderMacros.get().mapTo(result) { "--macro=${it.key}::${it.value.asFile}" }
}

internal fun addVarMacros(result: MutableCollection<String>, varMacros: Provider<Map<String, String>>) {
    varMacros.get().mapTo(result) { "--macro=${it.key}::${it.value}" }
}

internal fun addLogLevel(result: MutableCollection<String>, logLevel: Provider<LogLevel>) {
    when (logLevel.get()) {
        LogLevel.INFO -> result.add("--log-level=INFO")
        LogLevel.DEBUG -> result.add("--log-level=ALL")
        else -> {}
    }
}

/**
 * Jars in the MPS home that must not be put on a flat JVM classpath.
 *
 * MPS 2026.3 (platform 263) ships the platform's shrunk copy of kotlin-metadata in `lib/` next to MPS's own
 * `kotlin-metadata-jvm-*-mps.jar`. With both on one classpath, loading Kotlin stub models fails with
 * `NoSuchMethodError: ExtensionNodesKt.getExtension(KmEnumEntry, KmExtensionType)`. MPS filters this jar out of the
 * classpaths it computes itself (MPS-40180). Older MPS versions do not have this jar in `lib/`.
 */
private val MPS_JARS_EXCLUDED_FROM_CLASSPATH = listOf("lib/intellij.libraries.kotlin.metadata.jar")

/**
 * Jars from [mpsHome] matching [includes], minus the jars that must not be on a flat JVM classpath.
 */
internal fun mpsClasspathJars(mpsHome: DirectoryProperty, vararg includes: String): FileTree =
    mpsHome.asFileTree.matching {
        include(*includes)
        exclude(MPS_JARS_EXCLUDED_FROM_CLASSPATH)
    }
