package de.itemis.mps.gradle

import de.itemis.mps.gradle.launcher.MpsVersionDetection
import de.itemis.mps.gradle.tasks.MpsTask

/**
 * A side effect of this plugin is that it lets us use `plugins` block rather than `buildscript` to put the task classes
 * ([RunAntScript], [BuildLanguages], etc.) onto the classpath.
 */

tasks.withType(MpsTask::class.java).configureEach {
    mpsVersion.convention(MpsVersionDetection.fromMpsHome(project.layout, providers, mpsHome.asFile))
}

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
