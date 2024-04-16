package de.itemis.mps.gradle

import org.gradle.api.artifacts.Configuration
import java.io.File

internal object ErrorMessages {
    const val MPS_VERSION_NOT_SUPPORTED = "This version of mps-gradle-plugin only supports MPS 2020.1 and above. Please use version 1.4 with an older version of MPS."

    internal fun noMpsProjectIn(dir: File): String = "Directory does not contain an MPS project: $dir"

    internal fun couldNotDetermineMpsVersionFromConfiguration(mpsConfig: Configuration) =
        "Could not determine MPS version from configuration ${mpsConfig.name} (configuration must contain com.jetbrains:mps dependency)."

    internal fun mustSetConfigOrLocation(extensionName: String) = "Either mpsConfig or mpsLocation needs to specified for extension $extensionName."
    internal fun mustSetVersionWhenNoMpsConfiguration(extensionName: String) =
        "Could not determine MPS version because mpsConfiguration was not specified. Set mpsVersion of $extensionName" +
                " extension explicitly."
}
