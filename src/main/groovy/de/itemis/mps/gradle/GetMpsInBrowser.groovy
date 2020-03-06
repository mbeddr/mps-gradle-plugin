package de.itemis.mps.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.apache.tools.ant.taskdefs.condition.Os

import java.awt.Desktop

class GetMpsInBrowser extends DefaultTask {

    @Input
    String version

    def setVersion(String version) {
        this.version = version
    }

    private String getMajorPart() {
        def split = version.split("\\.")
        if (split.length == 2) {
            return version
        }

        return split.take(2).join(".")
    }

    private URI getDownloadUrl() {
        def major = getMajorPart()
        
        if (Os.isFamily(Os.FAMILY_WINDOWS)) {
            return new URI("https://download.jetbrains.com/mps/${major}/MPS-${version}.exe")
        } else if (Os.isFamily(Os.FAMILY_MAC)) {
            return new URI("https://download.jetbrains.com/mps/${major}/MPS-${version}-macos-jdk-bundled.dmg")
        } else if (Os.isFamily(Os.FAMILY_UNIX)) {
            return new URI("https://download.jetbrains.com/mps/${major}/MPS-${version}.tar.gz")
        } else {
            print "Warning: could not determine OS downloading generic distribution"
            return new URI("http://download.jetbrains.com/mps/${major}/MPS-${version}.zip")
        }

    }

    @TaskAction
    def build() {

        if (Desktop.isDesktopSupported()) {
            Desktop.getDesktop().browse(getDownloadUrl())
        } else {
            throw new GradleException("this task is not supported in headless mode")
        }
    }
}
