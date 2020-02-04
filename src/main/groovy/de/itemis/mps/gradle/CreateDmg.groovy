package de.itemis.mps.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.artifacts.Dependency
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.Optional

class CreateDmg extends DefaultTask {
    @InputFile
    File rcpArtifact

    @InputFile
    File backgroundImage

    @InputFile
    File jdk

    @OutputFile
    File dmgFile

    @Optional @Input
    String signKeyChainPassword

    @Optional @Input
    String signIdentity

    @InputFile @Optional
    File signKeyChain

    def setSignKeyChain(Object file) {
        this.signKeyChain = project.file(file)
    }

    def setRcpArtifact(Object file) {
        this.rcpArtifact = project.file(file)
    }

    def setBackgroundImage(Object file) {
        this.backgroundImage = project.file(file)
    }

    def setJdk(Object file) {
        this.jdk = project.file(file)
    }

    /**
     * Sets the {@link #jdk} property from a dependency, given as either a {@link Dependency} object or in dependency
     * notation.
     */
    def setJdkDependency(Object jdkDependency) {
        Dependency dep = project.dependencies.create(jdkDependency)
        def files = project.configurations.detachedConfiguration(dep).resolve()
        if (files.size() != 1) {
            throw new GradleException(
                    "Expected a single file for jdkDependency '$jdkDependency', got ${files.size()} files")
        }
        this.jdk = files.first()
    }

    def setDmgFile(Object file) {
        this.dmgFile = project.file(file)
        if (dmgFile != null && !dmgFile.name.endsWith(".dmg")) {
            throw new GradleException("Value of dmgFile must end with .dmg but was $dmgFile")
        }
    }

    @TaskAction
    def build() {
        String[] scripts = ['mpssign.sh', 'mpsdmg.sh', 'mpsdmg.pl',
                            'Mac/Finder/DSStore/BuddyAllocator.pm', 'Mac/Finder/DSStore.pm']
        File scriptsDir = File.createTempDir()
        File dmgDir = File.createTempDir()
        def signingInfo = [signKeyChainPassword, signKeyChain, signIdentity]
        try {
            BundledScripts.extractScriptsToDir(scriptsDir, scripts)
            project.exec {
                executable new File(scriptsDir, 'mpssign.sh')

                if(signingInfo.every {it != null}) {
                    args '-r', rcpArtifact, '-o', dmgDir, '-j', jdk, '-p', signKeyChainPassword, '-k', signKeyChain, '-i', signIdentity
                }else if (signingInfo.every {it == null}){
                    args '-r', rcpArtifact, '-o', dmgDir, '-j', jdk
                }else{
                    throw new IllegalArgumentException("Not all signing paramters set.  signKeyChain: ${getSigningInfo[1]}, signIdentity: ${getSigningInfo[2]} and signKeyChainPassword needs to be set. ")
                }
                workingDir scriptsDir
            }
            project.exec {
                executable new File(scriptsDir, 'mpsdmg.sh')
                args dmgDir, dmgFile, backgroundImage
                workingDir scriptsDir
            }
        } finally {
            // Do not use File.deleteDir() because it follows symlinks!
            // (e.g. the symlink to /Applications inside dmgDir)
            project.exec {
                commandLine 'rm', '-rf', scriptsDir, dmgDir
            }
        }
    }

}
