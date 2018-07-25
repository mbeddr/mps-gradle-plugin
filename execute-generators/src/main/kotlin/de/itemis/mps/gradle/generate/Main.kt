package de.itemis.mps.gradle.generate

import com.intellij.application.options.PathMacrosImpl
import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.default
import com.xenomachina.argparser.mainBody
import jetbrains.mps.tool.environment.Environment
import jetbrains.mps.tool.environment.EnvironmentConfig
import jetbrains.mps.tool.environment.IdeaEnvironment
import org.apache.log4j.Logger
import java.io.File

private val logger = Logger.getLogger("de.itemis.mps.gradle.generate")
const val PROPERTY_PLUGINS_PATH = "idea.plugins.path"

fun basicEnvironmentConfig(): EnvironmentConfig {

    // This is a somewhat "save" set of default plugins. It should work with most of the projects we have encountered
    // mbeddr projects won't build build with this set of plugins for unknown reasons, most probably the runtime
    // dependencies in the mbeddr plugins are so messed up that they simply broken beyond repair.

    val config = EnvironmentConfig
            .emptyConfig()
            .withDefaultPlugins()
            .withDevkitPlugin()
            .withBuildPlugin()
            .withBootstrapLibraries()
            .withWorkbenchPath()
            .withGit4IdeaPlugin()
            .withJavaPlugin()
            .addPlugin("http-support", "jetbrains.mps.ide.httpsupport")
    return config
}

private fun splitAtColumn(s: String): Pair<String, String> {
    val split = s.split(":")
    if (split.size < 2) {
        throw RuntimeException("string if not of the right format. Expected <key>:<value>")
    }
    return Pair(split[0], split[1])
}

class Args(parser: ArgParser) {

    val models by parser.adding("--model",
            help = "list of models to generate")
    val plugins by parser.adding("--plugin",
            help = "plugin to to load. The format is --plugin=<name>:<id>")
    { splitAtColumn(this) }
    val macros by parser.adding("--macros",
            help = "macro to define. The format is --macro=<name>:<value>")
    { splitAtColumn(this) }

    val pluginLocation by parser.storing("--plugin-location",
            help = "location to load additional plugins from").default<String?>(null)

    val project by parser.positional("PROJECT",
            help = "project to generate from")
}


fun main(args: Array<String>) = mainBody {

    val parsed = ArgParser(args).parseInto(::Args)

    if (!parsed.pluginLocation.isNullOrEmpty()) {
        System.setProperty(PROPERTY_PLUGINS_PATH, parsed.pluginLocation)
    }

    val cfg = basicEnvironmentConfig()

    parsed.plugins.forEach { cfg.addPlugin(it.first, it.second) }

    var env: Environment? = null
    try {
        logger.info("Creating Env")

        env = IdeaEnvironment.getOrCreate(cfg)
        env.flushAllEvents()

        val pathMacrosImpl = PathMacrosImpl.getInstanceEx()
        parsed.macros.forEach { pathMacrosImpl.setMacro(it.first, it.second) }

        val project = env.openProject(File(parsed.project))

        env.flushAllEvents()

        generateProject(parsed, project)

        env.flushAllEvents()
    } catch (ex: java.lang.Exception) {
        logger.fatal("error generating", ex)
    } catch (t: Throwable) {
        logger.fatal("error generating", t)
    } finally {
        if (env != null) env.dispose()
    }

    System.exit(0)
}