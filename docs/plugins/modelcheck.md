## `checkModels`

Run the model check on a subset or all models in a project directly from gradle.

This functionality currently runs all model checks (typesystem, structure, constrains, etc.) from Gradle. By default, if
any of checks fails, the complete build is failed. All messages (Info, Warning or Error) are reported through log4j to
the command line.

### Usage

A minimal build script to check all models in an MPS project with no external plugins would look like this:

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
* `mpsConfig` - the configuration used to resolve MPS. Custom plugins are supported via the `pluginLocation` parameter.
* `mpsLocation` - optional location where to place the MPS files if `mpsConfig` is specified, or where to take them from
  otherwise.
* `mpsVersion` - optionally overrides automated version detection from `mpsConfig`. Required if you use
  a [custom distribution](../notes/custom-mps-distribution.md) of MPS.
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
* `junitFile` - allows storing the results of the model check as a JUnit XML file. By default, the file will contain one
  testcase for each model that was checked (s. `junitFormat`).
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
* `maxHeap` - maximum heap size setting for the JVM that executes the model checker. This is useful to limit the heap usage
  in scenarios like containerized build agents where the OS reported memory limit is not the maximum
  to be consumed by the container. The value is a string understood by the JVM command line argument `-Xmx` e.g. `3G` or `512M`.
* `backendConfig` - optional configuration providing the backend. If not given, the `modelcheck` backend from
  [mps-build-backends](https://github.com/mbeddr/mps-build-backends) will be used.
* `environmentKind` - optional kind of environment (MPS or IDEA) to execute the generators in. IDEA environment is used
  by default for backwards compatibility but MPS environment may be faster. See [MPS vs IDEA environment](../notes/mps-vs-idea-environment.md).

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
