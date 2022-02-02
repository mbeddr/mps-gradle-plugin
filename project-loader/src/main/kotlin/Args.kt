package de.itemis.mps.gradle.project.loader

import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.default
import java.io.File

private fun <T> splitAndCreate(str: String, creator: (String, String) -> T): T {
    val split = str.split("::")
    if (split.size < 2) {
        throw RuntimeException("string if not of the right format. Expected <key>::<value>")
    }
    return creator(split[0], split[1])
}

private fun toMacro(str: String) = splitAndCreate(str, ::Macro)
private fun toPlugin(str: String) = splitAndCreate(str, ::Plugin)

/**
 * Default set of arguments required to start a "headless" MPS. This class should be used by other users of the
 * project-loader in order to establish a somewhat standardised command line interface. Passing instances of this or
 * subclasses to [executeWithProject] is directly supported.
 */
open class Args(parser: ArgParser) {

    val plugins by parser.adding("--plugin",
            help = "plugin to to load. The format is --plugin=<id>::<path>")
    { toPlugin(this) }

    val macros by parser.adding("--macro",
            help = "macro to define. The format is --macro=<name>::<value>")
    { toMacro(this) }

    val pluginLocation by parser.storing("--plugin-location",
            help = "location to load additional plugins from") { File(this) }.default<File?>(null)

    val buildNumber by parser.storing("--build-number",
            help = "build number used to determine if the plugins are compatible").default<String?>(null)

    val project by parser.storing("--project",
            help = "project to generate from") { File(this) }

    val testMode by parser.flagging("--test-mode", help = "run in test mode")
}
