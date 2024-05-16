package de.itemis.mps.gradle.tasks

import org.gradle.api.Task
import org.gradle.api.logging.LogLevel

internal fun Task.addLogLevel(args: MutableCollection<String>) = addIfInfoLogLevel(args, "--log-level=info")

/**
 * A weird overload, usable for building a map of Ant task attributes, as well as a list of command line backend
 * attributes.
 */
internal fun <T> Task.addIfInfoLogLevel(result: MutableCollection<T>, value: T) {
    val effectiveLogLevel = logging.level ?: project.logging.level ?: project.gradle.startParameter.logLevel
    if (effectiveLogLevel <= LogLevel.INFO) result.add(value)
}
