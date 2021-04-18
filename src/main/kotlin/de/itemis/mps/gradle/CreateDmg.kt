package de.itemis.mps.gradle

import org.gradle.api.GradleException
import org.gradle.api.file.ProjectLayout
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.tasks.*
import org.gradle.kotlin.dsl.property
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FilterOutputStream
import javax.inject.Inject

open class CreateDmg @Inject constructor(
    objects: ObjectFactory,
    providers: ProviderFactory,
    layout: ProjectLayout
) : BaseBundleMacOsTask(objects, providers, layout) {
    @InputFile
    val backgroundImage = objects.fileProperty()

    @OutputFile
    val dmgFile = objects.fileProperty()
        .convention(layout.buildDirectory.file("createDmg/$name/$name.dmg"))

    @Input
    @Optional
    val signKeyChainPassword = objects.property<String>()

    @Input
    @Optional
    val signIdentity = objects.property<String>()

    @InputFile
    @Optional
    val signKeyChain = objects.fileProperty()

    @Internal
    val scriptsDir = layout.buildDirectory.dir("createDmg/$name/scripts")

    @Internal
    val dmgDir = layout.buildDirectory.dir("createDmg/$name/dmg")

    @TaskAction
    fun run() {
        val dmgFile = dmgFile.get().asFile

        if (!dmgFile.path.endsWith(".dmg")) {
            throw GradleException("Value of dmgFile must end with .dmg but was $dmgFile")
        }

        val scripts = arrayOf(
            "mpssign.sh", "mpsdmg.sh", "mpsdmg.pl",
            "Mac/Finder/DSStore/BuddyAllocator.pm", "Mac/Finder/DSStore.pm"
        )
        val signingInfo = arrayOf(signKeyChainPassword, signKeyChain, signIdentity)
        val scriptsDir = scriptsDir.get().asFile
        val dmgDir = dmgDir.get().asFile
        try {
            // Cleanup temporary directory so stale files do not affect the current task execution
            rmdirs(dmgDir)
            dmgDir.mkdirs()
            BundledScripts.extractScriptsToDir(scriptsDir, *scripts)
            project.exec {
                executable = "./mpssign.sh"
                workingDir = scriptsDir
                args("-r", rcpArtifact.get().asFile, "-o", dmgDir, "-j", jdk.get().asFile)

                if (signingInfo.all { it.isPresent }) {
                    args("-p", signKeyChainPassword.get())
                    args("-k", signKeyChain.get().asFile)
                    args("-i", signIdentity.get())
                } else if (signingInfo.any { it.isPresent }) {
                    throw IllegalArgumentException("Not all signing parameters are set. signKeyChain: ${signKeyChain.orNull}, signIdentity: ${signIdentity.orNull} and signKeyChainPassword needs to be set. ")
                }
            }
            project.exec {
                executable = "./mpsdmg.sh"
                workingDir = scriptsDir
                args(dmgDir, dmgFile, backgroundImage.get().asFile)
            }
        } finally {
            if (cleanTemporaryDirectories.get()) {
                rmdirs(scriptsDir, dmgDir)
            }
        }
    }
}
