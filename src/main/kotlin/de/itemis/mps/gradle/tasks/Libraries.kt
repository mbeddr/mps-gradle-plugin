package de.itemis.mps.gradle.tasks

import org.gradle.api.file.Directory
import java.io.File
import java.io.FileNotFoundException

internal fun readLibraries(projectDir: File, getMacroPath: (String) -> Directory?) = try {
    projectDir.resolve(".mps/libraries.xml").useLines { lines ->
        lines
            .map { pathRegex.find(it) }
            .filterNotNull()
            .map { it.groups[1]!!.value }
            .map { substituteMacros(it, projectDir, getMacroPath) }
            .toList()
    }
} catch (e : FileNotFoundException) {
    listOf()
}

private val pathRegex = """<option name=["']path["'] value=["']([^"']+)["']""".toRegex()

private fun substituteMacros(input: String, projectDir: File, getMacro: (String) -> Directory?): String {
    if (input.startsWith("\$PROJECT_DIR$"))
        return projectDir.toString() + input.substring("\$PROJECT_DIR$".length)
    else if (input.startsWith("\${")) {
        val closingBrace = input.indexOf('}')
        if (closingBrace < 0) return input

        val macro = input.substring(2, closingBrace)
        val folder = getMacro(macro) ?: return input

        return folder.toString() + input.substring(closingBrace + 1)
    }

    return input
}
