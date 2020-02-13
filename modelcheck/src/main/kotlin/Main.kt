package de.itemis.mps.gradle.modelcheck

import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.default
import com.xenomachina.argparser.mainBody
import de.itemis.mps.gradle.junit.*
import de.itemis.mps.gradle.project.loader.Args
import de.itemis.mps.gradle.project.loader.executeWithProject
import jetbrains.mps.checkers.*
import jetbrains.mps.errors.MessageStatus
import jetbrains.mps.errors.item.IssueKindReportItem
import jetbrains.mps.ide.httpsupport.runtime.base.HttpSupportUtil
import jetbrains.mps.ide.modelchecker.platform.actions.UnresolvedReferencesChecker
import jetbrains.mps.progress.EmptyProgressMonitor
import jetbrains.mps.project.Project
import jetbrains.mps.project.validation.StructureChecker
import jetbrains.mps.smodel.SModelStereotype
import jetbrains.mps.typesystemEngine.checker.NonTypesystemChecker
import jetbrains.mps.typesystemEngine.checker.TypesystemChecker
import jetbrains.mps.util.CollectConsumer
import org.apache.log4j.Logger
import org.jetbrains.mps.openapi.model.SModel
import java.io.File
import kotlin.test.fail

private val logger = Logger.getLogger("de.itemis.mps.gradle.generate")

class ModelCheckArgs(parser: ArgParser) : Args(parser) {
    val models by parser.adding("--model",
            help = "list of models to check")
    val modules by parser.adding("--module",
            help = "list of modules to check")
    val warningAsError by parser.flagging("--warning-as-error", help = "treat model checker warning as errors")
    val dontFailOnError by parser.flagging("--error-no-fail", help = "report errors but don't fail the build")
    val xmlFile by parser.storing("--result-file", help = "stores the result as an JUnit xml file").default<String?>(null)
}

fun printInfo(msg: String) {
    logger.info(msg)
}

fun printWarn(msg: String) {
    logger.warn(msg)
}

fun printError(msg: String) {
    logger.error(msg)
}

fun printResult(item: IssueKindReportItem, project: Project, args: ModelCheckArgs) {
    val path = IssueKindReportItem.PATH_OBJECT.get(item)

    val info = ::printInfo
    val warn = if (args.warningAsError) {
        ::printError
    } else {
        ::printWarn
    }

    val err = ::printError

    val print = fun(severity: MessageStatus, msg: String) {
        when (severity) {
            MessageStatus.OK -> info(msg)
            MessageStatus.WARNING -> warn(msg)
            MessageStatus.ERROR -> err(msg)
        }
    }

    when (path) {
        is IssueKindReportItem.PathObject.ModulePathObject -> {
            val module = path.resolve(project.repository)
            print(item.severity, "${item.message}[${module.moduleName}]")
        }
        is IssueKindReportItem.PathObject.ModelPathObject -> {
            val model = path.resolve(project.repository)
            print(item.severity, "${item.message} [${model.name.longName}]")
        }
        is IssueKindReportItem.PathObject.NodePathObject -> {
            val node = path.resolve(project.repository)
            val url = HttpSupportUtil.getURL(node)
            print(item.severity, "${item.message} [$url]")
        }
        else -> print(item.severity, item.message)
    }
}


