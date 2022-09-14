package test.de.itemis.mps.gradle

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class JBRDownloadTest {

    @Rule
    @JvmField
    val testProjectDir: TemporaryFolder = TemporaryFolder()
    private lateinit var settingsFile: File
    private lateinit var buildFile: File
    private lateinit var cp: List<File>

    @Before
    fun setup() {
        settingsFile = testProjectDir.newFile("settings.gradle.kts")
        buildFile = testProjectDir.newFile("build.gradle.kts")
        cp = javaClass.classLoader.getResource(
                "plugin-classpath.txt")!!.readText().lines().map { File(it) }
    }

    @Test
    fun `download with download dir`() {
        settingsFile.writeText("""
            rootProject.name = "hello-world"
        """.trimIndent())

        buildFile.writeText("""
            import java.net.URI
            buildscript {
                dependencies {
                    "classpath"(files(${cp.joinToString { """"${it.invariantSeparatorsPath}"""" }}))
                }
            }
            
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
                jbrVersion.set("11_0_10-b1145.96")
                downloadDir.set(file("jbrdl"))
            }
        """.trimIndent())

        val result = GradleRunner.create()
                .withProjectDir(testProjectDir.root)
                .withArguments("downloadJbr")
                .withPluginClasspath(cp)
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
            buildscript {
                dependencies {
                    "classpath"(files(${cp.joinToString { """"${it.invariantSeparatorsPath}"""" }}))
                }
            }
            
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
                jbrVersion.set("11_0_10-b1145.96")
            }
        """.trimIndent())

        val result = GradleRunner.create()
                .withProjectDir(testProjectDir.root)
                .withArguments("downloadJbr")
                .withPluginClasspath(cp)
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
            buildscript {
                dependencies {
                    "classpath"(files(${cp.joinToString { """"${it.invariantSeparatorsPath}"""" }}))
                }
            }
            
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
                jbrVersion.set("11_0_11-b1341.60")
            }
        """.trimIndent())

        val result = GradleRunner.create()
                .withProjectDir(testProjectDir.root)
                .withArguments("downloadJbr")
                .withPluginClasspath(cp)
                .build()
        Assert.assertEquals(TaskOutcome.SUCCESS, result.task(":downloadJbr")?.outcome)
        val jbrDownloadDir = File(testProjectDir.root, "build/jbrDownload")
        Assert.assertTrue(jbrDownloadDir.exists())
        val jbrReleaseFile = File(jbrDownloadDir, "jbr/release")
        Assert.assertTrue(jbrReleaseFile.exists())
        Assert.assertTrue("downloaded jbr doesn't contain expected distro", jbrReleaseFile.readText().contains("1341.60-jcef"))
    }

    @Test
    fun `download new version with distribution type`() {
        settingsFile.writeText("""
            rootProject.name = "hello-world"
        """.trimIndent())

        buildFile.writeText("""
            import java.net.URI
            buildscript {
                dependencies {
                    "classpath"(files(${cp.joinToString { """"${it.invariantSeparatorsPath}"""" }}))
                }
            }
            
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
                jbrVersion.set("11_0_11-b1341.60")
                distributionType.set("jbr_nomod")
            }
        """.trimIndent())

        val result = GradleRunner.create()
            .withProjectDir(testProjectDir.root)
            .withArguments("downloadJbr")
            .withPluginClasspath(cp)
            .build()
        Assert.assertEquals(TaskOutcome.SUCCESS, result.task(":downloadJbr")?.outcome)
        val jbrDownloadDir = File(testProjectDir.root, "build/jbrDownload")
        Assert.assertTrue(jbrDownloadDir.exists())
        val jbrReleaseFile = File(jbrDownloadDir, "jbr/release")
        Assert.assertTrue(jbrReleaseFile.exists())
        Assert.assertTrue("downloaded jbr doesn't contain expected distro", jbrReleaseFile.readText().contains("1341.60-nomod"))
    }

    @Test
    fun `executed downloaded java`() {
        settingsFile.writeText("""
            rootProject.name = "hello-world"
        """.trimIndent())

        buildFile.writeText("""
            import java.net.URI
            buildscript {
                dependencies {
                    "classpath"(files(${cp.joinToString { """"${it.invariantSeparatorsPath}"""" }}))
                }
            }
            
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
                jbrVersion.set("11_0_11-b1341.60")
            }
            tasks.register<Exec>("exec") {
                dependsOn(tasks.getByName("downloadJbr", de.itemis.mps.gradle.downloadJBR.DownloadJbrForPlatform::class))
              
                
                executable = tasks.getByName("downloadJbr", de.itemis.mps.gradle.downloadJBR.DownloadJbrForPlatform::class).javaExecutable.get().getAsFile().getAbsolutePath()
                args("--version")
            }
        """.trimIndent())

        val result = GradleRunner.create()
                .withProjectDir(testProjectDir.root)
                .withArguments("exec")
                .withPluginClasspath(cp)
                .build()
        Assert.assertEquals(TaskOutcome.SUCCESS, result.task(":exec")?.outcome)
    }
}