## `MpsMigrate` Task Type

Migrates the specified projects.

### Usage

```groovy
import de.itemis.mps.gradle.tasks.MpsMigrate

plugins {
    // Required in order to use the task
    id("de.itemis.mps.gradle.common")
}

tasks.register('migrate', MpsMigrate) {
    mpsHome = mpsHomeDir

    // MpsMigrate task can migrate multiple projects at once
    projectLocations.from(projectDir)

    ...
}
```

Parameters:

* `mpsHome` - the home directory of the MPS distribution (or RCP) to use for testing.
* `mpsVersion` - the MPS version, such as "2021.3". Autodetected by reading `$mpsHome/build.properties` by default.
* `haltOnPrecheckFailure` - fail if the migration pre-check (e.g. broken references) fails.
* `haltOnDependencyError` - fail if non-migrated dependencies are found.
* `projectLocation` or `projectLocations` - the project or projects to migrate. The properties are mutually exclusive,
  exactly one should be set.
* `folderMacros` - path variables/macros that are necessary to open the project. Keys are macro names, values are
  directories. Path macros are not considered part of Gradle build cache key.
* `pluginRoots` - directories that will be searched (recursively) for additional plugins to load.
* `maxHeapSize` - maximum heap size setting for the JVM that executes the migrations. The value is a string
  understood by the JVM command line argument `-Xmx` e.g. `3G` or `512M`.
