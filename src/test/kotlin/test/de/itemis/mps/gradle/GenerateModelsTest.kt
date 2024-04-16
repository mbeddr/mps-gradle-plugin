package test.de.itemis.mps.gradle

import de.itemis.mps.gradle.ErrorMessages
import de.itemis.mps.gradle.generate.GeneratePluginExtensions
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.junit.*
import org.junit.rules.TemporaryFolder
import java.io.File

class GenerateModelsTest {
    @Rule
    @JvmField
    val testProjectDir: TemporaryFolder = TemporaryFolder()
    private lateinit var settingsFile: File
    private lateinit var buildFile: File
    private lateinit var cp: List<File>
    private lateinit var mpsTestPrjLocation: File


    @Before
    fun setup() {
        settingsFile = testProjectDir.newFile("settings.gradle.kts")
        buildFile = testProjectDir.newFile("build.gradle.kts")
        mpsTestPrjLocation = testProjectDir.newFolder("mps-prj")
        extractTestProject("test-project", mpsTestPrjLocation)
    }

    @Test
    fun `generate works with latest MPS in MPS environment`() {
        settingsFile.writeText(
            """
            rootProject.name = "hello-world"
        """.trimIndent()
        )

        buildFile.writeText(
            """
            import java.net.URI
            import de.itemis.mps.gradle.EnvironmentKind
            
            plugins {
                id("generate-models")
            }
            
            repositories {
                mavenCentral()
                maven {
                    url = URI("https://artifacts.itemis.cloud/repository/maven-mps")
                }
            }
            
            val mps = configurations.create("mps")
            
            dependencies {
                mps("com.jetbrains:mps:2021.3.1")
            }
            
            generate {
                projectLocation = file("${mpsTestPrjLocation.toPath()}")
                mpsConfig = mps
                environmentKind.set(EnvironmentKind.MPS)
            }
        """.trimIndent()
        )

        val result = GradleRunner.create()
            .withProjectDir(testProjectDir.root)
            .withArguments("generate")
            .withPluginClasspath()
            .build()
        Assert.assertEquals(TaskOutcome.SUCCESS, result.task(":generate")?.outcome)
    }
    @Test
    fun `generate fails with unsupported MPS`() {
        settingsFile.writeText(
            """
            rootProject.name = "hello-world"
        """.trimIndent()
        )

        buildFile.writeText(
            """
            import java.net.URI
            
            plugins {
                id("generate-models")
            }
            
            repositories {
                mavenCentral()
                maven {
                    url = URI("https://artifacts.itemis.cloud/repository/maven-mps")
                }
            }
            
            val mps = configurations.create("mps")
            
            dependencies {
                mps("com.jetbrains:mps:2019.3.7")
            }
            
            generate {
                projectLocation = file("${mpsTestPrjLocation.toPath()}")
                mpsConfig = mps
            }
        """.trimIndent()
        )

        val result = GradleRunner.create()
            .withProjectDir(testProjectDir.root)
            .withArguments("generate")
            .withPluginClasspath()
            .buildAndFail()

        MatcherAssert.assertThat(result.output, CoreMatchers.containsString(ErrorMessages.MPS_VERSION_NOT_SUPPORTED))
    }
    @Test
    fun `generate works with set MPS version and path`() {
        settingsFile.writeText(
            """
            rootProject.name = "hello-world"
        """.trimIndent()
        )

        val mpsFolder = testProjectDir.newFolder("mps")

        buildFile.writeText(
            """
            import java.net.URI
            
            plugins {
                id("generate-models")
            }
            
            repositories {
                mavenCentral()
                maven {
                    url = URI("https://artifacts.itemis.cloud/repository/maven-mps")
                }
            }
            
            val mps = configurations.create("mps")
            
            dependencies {
                mps("com.jetbrains:mps:2020.3.3")
            }
            
            val resolveMps by tasks.registering(Sync::class) {
                from({ project.zipTree(mps.singleFile) })
                into("$mpsFolder")
            }
            
            generate {
                projectLocation = file("${mpsTestPrjLocation.toPath()}")
                mpsVersion = "2020.2.2"
                mpsLocation = file("$mpsFolder")
            }
        """.trimIndent()
        )

        val result = GradleRunner.create()
            .withProjectDir(testProjectDir.root)
            .withArguments("resolveMps", "generate")
            .withPluginClasspath()
            .build()

        Assert.assertEquals(TaskOutcome.SUCCESS, result.task(":generate")?.outcome)
    }

