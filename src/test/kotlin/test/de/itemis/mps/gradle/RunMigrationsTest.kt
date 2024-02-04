package test.de.itemis.mps.gradle

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.hamcrest.CoreMatchers.containsString
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Assert
import org.junit.Assert.assertEquals
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
    private lateinit var mpsTestPrjLocation: File
    
    private fun commonGradleScriptPart():String {
        return """
            import java.net.URI
            
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
        mpsTestPrjLocation = testProjectDir.newFolder("mps-prj")
        extractProject("test-project")
    }

    private fun extractProject(name: String) = extractTestProject(name, mpsTestPrjLocation)

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

        val result = gradleRunner()
            .withArguments("runMigrations")
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

        val result = gradleRunner()
            .withArguments("runMigrations")
            .buildAndFail()

        assertThat(result.output, containsString("The force migration flag is only supported for MPS version 2021.3.0 and higher."))
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

        val result = gradleRunner()
            .withArguments("runMigrations")
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

        val result = gradleRunner()
            .withArguments("runMigrations")
            .buildAndFail()

        assertThat(result.output, containsString("The path to the project doesn't exist:"))
    }

    @Test
    fun `check run migrations fails with invalid mps path`() {
        buildFile.writeText(
            """
            ${commonGradleScriptPart()}
            
            runMigrations {
                projectLocation = file("${mpsTestPrjLocation.toPath()}")
                mpsLocation = file("not_existing")
                mpsVersion = "2021.3.2"
            }
        """.trimIndent()
        )

        val result = gradleRunner()
            .withArguments("runMigrations")
            .buildAndFail()

        assertThat(result.output, containsString("Specified MPS location does not exist or is not a directory:"))
    }

    @Test
    fun `MpsMigrate task works`() {
        buildFile.writeText(
            """
                import de.itemis.mps.gradle.tasks.MpsMigrate

                plugins {
                    id("de.itemis.mps.gradle.common")
                }

                repositories {
                    mavenCentral()
                    maven("https://artifacts.itemis.cloud/repository/maven-mps")
                }

                val mps = configurations.create("mps")
                dependencies {
                    mps("com.jetbrains:mps:2021.3.2")
                }

                val resolveMps by tasks.registering(Sync::class) {
                    from(Callable { zipTree(mps.singleFile) })
                    into(layout.buildDirectory.dir("mps"))
                }

                val migrate by tasks.registering(MpsMigrate::class) {
                    projectDirectories.from("$mpsTestPrjLocation")
                    mpsHome.set(layout.dir(resolveMps.map { it.destinationDir }))
                }
            """.trimIndent()
        )

        val result = gradleRunner().withArguments("migrate").build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":migrate")?.outcome)
    }

    private fun gradleRunner(): GradleRunner = GradleRunner.create()
        .withProjectDir(testProjectDir.root)
        .withPluginClasspath()
}
