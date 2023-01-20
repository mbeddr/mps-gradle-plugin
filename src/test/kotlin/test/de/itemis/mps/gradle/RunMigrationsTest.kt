package test.de.itemis.mps.gradle

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class RunMigrationsTest {
    @Rule
    @JvmField
    val testProjectDir: TemporaryFolder = TemporaryFolder()
    private lateinit var settingsFile: File
    private lateinit var buildFile: File
    private lateinit var cp: List<File>
    private lateinit var mpsTestPrjLocation: File
    
    private fun commonGradleScriptPart():String {
        return """
            import java.net.URI
            buildscript {
                dependencies {
                    "classpath"(files(${cp.map { """"${it.invariantSeparatorsPath}"""" }.joinToString()}))
                }
            }
            
            plugins {
                id("run-migrations")
            }
            
            repositories {
                mavenCentral()
                maven {
                    url = URI("https://artifacts.itemis.cloud/repository/maven-mps")
                }
            }
            
             val mps = configurations.create("mps")
        """.trimIndent()
    }

    @Before
    fun setup() {
        settingsFile = testProjectDir.newFile("settings.gradle.kts")
        settingsFile.writeText(
            """
            rootProject.name = "hello-world"
        """.trimIndent()
        )
        buildFile = testProjectDir.newFile("build.gradle.kts")
        cp = javaClass.classLoader.getResource(
            "plugin-classpath.txt"
        )!!.readText().lines().map { File(it) }
        mpsTestPrjLocation = testProjectDir.newFolder("mps-prj")
        extractProject("test-project")
    }

    private fun extractProject(name: String) = ProjectHelper().extractTestProject(name, mpsTestPrjLocation)

    @Test
    fun `check run migrations works with MPS 2020_3`() {
        buildFile.writeText(
            """
            ${commonGradleScriptPart()}
            
            dependencies {
                mps("com.jetbrains:mps:2020.3.4")
            }
            
            runMigrations {
                projectLocation = file("${mpsTestPrjLocation.toPath()}")
                mpsConfig = mps
            }
        """.trimIndent()
        )

        val result = GradleRunner.create()
            .withProjectDir(testProjectDir.root)
            .withArguments("runMigrations")
            .withPluginClasspath(cp)
            .build()
        Assert.assertEquals(TaskOutcome.SUCCESS, result.task(":runMigrations")?.outcome)
    }

    @Test
    fun `check run migrations fails with MPS 2020_3 with force set to true`() {
        buildFile.writeText(
            """
            ${commonGradleScriptPart()}
            
            dependencies {
                mps("com.jetbrains:mps:2020.3.4")
            }
            
            runMigrations {
                projectLocation = file("${mpsTestPrjLocation.toPath()}")
                mpsConfig = mps
                force = true
            }
        """.trimIndent()
        )

        GradleRunner.create()
            .withProjectDir(testProjectDir.root)
            .withArguments("runMigrations")
            .withPluginClasspath(cp)
            .buildAndFail()
    }


    @Test
    fun `check run migrations works with MPS 2021_3 with force flag`() {
        buildFile.writeText(
            """
            ${commonGradleScriptPart()}
            
            dependencies {
                mps("com.jetbrains:mps:2021.3.2")
            }
            
            runMigrations {
                projectLocation = file("${mpsTestPrjLocation.toPath()}")
                mpsConfig = mps
                force = true
            }
        """.trimIndent()
        )

        val result = GradleRunner.create()
            .withProjectDir(testProjectDir.root)
            .withArguments("runMigrations")
            .withPluginClasspath(cp)
            .build()
        Assert.assertEquals(TaskOutcome.SUCCESS, result.task(":runMigrations")?.outcome)
    }

    @Test
    fun `check run migrations fails with invalid project path`() {
        buildFile.writeText(
            """
            ${commonGradleScriptPart()}
            
            dependencies {
                mps("com.jetbrains:mps:2021.3.2")
            }
            
            runMigrations {
                projectLocation = file("not_existing")
                mpsConfig = mps
            }
        """.trimIndent()
        )

        GradleRunner.create()
            .withProjectDir(testProjectDir.root)
            .withArguments("runMigrations")
            .withPluginClasspath(cp)
            .buildAndFail()
    }

    @Test
    fun `check run migrations fails with invalid mps path`() {
        buildFile.writeText(
            """
            ${commonGradleScriptPart()}
            
            dependencies {
                mps("com.jetbrains:mps:2021.3.2")
            }
            
            runMigrations {
                projectLocation = file("${mpsTestPrjLocation.toPath()}")
                mpsLocation = file("not_existing")
            }
        """.trimIndent()
        )

        GradleRunner.create()
            .withProjectDir(testProjectDir.root)
            .withArguments("runMigrations")
            .withPluginClasspath(cp)
            .buildAndFail()
    }
}