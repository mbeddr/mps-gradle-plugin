# mps-gradle-plugin

Miscellaneous task that were found useful when building MPS-based
projects with Gradle.

# Using the Plugin

Add the following `buildscript` block to your build script:

```
buildscript {
    repositories {
        maven { url 'https://projects.itemis.de/nexus/content/repositories/mbeddr' }
    }

    dependencies {
        classpath 'de.itemis.mps:mps-gradle-plugin:1.0+'
    }
}
```

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
