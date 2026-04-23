## GenerateLibrariesXml

Generates a `.mps/libraries.xml` file using data from property files.

### Usage

```
task generateLibrariesXml(type: de.itemis.mps.gradle.GenerateLibrariesXml) {
    defaults rootProject.file('projectlibraries.properties')
    overrides rootProject.file('projectlibraries.overrides.properties')
    destination file('.mps/libraries.xml')
}
```

Parameters:
* `defaults` - path to default properties (checked in to version control)
* `overrides` - path to property overrides (ignored, not checked in to
  version control, absent by default)
* `destination` - path to the output `libraries.xml`

### Operation

The task reads properties file `defaults`, then `overrides` (if
present). `destination` is then generated based on the properties.

Each property represents an entry in `destination` (a project library),
where the property name is the library name and the property value is
the path to the library.
