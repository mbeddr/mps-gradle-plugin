## Download JetBrains Runtime

When building MPS projects with the JetBrains Runtime, the JDK/JRE used by MPS and other intellij based IDEs, it's
required to download the correct version of the runtime. Since the runtime is platform dependent it's required to
download a platform dependent binary. While it's possible to add the logic to your own build script we provide a convenient
way of doing this with a Gradle plugin.

The download-jbr plugin will add new dependencies and a task to your build. It will add a dependency to `com.jetbrains.jdk:jbr`
to your build, you need to make sure that it is available in your dependency repositories. The itemis Maven repository at
https://artifacts.itemis.cloud/repository/maven-mps provides this dependency, but you can create your own with
the scripts located in [mbeddr/build.publish.jdk](https://github.com/mbeddr/build.publish.jdk).

For easy consumption and incremental build support the plugin creates a task `downloadJbr` which exposes the location of
the java executable via the `javaExecutable` property. See
[the tests](../../src/test/kotlin/test/others/JBRDownloadTest.kt) for an example of how to use it.

### Usage


Kotlin:
```
plugins {
    id("download-jbr")
}

repositories {
    mavenCentral()
    maven {
        url = URI("https://artifacts.itemis.cloud/repository/maven-mps")
    }
}

downloadJbr {
    jbrVersion = "11_0_6-b520.66"
}
```

Groovy:
```
apply plugin: 'download-jbr'
...

repositories {
    maven { url 'https://artifacts.itemis.cloud/repository/maven-mps' }
    mavenCentral()
}

downloadJbr {
    jbrVersion = '11_0_6-b520.66'
}
```

### Parameters
* `jbrVersion` - version of the JBR to download. While this supports maven version selectors we highly recommend not
  using wildcards like `*` or `+` in there for reproducible builds.
* `distributionType` - optional distribution type for the JBR to use. Will default to `jbr_jcef` if omitted.
* `downloadDir` - optional directory where the downloaded JBR is downloaded and extracted to. The plugin defaults to
  `build/jbrDownload`. 
