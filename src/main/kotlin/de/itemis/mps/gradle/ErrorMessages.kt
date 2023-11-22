package de.itemis.mps.gradle

import java.io.File

internal object ErrorMessages {
    const val MUST_SET_CONFIG_OR_VERSION = "Either mpsConfig or mpsVersion needs to specified!"
    const val MUST_SET_VERSION_AND_LOCATION = "Setting an MPS version but no MPS location is not supported!"
    const val MPS_VERSION_NOT_SUPPORTED = "This version of mps-gradle-plugin only supports MPS 2020.1 and above. Please use version 1.4 with an older version of MPS."

    fun noMpsProjectIn(dir: File): String = "Directory does not contain an MPS project: $dir"
}
