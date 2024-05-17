package de.itemis.mps.gradle

import org.gradle.language.base.plugins.LifecycleBasePlugin

internal object TaskGroups {
    const val VERIFICATION = LifecycleBasePlugin.VERIFICATION_GROUP // "verification"
    const val GENERATION = "generation"
    const val MIGRATION = "migration"
}
