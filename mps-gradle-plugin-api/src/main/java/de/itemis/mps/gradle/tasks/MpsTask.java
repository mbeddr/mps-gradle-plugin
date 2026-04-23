package de.itemis.mps.gradle.tasks;

import org.gradle.api.Incubating;
import org.gradle.api.Task;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.Directory;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.logging.LogLevel;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.Console;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Nested;
import org.gradle.jvm.toolchain.JavaLauncher;

/**
 * A Gradle task that operates on an MPS installation.
 *
 * <p>Use {@link org.gradle.api.tasks.TaskCollection#withType(Class)} to configure all MPS tasks at once:
 *
 * <pre>{@code
 * tasks.withType(MpsTask.class).configureEach(task -> {
 *     task.getMpsHome().set(layout.getProjectDirectory().dir("mps"));
 *     task.getPluginRoots().from(task.getMpsHome().dir("plugins"));
 * });
 * }</pre>
 */
@Incubating
public interface MpsTask extends Task {
    /**
     * The MPS installation directory.
     */
    @Internal("not considered an input by itself")
    DirectoryProperty getMpsHome();

    /**
     * The MPS version string (e.g. "2021.3.3"). Detected automatically from {@link #getMpsHome()} if not set
     * explicitly.
     */
    @Input
    Property<String> getMpsVersion();

    /**
     * The Java launcher to use for running MPS. Each MPS version requires a specific Java version.
     */
    @Nested
    Property<JavaLauncher> getJavaLauncher();

    /**
     * Root directories containing MPS plugins to load.
     */
    @Classpath
    ConfigurableFileCollection getPluginRoots();

    /**
     * Folder macros (path variables) to pass to MPS. Keys are macro names, values are directories.
     */
    @Internal("not considered input")
    MapProperty<String, Directory> getFolderMacros();

    /**
     * Log level for the MPS backend process.
     */
    @Console
    Property<LogLevel> getLogLevel();
}
