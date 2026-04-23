package de.itemis.mps.gradle

import java.io.File

internal object ErrorMessages {
    internal fun noMpsProjectIn(dir: File): String = "Directory does not contain an MPS project: $dir"
}
