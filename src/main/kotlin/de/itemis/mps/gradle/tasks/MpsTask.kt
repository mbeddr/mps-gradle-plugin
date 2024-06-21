package de.itemis.mps.gradle.tasks

import org.gradle.api.Task
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional

/**
 * A common interface for all tasks that use MPS, either to launch a backend or to call an Ant task.
 */
interface MpsTask : Task {
    @get:Internal("covered by mpsVersion and other properties")
    val mpsHome: DirectoryProperty

    @get:Input
    @get:Optional
    val mpsVersion: Property<String>

    @get:Internal("Folder macros are ignored for the purposes of up-to-date checks and caching")
    val folderMacros: MapProperty<String, Directory>

    @get:Classpath
    val pluginRoots: ConfigurableFileCollection
}
