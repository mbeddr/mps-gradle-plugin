package de.itemis.mps.gradle.generate

import com.intellij.openapi.util.AsyncResult
import jetbrains.mps.make.MakeSession
import jetbrains.mps.messages.IMessage
import jetbrains.mps.messages.IMessageHandler
import jetbrains.mps.messages.MessageKind
import jetbrains.mps.project.Project
import jetbrains.mps.smodel.resources.ModelsToResources
import jetbrains.mps.tool.builder.make.BuildMakeService
import org.apache.log4j.Logger
import org.jetbrains.mps.openapi.model.SModel

private val logger = Logger.getLogger("de.itemis.mps.build.generate")


fun generateProject(parsed: Args, project: Project) {
    val ftr = AsyncResult<List<SModel>>()

    project.modelAccess.runReadAction {
        var modelsToGenerate = project.projectModels
        if (parsed.models.isNotEmpty()) {
            modelsToGenerate = modelsToGenerate.filter { parsed.models.contains(it.name.longName) }
        }
        ftr.setDone(modelsToGenerate.toList())
    }

    val modelsToGenerate = ftr.resultSync

    makeModels(project, modelsToGenerate)
}



private class MsgHandler : IMessageHandler {
    val logger = Logger.getLogger("de.itemis.mps.build.generate.messages")
    override fun handle(msg: IMessage) {
        when (msg.kind) {
            MessageKind.INFORMATION -> logger.info(msg.text, msg.exception)
            MessageKind.WARNING -> logger.warn(msg.text, msg.exception)
            MessageKind.ERROR -> logger.error(msg.text, msg.exception)
        }
    }

}

private fun makeModels(proj: Project, models: List<org.jetbrains.mps.openapi.model.SModel>) {
    val session = MakeSession(proj, MsgHandler(), true)
    val res = ModelsToResources(models).resources().toList()
    val makeService = BuildMakeService()
    if (res.isEmpty()) {
        logger.warn("nothing to generate")
        return
    }
    logger.info("starting generation")
    val future = makeService.make(session, res)
    try {
        future.get()
        logger.info("generation finished")
    } catch (ex: Exception) {
        logger.error("failed to generate", ex)
    }
}

