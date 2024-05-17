## `generate`

Generate a specific or all models in a project without the need for an MPS model.

While technically possible generating languages with this task makes little sense as there is no way of packaging the
generated artifacts into JAR files. We only recommend using this for simple tasks where user defined models should be
generated in the CI build or from the commandline.

### Usage

A minimal build script to generate an MPS project with no external plugins would look like this:

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
  by default for backwards compatibility but MPS environment may be faster. See [MPS vs IDEA environment](../notes/mps-vs-idea-environment.md).
* `maxHeap` (since 1.15) - maximum heap size setting for the JVM that executes the generator. The value is a string
  understood by the JVM command line argument `-Xmx` e.g. `3G` or `512M`.
