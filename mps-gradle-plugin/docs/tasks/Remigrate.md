## `Remigrate` Task Type

Execute re-runnable migrations and project migrations on a project or several projects.

### Usage

```groovy
import de.itemis.mps.gradle.tasks.Remigrate

plugins {
    // Required in order to use the task
    id("de.itemis.mps.gradle.common")
}

tasks.register('remigrate', Remigrate) {
    mpsHome = mpsHomeDir

    // Remigrate task can run migrations on multiple projects
    projectLocations.from(projectDir1)
    projectLocations.from(projectDir2)
}
```

Parameters:

* `mpsHome` - the home directory of the MPS distribution (or RCP) to use for testing.
* `mpsVersion` - the MPS version, such as "2021.3". Autodetected by reading `$mpsHome/build.properties` by default.
* `projectLocation` or `projectLocations` - the project or projects to migrate. The properties are mutually exclusive,
  exactly one should be set.
* `folderMacros` - path variables/macros that are necessary to open the project. Keys are macro names, values are
  directories. Path macros are not considered part of Gradle build cache key.
* `pluginRoots` - directories that will be searched (recursively) for additional plugins to load.

### Operation

The task will execute re-runnable migrations and project migrations on the specified projects.
