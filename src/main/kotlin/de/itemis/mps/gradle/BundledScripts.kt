package de.itemis.mps.gradle

import org.gradle.api.GradleException
import java.io.File
import java.nio.file.Files
import java.nio.file.attribute.PosixFilePermissions

internal object BundledScripts {
    fun extractScriptsToDir(dir: File, vararg scriptNames: String) {
        val rwxPermissions = PosixFilePermissions.fromString("rwx------")

        for (name in scriptNames) {
            val file = File(dir, name)
            if (!file.parentFile.isDirectory && !file.parentFile.mkdirs()) {
                throw GradleException("Could not create directory " + file.parentFile)
            }
            val resourceStream = BundledScripts::class.java.getResourceAsStream(name)
                ?: throw GradleException("Resource $name was not found")

            resourceStream.use { resource ->
                file.outputStream().use { output ->
                    resource.copyTo(output)
                }
            }
            Files.setPosixFilePermissions(file.toPath(), rwxPermissions)
        }
    }
}
