# mps-gradle-plugin

Miscellaneous tasks that were found useful when building MPS-based
projects with Gradle.

# Using the Plugin

Add the following `buildscript` block to your build script:

```
buildscript {
    repositories {
        maven { url 'https://projects.itemis.de/nexus/content/repositories/mbeddr' }
        mavenCentral()
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
* `signKeyChainPassword (optional)` - the password which should be use to unlock the keychain.
* `signIdentity (optional)` - the application ID of the code signing certificate.

### Operation

The task unpacks `rcpArtifact` into a temporary directory, unpacks
the JDK given by `jdkDependency`/`jdk` under the `jre` subdirectory of
the unpacked RCP artifact, fixes file permissions and creates missing
symlinks. If the additional properties for code signing (`signKeyChain`, `signKeyChainPassword`, `signIdentity`) are defined,
the application will be signed with the given certificate. Afterwards a DMG image is created and its layout is configured using the
background image. Finally, the DMG is copied to `dmgFile`.

## BundleMacosJdk

(Linux/macOS) Creates a .tar.gz by combining an RCP artifact and a JDK.
This task is intended as a substitute for the macOS-specific CreateDmg
task.

### Usage

```
task bundleMacosJdk(type: de.itemis.mps.gradle.BundleMacosJdk) {
    rcpArtifact file('path/to/RCP.tgz')

    jdkDependency "com.jetbrains.jdk:jdk:${jdkVersion}:osx_x64@tgz"
    // -or -
    jdk file('path/to/jdk.tgz')

    outputFile file('output.tar.gz')
}
```

Parameters:
* `rcpArtifact` - the path to the RCP artifact produced by a build script.
* `jdkDependency` - the coordinates of a JDK in case it's available in
  a repository and can be resolved as a Gradle dependency.
* `jdk` - the path to a JDK .tgz file.
* `outputFile` - the path and file name of the output gzipped tar archive.

### Operation

The task unpacks `rcpArtifact` into a temporary directory, unpacks
the JDK given by `jdkDependency`/`jdk` under the `jre` subdirectory of
the unpacked RCP artifact, fixes file permissions and creates missing
symlinks. Finally, the file is repackaged again as tar/gzip.

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
the path to the library.

## Generate

Generate a specific or all models in a project without the need for a MPS model.

While technically possible generating languages with this task makes little sense as there is no way of packaging the
generated artifacts into JAR files. We only recommend using this for simple tasks where user defined models should be
generated in the CI build or from the commandline.

### Usage

A minimal build script to generate a MPS project with no external plugins would look like this:

```
apply plugin: 'generate-models'

configurations {
    mps
}

ext.mpsVersion = '2018.2.5'

generate {
    projectLocation = new File("./mps-prj")
    mpsConfig = configurations.mps
}

dependencies {
    mps "com.jetbrains:mps:$mpsVersion"
}
```

Parameters:
* `mpsConfig` - the configuration used to resolve MPS. Currently only vanilla MPS is supported and no custom RCPs.
  Custom plugins are supported via the `pluginLocation` parameter.
* `mpsLocation` - optional location where to place the MPS files.
* `plugins` - optional list of plugins to load before generation is attempted.
  The notation is `new Plugin("someID", "somePath")`. Where the first parameter is the plugin id and the second the `short (folder) name`.
* `pluginLocation` - location where to load the plugins from. Structure needs to be a flat folder structure similar to the
  `plugins` directory inside of the MPS installation.
* `models` - optional list of models to generate. If omitted all models in the project will be generated. Only full name
  matched are supported and no RegEx or partial name matching.
* `macros` - optional list of path macros. The notation is `new Macro("name", "value")`.
* `projectLocation` - location of the MPS project to generate.
* `debug` - optionally allows to start the JVM that is used to generated with a debugger. Setting it to `true` will cause
  the started JVM to suspend until a debugger is attached. Useful for debugging classloading problems or exceptions during
  the build.

## Model Check

Run the model check on a subset or all models in a project directly from gradle.

This functionality currently runs all model checks (typesystem, structure, constrains, etc.) from gralde. By default if
any of checks fails the complete build is failed. All messages (Info, Warning or Error) are reported through log4j to
the command line.

### Usage

A minimal build script to check all models in a MPS project with no external plugins would look like this: 

```
apply plugin: 'modelcheck'

configurations {
    mps
}

dependencies {
    mps "com.jetbrains:mps:$mpsVersion"
}

ext.mpsVersion = '2018.2.5'

modelcheck {
    projectLocation = new File("./mps-prj")
    mpsConfig = configurations.mps
}
```

Parameters:
* `mpsConfig` - the configuration used to resolve MPS. Currently only vanilla MPS is supported and no custom RCPs.
  Custom plugins are supported via the `pluginLocation` parameter.
* `mpsLocation` - optional location where to place the MPS files.
* `plugins` - optional list of plugins to load before generation is attempted.
  The notation is `new Plugin("someID", "somePath")`. Where the first parameter is the plugin id and the second the `short (folder) name`.
* `pluginLocation` - location where to load the plugins from. Structure needs to be a flat folder structure similar to the
  `plugins` directory inside of the MPS installation.
* `models` - optional list of models to generate. If omitted all models in the project will be generated. Only full name
  matched are supported and no RegEx or partial name matching.
* `macros` - optional list of path macros. The notation is `new Macro("name", "value")`.
* `projectLocation` - location of the MPS project to generate.
* `errorNoFail` - report errors but do not fail the build.
* `warningsAsError` - handles warnings as errors and will fail the build if any is found when `errorNoFail` is not set. 
* `debug` - optionally allows to start the JVM that is used to generated with a debugger. Setting it to `true` will cause
  the started JVM to suspend until a debugger is attached. Useful for debugging classloading problems or exceptions during
  the build.
  
### Additional Plugins 

By default only the minimum required set of plugins are loaded. This includes base language and some utilities like the
HTTP server from MPS. If your project requires additional plugins to be loaded this is done by setting plugin location 
to the place where your jar files are placed and adding your plugin id and folder name to the `plugins` list: 

```
apply plugin: 'modelcheck'
...

modelcheck {
    pluginLocation = new File("path/to/my/plugins")
    plugins = [new Plugin("com.mbeddr.core", "mbeddr.core")]
    projectLocation = new File("./mps-prj")
    mpsConfig = configurations.mps
}

```

Dependencies of the specified plugins are automatically loaded from the `pluginlocation` and the plugins directory of 
MPS. If they are not found the the build will fail.