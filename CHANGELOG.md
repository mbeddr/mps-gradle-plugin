# Changelog
The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

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
