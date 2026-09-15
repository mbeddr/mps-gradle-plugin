package de.itemis.mps.gradle.tasks;

import org.gradle.api.Incubating;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.tasks.Internal;

/**
 * A Gradle task that operates on an MPS project.
 *
 * <p>Adds {@link #getProjectLocation()} to the properties inherited from {@link MpsTask}. Use
 * {@link org.gradle.api.tasks.TaskCollection#withType(Class)} to configure all MPS project tasks at once:
 *
 * <pre>{@code
 * tasks.withType(MpsProjectTask.class).configureEach(task -> {
 *     task.getProjectLocation().set(layout.getProjectDirectory().dir("my-mps-project"));
 * });
 * }</pre>
 */
@Incubating
public interface MpsProjectTask extends MpsTask {
    /**
     * The MPS project directory. Convention: the Gradle project directory (but no convention is set on
     * multi-project tasks like MpsMigrate and Remigrate, so that the mutual exclusivity check with
     * {@code projectLocations} works correctly).
     */
    @Internal("too coarse to be used as input")
    DirectoryProperty getProjectLocation();
}
