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

Use a more precisely specified version such as `1.26.0.+` or Gradle dependency locking for better build
reproducibility.

## Reference

Tasks:

* [RunAntScript](docs/tasks/RunAntScript.md) -- run an MPS-generated Ant script.
* [GenerateLibrariesXml](docs/tasks/GenerateLibrariesXml.md) -- generate a `.mps/libraries.xml` file from property files.
* [MpsCheck](docs/tasks/MpsCheck.md) -- check (a subset of) models in a project.
* [MpsExecute](docs/tasks/MpsExecute.md) -- execute a specified method in a generated class in the context of a running
  MPS instance with an open project.
* [MpsGenerate](docs/tasks/MpsGenerate.md) -- generate (a subset of) models in a project without the need for a MPS
  build model.
* [MpsMigrate](docs/tasks/MpsMigrate.md) -- Run pending migrations on one or several MPS projects. 

