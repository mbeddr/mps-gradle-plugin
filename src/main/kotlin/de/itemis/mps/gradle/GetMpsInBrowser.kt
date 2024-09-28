package de.itemis.mps.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.apache.tools.ant.taskdefs.condition.Os
import java.awt.Desktop
import java.net.URI

open class GetMpsInBrowser : DefaultTask() {

    @Input
    var version: String? = null

    private fun getMajorPart(): String {
        val split = version?.split(".")
        return if (split?.size == 2) {
            version!!
        } else {
            split?.take(2)?.joinToString(".") ?: ""
        }
    }

    private fun getDownloadUrl(): URI {
        val major = getMajorPart()
        return when {
            Os.isFamily(Os.FAMILY_WINDOWS) -> URI("https://download.jetbrains.com/mps/$major/MPS-$version.exe")
            Os.isFamily(Os.FAMILY_MAC) -> URI("https://download.jetbrains.com/mps/$major/MPS-$version-macos-jdk-bundled.dmg")
            Os.isFamily(Os.FAMILY_UNIX) -> URI("https://download.jetbrains.com/mps/$major/MPS-$version.tar.gz")
            else -> {
                println("Warning: could not determine OS, downloading generic distribution")
                URI("http://download.jetbrains.com/mps/$major/MPS-$version.zip")
            }
        }
    }

    @TaskAction
    fun build() {
        if (Desktop.isDesktopSupported()) {
            Desktop.getDesktop().browse(getDownloadUrl())
        } else {
            throw GradleException("This task is not supported in headless mode")
        }
    }
}