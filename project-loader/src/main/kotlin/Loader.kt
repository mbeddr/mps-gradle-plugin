package de.itemis.mps.gradle.project.loader

import com.intellij.application.options.PathMacrosImpl
import jetbrains.mps.project.Project
import jetbrains.mps.tool.environment.EnvironmentConfig
import jetbrains.mps.tool.environment.IdeaEnvironment
import org.apache.log4j.Logger
import java.io.File

data class Plugin(
        val path: String,
        val id: String
)

data class Macro(
        val name: String,
        val value: String
)

private val logger = Logger.getLogger("de.itemis.mps.gradle.project.loader")

/**
 *  The Idea platform reads this property first to determine where additional plugins are loaded from.
 */
private const val PROPERTY_PLUGINS_PATH = "idea.plugins.path"

/**
 * At least starting from MPS 2018.2 this property is read by the platform to check against this value if a plugin
 * is compatible with the application. It seems to override all other means of checks, e.g. build number in
 * ApplicationInfo.
 */
private const val PROPERTY_PLUGINS_COMPATIBLE_BUILD = "idea.plugins.compatible.build"

private fun basicEnvironmentConfig(): EnvironmentConfig {

    // This is a somewhat "save" set of default plugins. It should work with most of the projects we have encountered
    // mbeddr projects won't build build with this set of plugins for unknown reasons, most probably the runtime
    // dependencies in the mbeddr plugins are so messed up that they simply broken beyond repair.

    val config = EnvironmentConfig
            .emptyConfig()
            .withDefaultPlugins()
            .withBuildPlugin()
            .withBootstrapLibraries()
            .withWorkbenchPath()
            .withGit4IdeaPlugin()
            .withJavaPlugin()
            .addPlugin("http-support", "jetbrains.mps.ide.httpsupport")
    return config
}

/**
 * Loads up an idea environment with the given parameters, passes the project into the lambda, closes the project then
 * shuts down the idea environment and returns the value calculated by the lambda.
 *
 * The environment is also shutdown in cases where the lambda throws an exception.
 *
 *
 * @param project the location of the project to open.
 * @param action the action to execute with the project.
 * @param pluginLocation optional location where plugins lo load are located. This is for additional plugins, the plugins
 * located in the pre-installed pluging location (usual the "plugins" folder of MPS) are still considered.
 * @param plugins optional list of plugins to load. Path is the what MPS calls the `short (folder) name` in it's build
 * language. The id is the plugin id defined in the plugin descriptor. Both are required because MPS supports multiple
 * plugins the same location.
 * @param macros optional list of path macros to define before the project is opened
 * @param buildNumber optional build number that is used to determine if the plugins are compatible. Only guaranteed to
 * work with MPS 2018.2+ but might work in earlier versions as well.
 */
fun <T> executeWithProject(project: File,
                           plugins: List<Plugin> = emptyList(),
                           macros: List<Macro> = emptyList(),
                           pluginLocation: File? = null,
                           buildNumber: String? = null,
                           action: (Project) -> T): T {

    if (pluginLocation != null) {
        logger.info("overriding plugin location with: ${pluginLocation.absolutePath}")
        System.setProperty(PROPERTY_PLUGINS_PATH, pluginLocation.absolutePath)
    }

    if (buildNumber != null) {
        logger.info("setting build number to \"$buildNumber\"")
        System.setProperty(PROPERTY_PLUGINS_COMPATIBLE_BUILD, buildNumber)
    }

    val cfg = basicEnvironmentConfig()

    plugins.forEach { cfg.addPlugin(it.path, it.id) }

    val ideaEnvironment = IdeaEnvironment(cfg)

    logger.info("initialising environment")
    ideaEnvironment.init()
    logger.info("flushing events")
    ideaEnvironment.flushAllEvents()

    val pathMacrosImpl = PathMacrosImpl.getInstanceEx()
    macros.forEach { pathMacrosImpl.setMacro(it.name, it.value) }

    logger.info("opening project: ${project.absolutePath}")
    val ideaProject = ideaEnvironment.openProject(project)

    logger.info("flushing events")
    ideaEnvironment.flushAllEvents()

    val res: T

    try {
        res = action(ideaProject)
    } finally {
        ideaProject.dispose()
        logger.info("project disposed")
        ideaEnvironment.flushAllEvents()
        logger.info("disposing environment")
        ideaEnvironment.dispose()
        logger.info("project and environment disposed")
    }

    return res
}

/**
 *  Convenient function to invoke [executeWithProject] with arguments parsed form the command line.
 *
 *  @see [executeWithProject] for more details.
 *
 *  @param parsed parsed arguemnts.
 *  @param action the action to execute with the project.
 *
 */
fun <T> executeWithProject(parsed: Args, action: (Project) -> T) = executeWithProject(
        project = parsed.project,
        plugins = parsed.plugins,
        macros = parsed.macros,
        buildNumber = parsed.buildNumber,
        pluginLocation = parsed.pluginLocation,
        action = action)
