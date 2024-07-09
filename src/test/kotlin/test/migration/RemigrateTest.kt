package test.migration

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import support.extractTestProject
import java.io.File

class RemigrateTest {
    @Rule
    @JvmField
    val testProjectDir: TemporaryFolder = TemporaryFolder()
    private lateinit var settingsFile: File
    private lateinit var buildFile: File
    private lateinit var mpsTestPrjLocation: File

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
    fun `Remigrate task works`() {
        buildFile.writeText(
            """
                import de.itemis.mps.gradle.tasks.Remigrate
                import de.itemis.mps.gradle.tasks.ExcludedModuleMigration

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

                val remigrate by tasks.registering(Remigrate::class) {
                    projectDirectories.from("$mpsTestPrjLocation")
                    mpsHome.set(layout.dir(resolveMps.map { it.destinationDir }))

                    excludedModuleMigrations.add(ExcludedModuleMigration("foo", 0))
                    excludeModuleMigration("bar", 1)
                }
            """.trimIndent()
        )

        val result = gradleRunner().withArguments("remigrate").build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":remigrate")?.outcome)
    }

    private fun gradleRunner(): GradleRunner = GradleRunner.create()
        .withProjectDir(testProjectDir.root)
        .withPluginClasspath()
}
