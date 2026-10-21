## Custom MPS Distribution

All MPS task types (`MpsCheck`, `MpsGenerate`, `MpsExecute`, `MpsMigrate`, `Remigrate`, `RunAntScript`) take the MPS
installation to use from the `mpsHome` property. This can be a vanilla MPS distribution or a custom distribution of
MPS with preinstalled plugins and its own versioning scheme, also called an MPS RCP.

By default, the tasks detect the MPS version by reading `mps.build.number` from `$mpsHome/build.properties`. A custom
distribution may not contain this file or may use a build number that does not follow the MPS scheme. In that case set
`mpsVersion` explicitly to the exact MPS version your distribution is based on. For example, if you build an RCP with
MPS 2025.1.2, set `mpsVersion` to `2025.1.2`.

Since the tasks only need a directory, extracting the distribution is up to the build script. Wire the task that
extracts the distribution into `mpsHome` so that the dependency is tracked automatically:

```kotlin
import de.itemis.mps.gradle.tasks.MpsCheck

val extractCustomMps by tasks.registering(Sync::class) {
    from(zipTree(customMpsZip))
    into(layout.buildDirectory.dir("myAwesomeMPS"))
}

tasks.register<MpsCheck>("checkProject") {
    mpsHome = layout.dir(extractCustomMps.map { it.destinationDir })
    mpsVersion = "2025.1.2"
    projectLocation = layout.projectDirectory.dir("mps-prj")
}
```
