package de.itemis.mps.gradle.downloadJBR

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.*
import java.io.File

open class DownloadJbrForPlatform : DefaultTask() {

    @get:Internal
    internal val jbrDirProperty: DirectoryProperty = project.objects.directoryProperty()

    @get:Internal
    var jbrDir : File
        get() = jbrDirProperty.get().asFile
        set(value) {
            jbrDirProperty.set(value)
        }

    @get:Internal
    internal val javaExecutableProperty: RegularFileProperty = project.objects.fileProperty()

    @get:Internal
    var javaExecutable: File
        get() = javaExecutableProperty.get().asFile
        set(value) {
            javaExecutableProperty.set(value)
        }
}
