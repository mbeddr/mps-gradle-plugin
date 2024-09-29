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
    
    private fun getMajorMinorPart(): String {
        val mpsVersion = MPSVersion.parse(version!!)
       return "${mpsVersion.version.major}.${mpsVersion.version.minor}"
    }

    private fun getDownloadUrl(): URI {
        val majorMinor = getMajorMinorPart()
        return when {
            Os.isFamily(Os.FAMILY_WINDOWS) -> URI("https://download.jetbrains.com/mps/$majorMinor/MPS-$version.exe")
            Os.isFamily(Os.FAMILY_MAC) -> {
                val suffix = if(Os.isArch("aarch64")) "-aarch64" else ""
                URI("https://download.jetbrains.com/mps/$majorMinor/MPS-$version-macos${suffix}.dmg")
            }
            Os.isFamily(Os.FAMILY_UNIX) -> URI("https://download.jetbrains.com/mps/$majorMinor/MPS-$version.tar.gz")
            else -> {
                println("Warning: could not determine OS, downloading generic distribution")
                URI("http://download.jetbrains.com/mps/$majorMinor/MPS-$version.zip")
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