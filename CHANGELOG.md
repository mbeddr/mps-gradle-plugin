# Changelog
The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## 1.25.2

### Fixed

- `runMigrations`: existing `debug` parameter is used to start migration process in debug mode (suspended and listening 
  on port 5005).

## 1.25.1

### Fixed

- `Remigrate`: change classpath construction to fix Kotlin library incompatibilities with latest MPS.

## 1.25.0

### Added

- `Remigrate` task type to run re-runnable migrations on a project or several projects.

### Changed

- Extensions now use `--plugin-root` command line argument of backends, requiring backend versions 1.15.0 or above.

## 1.24.0

### Changed

- Extensions (`generate`, `modelcheck`, `runMigrations`) will use `mpsVersion` if specified, instead of relying on
  auto-detection logic. This makes it possible to use extensions with a non-standard MPS dependency (such as a
  pre-release).
- Setting `mpsVersion` but not `mpsLocation` was not supported previously but is supported now. The default location
  (`$buildDir/mps`) will be used and `mpsConfig` will be unpacked there. At least one of `mpsLocation` and `mpsConfig`
  must still be specified.
- Extension-related error messages now include the name of the extension.

## 1.23.1

### Fixed

- `MpsMigrate` and `runMigrations` tasks will clean their temporary directory (where MPS caches and the generated Ant
  script are located) before each run. This helps avoid complaints by MPS that files have been modified externally.

## 1.23.0

### Added

- `MpsMigrate` task for migrating a project (or projects), similar to the `runMigrations` extension.

## 1.22.2

### Fixed

- Minor fix in the computation of `MpsCheck` input files.

## 1.22.1

### Fixed

- `MpsGenerate` task is placed into "generation" group by default, rather than (incorrect) "verification".

## 1.22.0

### Added

- `MpsGenerate` custom task to run the MPS make. In contrast to the `generate` plugin there can be multiple instances
  of the task, and the task is written with the current Gradle best practices (lazy properties, up-to-date checking).
  In order to properly support up-to-date checking and caching, task outputs have to be specified (see
  [Incremental build](https://docs.gradle.org/current/userguide/incremental_build.html) in the Gradle documentation).

### Changed

- `MpsCheck` and `MpsExecute` (together with `MpsGenerate`) marked as incubating. They may experience some breaking
  changes in future releases.

## 1.21.2

### Fixed

- `downloadJbr` handles JBR versions with dashes in their name

## 1.21.1

### Fixed

- `downloadJbr` properly accepts the `distributionType` parameter 

## 1.21.0

### Added

- `MpsExecute` custom task to execute specified method from a generated class.

## 1.20.0

### Added

- `modelcheck` and `MpsCheck` now support `parallel` flag to launch model checker in parallel mode.

## 1.19.3

### Fixed

- Support for explicitly setting `javaExec` property of `generate` and `checkmodels` (the Java executable to use) was
  broken in 1.19.1 and 1.19.2 and should now be fixed.

## 1.19.2

### Fixed

- `runMigrations` task now supports MPS 2022.2 and above by adding the necessary `--add-opens` JVM
  arguments.
- `runMigrations` task now honors its `javaExec` property.

## 1.19.1

### Fixed

- `generate` and `checkmodels` tasks now support MPS 2022.2 and above by adding the necessary `--add-opens` JVM
  arguments (using `mps-build-backends/launcher`).

## 1.19

### Added

- `MpsCheck` task for running the model checker. In contrast to the `modelcheck` plugin there can be multiple instances
  of the task, and the task is written with the current Gradle best practices (lazy properties, caching).

## 1.18

### Changed

- `extractJbr` task is no longer a `Sync` but will use platform-specific tools (`tar`) to make sure symlinks in the JBR
  are extracted properly. Native libraries rely on symlinks and do not function unless properly extracted.

## 1.17

### Added

- `generate` now supports `parallelGenerationThreads` option. Positive values will turn on parallel generation, and
  the value will be used as the number of threads. The default is 0, which means no parallel generation.

## 1.16

### Added

- `runMigrations` task now passes configured `plugins` and `macros` to MPS.
- `runMigrations` task now supports `haltOnPrecheckFailure` parameter on MPS 2021.1 and above.

### Changed

- Extracted Git-based versioning into a separate subproject, `git-based-versioning`.
- `runMigrations`: `force` and `haltOnPrecheckFailure` are now nullable and null by default.

## 1.15

### Added

- `generate`, `run-migrations` now support `maxHeap` option for specifying the maximum JVM heap size, similar to `modelcheck`.

### Fixed

- `downloadJbr`: property `downloadJbr.javaExecutable` now correctly points to `<JBR>/bin/java.exe` on Windows rather
  than `<JBR>/bin/java`.

## 1.14

### Added

- `generate`: can now define include and exclude lists of models and modules, similar to the `modelcheck` plugin.

## 1.13

### Added

- `de.itemis.gradle.common` empty plugin that can be used in a `plugins` block to put task classes onto the build script
  classpath.

## 1.12

### Added

- `run-migrations` plugin to execute pending migrations on a project.

### Changed

- `download-jbr`: `extractJbr` task class changed from `Copy` to `Sync`.
- `download-jbr`: `extractJbr` task will ensure all extracted files are user-writable.

### Fixed

- `download-jbr`: as a result of the above change, `extractJbr` task no longer fails due to trying to overwrite
  read-only files it previously extracted.

## 1.11

### Added

- `GitBasedVersioning#getVersionWithBugfixAndCount(major, minor, bugfix, count)` method.

## 1.10

### Added
- `generate`, `modelcheck`: new `environmentKind` property to choose between executing in MPS or IDEA environment.
  Default is `IDEA` for backwards compatibility, but `MPS` environment might perform faster.

## 1.9

### Added
- `generate`, `modelcheck`: Can specify the list of plugins lazily via the new `pluginsProperty`. This is useful if
  they may be downloaded by a preceding task in the same build.
- `generate`, `modelcheck`: Register the `generate` and `checkmodels` tasks when the plugin is applied instead of in
  `project.afterEvaluate`, so that they can be configured by the build.

### Changed
- `generate`, `modelcheck`: Use `register` rather than `create` to help with configuration avoidance.

## 1.8

### Added
- `modelcheck`: Can now exclude models or modules from model checking.
- `generate`, `modelcheck`: Can specify configuration for the "backend" explicitly so that it can be locked via 
  Gradle lockfiles. 

### Changed
- Upgraded to Gradle 7.4.1, Kotlin 1.5.31.
- Build backends (`execute-generators`, `modelcheck`) extracted into a separate repository (mbeddr/mps-build-backends).

## 1.7

### Added
- Can now specify distribution type for `downloadJbr`.

## 1.6

### Added
- `RunAntScript` task now has `incremental` flag to enable incremental builds.

## 1.5

### Added
- Support for using custom MPS distribution.

### Removed
- The plugin is no longer compatible with MPS 2019.3 and below.
