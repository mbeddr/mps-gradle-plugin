# mps-gradle-plugin

Miscellaneous tasks that were found useful when building MPS-based
projects with Gradle.

**Table of Contents**

<!-- TOC -->
* [mps-gradle-plugin](#mps-gradle-plugin)
* [Configuring the plugin repository](#configuring-the-plugin-repository)
* [Custom tasks](#custom-tasks)
  * [RunAntScript](#runantscript)
    * [Usage](#usage-)
    * [Providing Global Defaults For Class Path And Arguments](#providing-global-defaults-for-class-path-and-arguments)
    * [Providing Global Defaults For The Java Executable](#providing-global-defaults-for-the-java-executable)
    * [Incremental Builds](#incremental-builds)
  * [CreateDmg](#createdmg)
    * [Usage](#usage)
    * [Operation](#operation)
  * [BundleMacosJdk](#bundlemacosjdk)
    * [Usage](#usage-1)
    * [Operation](#operation-1)
  * [GenerateLibrariesXml](#generatelibrariesxml)
    * [Usage](#usage-2)
    * [Operation](#operation-2)
  * [`generate`](#generate)
    * [Usage](#usage-3)
  * [`checkModels`](#checkmodels)
    * [Usage](#usage-4)
    * [Additional Plugins](#additional-plugins)
  * [`MpsCheck` Task Type](#mpscheck-task-type)
    * [Usage](#usage-5)
  * [Run migrations](#run-migrations)
    * [Usage](#usage-6)
  * [Download JetBrains Runtime](#download-jetbrains-runtime)
    * [Usage](#usage-7)
    * [Parameters](#parameters)
  * [Custom MPS Distribution](#custom-mps-distribution)
  * [MPS vs IDEA environment](#mps-vs-idea-environment)
<!-- TOC -->

# Configuring the plugin repository

This plugin is not published to the Gradle plugin portal but to a public repository of itemis. To configure this
repository add the following at the beginning of your `settings.gradle`:

```groovy
pluginManagement {
    repositories {
        maven { url 'https://artifacts.itemis.cloud/repository/maven-mps' }
      
        // Need to manually include the default Gradle plugin portal repository when overriding the defaults.
        gradlePluginPortal()
    }
}
```

# Custom tasks

To make use of custom task types, add the following `plugins` block to your build script:

```
plugins {
    id 'de.itemis.mps.gradle.common' version '1.13.+'
}
```

Use a fully specified version such as `1.0.123` for better build reproducibility.

The `de.itemis.mps.gradle.common` plugin is empty and does not do anything to your project by itself. However, applying
it causes Gradle to put the included task classes (described below) onto the classpath of the build script.

## RunAntScript

This task is the base for other tasks that run MPS-generated Ant scripts (`BuildLanguages`, `TestLanguages`).

The custom tasks are useful when you don't check in the build scripts generated by MPS into source control but want to
generate them during the Gradle build. In that case you can't use the Ant integration of Gradle to run these files
because they may not exist yet when the build is started.

### Usage 

Parameters:

- `script`: path to the ANT to execute
- `scriptClasspath`: classpath used for the JVM that will execute the generated ANT script. Needs to contain ANT to be 
  able to run the build script. See below section "Providing Global Defaults" for project wide defaults.
- `scriptArgs`: additional command line arguments provided to the JVM that will execute the generated ANT scripts. This
  is often used to provide property valued via "-Dprop=value". See below section "Providing Global Defaults" for project wide defaults.
- `executable`: the `java` executable to use. Optional. If `itemis.mps.gradle.ant.defaultJavaExecutable` extended
  property is set, its value is used as the default value for the parameter.
- `includeDefaultArgs`: controls whether the project-wide default values for arguments are used. 
  It's set to `true` by default.
- `includeDefaultClasspath`: controls whether the project-wide default values for the classpath are used. 
  It's set to `true` by default.
- `targets`: the targets to execute of the ANT files.
- `incremental`: enable incremental build, see below. (Since 1.6.)

### Providing Global Defaults For Class Path And Arguments

All tasks derived from the `RunAntScript` base class allow to specify default values for the classpath and script arguments
via project properties. By default these values are added to the value specified for the parameters `scriptArgs` and 
`scriptClasspath` if they are present. To opt out from the defaults see above the parameters `includeDefaultArgs` and 
`includeDefaultClasspath`.

The property `itemis.mps.gradle.ant.defaultScriptArgs` controls the default arguments provided to the build scripts 
execution. In belows example the default arguments contain the version and build date. At runtime the default arguments
are combined with the arguments defined via `scriptArgs`. 

The property `itemis.mps.gradle.ant.defaultScriptClasspath` controls the default classpath provided to the build scripts
execution. In belows example the classpath contains ANT (via dependency configuration) and the tools jar from the JDK.
At runtime the default classpath are combined with the classpath defined via `scriptClasspath`.  
```
def defaultScriptArgs = ["-Dversion=$version", "-DbuildDate=${new Date().toString()}"]
def buildScriptClasspath = project.configurations.ant_lib.fileCollection({true}) + project.files("$project.jdk_home/lib/tools.jar")

ext["itemis.mps.gradle.ant.defaultScriptArgs"] = defaultScriptArgs
ext["itemis.mps.gradle.ant.defaultScriptClasspath"] = buildScriptClasspath
```

### Providing Global Defaults For The Java Executable

The `itemis.mps.gradle.ant.defaultJavaExecutable` property specifies the value to use as the underlying
`JavaExec.executable`. The `executable` parameter of each individual task takes precedence over the global default.

### Incremental Builds

Incremental builds can be enabled by setting the `incremental` property to `true`. This has the following effects:
* The `clean` target is removed from the `targets` list.
* Argument `-Dmps.generator.skipUnmodifiedModels=true` is passed to Ant. This property tells the MPS generator to skip 
  generating and compiling models that have not been modified.

NOTE: While incremental builds are convenient, it is necessary to be aware of their limitations. To determine whether
a model should be regenerated the generator only looks at the hash of the model contents. If the contents have not
changed since the last generation the generation is skipped. This may not be fully correct in the general case. Changing
the generator of a language used by the model may affect the generated code for the model, for example. Changes in
imported models may affect the generation output of this model as well. None of these changes would be detected via the
model contents hash.

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

## `generate`

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

ext.mpsVersion = '2018.3.6'

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
* `mpsVersion` - optional if you use a [custom distribution](#Custom MPS Distribution) of MPS
* `javaExec` - optional `java` executable to use.
* `pluginLocation` - location where to load the plugins from. Structure needs to be a flat folder structure similar to the
  `plugins` directory inside of the MPS installation.
* `plugins` - deprecated, use `pluginsProperty`.
* `pluginsProperty` - optional list of plugins to load before generation is attempted.
  The notation is `new Plugin("pluginID", "somePath")`. The first parameter is the plugin id.
  For the second parameter `"somePath"` there are several options:
  * if it's an absolute path, the plugin is loaded from that path
  * if it's a folder located under `pluginLocation` the plugin is loaded from that folder
  * otherwise it should be a plugin folder located under the default `mps/plugins` 
* `models` - optional list of models to generate. If omitted all models in the project will be generated. Only full name
  matched are supported and no RegEx or partial name matching.
* `excludeModels` (since 1.14) - optional list of models to exclude from generating. RegEx can be used for matching multiple models.
* `modules` (since 1.14) - optional list of modules to generate. Expects ordinary name (w/o virtual folders). RegEx can be used for matching multiple modules.
  If both parameters, `models` and `modules`, are omitted - all models in the project will be generated, except as
  excluded by `excludeModels` and `excludeModules`.
* `excludeModules` (since 1.14) - optional list of modules to exclude from generate. RegEx can be used for matching multiple modules.
* `macros` - optional list of path macros. The notation is `new Macro("name", "value")`.
* `projectLocation` - location of the MPS project to generate.
* `parallelGenerationThreads` (since 1.17) - optional number of threads to use for parallel generation. Defaults to `0`, 
  which means that parallel generation is turned off.
* `debug` - optionally allows to start the JVM that is used to generated with a debugger. Setting it to `true` will cause
  the started JVM to suspend until a debugger is attached. Useful for debugging classloading problems or exceptions during
  the build.
* `backendConfig` - optional configuration providing the backend. If not given, the `execute-generators` backend from
  [mps-build-backends](https://github.com/mbeddr/mps-build-backends) will be used.
* `environmentKind` - optional kind of environment (MPS or IDEA) to execute the generators in. IDEA environment is used
  by default for backwards compatibility but MPS environment may be faster. See
  [MPS vs IDEA environment](#mps-vs-idea-environment) below.
* `maxHeap` (since 1.15) - maximum heap size setting for the JVM that executes the generator. The value is a string
  understood by the JVM command line argument `-Xmx` e.g. `3G` or `512M`.

## `checkModels`

Run the model check on a subset or all models in a project directly from gradle.

This functionality currently runs all model checks (typesystem, structure, constrains, etc.) from Gradle. By default, if
any of checks fails, the complete build is failed. All messages (Info, Warning or Error) are reported through log4j to
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

ext.mpsVersion = '2018.3.6'

modelcheck {
    projectLocation = new File("./mps-prj")
    mpsConfig = configurations.mps
    macros = [Macro("mypath", "/your/path")]
}
```

Parameters:
* `mpsConfig` - the configuration used to resolve MPS. Currently only vanilla MPS is supported and no custom RCPs.
  Custom plugins are supported via the `pluginLocation` parameter.
* `mpsLocation` - optional location where to place the MPS files.
* `mpsVersion` - optional if you use a [custom distribution](#Custom MPS Distribution) of MPS
* `javaExec` - optional `java` executable to use.
* `pluginLocation` - location where to load the plugins from. Structure needs to be a flat folder structure similar to the
  `plugins` directory inside of the MPS installation.
* `plugins` - deprecated, use `pluginsProperty`. 
* `pluginsProperty` - optional list of plugins to load before generation is attempted.
  The notation is `new Plugin("pluginID", "somePath")`. The first parameter is the plugin id.
  For the second parameter `"somePath"` there are several options:
  * if it's an absolute path, the plugin is loaded from that path
  * if it's a folder located under `pluginLocation` the plugin is loaded from that folder
  * otherwise it should be a plugin folder located under the default `mps/plugins` 
* `models` - optional list of models to check. RegEx can be used for matching multiple models.
* `excludeModels` - optional list of models to exclude from checking. RegEx can be used for matching multiple models.
* `modules` - optional list of modules to check. Expects ordinary name (w/o virtual folders). RegEx can be used for matching multiple modules.
  If both parameters, `models` and `modules`, are omitted - all models in the project will be checked, except as
  excluded by `excludeModels` and `excludeModules`.
* `excludeModules` - optional list of modules to exclude from checking. RegEx can be used for matching multiple modules.
* `macros` - optional list of path macros. The notation is `new Macro("name", "value")`.
* `projectLocation` - location of the MPS project to check.
* `errorNoFail` - report errors but do not fail the build.
* `warningAsError` - handles warnings as errors and will fail the build if any is found when `errorNoFail` is not set. 
* `debug` - optionally allows to start the JVM that is used to load MPS project with a debugger. Setting it to `true` will cause
  the started JVM to suspend until a debugger is attached. Useful for debugging classloading problems or exceptions during
  the build.
* `junitFile` - allows storing the the results of the model check as a JUnit XML file. By default, the file will contain one
  testcase for each model that was checked (s. `junitFormat`).
* `junitFormat` - allows to change the format of the JUnit XML file, how the model checking errors will be reported. Possible options:
  * `model` (default) - generates one testcase for each model that was checked. If the model check reported any error for the model, 
    the testcase will fail and the message of the model checking error will be reported.
  * `module-and-model` - generates one testcase per module and tested model. The report also contains module-level errors in contrast to 
    the `model` format.
  * `message` - generates one testcase for each model check error. For uniqueness reasons, the name of the testcase will reflect the specific
    model check error and the name of the testclass will be constructed from the checked node ID and its containing root node. 
    Full error message and the node URL will be reported in the testcase failure. Checked models will be mapped to testsuites with this option.
* `junitReportAllErrors` - whether to include in the JUnit XML file the errors from outside the check scope. Defaults to `false`.
* `maxHeap` - maximum heap size setting for the JVM that executes the modelchecker. This is useful to limit the heap usage
  in scenarios like containerized build agents where the OS reported memory limit is not the maximum
  to be consumed by the container. The value is a string understood by the JVM command line argument `-Xmx` e.g. `3G` or `512M`.
* `backendConfig` - optional configuration providing the backend. If not given, the `modelcheck` backend from
  [mps-build-backends](https://github.com/mbeddr/mps-build-backends) will be used.
* `environmentKind` - optional kind of environment (MPS or IDEA) to execute the generators in. IDEA environment is used
  by default for backwards compatibility but MPS environment may be faster. See
  [MPS vs IDEA environment](#mps-vs-idea-environment) below.

### Additional Plugins

By default, only the minimum required set of plugins is loaded. This includes base language and some utilities like the
HTTP server from MPS. If your project requires additional plugins to be loaded this is done by setting plugin location
to the place where your jar files are placed and adding your plugin id and folder name to the `plugins` list:

```
apply plugin: 'modelcheck'
...

modelcheck {
    pluginLocation = new File("path/to/my/plugins")
    pluginsProperty = [new Plugin("com.mbeddr.core", "mbeddr.core")]
    projectLocation = new File("./mps-prj")
    mpsConfig = configurations.mps
}

```

Dependencies of the specified plugins are automatically loaded from the `pluginLocation` and the plugins directory of 
MPS. If they are not found the build will fail.

## `MpsCheck` Task Type

This task improves over the `modelcheck` plugin described above and fixes some of its deficiencies.

The `modelcheck` extension provided by the eponymous plugin can only be configured once per Gradle project. Checking
multiple subprojects is not possible without resorting to tricks. In addition, the extension only has limited support
for lazy configuration and does not support Gradle build cache.

The `MpsCheck` task works similarly to the `checkmodels` task of `modelcheck` plugin but allows defining multiple
instances of itself, supports lazy configuration and caching.

### Usage

```groovy
import de.itemis.mps.gradle.tasks.MpsCheck

plugins {
    // Required in order to use the MpsCheck task
    id("de.itemis.mps.gradle.common")
}

tasks.register('checkProject', MpsCheck) {
    mpsHome = file("...") // MPS home directory
    projectLocation = projectDir
}
```

Parameters:

* `projectLocation` - the location of the project to check. Default is the Gradle project directory.
* `models`, `modules`, `excludeModels`, `excludeModules` - regular expressions. Matching modules and models will be
  included or excluded from checking.
* `additionalModelcheckBackendClasspath` - any extra libraries that should be on the classpath of the modelcheck
  backend.
* `folderMacros` - path variables/macros that are necessary to open the project. Path macros are not considered part of
  Gradle build cache key.
* `varMacros` - non-path variables/macros that are necessary to open the project. Variable macros *are* considered part
  of Gradle build cache key.
* `junitFile` - the JUnit XML file to produce. Defaults to `$buildDir/TEST-${task.name}.xml`
* `junitFormat` - the format of the JUnit XML file. Defaults to `module-and-model`.
* `junitReportAllErrors` - whether to include errors from outside the check scope in the JUnit XML file. Can be useful
  when checking linters that report errors from a different model. Defaults to `false`.
* `mpsHome` - the home directory of the MPS distribution (or RCP) to use for testing.
* `mpsVersion` - the MPS version, such as "2021.3". Autodetected by reading `$mpsHome/build.properties` by default.
* `pluginRoots` - directories containing additional plugins to load
* `warningAsError` - whether to treat warnings as errors.
* `ignoreFailures` (inherited from `VerificationTask`) - whether to fail the build if an error is found.

Compatibility note: `MpsCheck` task currently extends `JavaExec` but this may change in the future. Do not rely on this.

## Run migrations

Run all pending migrations in the project.

### Usage

A minimal build script to check all models in a MPS project with no external plugins would look like this:

```
apply plugin: 'run-migrations"'

configurations {
    mps
}

dependencies {
    mps "com.jetbrains:mps:$mpsVersion"
}

runMigrations {
    projectLocation = new File("./mps-prj")
    mpsConfig = configurations.mps
}
```

Parameters:
* `mpsConfig` - configuration used to resolve MPS.
* `mpsLocation` - location where to place the MPS files.
* `mpsVersion` - if you use a [custom distribution](#custom-mps-distribution) of MPS.
* `projectLocation` - location of the project that should be migrated.
* `force` - ignores the marker files for projects which allow pending migrations, migrate them anyway. Supported in 2021.3.0 and higher.
* `haltOnPrecheckFailure` - controls whether migration is aborted if pre-checks fail (except the check for migrated dependecies) Default: `true`. Supported in 2021.1 and higher. 
* `haltOnDependencyError` - controls whether migration is aborted when non-migrated dependencies are discovered. Default: `true`. Supported in 2021.3.4 and 2023.2 and higher.
* `maxHeap` (since 1.15) - maximum heap size setting for the JVM that executes the migrations. The value is a string
  understood by the JVM command line argument `-Xmx` e.g. `3G` or `512M`.

At least `mpsConfig` or `mpsLocation` + `mpsVersion` must be set.

## Download JetBrains Runtime

When building MPS projects with the JatBrains Runtime, the JDK/JRE used by MPS and other intellij based IDEs, it's
required to download the correct version of the runtime. Since the runtime is platform dependent it's required to 
download a platform dependent binary. While it's possible to add the logic to your own build script we provide a convenient
way of doing this with a Gradle plugin. 

The download-jbr plugin will add new dependencies and a task to your build. It will add a dependency to `com.jetbrains.jdk:jbr`
to your build, you need to make sure that it is available in your dependency repositories. The itemis Maven repository at 
`https://artifacts.itemis.cloud/repository/maven-mps` provides this dependency, but you can create your own with
the scripts located in mbeddr/build.publish.jdk 

For easy consumption and incremental build support the plugin creates a task `downloadJbr` which exposes the location of 
the java executable via the `javaExecutable` property. See
[the tests](src/test/kotlin/test/de/itemis/mps/gradle/JBRDownloadTest.kt) for an example of how to use it. 

### Usage


Kotlin: 
```
plugins {
    id("download-jbr")
}

repositories {
    mavenCentral()
    maven {
        url = URI("https://artifacts.itemis.cloud/repository/maven-mps")
    }
}

downloadJbr {
    jbrVersion = "11_0_6-b520.66"
}
```

Groovy: 
```
apply plugin: 'download-jbr'
...

repositories {
    maven { url 'https://artifacts.itemis.cloud/repository/maven-mps' }
    mavenCentral()
}

downloadJbr {
    jbrVersion = '11_0_6-b520.66'
}
```

### Parameters
* `jbrVersion` - version of the JBR to download. While this supports maven version selectors we highly recomment not
  using wildcards like `*` or `+` in there for reproducible builds. 
* `distributionType` - optional distribution type for the JBR to use. Will default to `jbr_jcef` if omitted. 
* `downloadDir` - optional directory where the downloaded JBR is downloaded and extracted to. The plugin defaults to
  `build/jbrDownload`. 

## Custom MPS Distribution

Features that perform an action inside an MPS project, like the `modelcheck` or `generate-models` plugin, require 
an MPS available to them. While for vanilla MPS it is enough to pass in a  reference to the MPS dependency via the
`mpsConfig` property this doesn't work for custom distributions of MPS. A custom distribution of MPS is also called 
a MPS RCP. If you like to use your own MPS distribution with preinstalled plugins and your own versioning scheme 
then this is possible but requires additional steps in the build script. 

When you are using a custom distribution of MPS you can no longer use the `mpsConfig` property and rely on 
the plugin resolving it. The plugin needs to be configured with the properties `mpsVersion` and `mpsLocation`
being set and no value set for `mpsConfig`. If you set `mpsVersion` but also set `mpsConfig` then `mpsConfig` 
will take precedence over `mpsVersion` and the plugin will resolve that configuration into `mpsLocation`. 

`mpsVersion` needs to be set to the exact MPS version your custom distribution is based on e.g. if you build a
RCP with MPS 2020.3.3 you need to set this property to `2020.3.3`. `mpsLocation` needs to point to the location
where you extracted your custom MPS distribution into e.g. `$buildDir/myAwesomeMPS` if you extracted into that location. 

Each of the plugins creates a `resolveMpsFor<name>` task in the build. When `mpsVersion` and `mpsLocation` are set
this task is still present in the task graph but becomes a noop. The task is present to be able to add your own task(s)
as dependency to it. This is useful for extracting your custom distribution before its being used. A minimal example
could look like this: 

```
def myCustomLocation = "$buildDir/myAwesomeMPS"

task downloadAndExtractCustomMPS() {
    // your logic to download and extract here
}

modelcheck {
    mpsLocation = myCustomLocation
    mpsVersion = "2020.3.3"
    projectLocation = file("$rootDir/mps-prj")
    modules = ["my.solution.with.errors"]
    junitFile = file("$buildDir/TEST-modelcheck-results.xml")
}

tasks.getByName("resolveMpsForModelcheck").dependsOn(downloadAndExtractCustomMPS)
```

## MPS vs IDEA environment

Since plugin version 1.10, the `environmentKind` parameter allows to choose between MPS or IDEA environment for
generation or model check. The default is to use the IDEA environment because that was the case in earlier versions, but
the MPS environment is lighter and likely to be more performant. On the other hand, the MPS environment does not load
plugins (only extensions) and may lead to different results.
