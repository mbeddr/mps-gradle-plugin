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
    projectDirectories.from(projectDir)

    ...
}
```

Parameters:

* `mpsHome` - the home directory of the MPS distribution (or RCP) to use for testing.
* `mpsVersion` - the MPS version, such as "2021.3". Autodetected by reading `$mpsHome/build.properties` by default.
* `haltOnPrecheckFailure` - fail if the migration pre-check (e.g. broken references) fails.
* `haltOnDependencyError` - fail if non-migrated dependencies are found.
* `projectDirectories` - project directories to migrate.
* `folderMacros` - path variables/macros that are necessary to open the project. Path macros are not considered part of
  Gradle build cache key.
* `pluginRoots` - directories that will be searched (recursively) for additional plugins to load.

## Run migrations

Run all pending migrations in the project.

### Usage

A minimal build script to check all models in an MPS project with no external plugins would look like this:

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
* `mpsConfig` - the configuration used to resolve MPS.
* `mpsLocation` - optional location where to place the MPS files if `mpsConfig` is specified, or where to take them from
  otherwise.
* `mpsVersion` - optionally overrides automated version detection from `mpsConfig`. Required if you use
  a [custom distribution](../notes/custom-mps-distribution.md) of MPS.
* `projectLocation` - location of the project that should be migrated.
* `force` - ignores the marker files for projects which allow pending migrations, migrate them anyway. Supported in 2021.3.0 and higher.
* `haltOnPrecheckFailure` - controls whether migration is aborted if pre-checks fail (except the check for migrated dependencies) Default: `true`. Supported in 2021.1 and higher.
* `haltOnDependencyError` - controls whether migration is aborted when non-migrated dependencies are discovered. Default: `true`. Supported in 2021.3.4 and 2023.2 and higher.
* `maxHeap` (since 1.15) - maximum heap size setting for the JVM that executes the migrations. The value is a string
  understood by the JVM command line argument `-Xmx` e.g. `3G` or `512M`.

At least `mpsConfig` or `mpsLocation` + `mpsVersion` must be set.
