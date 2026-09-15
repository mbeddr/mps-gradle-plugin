## `MpsCheck` Task Type

Run the model check on a subset or all models in a project.

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
* `folderMacros` - path variables/macros that are necessary to open the project. Keys are macro names, values are
  directories. Path macros are not considered part of Gradle build cache key.
* `junitFile` - the JUnit XML file to produce. Defaults to `$buildDir/TEST-${task.name}.xml`
* `junitFormat` - specifies how errors are reported in the JUnit XML file. Possible options:
  * `model` (default, deprecated) - generates one test case for each model that was checked. If the model check reported
    any error for the model, the test case will contain a failure with the error message.
  * `module-and-model` (preferred) - generates one test case for each module and model that was checked. If the model
    check reported any error for the model or module, the test case will contain a failure with the error message.
  * `message` - generates one testcase for each model check error. For uniqueness reasons, the name of the testcase will
    reflect the specific model check error and the name of the test class will be constructed from the checked node ID
    and its containing root node. Full error message and the node URL will be reported in the testcase failure. Checked
    models will be mapped to test suites with this option.
* `parallel` (since 1.20) - runs model checker in parallel mode. Supported in MPS 2021.3.4. Default is `false`.
* `mpsHome` - the home directory of the MPS distribution (or RCP) to use for testing.
* `mpsVersion` - the MPS version, such as "2021.3". Autodetected by reading `$mpsHome/build.properties` by default.
* `pluginRoots` - directories containing additional plugins to load
* `warningAsError` - whether to treat warnings as errors.
* `ignoreFailures` (inherited from `VerificationTask`) - whether to fail the build if an error is found.

Compatibility note: `MpsCheck` task currently extends `JavaExec` but this may change in the future. Do not rely on this.
