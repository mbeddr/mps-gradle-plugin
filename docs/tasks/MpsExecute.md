## `MpsExecute` Task Type

A custom task to execute a specified method in a generated class in the context of a running MPS instance with an open
project.

### Usage

```groovy
import de.itemis.mps.gradle.tasks.MpsExecute

plugins {
    // Required in order to use the MpsExecute task
    id("de.itemis.mps.gradle.common")
}

tasks.register('executeMyTask', MpsExecute) {
    mpsHome = file("...") // MPS home directory

    module = "my.module"
    className = "my.module.GeneratedClass"
    method = "myMethod"
    methodArguments = ["arg1", "arg2"]
}
```

Parameters:

* `projectLocation` - the location of the MPS project. Default is the Gradle project directory.
* `additionalExecuteBackendClasspath` - any extra libraries that should be on the classpath of the execute backend.
* `folderMacros` - folder/path macros that are necessary to open the project.
* `mpsHome` - the home directory of the MPS distribution (or RCP) to use for testing.
* `mpsVersion` - the MPS version, such as "2021.3". Default is autodetection by reading `$mpsHome/build.properties`.
* `pluginRoots` - directories that will be searched (recursively) for additional plugins to load.
* `module` - the module that contains the generated class.
* `className` - fully qualified name of the generated class, that contains the method to execute.
* `method` - name of the method. The method should be public and static. Supported signatures are `(Project)` (from
  `jetbrains.mps.project` model) or `(Project, String[])`.
* `methodArguments` - list of arguments to pass to the method. Default is an empty list. If arguments are provided the
  method signature must be `(Project, String[])`.
