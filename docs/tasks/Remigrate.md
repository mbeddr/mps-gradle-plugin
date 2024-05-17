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
    projectDirectories.from(projectDir1)
    projectDirectories.from(projectDir2)
}
```

Parameters:

* `mpsHome` - the home directory of the MPS distribution (or RCP) to use for testing.
* `mpsVersion` - the MPS version, such as "2021.3". Autodetected by reading `$mpsHome/build.properties` by default.
* `projectDirectories` - project directories to migrate.
* `folderMacros` - path variables/macros that are necessary to open the project. Path macros are not considered part of
  Gradle build cache key.
* `pluginRoots` - directories that will be searched (recursively) for additional plugins to load.

### Operation

The task will execute re-runnable migrations and project migrations on the specified projects.