    @Test
    fun `generate fails with set MPS invalid version and path`() {
        settingsFile.writeText(
            """
            rootProject.name = "hello-world"
        """.trimIndent()
        )

        buildFile.writeText(
            """
            import java.net.URI
            
            plugins {
                id("generate-models")
            }
            
            repositories {
                mavenCentral()
                maven {
                    url = URI("https://artifacts.itemis.cloud/repository/maven-mps")
                }
            }
            
            val mps = configurations.create("mps")
            
            dependencies {
                mps("com.jetbrains:mps:2020.3.3")
            }
            
            generate {
                projectLocation = file("${mpsTestPrjLocation.toPath()}")
                mpsVersion = "2019.2.2"
                mpsLocation = file(".")
            }
        """.trimIndent()
        )

        val result = GradleRunner.create()
            .withProjectDir(testProjectDir.root)
            .withArguments()
            .withPluginClasspath()
            .buildAndFail()

        MatcherAssert.assertThat(result.output, CoreMatchers.containsString(ErrorMessages.MPS_VERSION_NOT_SUPPORTED))

    }
    @Test
    fun `generate fails with only MPS version set`() {
        settingsFile.writeText(
            """
            rootProject.name = "hello-world"
        """.trimIndent()
        )

        buildFile.writeText(
            """
            import java.net.URI
            
            plugins {
                id("generate-models")
            }
            
            repositories {
                mavenCentral()
                maven {
                    url = URI("https://artifacts.itemis.cloud/repository/maven-mps")
                }
            }
            
            val mps = configurations.create("mps")
            
            dependencies {
                mps("com.jetbrains:mps:2020.3.3")
            }
            
            generate {
                projectLocation = file("${mpsTestPrjLocation.toPath()}")
                mpsVersion = "2020.2.2"
            }
        """.trimIndent()
        )

        val result = GradleRunner.create()
            .withProjectDir(testProjectDir.root)
            .withArguments()
            .withPluginClasspath()
            .buildAndFail()
        MatcherAssert.assertThat(result.output, CoreMatchers.containsString(ErrorMessages.mustSetConfigOrLocation("generate")))
    }
    @Test
    fun `generate fails with only MPS path set`() {
        settingsFile.writeText(
            """
            rootProject.name = "hello-world"
        """.trimIndent()
        )

        buildFile.writeText(
            """
            import java.net.URI
            
            plugins {
                id("generate-models")
            }
            
            repositories {
                mavenCentral()
                maven {
                    url = URI("https://artifacts.itemis.cloud/repository/maven-mps")
                }
            }
            
            val mps = configurations.create("mps")
            
            dependencies {
                mps("com.jetbrains:mps:2020.3.3")
            }
            
            generate {
                projectLocation = file("${mpsTestPrjLocation.toPath()}")
                mpsLocation = file(".")
            }
        """.trimIndent()
        )

        val result = GradleRunner.create()
            .withProjectDir(testProjectDir.root)
            .withArguments()
            .withPluginClasspath()
            .buildAndFail()
        MatcherAssert.assertThat(result.output, CoreMatchers.containsString(ErrorMessages.mustSetVersionWhenNoMpsConfiguration("generate")))
    }

    @Test
    fun `explicit javaExec`() {
        buildFile.writeText(
            """
                import de.itemis.mps.gradle.downloadJBR.DownloadJbrForPlatform
                
                plugins {
                    id("generate-models")
                    id("download-jbr")
                }
                
                downloadJbr {
                    jbrVersion = "11_0_10-b1341.41"
                }

                generate {
                    projectLocation = projectDir
                    mpsLocation = file("build/mps")
                    mpsVersion = "2020.3.3"
                    javaExec = (tasks.getByName("downloadJbr") as DownloadJbrForPlatform).javaExecutable
                }
                
                tasks.register("verify") {
                    doLast {
                        val generateLauncherPresent = (tasks.getByName("generate") as JavaExec).javaLauncher.isPresent
                        println("generate.javaLauncher.isPresent: " + generateLauncherPresent)
                    }
                }
            """.trimIndent()
        )

        val result = GradleRunner.create()
            .withProjectDir(testProjectDir.root)
            .withArguments("verify")
            .withPluginClasspath()
            .build()

        Assert.assertEquals(TaskOutcome.SUCCESS, result.task(":verify")?.outcome)

        // When javaExec is explicitly set, the launcher should be absent
        Assert.assertTrue("generate.javaLauncher should not be present",
            result.output.contains("generate.javaLauncher.isPresent: false"))
    }
}
