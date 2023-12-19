package test.de.itemis.mps.gradle

import de.itemis.mps.gradle.ErrorMessages
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.hamcrest.CoreMatchers.containsString
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class MpsGenerateTaskTest {
    @Rule
    @JvmField
    val testProjectDir: TemporaryFolder = TemporaryFolder()

    private fun settingsScriptBoilerplate() = """
        plugins {
            id("org.gradle.toolchains.foojay-resolver-convention") version ("0.7.0")
        }
    """.trimIndent()

    private fun buildScriptBoilerplate(mpsVersion: String) = """
            import de.itemis.mps.gradle.tasks.MpsGenerate

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
        buildFile.writeText(buildScriptBoilerplate("2021.3.3") + """
            val generateProject by tasks.registering(MpsGenerate::class) {
                mpsHome.set(layout.dir(resolveMps.map { it.destinationDir }))
            }
        """.trimIndent())

        val result = gradleRunner().withArguments("generateProject").buildAndFail()

        Assert.assertEquals(TaskOutcome.FAILED, result.task(":generateProject")?.outcome)
        assertThat(result.output, containsString(ErrorMessages.noMpsProjectIn(testProjectDir.root.canonicalFile)))
    }

    @Test
    fun simple() {
        val settingsFile = testProjectDir.newFile("settings.gradle.kts")
        val buildFile = testProjectDir.newFile("build.gradle.kts")
        val mpsTestPrjLocation = testProjectDir.newFolder("mps-prj")

        extractTestProject("test-project", mpsTestPrjLocation)

        settingsFile.writeText(settingsScriptBoilerplate())
        buildFile.writeText(buildScriptBoilerplate("2021.3.3") + """
            val generateProject by tasks.registering(MpsGenerate::class) {
                mpsHome.set(layout.dir(resolveMps.map { it.destinationDir }))
                projectLocation.set(file("mps-prj"))
            }
        """.trimIndent())

        val result = gradleRunner().withArguments("generateProject").build()

        Assert.assertEquals(TaskOutcome.SUCCESS, result.task(":generateProject")?.outcome)

        Assert.assertTrue("generated sources directory must exist", mpsTestPrjLocation.resolve("solutions/NewSolution/source_gen").isDirectory)
        Assert.assertTrue("compiled classes directory must exist", mpsTestPrjLocation.resolve("solutions/NewSolution/classes_gen").isDirectory)
    }

    private fun gradleRunner(): GradleRunner = GradleRunner.create()
        .withProjectDir(testProjectDir.root)
        .forwardOutput()
        .withPluginClasspath()
}
