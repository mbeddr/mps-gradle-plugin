# Changelog
The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

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
