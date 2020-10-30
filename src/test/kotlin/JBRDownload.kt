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
                    "classpath"(files(${cp.map { """"${it.invariantSeparatorsPath}"""" }.joinToString() }))
                }
            }
            
            plugins {
                id("JbrDownload")
            }
            
            repositories {
                mavenCentral()
                maven {
                    url = URI("https://projects.itemis.de/nexus/content/repositories/mbeddr")
                }
            }
            
            JbrDownload {
                jbrVersion = "11_0_6-b520.66"
                downloadDir = file("jbrdl")
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
                    "classpath"(files(${cp.map { """"${it.invariantSeparatorsPath}"""" }.joinToString() }))
                }
            }
            
            plugins {
                id("JbrDownload")
            }
            
            repositories {
                mavenCentral()
                maven {
                    url = URI("https://projects.itemis.de/nexus/content/repositories/mbeddr")
                }
            }
            
            JbrDownload {
                jbrVersion = "11_0_6-b520.66"
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
    fun `executed downloaded java`() {
        settingsFile.writeText("""
            rootProject.name = "hello-world"
        """.trimIndent())

        buildFile.writeText("""
            import java.net.URI
            buildscript {
                dependencies {
                    "classpath"(files(${cp.map { """"${it.invariantSeparatorsPath}"""" }.joinToString() }))
                }
            }
            
            plugins {
                id("JbrDownload")
            }
            
            repositories {
                mavenCentral()
                maven {
                    url = URI("https://projects.itemis.de/nexus/content/repositories/mbeddr")
                }
            }
            
            JbrDownload {
                jbrVersion = "11_0_6-b520.66"
            }
            tasks.register<Exec>("exec") {
                dependsOn(tasks.getByName("downloadJbr", de.itemis.mps.gradle.downloadJBR.DownloadJBRForPlatform::class))
                executable = tasks.getByName("downloadJbr", de.itemis.mps.gradle.downloadJBR.DownloadJBRForPlatform::class).javaExecutable.absolutePath
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