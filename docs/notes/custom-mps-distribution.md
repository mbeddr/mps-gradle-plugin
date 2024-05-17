## Custom MPS Distribution

Features that perform an action inside an MPS project, like the `modelcheck` or `generate-models` plugin, require
an MPS available to them. While for vanilla MPS it is enough to pass in a reference to the MPS dependency via the
`mpsConfig` property, this doesn't work for custom distributions of MPS. A custom distribution of MPS is also called
an MPS RCP. If you like to use your own MPS distribution with preinstalled plugins and your own versioning scheme
then this is possible but requires additional steps in the build script.

When you are using a custom distribution of MPS you can still use the `mpsConfig` property and rely on
the plugin resolving it. However, you may need to configure explicit `mpsVersion` for the plugin. You can also use a
custom `mpsLocation` with no value set for `mpsConfig`. In this case you _must_ configure `mpsVersion` as well.

If you set `mpsVersion` but also set `mpsConfig` then `mpsVersion` will take precedence over the version of the
dependency in the configuration. The plugin will resolve the specified configuration into `mpsLocation`.

`mpsVersion` needs to be set to the exact MPS version your custom distribution is based on. For example, if you build an
RCP with MPS 2020.3.3 you need to set this property to `2020.3.3`. `mpsLocation` needs to point to the location
where you extracted your custom MPS distribution into e.g. `$buildDir/myAwesomeMPS` if you extracted into that location.

Each of the plugins creates a `resolveMpsFor<name>` task in the build. When `mpsVersion` and `mpsLocation` are set
this task is still present in the task graph but becomes a noop. The task is present to be able to add your own task(s)
as dependency to it. This is useful for extracting your custom distribution before its being used. A minimal example
could look like this:

```
def myCustomLocation = "$buildDir/myAwesomeMPS"

task downloadAndExtractCustomMPS() {
    // your logic to download and extract here
}

modelcheck {
    mpsLocation = myCustomLocation
    mpsVersion = "2020.3.3"
    projectLocation = file("$rootDir/mps-prj")
    modules = ["my.solution.with.errors"]
    junitFile = file("$buildDir/TEST-modelcheck-results.xml")
}

tasks.getByName("resolveMpsForModelcheck").dependsOn(downloadAndExtractCustomMPS)
```
