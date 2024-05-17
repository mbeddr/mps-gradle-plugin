package de.itemis.mps.gradle.tasks

import de.itemis.mps.gradle.ErrorMessages
import org.gradle.api.GradleException
import org.gradle.api.Task
import org.gradle.api.file.Directory
import org.gradle.api.file.FileCollection
import org.gradle.api.file.FileSystemLocation
import org.gradle.api.logging.LogLevel
import org.gradle.api.provider.Provider
import java.io.File

internal fun Task.addLogLevel(args: MutableCollection<String>) = addIfInfoLogLevel(args, "--log-level=info")

/**
 * A weird overload, usable for building a map of Ant task attributes, as well as a list of command line backend
 * attributes.
 */
internal fun <T> Task.addIfInfoLogLevel(result: MutableCollection<T>, value: T) {
    val effectiveLogLevel = logging.level ?: project.logging.level ?: project.gradle.startParameter.logLevel
    if (effectiveLogLevel <= LogLevel.INFO) result.add(value)
}

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