fun writeJunitXml(models: Iterable<SModel>,
                  results: Iterable<IssueKindReportItem>,
                  project: Project,
                  warnAsErrors: Boolean,
                  file: File) {

    val allErrors = results.filter {
        it.severity == MessageStatus.ERROR || (warnAsErrors && it.severity == MessageStatus.WARNING)
    }
    val errorsPerModel = allErrors
            .filter {
                val path = IssueKindReportItem.PATH_OBJECT.get(it)
                path is IssueKindReportItem.PathObject.ModelPathObject || path is IssueKindReportItem.PathObject.NodePathObject
            }.groupBy {
                when (val path = IssueKindReportItem.PATH_OBJECT.get(it)) {
                    is IssueKindReportItem.PathObject.ModelPathObject -> {
                        path.resolve(project.repository)!!
                    }
                    is IssueKindReportItem.PathObject.NodePathObject -> {
                        val node = path.resolve(project.repository)
                        node.model!!
                    }
                    else -> fail("unexpected item type")
                }
            }

    val testcases = models.map {
        val errors = errorsPerModel.getOrDefault(it, emptyList())

        fun reportItemToContent(item: IssueKindReportItem): Failure {
            return when (val path = IssueKindReportItem.PATH_OBJECT.get(item)) {
                is IssueKindReportItem.PathObject.ModelPathObject -> {
                    val model = path.resolve(project.repository)!!
                    val message = "${item.message} [${model.name.longName}]"
                    Failure(message = message, content = message)
                }
                is IssueKindReportItem.PathObject.NodePathObject -> {
                    val node = path.resolve(project.repository)
                    val url = HttpSupportUtil.getURL(node)
                    val message = "${item.message} [$url]"
                    Failure(message = message, content = message)
                }
                else -> fail("unexpected issue kind")
            }
        }

        Testcase(name = it.name.simpleName,
                classname = it.name.longName,
                failures = errors.map(::reportItemToContent))
    }

    val testsuite = Testsuite(name = "model checking",
            failures = allErrors.size,
            testcases = testcases,
            id = 1,
            tests = models.count())


    val xmlMapper = XmlMapper()
    xmlMapper.writeValue(file, testsuite)
}

fun modelCheckProject(args: ModelCheckArgs, project: Project): Boolean {

    // see ModelCheckerSettings.getSpecificCheckers for details
    // we do not call into that class because we don't want to load the settings from the user
    val checkers = listOf(TypesystemChecker(),
            NonTypesystemChecker(),
            ConstraintsChecker(null),
            RefScopeChecker(),
            TargetConceptChecker(),
            StructureChecker(),
            UsedLanguagesChecker(),
            ModelPropertiesChecker(),
            UnresolvedReferencesChecker(project),
            ModuleChecker(),
            SuppressErrorsChecker())


    // We don't use ModelCheckerIssueFinder because it has strange dependency on the ModelCheckerSettings which we
    // want to avoid when running in headless mode
    val errorCollector = CollectConsumer<IssueKindReportItem>()
    val checker = ModelCheckerBuilder(false).createChecker(checkers)

    val itemsToCheck = ModelCheckerBuilder.ItemsToCheck()

    project.modelAccess.runReadAction {
        if (args.models.isNotEmpty()) {
            itemsToCheck.models.addAll(project.projectModulesWithGenerators
                    .flatMap { it.models.filter { !SModelStereotype.isDescriptorModel(it) && !SModelStereotype.isStubModel(it) } }
                    .filter { m -> args.models.any { it.toRegex().matches(m.name.longName) } })
        }
        if (args.modules.isNotEmpty()) {
            itemsToCheck.modules.addAll(project.projectModulesWithGenerators
                    .filter { m -> args.modules.any { it.toRegex().matches(m.moduleName as CharSequence) } })
        }
        if (args.models.isEmpty() && args.modules.isEmpty()) {
            itemsToCheck.modules.addAll(project.projectModulesWithGenerators)
        }
        checker.check(itemsToCheck, project.repository, errorCollector, EmptyProgressMonitor())

        // We need read access here to resolve the node pointers in the report items
        errorCollector.result.map { printResult(it, project, args) }

        if (args.xmlFile != null) {
            val allCheckedModels = itemsToCheck.modules.flatMap {
                it.models.filter { !SModelStereotype.isDescriptorModel(it) }
            }.union(itemsToCheck.models)
            writeJunitXml(allCheckedModels, errorCollector.result, project, args.warningAsError, File(args.xmlFile!!))
        }
    }

    val hasErrors = if (args.warningAsError) {
        errorCollector.result.any { it.severity == MessageStatus.WARNING }
    } else {
        errorCollector.result.any { it.severity == MessageStatus.ERROR }
    }

    return hasErrors
}

fun main(args: Array<String>) = mainBody {

    val parsed = ArgParser(args).parseInto(::ModelCheckArgs)
    var hasErrors = true
    try {
        hasErrors = executeWithProject(parsed) { project -> modelCheckProject(parsed, project) }
    } catch (ex: java.lang.Exception) {
        logger.fatal("error model checking", ex)
    } catch (t: Throwable) {
        logger.fatal("error model checking", t)
    }

    if (hasErrors && !parsed.dontFailOnError) {
        System.exit(-1)
    }

    System.exit(0)
}
