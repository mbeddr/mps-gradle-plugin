## CreateDmg

(macOS only) Creates a .dmg installer by combining an RCP artifact (as
created by an MPS-generated Ant script), a JDK, and a background image.

### Usage

```
task buildDmg(type: de.itemis.mps.gradle.CreateDmg) {
    rcpArtifact file('path/to/RCP.tgz')

    jdkDependency "com.jetbrains.jdk:jdk:${jdkVersion}:osx_x64@tgz"
    // -or -
    jdk file('path/to/jdk.tgz')

    backgroundImage file('path/to/background.png')
    dmgFile file('output.dmg')

    signKeyChain file("/path/to/my.keychain-db")

    signKeyChainPassword "my.keychain-db-password"

    signIdentity "my Application ID Name"
}
```

Parameters:
* `rcpArtifact` - the path to the RCP artifact produced by a build script.
* `jdkDependency` - the coordinates of a JDK in case it's available in
  a repository and can be resolved as a Gradle dependency.
* `jdk` - the path to a JDK .tgz file.
* `backgroundImage` - the path to the background image.
* `dmgFile` - the path and file name of the output DMG image. Must end
  with `.dmg`.
* `signKeyChain (optional)` - the path and file name of the keychain which contains a code signing certificate.
* `signKeyChainPassword (optional)` - the password which should be used to unlock the keychain.
* `signIdentity (optional)` - the application ID of the code signing certificate.

### Operation

The task unpacks `rcpArtifact` into a temporary directory, unpacks
the JDK given by `jdkDependency`/`jdk` under the `jre` subdirectory of
the unpacked RCP artifact, fixes file permissions and creates missing
symlinks. If the additional properties for code signing (`signKeyChain`, `signKeyChainPassword`, `signIdentity`) are defined,
the application will be signed with the given certificate. Afterwards a DMG image is created and its layout is configured using the
background image. Finally, the DMG is copied to `dmgFile`.
