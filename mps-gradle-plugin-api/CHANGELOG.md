# Changelog
The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## 1.0.0

Initial release, extracted from `mps-gradle-plugin`.

### Added

- `MpsTask` interface for any task that operates on an MPS installation. Provides `mpsHome`, `mpsVersion`, and
  `javaLauncher` properties.
- `MpsProjectTask` interface (extends `MpsTask`) for tasks that operate on MPS projects. Provides `projectLocation`,
  `pluginRoots`, `folderMacros`, and `logLevel` properties.
