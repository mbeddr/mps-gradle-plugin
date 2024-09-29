package de.itemis.mps.gradle

import org.gradle.api.GradleException
import java.io.File
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.attribute.PosixFilePermissions

class BundledScripts {
    companion object {
        @JvmStatic
        fun extractScriptsToDir(dir: File, vararg scriptNames: String) {
            val rwxPermissions = PosixFilePermissions.fromString("rwx------")

            for (name in scriptNames) {
                val file = File(dir, name)
                if (!file.parentFile.isDirectory && !file.parentFile.mkdirs()) {
                    throw GradleException("Could not create directory ${file.parentFile}")
                }
                val resourceStream: InputStream = BundledScripts::class.java.getResourceAsStream(name)
                    ?: throw IllegalArgumentException("Resource $name was not found")

                resourceStream.use { inputStream ->
                    file.outputStream().use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                Files.setPosixFilePermissions(file.toPath(), rwxPermissions)
            }
        }
    }
}