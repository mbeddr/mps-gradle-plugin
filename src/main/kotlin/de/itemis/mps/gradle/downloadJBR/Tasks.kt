package de.itemis.mps.gradle.downloadJBR

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import javax.inject.Inject

open class DownloadJbrForPlatform @Inject constructor(of: ObjectFactory) : DefaultTask() {

    @get:OutputDirectory
    val jbrDir: DirectoryProperty = of.directoryProperty()

    @get:OutputFile
    val javaExecutable: RegularFileProperty = of.fileProperty()
}