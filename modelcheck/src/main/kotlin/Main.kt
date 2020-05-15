package de.itemis.mps.gradle.modelcheck

import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.default
import com.xenomachina.argparser.mainBody
import de.itemis.mps.gradle.junit.Failure
import de.itemis.mps.gradle.junit.Testcase
import de.itemis.mps.gradle.junit.Testsuite
import de.itemis.mps.gradle.junit.Testsuites
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
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.min
import kotlin.test.fail

private val logger = Logger.getLogger("de.itemis.mps.gradle.generate")

enum class ReportFormat {
    ONE_TEST_PER_MODEL,
    ONE_TEST_PER_FAILED_MESSAGE
}

class ModelCheckArgs(parser: ArgParser) : Args(parser) {
    val models by parser.adding("--model",
            help = "list of models to check")
    val modules by parser.adding("--module",
            help = "list of modules to check")
    val warningAsError by parser.flagging("--warning-as-error", help = "treat model checker warning as errors")
    val dontFailOnError by parser.flagging("--error-no-fail", help = "report errors but don't fail the build")
    val xmlFile by parser.storing("--result-file", help = "stores the result as an JUnit xml file").default<String?>(null)
    val xmlReportFormat by parser.storing("--result-format", help = "reporting format for the JUnit file") {
        when (this) {
            "model" -> ReportFormat.ONE_TEST_PER_MODEL
            "message" -> ReportFormat.ONE_TEST_PER_FAILED_MESSAGE
            else -> fail("unsupported result format")
        }
    }.default(ReportFormat.ONE_TEST_PER_MODEL)
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

fun getCurrentTimeStamp(): String {
    val df = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss")
    return df.format(Date())
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
                  format: ReportFormat,
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

    val xmlMapper = XmlMapper()

    when (format) {
        ReportFormat.ONE_TEST_PER_MODEL -> {
            val testcases = oneTestCasePerModel(models, errorsPerModel, project)
            val testsuite = Testsuite(name = "model checking",
                    failures = allErrors.size,
                    testcases = testcases,
                    tests = models.count(),
                    timestamp = getCurrentTimeStamp())
            xmlMapper.writeValue(file, testsuite)
        }

        ReportFormat.ONE_TEST_PER_FAILED_MESSAGE -> {
            val testsuits = models.mapIndexed { i: Int, mdl: SModel ->
                val errorsInModel = errorsPerModel[mdl] ?: emptyList()
                Testsuite(name = mdl.name.simpleName,
                        pkg = mdl.name.namespace,
                        failures = errorsInModel.size,
                        id = i,
                        tests = errorsInModel.size,
                        timestamp = getCurrentTimeStamp(),
                        testcases = errorsInModel.map { item -> oneTestCasePerMessage(item, mdl, project) })
            }
            xmlMapper.writeValue(file, Testsuites(testsuits))
        }
    }


}

private fun oneTestCasePerMessage(item: IssueKindReportItem, model: SModel, project: Project): Testcase {
    // replace also ':', as otherwise the string before could be recognized as class name
    val testCaseName = item.message.replace(Regex("[:\\s]"), "_").substring(0, min(item.message.length, 120))
    return when (val path = IssueKindReportItem.PATH_OBJECT.get(item)) {
        is IssueKindReportItem.PathObject.ModelPathObject -> {
            val message = "${item.message} [${model.name.longName}]"
            val className = model.name.longName
            Testcase(
                    name = testCaseName,
                    classname = className,
                    failure = Failure(message = message, type = item.issueKind.toString()),
                    time = 0
            )
        }
        is IssueKindReportItem.PathObject.NodePathObject -> {
            val node = path.resolve(project.repository)
            val url = HttpSupportUtil.getURL(node)
            val message = "${item.message} [$url]"
            val className = node.containingRoot.presentation + "." + node.nodeId
            Testcase(
                    name = testCaseName,
                    classname = className,
                    failure = Failure(message = message, type = item.issueKind.toString()),
                    time = 0
            )
        }
        else -> fail("unexpected issue kind")
    }
}


private fun oneTestCasePerModel(models: Iterable<SModel>, errorsPerModel: Map<SModel, List<IssueKindReportItem>>, project: Project): List<Testcase> {
    return models.map {
        val errors = errorsPerModel.getOrDefault(it, emptyList())
        fun reportItemToContent(s: Failure, item: IssueKindReportItem): Failure {
            return when (val path = IssueKindReportItem.PATH_OBJECT.get(item)) {
                is IssueKindReportItem.PathObject.ModelPathObject -> {
                    val model = path.resolve(project.repository)!!
                    val message = "${item.message} [${model.name.longName}]"
                    Failure(
                            message = "${s.message}\n $message",
                            type = s.type
                    )
                }
                is IssueKindReportItem.PathObject.NodePathObject -> {
                    val node = path.resolve(project.repository)
                    val url = HttpSupportUtil.getURL(node)
                    val message = "${item.message} [$url]"
                    Failure(
                            message = "${s.message}\n $message",
                            type = s.type
                    )
                }
                else -> fail("unexpected issue kind")
            }
        }

        Testcase(
                name = it.name.simpleName,
                classname = it.name.longName,
                failure = errors.fold(Failure(message = "",  type = "model checking"), ::reportItemToContent),
                time = 0
        )
    }
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
                    .flatMap { module -> module.models.filter { !SModelStereotype.isDescriptorModel(it) && !SModelStereotype.isStubModel(it) } }
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
            val allCheckedModels = itemsToCheck.modules.flatMap { module ->
                module.models.filter { !SModelStereotype.isDescriptorModel(it) }
            }.union(itemsToCheck.models)
            writeJunitXml(allCheckedModels, errorCollector.result, project, args.warningAsError, args.xmlReportFormat, File(args.xmlFile!!))
        }
    }

    return if (args.warningAsError) {
        errorCollector.result.any { it.severity == MessageStatus.WARNING }
    } else {
        errorCollector.result.any { it.severity == MessageStatus.ERROR }
    }
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
