## `MpsCheck` Task Type

This task improves over the [`modelcheck` plugin](../plugins/modelcheck.md) and fixes some of its deficiencies.

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
* `parallel` (since 1.20) - runs model checker in parallel mode. Supported in MPS 2021.3.4. Default is `false`.
* `mpsHome` - the home directory of the MPS distribution (or RCP) to use for testing.
* `mpsVersion` - the MPS version, such as "2021.3". Autodetected by reading `$mpsHome/build.properties` by default.
* `pluginRoots` - directories containing additional plugins to load
* `warningAsError` - whether to treat warnings as errors.
* `ignoreFailures` (inherited from `VerificationTask`) - whether to fail the build if an error is found.

Compatibility note: `MpsCheck` task currently extends `JavaExec` but this may change in the future. Do not rely on this.
