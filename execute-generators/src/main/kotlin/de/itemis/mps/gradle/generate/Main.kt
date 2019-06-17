package de.itemis.mps.gradle.generate

import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.SystemExitException
import com.xenomachina.argparser.mainBody
import de.itemis.mps.gradle.project.loader.Args
import de.itemis.mps.gradle.project.loader.executeWithProject
import org.apache.log4j.Logger

private val logger = Logger.getLogger("de.itemis.mps.gradle.generate")

class GenerateArgs(parser: ArgParser) : Args(parser) {
    val models by parser.adding("--model",
            help = "list of models to generate")
}


fun main(args: Array<String>) = mainBody {

    val parsed = ArgParser(args).parseInto(::GenerateArgs)
    var result = false

    try {
        result = executeWithProject(parsed) { project -> generateProject(parsed, project) }
    } catch (ex: java.lang.Exception) {
        logger.fatal("error generating", ex)
    } catch (t: Throwable) {
        logger.fatal("error generating", t)
    }
    if(!result) {
        throw SystemExitException("generation failed", -1)
    }

    System.exit(0)
}