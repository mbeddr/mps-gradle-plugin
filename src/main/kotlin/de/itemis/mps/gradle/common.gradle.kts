package de.itemis.mps.gradle

/**
 * A side effect of this plugin is that it lets us use `plugins` block rather than `buildscript` to put the task classes
 * ([RunAntScript], [BuildLanguages], etc.) onto the classpath.
 */

val modelcheckBackend = configurations.create(BackendConfigurations.MODELCHECK_BACKEND_CONFIGURATION_NAME)
val generateBackend = configurations.create(BackendConfigurations.GENERATE_BACKEND_CONFIGURATION_NAME)
val executeBackend = configurations.create(BackendConfigurations.EXECUTE_BACKEND_CONFIGURATION_NAME)

modelcheckBackend.defaultDependencies {
    add(dependencies.create("de.itemis.mps.build-backends:modelcheck:${MPS_BUILD_BACKENDS_VERSION}"))
}

generateBackend.defaultDependencies {
    add(dependencies.create("de.itemis.mps.build-backends:execute-generators:${MPS_BUILD_BACKENDS_VERSION}"))
}

executeBackend.defaultDependencies {
    add(dependencies.create("de.itemis.mps.build-backends:execute:${MPS_BUILD_BACKENDS_VERSION}"))
}

configurations.create(BackendConfigurations.REMIGRATE_BACKEND_CONFIGURATION_NAME) {
    isCanBeConsumed = false
    defaultDependencies {
        add(project.dependencies.create("de.itemis.mps.build-backends:remigrate:[0,2)"))
    }
}

extensions.create<CI>("ci", project)

abstract class CI(val _project: Project) {
    fun determineCI() {
        if(System.getenv("CI").toBoolean()) {
            _project.extra["ciBuild"] = true
        } else{
            _project.extra["ciBuild"] = _project.hasProperty("teamcity")
        }
    }
    
    fun isCI() = _project.extra["ciBuild"]
}