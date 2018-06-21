package de.itemis.mps.gradle.generate

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.io.PrintWriter
import java.util.*


open class FakeBuildNumberTask: DefaultTask() {

    @InputDirectory
    lateinit var mpsDir: File

    @TaskAction
    fun fakeBuildNumber() {

        val buildProperties = mpsDir.listFiles().find { "build.properties" == it.name } ?: throw GradleException("can't locate build.properties file in MPS directory")

        val props = Properties()
        props.load(buildProperties.inputStream())
        val buildNumber = props.getProperty("mps.build.number")
        val buildTxt = this.getBuildTxt()
        val writer = PrintWriter(buildTxt.outputStream())

        writer.write(buildNumber)
        writer.close()
    }

    @OutputFile
    fun getBuildTxt(): File {
        return File(mpsDir, "build.txt")
    }
}

