package de.itemis.mps.gradle.de.itemis.mps.gradle

import de.itemis.mps.gradle.BundledScripts
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.artifacts.Dependency
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.Optional
import java.io.File
import kotlin.io.path.createTempDirectory

open class CreateDmg : DefaultTask() {
    @InputFile
    lateinit var rcpArtifact: File

    @InputFile
    lateinit var backgroundImage: File

    @InputFile
    lateinit var jdk: File

    @OutputFile
    lateinit var dmgFile: File

    @Optional @Input
    var signKeyChainPassword: String? = null

    @Optional @Input
    var signIdentity: String? = null

    @InputFile @Optional
    var signKeyChain: File? = null

    fun setSignKeyChain(file: Any) {
        this.signKeyChain = project.file(file)
    }

    fun setRcpArtifact(file: Any) {
        this.rcpArtifact = project.file(file)
    }

    fun setBackgroundImage(file: Any) {
        this.backgroundImage = project.file(file)
    }

    fun setJdk(file: Any) {
        this.jdk = project.file(file)
    }

    /**
     * Sets the [jdk] property from a dependency, given as either a [Dependency] object or in dependency notation.
     */
    fun setJdkDependency(jdkDependency: Any) {
        val dep: Dependency = project.dependencies.create(jdkDependency)
        val files = project.configurations.detachedConfiguration(dep).resolve()
        if (files.size != 1) {
            throw GradleException("Expected a single file for jdkDependency '$jdkDependency', got ${files.size} files")
        }
        this.jdk = files.first()
    }

    fun setDmgFile(file: Any) {
        this.dmgFile = project.file(file)
        if (!dmgFile.name.endsWith(".dmg")) {
            throw GradleException("Value of dmgFile must end with .dmg but was $dmgFile")
        }
    }

    @TaskAction
    fun build() {
        val scripts = arrayOf("mpssign.sh", "mpsdmg.sh", "mpsdmg.pl",
            "Mac/Finder/DSStore/BuddyAllocator.pm", "Mac/Finder/DSStore.pm")
        val scriptsDir = createTempDirectory().toFile()
        val dmgDir = createTempDirectory().toFile()
        val signingInfo = arrayOf(signKeyChainPassword, signKeyChain, signIdentity)
        try {
            BundledScripts.extractScriptsToDir(scriptsDir, *scripts)
            project.exec {
                executable = File(scriptsDir, "mpssign.sh").toString()

                if (signingInfo.all { it != null }) {
                    args = listOf("-r", rcpArtifact.toString(), "-o", dmgDir.toString(), "-j", jdk.toString(), "-p", signKeyChainPassword, "-k", signKeyChain.toString(), "-i", signIdentity)
                } else if (signingInfo.all { it == null }) {
                    args = listOf("-r", rcpArtifact.toString(), "-o", dmgDir.toString(), "-j", jdk.toString())
                } else {
                    throw IllegalArgumentException("Not all signing parameters set. signKeyChain: ${signingInfo[1]}, signIdentity: ${signingInfo[2]} and signKeyChainPassword needs to be set.")
                }
                workingDir = scriptsDir
            }
            project.exec {
                executable = File(scriptsDir, "mpsdmg.sh").toString()
                args = listOf(dmgDir.toString(), dmgFile.toString(), backgroundImage.toString())
                workingDir = scriptsDir
            }
        } finally {
            // Do not use File.deleteDir() because it follows symlinks!
            // (e.g. the symlink to /Applications inside dmgDir)
            project.exec {
                commandLine = listOf("rm", "-rf", scriptsDir.toString(), dmgDir.toString())
            }
        }
    }
}
