package test.de.itemis.mps.gradle

import de.itemis.mps.gradle.ErrorMessages
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class ModelCheckWithPluginTest {
    @Rule
    @JvmField
    val testProjectDir: TemporaryFolder = TemporaryFolder()
    private lateinit var settingsFile: File
    private lateinit var buildFile: File
    private lateinit var mpsTestPrjLocation: File
    private lateinit var junitFile: File

    @Before
    fun setup() {
        settingsFile = testProjectDir.newFile("settings.gradle.kts")
        buildFile = testProjectDir.newFile("build.gradle.kts")
        mpsTestPrjLocation = testProjectDir.newFolder("mps-prj")
        junitFile = File(mpsTestPrjLocation, "junit.xml")
    }

    private fun extractProject(name: String) = extractTestProject(name, mpsTestPrjLocation)

    private fun buildScriptBoilerplate(mpsVersion: String) = """
            plugins {
                id("modelcheck")
            }
            
            repositories {
                mavenCentral()
                maven("https://artifacts.itemis.cloud/repository/maven-mps")
            }
            
            val mps = configurations.create("mps")
            
            dependencies {
                mps("com.jetbrains:mps:$mpsVersion")
            }
    """.trimIndent() + "\n"

    @Test
    fun `check model works with latest MPS`() {
        extractProject("test-project")

        settingsFile.writeText(
            """
            rootProject.name = "hello-world"
        """.trimIndent()
        )

        buildFile.writeText(
            buildScriptBoilerplate("2020.3.3") + """
                modelcheck {
                    projectLocation = file("${mpsTestPrjLocation.toPath()}")
                    mpsConfig = mps
                    junitFile = file("${junitFile.absolutePath}")
                }
            """.trimIndent()
        )

        val result = GradleRunner.create()
            .withProjectDir(testProjectDir.root)
            .withArguments("checkmodels")
            .withPluginClasspath()
            .build()
        Assert.assertEquals(TaskOutcome.SUCCESS, result.task(":checkmodels")?.outcome)
        Assert.assertTrue(junitFile.exists())
    }

    @Test
    fun `check model fails if errors are found`() {
        extractProject("test-project-with-errors")

        settingsFile.writeText(
            """
            rootProject.name = "hello-world"
        """.trimIndent()
        )

        buildFile.writeText(
            buildScriptBoilerplate("2021.1.4") +
            """
                modelcheck {
                    projectLocation = file("${mpsTestPrjLocation.toPath()}")
                    mpsConfig = mps
                    junitFile = file("${junitFile.absolutePath}")
                }
            """.trimIndent()
        )

        val result = GradleRunner.create()
            .withProjectDir(testProjectDir.root)
            .withArguments("checkmodels")
            .withPluginClasspath()
            .buildAndFail()
        Assert.assertEquals(TaskOutcome.FAILED, result.task(":checkmodels")?.outcome)
        Assert.assertTrue(junitFile.exists())
    }

    @Test
    fun `check model works with latest MPS and excluded models`() {
        extractProject("test-project-with-errors")

        settingsFile.writeText(
            """
            rootProject.name = "hello-world"
        """.trimIndent()
        )

        buildFile.writeText(
            buildScriptBoilerplate("2021.1.4") +
            """
            modelcheck {
                projectLocation = file("${mpsTestPrjLocation.toPath()}")
                mpsConfig = mps
                junitFile = file("${junitFile.absolutePath}")
                excludeModels = listOf("my.solution.with.errors.java")
            }
            """.trimIndent()
        )

        val result = GradleRunner.create()
            .withProjectDir(testProjectDir.root)
            .withArguments("checkmodels")
            .withPluginClasspath()
            .build()
        Assert.assertEquals(TaskOutcome.SUCCESS, result.task(":checkmodels")?.outcome)
        Assert.assertTrue(junitFile.exists())
    }

    @Test
    fun `check model fails with unsupported MPS`() {
        extractProject("test-project")

        settingsFile.writeText(
            """
            rootProject.name = "hello-world"
        """.trimIndent()
        )

        buildFile.writeText(
            buildScriptBoilerplate("2019.3.7") +
                    """            
            modelcheck {
                projectLocation = file("${mpsTestPrjLocation.toPath()}")
                mpsConfig = mps
                junitFile = file("${junitFile.absolutePath}")

            }
            """.trimIndent()
        )

        val result = GradleRunner.create()
            .withProjectDir(testProjectDir.root)
            .withArguments("checkmodels")
            .withPluginClasspath()
            .buildAndFail()
        MatcherAssert.assertThat(result.output, CoreMatchers.containsString(ErrorMessages.MPS_VERSION_NOT_SUPPORTED))
    }

    @Test
    fun `check model works with set MPS version and path`() {
        extractProject("test-project")

        settingsFile.writeText(
            """
            rootProject.name = "hello-world"
        """.trimIndent()
        )

        buildFile.writeText(
            buildScriptBoilerplate("2020.3.3") +
            """
            modelcheck {
                projectLocation = file("${mpsTestPrjLocation.toPath()}")
                mpsVersion = "2020.2.2"
                mpsLocation = file(".")
                junitFile = file("${junitFile.absolutePath}")
            }
        """.trimIndent()
        )

        GradleRunner.create()
            .withProjectDir(testProjectDir.root)
            .withArguments()
            .withPluginClasspath()
            .build()
    }

    @Test
    fun `check model fails with set MPS invalid version and path`() {
        extractProject("test-project")

        settingsFile.writeText(
            """
            rootProject.name = "hello-world"
        """.trimIndent()
        )

        buildFile.writeText(
            buildScriptBoilerplate("2020.3.3") +
            """
            modelcheck {
                projectLocation = file("${mpsTestPrjLocation.toPath()}")
                mpsVersion = "2019.2.2"
                mpsLocation = file(".")
                junitFile = file("${junitFile.absolutePath}")
            }
        """.trimIndent()
        )

        GradleRunner.create()
            .withProjectDir(testProjectDir.root)
            .withArguments()
            .withPluginClasspath()
            .buildAndFail()
    }

    @Test
    fun `check model fails with only MPS version set`() {
        extractProject("test-project")

        settingsFile.writeText(
            """
            rootProject.name = "hello-world"
        """.trimIndent()
        )

        buildFile.writeText(
            buildScriptBoilerplate("2020.3.3") +
            """
            modelcheck {
                projectLocation = file("${mpsTestPrjLocation.toPath()}")
                mpsVersion = "2020.2.2"
                junitFile = file("${junitFile.absolutePath}")
            }
        """.trimIndent()
        )

        val result = GradleRunner.create()
            .withProjectDir(testProjectDir.root)
            .withArguments()
            .withPluginClasspath()
            .buildAndFail()

        MatcherAssert.assertThat(result.output, CoreMatchers.containsString(ErrorMessages.MUST_SET_VERSION_AND_LOCATION))
    }
    @Test
    fun `check model fails with only MPS path set`() {
        settingsFile.writeText(
            """
            rootProject.name = "hello-world"
        """.trimIndent()
        )

        buildFile.writeText(
            buildScriptBoilerplate("2020.3.3") +
            """
            modelcheck {
                projectLocation = file("${mpsTestPrjLocation.toPath()}")
                mpsLocation = file(".")
                junitFile = file("${junitFile.absolutePath}")
            }
        """.trimIndent()
        )

        val result = GradleRunner.create()
            .withProjectDir(testProjectDir.root)
            .withArguments()
            .withPluginClasspath()
            .buildAndFail()

        MatcherAssert.assertThat(result.output, CoreMatchers.containsString(ErrorMessages.MUST_SET_CONFIG_OR_VERSION))
    }
}