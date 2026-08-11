package test.modelchecking

import de.itemis.mps.gradle.ErrorMessages
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.hamcrest.CoreMatchers.containsString
import org.hamcrest.CoreMatchers.not
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import support.extractTestProject
import java.io.File

class MpsCheckTaskTest {
    val mpsVersion = "2025.1.3"

    @Rule
    @JvmField
    val testProjectDir: TemporaryFolder = TemporaryFolder()

    private fun settingsScriptBoilerplate() = """
        plugins {
            id("org.gradle.toolchains.foojay-resolver-convention") version ("0.7.0")
        }
    """.trimIndent()

    private fun buildScriptBoilerplate() = """
            import de.itemis.mps.gradle.tasks.MpsCheck

            plugins {
                id("de.itemis.mps.gradle.common")
            }
            
            repositories {
                mavenCentral()
                maven("https://artifacts.itemis.cloud/repository/maven-mps")
            }
            
            val mps = configurations.create("mps")
            
            dependencies {
                mps("com.jetbrains:mps:$mpsVersion")
            }
            
            val resolveMps by tasks.registering(Sync::class) {
                from({ zipTree(mps.singleFile) })
                into(layout.buildDirectory.dir("mps"))
            }
    """.trimIndent() + "\n"

    @Test
    fun noMpsProject() {
        val settingsFile = testProjectDir.newFile("settings.gradle.kts")
        val buildFile = testProjectDir.newFile("build.gradle.kts")
        val mpsTestPrjLocation = testProjectDir.newFolder("mps-prj")

        extractTestProject("test-project", mpsTestPrjLocation)

        settingsFile.writeText(settingsScriptBoilerplate())
        buildFile.writeText(buildScriptBoilerplate() + """
            val checkProject by tasks.registering(MpsCheck::class) {
                mpsHome.set(layout.dir(resolveMps.map { it.destinationDir }))
                junitFile.set(layout.buildDirectory.file("output.xml"))
            }
        """.trimIndent())

        val result = gradleRunner().withArguments("checkProject").buildAndFail()

        Assert.assertEquals(TaskOutcome.FAILED, result.task(":checkProject")?.outcome)
        assertThat(result.output, containsString(ErrorMessages.noMpsProjectIn(testProjectDir.root.canonicalFile)))
    }

    @Test
    fun simple() {
        val settingsFile = testProjectDir.newFile("settings.gradle.kts")
        val buildFile = testProjectDir.newFile("build.gradle.kts")
        val mpsTestPrjLocation = testProjectDir.newFolder("mps-prj")

        extractTestProject("test-project", mpsTestPrjLocation)

        settingsFile.writeText(settingsScriptBoilerplate())
        buildFile.writeText(buildScriptBoilerplate() + """
            val checkProject by tasks.registering(MpsCheck::class) {
                mpsHome.set(layout.dir(resolveMps.map { it.destinationDir }))
                projectLocation.set(file("mps-prj"))
                junitFile.set(layout.buildDirectory.file("output.xml"))
                pluginRoots.add(layout.dir(resolveMps.map { File(it.destinationDir, "plugins/mps-console") }))
            }
        """.trimIndent())

        val result = gradleRunner().withArguments("checkProject").build()

        Assert.assertEquals(TaskOutcome.SUCCESS, result.task(":checkProject")?.outcome)
        assertThat("no duplicate actions", result.output, not(containsString("Cannot add an action twice")))
        Assert.assertTrue("output file must exist", testProjectDir.root.resolve("build/output.xml").isFile)
    }

    @Test
    fun noDuplicateActions() {
        val settingsFile = testProjectDir.newFile("settings.gradle.kts")
        val buildFile = testProjectDir.newFile("build.gradle.kts")
        val mpsTestPrjLocation = testProjectDir.newFolder("mps-prj")

        extractTestProject("test-project", mpsTestPrjLocation)

        settingsFile.writeText(settingsScriptBoilerplate())
        buildFile.writeText(buildScriptBoilerplate() + """
            val checkProject by tasks.registering(MpsCheck::class) {
                mpsHome.set(layout.dir(resolveMps.map { it.destinationDir }))
                projectLocation.set(file("mps-prj"))
                junitFile.set(layout.buildDirectory.file("output.xml"))
            }
        """.trimIndent())

        val result = gradleRunner().withArguments("checkProject").build()

        Assert.assertEquals(TaskOutcome.SUCCESS, result.task(":checkProject")?.outcome)
        assertThat("no duplicate actions", result.output, not(containsString("Cannot add an action twice")))
        Assert.assertTrue("output file must exist", testProjectDir.root.resolve("build/output.xml").isFile)
    }

    @Test
    fun failsOnErrors() {
        val settingsFile = testProjectDir.newFile("settings.gradle.kts")
        val buildFile = testProjectDir.newFile("build.gradle.kts")
        val mpsTestPrjLocation = testProjectDir.newFolder("mps-prj")
        val junitFile = testProjectDir.root.resolve("build/output.xml")

        extractTestProject("test-project-with-errors", mpsTestPrjLocation)

        settingsFile.writeText(settingsScriptBoilerplate())
        buildFile.writeText(buildScriptBoilerplate() + """
            val checkProject by tasks.registering(MpsCheck::class) {
                mpsHome.set(layout.dir(resolveMps.map { it.destinationDir }))
                projectLocation.set(file("mps-prj"))
                junitFile.set(layout.buildDirectory.file("output.xml"))
            }
        """.trimIndent())

        val result = gradleRunner()
            .withArguments("checkProject")
            .forwardOutput()
            .buildAndFail()
        Assert.assertEquals(TaskOutcome.FAILED, result.task(":checkProject")?.outcome)
        Assert.assertTrue("JUnit-formatted results file should exist", junitFile.exists())
    }

    @Test
    fun caching() {
        val settingsFile = testProjectDir.newFile("settings.gradle.kts")
        val buildFile = testProjectDir.newFile("build.gradle.kts")
        val mpsTestPrjLocation = testProjectDir.newFolder("mps-prj")
        val buildDir = testProjectDir.root.resolve("build")

        extractTestProject("test-project", mpsTestPrjLocation)

        settingsFile.writeText(settingsScriptBoilerplate())
        buildFile.writeText(buildScriptBoilerplate() + """
            val checkProject by tasks.registering(MpsCheck::class) {
                mpsHome.set(layout.dir(resolveMps.map { it.destinationDir }))
                projectLocation.set(file("mps-prj"))
            }
        """.trimIndent())

        val result = gradleRunner().withArguments("checkProject", "--build-cache").build()

        Assert.assertEquals(TaskOutcome.SUCCESS, result.task(":checkProject")?.outcome)

        buildDir.deleteRecursively()

        val secondResult = gradleRunner().withArguments("checkProject", "--build-cache").build()

        println(secondResult.output)
        Assert.assertEquals(TaskOutcome.FROM_CACHE, secondResult.task(":checkProject")?.outcome)
    }

    private fun gradleRunner(): GradleRunner = GradleRunner.create()
        .withProjectDir(testProjectDir.root)
        .withPluginClasspath()
}
