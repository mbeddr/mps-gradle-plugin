# mps-gradle-plugin

Miscellaneous tasks that were found useful when building MPS-based
projects with Gradle.

# Using the Plugin

Add the following `buildscript` block to your build script:

```
buildscript {
    repositories {
        maven { url 'https://projects.itemis.de/nexus/content/repositories/mbeddr' }
    }

    dependencies {
        classpath 'de.itemis.mps:mps-gradle-plugin:1.0.+'
    }
}
```

Use a fully specified version such as `1.0.123` for better build reproducibility.

# Tasks

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

### Operation

The task unpacks `rcpArtifact` into a temporary directory, unpacks
the JDK given by `jdkDependency`/`jdk` under the `jre` subdirectory of
the unpacked RCP artifact, fixes file permissions and creates missing
symlinks, then creates a DMG image and configures its layout, using the
background image. Finally, the DMG is copied to `dmgFile`.

## GenerateLibrariesXml

Generates a `.mps/libraries.xml` file using data from property files.

### Usage

```
task generateLibrariesXml(type: de.itemis.mps.gradle.GenerateLibrariesXml) {
    defaults rootProject.file('projectlibraries.properties')
    overrides rootProject.file('projectlibraries.overrides.properties')
    destination file('.mps/libraries.xml')
}
```

Parameters:
* `defaults` - path to default properties (checked in to version control)
* `overrides` - path to property overrides (ignored, not checked in to
  version control, absent by default)
* `destination` - path to the output `libraries.xml`

### Operation

The task reads properties file `defaults`, then `overrides` (if
present). `destination` is then generated based on the properties.

Each property represents an entry in `destination` (a project library),
where the property name is the library name and the property value is
the path to the property.

## BuildLanguages, TestLanguages and RunAntScript

Used to invoke Ant scripts that have been generated from MPS build solutions. 

### Usage

The following code snippet shows an example usage of all three tasks for an example 
MPS project `myProject`. In the snippet below, we invoke three different Ant scripts 
that share a common Ant property `myProject.home` for configuring the file system 
location of the MPS project. We overwrite the value of this property once, together 
with `mps.home`, on line number 1.

```
ext.scriptArgs = ['-Dmps.home=C:/MPS', '-DmyProject.home=C:/myProject']

task build_myProject(type: de.itemis.mps.gradle.BuildLanguages) {
    script new File("build.xml")
}

task test_myProject_(type: de.itemis.mps.gradle.TestLanguages) {
    script new File("build-test.xml")
}

task build_myProjectRCP(type: de.itemis.mps.gradle.RunAntScript) {
    script new File("build-rcp.xml")
    targets 'clean', 'assemble'	
}
```

Global Configuration:
All three tasks require a global variable definition `ext.scriptArgs` (`ext` makes it globally visible) 
that is used to overwrite or define Ant properties such as `mps.home`.

Parameters:
* `script` - path to the Ant script
* `targets` - only available for `RunAntScript` and used for configuring the targets to be invoked
via a list of strings

### Operation

The `BuildLanguages` task is used for invoking scripts that build and package languages/solutions, `TestLanguages` 
for scripts that contain editor and type system tests. Both of them extend `RunAntScript`, which is a generic task 
that can be used to invoke any Ant script.


