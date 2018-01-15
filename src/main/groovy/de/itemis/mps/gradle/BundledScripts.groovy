package de.itemis.mps.gradle

import org.gradle.api.GradleException

import java.nio.file.Files
import java.nio.file.attribute.PosixFilePermissions

class BundledScripts {
    static void extractScriptsToDir(File dir, String... scriptNames) {
        def rwxPermissions = PosixFilePermissions.fromString("rwx------")

        for (name in scriptNames) {
            File file = new File(dir, name)
            if (!file.parentFile.isDirectory() && ! file.parentFile.mkdirs()) {
                throw new GradleException("Could not create directory " + file.parentFile)
            }
            InputStream resourceStream = BundledScripts.class.getResourceAsStream(name)
            if (resourceStream == null) {
                throw new IllegalArgumentException("Resource ${name} was not found")
            }

            resourceStream.withStream { is -> file.newOutputStream().withStream { os -> os << is } }
            Files.setPosixFilePermissions(file.toPath(), rwxPermissions)
        }
    }
}
