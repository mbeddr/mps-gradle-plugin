## `MpsGenerate` Task Type

This task improves over the [`generate` plugin](../plugins/generate.md) above and fixes some of its deficiencies.

The `generate` extension provided by the eponymous plugin can only be configured once per Gradle project. Generating
multiple subprojects or multiple sets of models is not possible without resorting to tricks. In addition, the extension
only has limited support for lazy configuration and does not support Gradle build cache.

The `MpsGenerate` task works similarly to the `generate` task created by the `generate` plugin but allows defining
multiple instances of itself, supports lazy configuration and caching.

### Usage

```groovy
import de.itemis.mps.gradle.tasks.MpsGenerate

plugins {
    // Required in order to use the task
    id("de.itemis.mps.gradle.common")
}

tasks.register('generateProject', MpsGenerate) {
    mpsHome = file("...") // MPS home directory
    projectLocation = projectDir
}
```

Parameters:

* `projectLocation` - the location of the project to generate. Default is the Gradle project directory.
* `models`, `modules`, `excludeModels`, `excludeModules` - regular expressions. Matching modules and models will be
  included or excluded from generation.
* `additionalGenerateBackendClasspath` - any extra libraries that should be on the classpath of the generate
  backend.
* `folderMacros` - path variables/macros that are necessary to open the project. Path macros are not considered part of
  Gradle build cache key.
* `varMacros` - Deprecated and will be removed (only folder macros are supported by MPS).
* `mpsHome` - the home directory of the MPS distribution (or RCP) to use for testing.
* `mpsVersion` - the MPS version, such as "2021.3". Autodetected by reading `$mpsHome/build.properties` by default.
* `pluginRoots` - directories that will be searched (recursively) for additional plugins to load.
* `environmentKind` - the environment to use for generation (`MPS` or `IDEA`). MPS environment is used by default,
  matching the standard `<generate>` Ant task. IDEA environment can be used when generating screenshots.
  See [MPS vs IDEA environment](../notes/mps-vs-idea-environment.md).
* `strictMode` - whether to use strict mode when generating. Enabled by default.
* `parallelGenerationThreads` - if set to higher than 0, parallel generation will be enabled with the specified number
  of threads.

Compatibility note: `MpsGenerate` task currently extends `JavaExec` but this may change in the future. Do not rely on
this.
