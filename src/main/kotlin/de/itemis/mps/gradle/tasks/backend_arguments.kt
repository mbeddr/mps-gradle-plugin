package de.itemis.mps.gradle.tasks

import de.itemis.mps.gradle.ErrorMessages
import org.gradle.api.GradleException
import org.gradle.api.file.Directory
import org.gradle.api.file.FileCollection
import org.gradle.api.file.FileSystemLocation
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
