package de.itemis.mps.gradle.tasks

import de.itemis.mps.gradle.TaskGroups
import de.itemis.mps.gradle.launcher.MpsVersionDetection
import groovy.xml.MarkupBuilder
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileCollection
import org.gradle.api.logging.LogLevel
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.tasks.*
import org.gradle.jvm.toolchain.JavaLauncher
import org.gradle.kotlin.dsl.withGroovyBuilder
import org.gradle.process.ExecOperations
import java.io.File
import javax.inject.Inject

@UntrackedTask(because = "Operates 'in place'")
abstract class MpsMigrate : DefaultTask(), MpsProjectTask {

    @Input
    abstract override fun getLogLevel(): Property<LogLevel>

    @Internal("covered by mpsVersion and classpath")
    abstract override fun getMpsHome(): DirectoryProperty

    @Input
    @Optional
    abstract override fun getMpsVersion(): Property<String>

    /**
     * (Since MPS 2021.1) Whether to halt if a pre-check has failed. Note that to ignore the check for migrated
     * dependencies the [haltOnDependencyError] option must be set to `false` as well.
     */
    @get:Input
    @get:Optional
    abstract val haltOnPrecheckFailure: Property<Boolean>

    /**
     * (Since MPS 2021.3.4) Whether to halt when a non-migrated dependency is discovered.
     */
    @get:Input
    @get:Optional
    abstract val haltOnDependencyError: Property<Boolean>

    @Internal("covered by allProjectFiles")
    abstract override fun getProjectLocation(): DirectoryProperty

    @get:Internal("covered by allProjectFiles")
    abstract val projectLocations: ConfigurableFileCollection

    @get:InputFiles
    @get:IgnoreEmptyDirectories
    @get:SkipWhenEmpty
    @get:PathSensitive(PathSensitivity.RELATIVE)
    protected val allProjectFiles = providerFactory.provider {
        effectiveProjectLocations().flatMap { objectFactory.fileTree().from(it) }
    }

    @Nested
    @Optional
    abstract override fun getJavaLauncher(): Property<JavaLauncher>

    @get:Input
    abstract val jvmArgs: ListProperty<String>

    @get:Input
    abstract val antJvmArgs: ListProperty<String>

    @get:Input
    @get:Optional
    abstract val maxHeapSize: Property<String>

    @Internal("Folder macros are ignored for the purposes of up-to-date checks and caching")
    abstract override fun getFolderMacros(): MapProperty<String, Directory>

    @Classpath
    abstract override fun getPluginRoots(): ConfigurableFileCollection

    @get:Inject
    protected abstract val execOperations: ExecOperations

    @get:Inject
    protected abstract val objectFactory: ObjectFactory

    @get:Inject
    protected abstract val providerFactory: ProviderFactory

    init {
        mpsVersion.convention(MpsVersionDetection.fromMpsHome(project.layout, providerFactory, mpsHome.asFile))
        group = TaskGroups.MIGRATION
    }

    private fun effectiveProjectLocations(): FileCollection {
        val hasMultiple = !projectLocations.isEmpty
        val hasSingle = projectLocation.isPresent
        if (hasMultiple && hasSingle) {
            throw GradleException("Cannot set both projectLocation and projectLocations. Use one or the other.")
        }
        if (!hasMultiple && !hasSingle) {
            throw GradleException("Must set either projectLocation or projectLocations.")
        }
        return if (hasMultiple) projectLocations else objectFactory.fileCollection().from(projectLocation)
    }

    @TaskAction
    fun execute() {
        // When MPS detects that files have changed externally then instead of updating the VFS cache it complains.
        // Cleaning temporary directory helps avoid this.
        cleanTemporaryDir()

        val effectiveLocations = effectiveProjectLocations()

        val buildFile = temporaryDir.resolve("build.xml")
        writeBuildFile(buildFile, effectiveLocations)

        val mpsAntClasspath = mpsHome.asFileTree.matching {
            include("lib/ant/lib/*.jar")
            include("lib/*.jar")
        }

        for (dir in effectiveLocations) {
            checkProjectLocation(dir)
        }

        execOperations.javaexec {
            mainClass.set("org.apache.tools.ant.launch.Launcher")
            workingDir = temporaryDir
            classpath = mpsAntClasspath
            val executableCandidate = javaLauncher.orNull?.executablePath?.asFile?.toString()
            if (executableCandidate != null) {
                executable = executableCandidate
            }
            jvmArgs(antJvmArgs.get())
        }
    }

    private fun cleanTemporaryDir() {
        temporaryDir.listFiles()?.forEach { it.deleteRecursively() }
    }

