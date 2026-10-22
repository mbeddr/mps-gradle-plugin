package de.itemis.mps.gradle

import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.artifacts.dsl.DependencyHandler

internal enum class BuildBackend(
    val moduleId: String,
    val configurationName: String,
    val minReleaseInclusive: String?,
    val maxReleaseExclusive: String
) {
    MODELCHECK(
        "modelcheck",
        BackendConfigurations.MODELCHECK_BACKEND_CONFIGURATION_NAME,
        BACKENDS_MIN_RELEASE,
        BACKENDS_MAX_RELEASE
    ),
    EXECUTE_GENERATORS(
        "execute-generators",
        BackendConfigurations.GENERATE_BACKEND_CONFIGURATION_NAME,
        BACKENDS_MIN_RELEASE,
        BACKENDS_MAX_RELEASE
    ),
    EXECUTE(
        "execute",
        BackendConfigurations.EXECUTE_BACKEND_CONFIGURATION_NAME,
        BACKENDS_MIN_RELEASE,
        BACKENDS_MAX_RELEASE
    ),
    REMIGRATE("remigrate", BackendConfigurations.REMIGRATE_BACKEND_CONFIGURATION_NAME,
        minReleaseInclusive = null, REMIGRATE_MAX_RELEASE);
}

internal fun DependencyHandler.backendDependency(backend: BuildBackend): ExternalModuleDependency {
    val result = create("$BACKENDS_GROUP_ID:${backend.moduleId}") as ExternalModuleDependency
    result.version {
        // Reject snapshots
        require("latest.release")

        // All rejections must be passed in one call: each call to `reject` replaces the previously rejected versions.
        reject(*generateRejectionPatterns(backend))
    }
    return result
}

private fun generateRejectionPatterns(backend: BuildBackend): Array<String> {
    // reject 'maxReleaseExclusive', its prereleases, and any later versions. `-dev` is the lowest qualifier in Gradle's
    // ordering, rejecting it also rejects all prereleases
    val rejectMaxAndAbove = "[${backend.maxReleaseExclusive}-dev,)"

    if (backend.minReleaseInclusive != null) {
        val rejectBelowMin = "(,${backend.minReleaseInclusive})"
        return arrayOf(rejectBelowMin, rejectMaxAndAbove)
    }

    return arrayOf(rejectMaxAndAbove)
}

private const val BACKENDS_GROUP_ID = "de.itemis.mps.build-backends"

// 1.26 required to run modelcheck without plugins on its classpath
private const val BACKENDS_MIN_RELEASE = "1.26"
private const val BACKENDS_MAX_RELEASE = "2"

private const val REMIGRATE_MAX_RELEASE = "2"
