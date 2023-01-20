package de.itemis.mps.gradle.runmigrations

import de.itemis.mps.gradle.*
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
    var force = false
}
@Suppress("unused")
open class RunMigrationsMpsProjectPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.run {
            val extension = extensions.create("runMigrations", MigrationExecutorPluginExtensions::class.java)
            tasks.register("runMigrations")
            
            afterEvaluate {
                val mpsLocation = extension.mpsLocation ?: File(project.buildDir, "mps")
                val projectLocation = extension.projectLocation ?: throw GradleException("No project path set")
                if(!file(projectLocation).exists()) {
                    throw GradleException("The path to the project doesn't exist:$projectLocation")
                }
                val forceMigration = extension.force
                
                val mpsVersion = extension.getMPSVersion()
                val parsedMPSVersion = SemVer.parse(mpsVersion)
                if(forceMigration && (parsedMPSVersion.major < 2021 || (parsedMPSVersion.major == 2021 && parsedMPSVersion.minor < 3))) {
                    throw GradleException("The force migration flag is only supported for 2021.3.0 and higher.")
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
                        ant.withGroovyBuilder { 
                            "path"("id" to "path.mps.ant.path",) {
                                // The different MPS versions need different jars. Let's just keep it simple and include all jars.
                                "fileset"("dir" to  "$mpsLocation/lib", "includes" to "**/*.jar")
                            }
                            "taskdef"("resource" to "jetbrains/mps/build/ant/antlib.xml", "classpathref" to "path.mps.ant.path")
                        }
                        if(forceMigration) {
                            ant.withGroovyBuilder {
                                "migrate"("project" to projectLocation, "mpsHome" to mpsLocation, "force" to true) {
                                    "macro"("name" to "mps_home", "path" to mpsLocation)
                                    "jvmargs"() {
                                        "arg"("value" to "-Didea.log.config.file=log.xml")
                                        "arg"("value" to "-ea")
                                    }
                                }
                            }
                        } else {
                            ant.withGroovyBuilder {
                                "migrate"("project" to projectLocation, "mpsHome" to mpsLocation) {
                                    "macro"("name" to "mps_home", "path" to mpsLocation)
                                    "jvmargs"() {
                                        "arg"("value" to "-Didea.log.config.file=log.xml")
                                        "arg"("value" to "-ea")
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
