package de.itemis.mps.gradle.downloadJBR

import org.apache.tools.ant.taskdefs.condition.Os
import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.Configuration
import org.gradle.api.tasks.*
import java.io.File

open class DownloadJBRForPlatform : DefaultTask() {

    @OutputDirectory
    lateinit var jbrDir : File

    @OutputFile
    lateinit var javaExecutable: File
}