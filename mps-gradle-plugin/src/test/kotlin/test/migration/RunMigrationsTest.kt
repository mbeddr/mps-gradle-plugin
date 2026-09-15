package test.migration

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import support.FOOJAY_RESOLVER_CONVENTION_VERSION
import support.JAVA_VERSION_FOR_MPS
import support.MPS_VERSION
import support.extractTestProject
import java.io.File

class RunMigrationsTest {
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
            plugins {
                id("org.gradle.toolchains.foojay-resolver-convention") version ("$FOOJAY_RESOLVER_CONVENTION_VERSION")
            }
            rootProject.name = "hello-world"
        """.trimIndent()
        )
        buildFile = testProjectDir.newFile("build.gradle.kts")
        mpsTestPrjLocation = testProjectDir.newFolder("mps-prj")
        extractTestProject("test-project", mpsTestPrjLocation)
    }

    @Test
    fun `MpsMigrate task works`() {
        buildFile.writeText(
            """
                import de.itemis.mps.gradle.tasks.MpsMigrate

                plugins {
                    id("de.itemis.mps.gradle.common")
                    `jvm-toolchains`
                }

                repositories {
                    mavenCentral()
                    maven("https://artifacts.itemis.cloud/repository/maven-mps")
                }

                val mps = configurations.create("mps")
                dependencies {
                    mps("com.jetbrains:mps:$MPS_VERSION")
                }

                val resolveMps by tasks.registering(Sync::class) {
                    from(Callable { zipTree(mps.singleFile) })
                    into(layout.buildDirectory.dir("mps"))
                }

                val migrate by tasks.registering(MpsMigrate::class) {
                    projectLocations.from("$mpsTestPrjLocation")
                    mpsHome = layout.dir(resolveMps.map { it.destinationDir })
                    javaLauncher = javaToolchains.launcherFor {
                        languageVersion = JavaLanguageVersion.of(${JAVA_VERSION_FOR_MPS})
                        vendor = JvmVendorSpec.JETBRAINS
                    }
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
