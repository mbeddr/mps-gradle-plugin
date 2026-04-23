package de.itemis.mps.gradle.tasks

import org.gradle.api.Incubating

@Incubating
data class ExcludedModuleMigration(val language: String, val version: Int)
