package de.itemis.mps.gradle

/**
 * A side effect of this plugin is that it lets us use `plugins` block rather than `buildscript` to put the task classes
 * ([RunAntScript], [BuildLanguages], etc.) onto the classpath.
 */

val modelcheckBackend by configurations.creating

modelcheckBackend.defaultDependencies {
    add(dependencies.create("de.itemis.mps.build-backends:modelcheck:${MPS_BUILD_BACKENDS_VERSION}"))
}
