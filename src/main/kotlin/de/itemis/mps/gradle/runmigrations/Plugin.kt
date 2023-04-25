package de.itemis.mps.gradle.runmigrations

import de.itemis.mps.gradle.BasePluginExtensions
import de.itemis.mps.gradle.getMPSVersion
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
     * (Since MPS 2021.1) Whether to halt if a pre-check has failed. Note that the check for migrated dependencies
     * cannot be skipped.
     */
    var haltOnPrecheckFailure: Boolean? = null

    /**
     * (Since MPS 2021.3) Whether to force a migration even if the project directory contains `.allow-pending-migrations` file.
     */

    var force: Boolean? = null
}

@Suppress("unused")
open class RunMigrationsMpsProjectPlugin : Plugin<Project> {
    companion object {
        val MIN_VERSION_FOR_HALT_ON_PRECHECK_FAILURE = SemVer(2021, 1)
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

                        ant.withGroovyBuilder { 
                            "path"("id" to "path.mps.ant.path",) {
                                // The different MPS versions need different jars. Let's just keep it simple and include all jars.
                                "fileset"("dir" to  "$mpsLocation/lib", "includes" to "**/*.jar")
                            }
                            "taskdef"("resource" to "jetbrains/mps/build/ant/antlib.xml", "classpathref" to "path.mps.ant.path")

                            val argsToMigrate = mutableListOf<Pair<String, Any>>().run {
                                add("project" to projectLocation)
                                add("mpsHome" to mpsLocation)

                                if (extension.force != null) add("force" to extension.force!!)
                                if (extension.haltOnPrecheckFailure != null) add("haltOnPrecheckFailure" to extension.haltOnPrecheckFailure!!)

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
                                }

                                extension.pluginsProperty.get().forEach {
                                    // Same handling as in mps-build-backends
                                    if (File(it.path).isAbsolute) {
                                        "plugin"("path" to it.path, "id" to it.id)
                                    }
                                    else if (extension.pluginLocation != null && File(extension.pluginLocation, it.path).exists()) {
                                        "plugin"("path" to File(extension.pluginLocation, it.path), "id" to it.id)
                                    } else {
                                        "plugin"("path" to mpsLocation.resolve("plugins").resolve(it.path), "id" to it.id)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
