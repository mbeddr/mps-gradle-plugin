package de.itemis.mps.gradle.tasks

import de.itemis.mps.gradle.ErrorMessages
import org.gradle.api.GradleException
import org.gradle.api.Task
import org.gradle.api.file.Directory
import org.gradle.api.file.FileCollection
import org.gradle.api.file.FileSystemLocation
import org.gradle.api.logging.LogLevel
import org.gradle.api.logging.Logger
import org.gradle.api.logging.LoggingManager
import org.gradle.api.provider.Provider
import org.gradle.process.CommandLineArgumentProvider
import java.io.File


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

internal fun MpsTask.backendArguments(): CommandLineArgumentProvider = CommandLineArgumentProvider {
    val result = mutableListOf<String>()

    addLogLevel(result)
    addPluginRoots(result, pluginRoots)
    addFolderMacros(result, folderMacros)

    result
}

private fun Task.addLogLevel(args: MutableCollection<String>) = addIfInfoLogLevel(args, "--log-level=info")

private fun addPluginRoots(result: MutableCollection<String>, pluginRoots: FileCollection) {
    pluginRoots.mapTo(result) { "--plugin-root=$it" }
}

private fun addFolderMacros(result: MutableCollection<String>, folderMacros: Provider<Map<String, Directory>>) {
    folderMacros.get().mapTo(result) { "--macro=${it.key}::${it.value.asFile}" }
}

internal fun addVarMacros(result: MutableCollection<String>, varMacros: Provider<Map<String, String>>, task: Task, propertyName: String = "varMacros") {
    if (result.isNotEmpty()) {
        task.logger.error("Task '${task.path}' defines '$propertyName' which are not supported and will be removed in a later release." +
                " All macros have to point to directories and have to use the folderMacros property instead.")
    }
    varMacros.get().mapTo(result) { "--macro=${it.key}::${it.value}" }
}
