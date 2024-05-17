# mps-gradle-plugin

Miscellaneous tasks that were found useful when building MPS-based
projects with Gradle.

# Configuring the plugin repository

This plugin is not published to the Gradle plugin portal but to a public repository of itemis. To configure this
repository add the following at the beginning of your `settings.gradle`:

```groovy
pluginManagement {
    repositories {
        maven { url 'https://artifacts.itemis.cloud/repository/maven-mps' }
      
        // Need to manually include the default Gradle plugin portal repository when overriding the defaults.
        gradlePluginPortal()
    }
}
```

# Provided Tasks

To make use of custom task types, add the following `plugins` block to your build script:

```
plugins {
    id 'de.itemis.mps.gradle.common' version '1.+'
}
```

Use a fully specified version such as `1.0.123` or Gradle dependency locking for better build reproducibility.

## Reference

Tasks:

* [RunAntScript](docs/tasks/RunAntScript.md) -- run an MPS-generated Ant script.
* [CreateDmg](docs/tasks/CreateDmg.md) -- (macOS only) create a .dmg installer.
* [BundleMacosJdk](docs/tasks/BundleMacosJdk.md) -- (Linux/macOS) create a .tar.gz by combining an RCP artifact and a JDK.
* [GenerateLibrariesXml](docs/tasks/GenerateLibrariesXml.md) -- generate a `.mps/libraries.xml` file from property files.
* [MpsCheck](docs/tasks/MpsCheck.md) -- check (a subset of) models in a project.
* [MpsExecute](docs/tasks/MpsExecute.md) -- execute a specified method in a generated class in the context of a running
  MPS instance with an open project.
* [MpsGenerate](docs/tasks/MpsGenerate.md) -- generate (a subset of) models in a project without the need for a MPS
  build model.
* [MpsMigrate](docs/tasks/MpsMigrate.md) -- Run pending migrations on one or several MPS projects. 

Plugins:

* [generate](docs/plugins/generate.md) -- Deprecated. Generate (a subset of) models in a project without the need for a MPS
  build model.
* [modelcheck/checkmodels](docs/plugins/modelcheck.md) -- Deprecated. Check (a subset of) models in a project.
* [download-jbr/downloadJbr](docs/plugins/download-jbr.md) -- Download JetBrains Runtime.
