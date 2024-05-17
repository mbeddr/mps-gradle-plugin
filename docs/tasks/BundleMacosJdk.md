## BundleMacosJdk

(Linux/macOS) Creates a .tar.gz by combining an RCP artifact and a JDK.
This task is intended as a substitute for the macOS-specific CreateDmg
task.

### Usage

```
task bundleMacosJdk(type: de.itemis.mps.gradle.BundleMacosJdk) {
    rcpArtifact file('path/to/RCP.tgz')

    jdkDependency "com.jetbrains.jdk:jdk:${jdkVersion}:osx_x64@tgz"
    // -or -
    jdk file('path/to/jdk.tgz')

    outputFile file('output.tar.gz')
}
```

Parameters:
* `rcpArtifact` - the path to the RCP artifact produced by a build script.
* `jdkDependency` - the coordinates of a JDK in case it's available in
  a repository and can be resolved as a Gradle dependency.
* `jdk` - the path to a JDK .tgz file.
* `outputFile` - the path and file name of the output gzipped tar archive.

### Operation

The task unpacks `rcpArtifact` into a temporary directory, unpacks
the JDK given by `jdkDependency`/`jdk` under the `jre` subdirectory of
the unpacked RCP artifact, fixes file permissions and creates missing
symlinks. Finally, the file is repackaged again as tar/gzip.