    private fun writeBuildFile(buildFile: File, effectiveLocations: FileCollection) {
        buildFile.printWriter().use { writer ->
            MarkupBuilder(writer).withGroovyBuilder {
                "project" {
                    "path"("id" to "path.mps.ant.path") {
                        // The different MPS versions need different jars. Let's just keep it simple and include all jars.
                        "fileset"("dir" to "${mpsHome.get()}/lib", "includes" to "**/*.jar")
                    }
                    "taskdef"(
                        "resource" to "jetbrains/mps/build/ant/antlib.xml",
                        "classpathref" to "path.mps.ant.path"
                    )

                    val argsToMigrate = mutableListOf<Pair<String, Any>>().run {
                        add("mpsHome" to mpsHome.get())

                        if (haltOnPrecheckFailure.isPresent) { add("haltOnPrecheckFailure" to haltOnPrecheckFailure.get()) }
                        if (haltOnDependencyError.isPresent) { add("haltOnDependencyError" to haltOnDependencyError.get()) }

                        if (mpsVersion.get() >= "2022.3") { add("jnaLibraryPath" to "lib/jna/${computeJnaArch()}") }

                        if (logLevel.get() <= LogLevel.INFO) {
                            add("loglevel" to "info")
                        }

                        toTypedArray()
                    }

                    val folderMacrosValue = folderMacros.get()
                    val allLibraries = effectiveLocations
                        .flatMap { readLibraries(it, folderMacrosValue::get) }
                        .toSortedSet()

                    "migrate"(*argsToMigrate) {
                        effectiveLocations.forEach {
                            "project"("path" to it)
                        }

                        "repository" {
                            allLibraries.forEach { "modules"("dir" to it) }
                        }

                        "macro"("name" to "mps_home", "path" to mpsHome.get())

                        folderMacrosValue.forEach {
                            "macro"("name" to it.key, "path" to it.value)
                        }

                        "jvmargs" {
                            "arg"("value" to "-Didea.log.config.file=log.xml")
                            "arg"("value" to "-Didea.config.path=${temporaryDir}/config")
                            "arg"("value" to "-Didea.system.path=${temporaryDir}/system")
                            "arg"("value" to "-ea")

                            if (maxHeapSize.isPresent) {
                                "arg"("value" to "-Xmx${maxHeapSize.get()}")
                            }

                            "arg"("value" to "--add-opens=java.base/java.io=ALL-UNNAMED")
                            "arg"("value" to "--add-opens=java.base/java.lang=ALL-UNNAMED")
                            "arg"("value" to "--add-opens=java.base/java.lang.reflect=ALL-UNNAMED")
                            "arg"("value" to "--add-opens=java.base/java.net=ALL-UNNAMED")
                            "arg"("value" to "--add-opens=java.base/java.nio=ALL-UNNAMED")
                            "arg"("value" to "--add-opens=java.base/java.nio.charset=ALL-UNNAMED")
                            "arg"("value" to "--add-opens=java.base/java.text=ALL-UNNAMED")
                            "arg"("value" to "--add-opens=java.base/java.time=ALL-UNNAMED")
                            "arg"("value" to "--add-opens=java.base/java.util=ALL-UNNAMED")
                            "arg"("value" to "--add-opens=java.base/java.util.concurrent=ALL-UNNAMED")
                            "arg"("value" to "--add-opens=java.base/java.util.concurrent.atomic=ALL-UNNAMED")
                            "arg"("value" to "--add-opens=java.base/jdk.internal.ref=ALL-UNNAMED")
                            "arg"("value" to "--add-opens=java.base/jdk.internal.vm=ALL-UNNAMED")
                            "arg"("value" to "--add-opens=java.base/sun.nio.ch=ALL-UNNAMED")
                            "arg"("value" to "--add-opens=java.base/sun.security.ssl=ALL-UNNAMED")
                            "arg"("value" to "--add-opens=java.base/sun.security.util=ALL-UNNAMED")
                            "arg"("value" to "--add-opens=java.desktop/java.awt=ALL-UNNAMED")
                            "arg"("value" to "--add-opens=java.desktop/java.awt.dnd.peer=ALL-UNNAMED")
                            "arg"("value" to "--add-opens=java.desktop/java.awt.event=ALL-UNNAMED")
                            "arg"("value" to "--add-opens=java.desktop/java.awt.image=ALL-UNNAMED")
                            "arg"("value" to "--add-opens=java.desktop/java.awt.peer=ALL-UNNAMED")
                            "arg"("value" to "--add-opens=java.desktop/javax.swing=ALL-UNNAMED")
                            "arg"("value" to "--add-opens=java.desktop/javax.swing.plaf.basic=ALL-UNNAMED")
                            "arg"("value" to "--add-opens=java.desktop/javax.swing.text.html=ALL-UNNAMED")
                            "arg"("value" to "--add-opens=java.desktop/sun.awt.datatransfer=ALL-UNNAMED")
                            "arg"("value" to "--add-opens=java.desktop/sun.awt.image=ALL-UNNAMED")
                            "arg"("value" to "--add-opens=java.desktop/sun.awt=ALL-UNNAMED")
                            "arg"("value" to "--add-opens=java.desktop/sun.font=ALL-UNNAMED")
                            "arg"("value" to "--add-opens=java.desktop/sun.java2d=ALL-UNNAMED")
                            "arg"("value" to "--add-opens=java.desktop/sun.swing=ALL-UNNAMED")
                            "arg"("value" to "--add-opens=jdk.attach/sun.tools.attach=ALL-UNNAMED")
                            "arg"("value" to "--add-opens=jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED")
                            "arg"("value" to "--add-opens=jdk.internal.jvmstat/sun.jvmstat.monitor=ALL-UNNAMED")
                            "arg"("value" to "--add-opens=jdk.jdi/com.sun.tools.jdi=ALL-UNNAMED")
                            "arg"("value" to "--add-opens=java.desktop/com.apple.laf=ALL-UNNAMED")
                            "arg"("value" to "--add-opens=java.desktop/com.apple.eawt=ALL-UNNAMED")
                            "arg"("value" to "--add-opens=java.desktop/com.apple.eawt.event=ALL-UNNAMED")
                            "arg"("value" to "--add-opens=java.management/sun.management=ALL-UNNAMED")

                            jvmArgs.get().forEach {
                                "arg"("value" to it)
                            }
                        }

                        pluginRoots.flatMap { findPluginsRecursively(it) }.forEach {
                            "plugin"("path" to it.path, "id" to it.id)
                        }
                    }
                }
            }
        }
    }
}

private fun computeJnaArch(): String = when (System.getProperty("os.arch")) {
    "aarch64" -> "aarch64"
    else -> "amd64"
}
