package de.itemis.mps.gradle.runmigrations

import de.itemis.mps.gradle.BasePluginExtensions
import de.itemis.mps.gradle.getMPSVersion
import de.itemis.mps.gradle.runAnt
import groovy.xml.MarkupBuilder
import net.swiftzer.semver.SemVer
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.Copy
import org.gradle.kotlin.dsl.withGroovyBuilder
import java.io.File
import javax.inject.Inject

open class MigrationExecutorPluginExtensions @Inject constructor(of: ObjectFactory) : BasePluginExtensions(of) {
    /**
     * (Since MPS 2021.1) Whether to halt if a pre-check has failed. Note that to ignore the check for migrated
     * dependencies the [haltOnDependencyError] option must be set to `false` as well.
     */
    var haltOnPrecheckFailure: Boolean? = null

    /**
     * (Since MPS 2021.3.4) Whether to halt when a non-migrated dependency is discovered.
     */
    var haltOnDependencyError: Boolean? = null

    /**
     * (Since MPS 2021.3) Whether to force a migration even if the project directory contains `.allow-pending-migrations` file.
     */
    var force: Boolean? = null
}

@Suppress("unused")
open class RunMigrationsMpsProjectPlugin : Plugin<Project> {
    companion object {
        val MIN_VERSION_FOR_HALT_ON_PRECHECK_FAILURE = SemVer(2021, 1)
        val MIN_VERSION_FOR_HALT_ON_DEPENDENCY_ERROR = SemVer(2021, 3, 4)
        val MIN_VERSION_FOR_FORCE = SemVer(2021, 3)
    }

    override fun apply(project: Project) {
        project.run {
            val extension = extensions.create("runMigrations", MigrationExecutorPluginExtensions::class.java)
            tasks.register("runMigrations")

            afterEvaluate {
                val mpsLocation = extension.mpsLocation ?: File(project.buildDir, "mps")
                val projectLocation = extension.projectLocation ?: throw GradleException("No project path set")
                if (!file(projectLocation).exists()) {
                    throw GradleException("The path to the project doesn't exist: $projectLocation")
                }

                val mpsVersion = extension.getMPSVersion()
                val parsedMPSVersion = SemVer.parse(mpsVersion)

                if (extension.force != null && parsedMPSVersion < MIN_VERSION_FOR_FORCE) {
                    throw GradleException("The force migration flag is only supported for MPS version $MIN_VERSION_FOR_FORCE and higher.")
                }

                if (extension.haltOnPrecheckFailure != null && parsedMPSVersion < MIN_VERSION_FOR_HALT_ON_PRECHECK_FAILURE) {
                    throw GradleException("The 'do not halt on pre-check failure' option is only supported for MPS version $MIN_VERSION_FOR_HALT_ON_PRECHECK_FAILURE and higher.")
                }

                if (extension.haltOnDependencyError != null && parsedMPSVersion < MIN_VERSION_FOR_HALT_ON_DEPENDENCY_ERROR) {
                    throw GradleException("The 'do not halt on dependency error' option is only supported for MPS version $MIN_VERSION_FOR_HALT_ON_DEPENDENCY_ERROR and higher.")
                }

                val resolveMps: Task = if (extension.mpsConfig != null) {
                    tasks.create("resolveMpsForMigrations", Copy::class.java) {
                        from({ extension.mpsConfig!!.resolve().map(::zipTree) })
                        into(mpsLocation)
                    }
                } else {
                    tasks.create("resolveMpsForMigrations")
                }

                tasks.named("runMigrations") {
                    dependsOn(resolveMps)
                    doLast {
                        if (!mpsLocation.isDirectory) {
                            throw GradleException("Specified MPS location does not exist or is not a directory: $mpsLocation")
                        }

                        val buildFile = temporaryDir.resolve("build.xml")
                        buildFile.printWriter().use {
                            MarkupBuilder(it).withGroovyBuilder {
                                "project" {
                                    "path"("id" to "path.mps.ant.path") {
                                        // The different MPS versions need different jars. Let's just keep it simple and include all jars.
                                        "fileset"("dir" to "$mpsLocation/lib", "includes" to "**/*.jar")
                                    }
                                    "taskdef"(
                                        "resource" to "jetbrains/mps/build/ant/antlib.xml",
                                        "classpathref" to "path.mps.ant.path"
                                    )

                                    val argsToMigrate = mutableListOf<Pair<String, Any>>().run {
                                        add("project" to projectLocation)
                                        add("mpsHome" to mpsLocation)

                                        extension.force?.let { add("force" to it) }
                                        extension.haltOnPrecheckFailure?.let { add("haltOnPrecheckFailure" to it) }
                                        extension.haltOnDependencyError?.let { add("haltOnDependencyError" to it) }

                                        toTypedArray()
                                    }

                                    "migrate"(*argsToMigrate) {
                                        "macro"("name" to "mps_home", "path" to mpsLocation)

                                        extension.macros.forEach {
                                            "macro"("name" to it.name, "path" to it.value)
                                        }

                                        "jvmargs" {
                                            "arg"("value" to "-Didea.log.config.file=log.xml")
                                            "arg"("value" to "-ea")

                                            if (extension.maxHeap != null) {
                                                "arg"("value" to "-Xmx${extension.maxHeap}")
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
                                        }

                                        extension.pluginsProperty.get().forEach {
                                            // Same handling as in mps-build-backends
                                            if (File(it.path).isAbsolute) {
                                                "plugin"("path" to it.path, "id" to it.id)
                                            } else if (extension.pluginLocation != null && File(
                                                    extension.pluginLocation,
                                                    it.path
                                                ).exists()
                                            ) {
                                                "plugin"(
                                                    "path" to File(extension.pluginLocation, it.path),
                                                    "id" to it.id
                                                )
                                            } else {
                                                "plugin"(
                                                    "path" to mpsLocation.resolve("plugins").resolve(it.path),
                                                    "id" to it.id
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        val classpath = project.fileTree(mpsLocation.resolve("lib")) {
                            include("ant/lib/*.jar")
                            include("*.jar")
                            builtBy(resolveMps)
                        }

                        runAnt(
                            extension.javaExec, temporaryDir, args = listOf(),
                            includeDefaultClasspath = false,
                            scriptClasspath = classpath
                        )
                    }
                }
            }
        }
    }
}
