package test.others

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class JBRDownloadTest {

    val JBR_VERSION = "11_0_10-b1341.41"

    @Rule
    @JvmField
    val testProjectDir: TemporaryFolder = TemporaryFolder()
    private lateinit var settingsFile: File
    private lateinit var buildFile: File

    @Before
    fun setup() {
        settingsFile = testProjectDir.newFile("settings.gradle.kts")
        buildFile = testProjectDir.newFile("build.gradle.kts")
    }

    @Test
    fun `download with download dir`() {
        settingsFile.writeText("""
            rootProject.name = "hello-world"
        """.trimIndent())

        buildFile.writeText("""
            import java.net.URI
            
            plugins {
                id("download-jbr")
            }
            
            repositories {
                mavenCentral()
                maven {
                    url = URI("https://artifacts.itemis.cloud/repository/maven-mps")
                }
            }
            
            downloadJbr {
                jbrVersion = "$JBR_VERSION"
                downloadDir = file("jbrdl")
            }
        """.trimIndent())

        val result = GradleRunner.create()
                .withProjectDir(testProjectDir.root)
                .withArguments("downloadJbr")
                .withPluginClasspath()
                .build()
        Assert.assertEquals(TaskOutcome.SUCCESS, result.task(":downloadJbr")?.outcome)
        Assert.assertTrue(File(testProjectDir.root, "jbrdl").exists())
    }
    @Test
    fun `download without download dir`() {
        settingsFile.writeText("""
            rootProject.name = "hello-world"
        """.trimIndent())

        buildFile.writeText("""
            import java.net.URI

            plugins {
                id("download-jbr")
            }
            
            repositories {
                mavenCentral()
                maven {
                    url = URI("https://artifacts.itemis.cloud/repository/maven-mps")
                }
            }
            
            downloadJbr {
                jbrVersion = "$JBR_VERSION"
            }
        """.trimIndent())

        val result = GradleRunner.create()
                .withProjectDir(testProjectDir.root)
                .withArguments("downloadJbr")
                .withPluginClasspath()
                .build()
        Assert.assertEquals(TaskOutcome.SUCCESS, result.task(":downloadJbr")?.outcome)
        Assert.assertTrue(File(testProjectDir.root, "build/jbrDownload").exists())
    }

    @Test
    fun `download new version without distribution type`() {
        settingsFile.writeText("""
            rootProject.name = "hello-world"
        """.trimIndent())

        buildFile.writeText("""
            import java.net.URI
            
            plugins {
                id("download-jbr")
            }
            
            repositories {
                mavenCentral()
                maven {
                    url = URI("https://artifacts.itemis.cloud/repository/maven-mps")
                }
            }
            
            downloadJbr {
                jbrVersion = "$JBR_VERSION"
            }
        """.trimIndent())

        val result = GradleRunner.create()
                .withProjectDir(testProjectDir.root)
                .withArguments("downloadJbr")
                .withPluginClasspath()
                .build()
        Assert.assertEquals(TaskOutcome.SUCCESS, result.task(":downloadJbr")?.outcome)
        Assert.assertTrue(File(testProjectDir.root, "build/jbrDownload").exists())
    }

    @Test
    fun `download new version with distribution type`() {
        settingsFile.writeText("""
            rootProject.name = "hello-world"
        """.trimIndent())

        buildFile.writeText("""
            import java.net.URI
            
            plugins {
                id("download-jbr")
            }
            
            repositories {
                mavenCentral()
                maven {
                    url = URI("https://artifacts.itemis.cloud/repository/maven-mps")
                }
            }
            
            downloadJbr {
                jbrVersion = "$JBR_VERSION"
                distributionType = "jbr_nomod"
            }
        """.trimIndent())

        val result = GradleRunner.create()
            .withProjectDir(testProjectDir.root)
            .withArguments("downloadJbr")
            .withPluginClasspath()
            .build()
        Assert.assertEquals(TaskOutcome.SUCCESS, result.task(":downloadJbr")?.outcome)
        Assert.assertTrue(File(testProjectDir.root, "build/jbrDownload").exists())
    }

    @Test
    fun `executed downloaded java`() {
        settingsFile.writeText("""
            rootProject.name = "hello-world"
        """.trimIndent())

        buildFile.writeText("""
            import java.net.URI
            
            plugins {
                id("download-jbr")
            }
            
            repositories {
                mavenCentral()
                maven {
                    url = URI("https://artifacts.itemis.cloud/repository/maven-mps")
                }
            }
            
            downloadJbr {
                jbrVersion = "$JBR_VERSION"
            }
            tasks.register<Exec>("exec") {
                dependsOn(tasks.getByName("downloadJbr", de.itemis.mps.gradle.downloadJBR.DownloadJbrForPlatform::class))
                executable = tasks.getByName("downloadJbr", de.itemis.mps.gradle.downloadJBR.DownloadJbrForPlatform::class).javaExecutable.absolutePath
                args("--version")
            }
        """.trimIndent())

        val result = GradleRunner.create()
                .withProjectDir(testProjectDir.root)
                .withArguments("exec")
                .withPluginClasspath()
                .build()
        Assert.assertEquals(TaskOutcome.SUCCESS, result.task(":exec")?.outcome)
    }
}
