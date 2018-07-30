package de.itemis.mps.gradle.generate

import com.intellij.openapi.util.AsyncResult
import jetbrains.mps.make.MakeSession
import jetbrains.mps.make.facet.FacetRegistry
import jetbrains.mps.make.facet.IFacet
import jetbrains.mps.make.facet.ITarget
import jetbrains.mps.make.script.IScript
import jetbrains.mps.make.script.ScriptBuilder
import jetbrains.mps.messages.IMessage
import jetbrains.mps.messages.IMessageHandler
import jetbrains.mps.messages.MessageKind
import jetbrains.mps.project.Project
import jetbrains.mps.smodel.SLanguageHierarchy
import jetbrains.mps.smodel.language.LanguageRegistry
import jetbrains.mps.smodel.resources.ModelsToResources
import jetbrains.mps.smodel.runtime.MakeAspectDescriptor
import jetbrains.mps.tool.builder.make.BuildMakeService
import org.apache.log4j.Logger
import org.jetbrains.mps.openapi.language.SLanguage
import org.jetbrains.mps.openapi.model.SModel

private val logger = Logger.getLogger("de.itemis.mps.gradle.generate")

private val DEFAULT_FACETS = listOf(
        IFacet.Name("jetbrains.mps.lang.core.Generate"),
        IFacet.Name("jetbrains.mps.lang.core.TextGen"),
        IFacet.Name("jetbrains.mps.make.facets.Make"))

private class MsgHandler : IMessageHandler {
    val logger = Logger.getLogger("de.itemis.mps.gradle.generate.messages")
    override fun handle(msg: IMessage) {
        when (msg.kind) {
            MessageKind.INFORMATION -> logger.info(msg.text, msg.exception)
            MessageKind.WARNING -> logger.warn(msg.text, msg.exception)
            MessageKind.ERROR -> logger.error(msg.text, msg.exception)
        }
    }

}

private fun createScript(proj: Project, models: List<org.jetbrains.mps.openapi.model.SModel>): IScript {

    val allUsedLanguagesAR: AsyncResult<Set<SLanguage>> = AsyncResult()
    val registry = LanguageRegistry.getInstance(proj.repository)

    proj.modelAccess.runReadAction {
        val allDirectlyUsedLanguages = models.map { it.module }.distinct().flatMap { it.usedLanguages }.distinct()
        allUsedLanguagesAR.setDone(SLanguageHierarchy(registry, allDirectlyUsedLanguages).extended)
    }

    val allUsedLanguages = allUsedLanguagesAR.resultSync

    val scb = ScriptBuilder()

    scb.withFacetNames(allUsedLanguages
            .mapNotNull { registry.getLanguage(it) }
            .mapNotNull { it.getAspect(MakeAspectDescriptor::class.java) }
            .flatMap { it.manifest.facets() }
            .map { it.name }
    )

    scb.withFacetNames(allUsedLanguages
            .flatMap { FacetRegistry.getInstance().getFacetsForLanguage(it.qualifiedName) }
            .map { it.name }
    )

    // For some reason MPS doesn't explicitly stat that there is a dependency on Generate, TextGen and Make, so we have
    // to make sure they are always included in the set of facets even if for MPS there is no dependency on them.

    // todo: not sure if we really need the final target to be Make.make all the time. The code was taken fom #BuildMakeService.defaultMakeScript
    return scb.withFacetNames(DEFAULT_FACETS).withFinalTarget(ITarget.Name("jetbrains.mps.make.facets.Make.make")).toScript()
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
    val future = makeService.make(session, res, createScript(proj, models))
    try {
        future.get()
        logger.info("generation finished")
    } catch (ex: Exception) {
        logger.error("failed to generate", ex)
    }
}


fun generateProject(parsed: GenerateArgs, project: Project) {
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



