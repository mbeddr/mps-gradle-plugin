package de.itemis.mps.gradle.downloadJBR

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import org.gradle.jvm.toolchain.JavaLauncher
import org.gradle.jvm.toolchain.JavaToolchainService
import org.gradle.jvm.toolchain.JavaToolchainSpec
import org.gradle.jvm.toolchain.internal.SpecificInstallationToolchainSpec
import java.io.File
import javax.inject.Inject

open class DownloadJbrForPlatform @Inject constructor(
    private val objects: ObjectFactory,
    private val javaToolchainService: JavaToolchainService
) : DefaultTask() {

    @get:Internal
    internal val jbrDirProperty: DirectoryProperty = objects.directoryProperty()

    @get:Internal
    var jbrDir : File
        get() = jbrDirProperty.get().asFile
        set(value) {
            jbrDirProperty.set(value)
        }

    @get:Internal
    internal val javaExecutableProperty: RegularFileProperty = objects.fileProperty()

    @get:Internal
    var javaExecutable: File
        get() = javaExecutableProperty.get().asFile
        set(value) {
            javaExecutableProperty.set(value)
        }

    /**
     * A [JavaToolchainSpec] that can be passed to [JavaToolchainService] to obtain various tools (Java compiler,
     * launcher, javadoc).
     */
    @get:Internal
    val toolchainSpec: Provider<JavaToolchainSpec> =
        javaExecutableProperty.map { SpecificInstallationToolchainSpec.fromJavaExecutable(objects, it.asFile.path) }

    /**
     * A [JavaLauncher] for the downloaded JBR that can be used with [JavaExec] task.
     */
    @get:Internal
    val javaLauncher: Provider<JavaLauncher> = toolchainSpec.flatMap(javaToolchainService::launcherFor)
}
