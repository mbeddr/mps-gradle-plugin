package de.itemis.mps.gradle.generate

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFile
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.io.PrintWriter
import java.util.*
import javax.inject.Inject


open class FakeBuildNumberTask @Inject constructor(of: ObjectFactory): DefaultTask() {

    @get:InputDirectory
    val mpsDir: DirectoryProperty = of.directoryProperty()

    @TaskAction
    fun fakeBuildNumber() {
        if (!mpsDir.isPresent){
            throw GradleException("'mpsDir' not present!")
        }
        val buildProperties = mpsDir.get().files().find { "build.properties" == it.name } ?: throw GradleException("can't locate build.properties file in MPS directory")

        val props = Properties()
        props.load(buildProperties.inputStream())
        val buildNumber = props.getProperty("mps.build.number")
        val buildTxt: Provider<RegularFile> = this.getBuildTxt()
        val writer = PrintWriter(buildTxt.get().asFile.outputStream())

        writer.write(buildNumber)
        writer.close()
    }

    @OutputFile
    fun getBuildTxt(): Provider<RegularFile> {
        return mpsDir.file("build.txt")
    }
}

