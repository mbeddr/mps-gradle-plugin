package de.itemis.mps.gradle.tasks

import de.itemis.mps.gradle.ErrorMessages
import org.gradle.api.GradleException
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
