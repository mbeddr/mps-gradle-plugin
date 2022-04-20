package test.de.itemis.mps.gradle

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
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
        cp = javaClass.classLoader.getResource(
            "plugin-classpath.txt"
        )!!.readText().lines().map { File(it) }
        mpsTestPrjLocation = testProjectDir.newFolder("mps-prj")
        ProjectHelper().extractTestProject(mpsTestPrjLocation)
    }

    @Test
    @Ignore
    fun `generate works with latest MPS`() {
        settingsFile.writeText(
            """
            rootProject.name = "hello-world"
        """.trimIndent()
        )

        buildFile.writeText(
            """
            import java.net.URI
            buildscript {
                dependencies {
                    "classpath"(files(${cp.map { """"${it.invariantSeparatorsPath}"""" }.joinToString()}))
                }
            }
            
            plugins {
                id("generate-models")
            }
            
            repositories {
                mavenCentral()
                maven {
                    url = URI("https://projects.itemis.de/nexus/content/repositories/mbeddr")
                }
            }
            
            val mps = configurations.create("mps")
            
            dependencies {
                mps("com.jetbrains:mps:2021.1.4")
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
            .withPluginClasspath(cp)
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
            buildscript {
                dependencies {
                    "classpath"(files(${cp.map { """"${it.invariantSeparatorsPath}"""" }.joinToString()}))
                }
            }
            
            plugins {
                id("generate-models")
            }
            
            repositories {
                mavenCentral()
                maven {
                    url = URI("https://projects.itemis.de/nexus/content/repositories/mbeddr")
                }
            }
            
            val mps = configurations.create("mps")
            
            dependencies {
                mps("com.jetbrains:mps:2019.3.3")
            }
            
            generate {
                projectLocation = file("${mpsTestPrjLocation.toPath()}")
                mpsConfig = mps
            }
        """.trimIndent()
        )

        GradleRunner.create()
            .withProjectDir(testProjectDir.root)
            .withArguments("generate")
            .withPluginClasspath(cp)
            .buildAndFail()
    }
    @Test
    fun `generate works with set MPS version and path`() {
        settingsFile.writeText(
            """
            rootProject.name = "hello-world"
        """.trimIndent()
        )

        buildFile.writeText(
            """
            import java.net.URI
            buildscript {
                dependencies {
                    "classpath"(files(${cp.map { """"${it.invariantSeparatorsPath}"""" }.joinToString()}))
                }
            }
            
            plugins {
                id("generate-models")
            }
            
            repositories {
                mavenCentral()
                maven {
                    url = URI("https://projects.itemis.de/nexus/content/repositories/mbeddr")
                }
            }
            
            val mps = configurations.create("mps")
            
            dependencies {
                mps("com.jetbrains:mps:2020.3.3")
            }
            
            generate {
                projectLocation = file("${mpsTestPrjLocation.toPath()}")
                mpsVersion = "2020.2.2"
                mpsLocation = file(".")
            }
        """.trimIndent()
        )

        GradleRunner.create()
            .withProjectDir(testProjectDir.root)
            .withArguments()
            .withPluginClasspath(cp)
            .build()
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
            buildscript {
                dependencies {
                    "classpath"(files(${cp.map { """"${it.invariantSeparatorsPath}"""" }.joinToString()}))
                }
            }
            
            plugins {
                id("generate-models")
            }
            
            repositories {
                mavenCentral()
                maven {
                    url = URI("https://projects.itemis.de/nexus/content/repositories/mbeddr")
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

        GradleRunner.create()
            .withProjectDir(testProjectDir.root)
            .withArguments()
            .withPluginClasspath(cp)
            .buildAndFail()
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
            buildscript {
                dependencies {
                    "classpath"(files(${cp.map { """"${it.invariantSeparatorsPath}"""" }.joinToString()}))
                }
            }
            
            plugins {
                id("generate-models")
            }
            
            repositories {
                mavenCentral()
                maven {
                    url = URI("https://projects.itemis.de/nexus/content/repositories/mbeddr")
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

        GradleRunner.create()
            .withProjectDir(testProjectDir.root)
            .withArguments()
            .withPluginClasspath(cp)
            .buildAndFail()
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
            buildscript {
                dependencies {
                    "classpath"(files(${cp.map { """"${it.invariantSeparatorsPath}"""" }.joinToString()}))
                }
            }
            
            plugins {
                id("generate-models")
            }
            
            repositories {
                mavenCentral()
                maven {
                    url = URI("https://projects.itemis.de/nexus/content/repositories/mbeddr")
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

        GradleRunner.create()
            .withProjectDir(testProjectDir.root)
            .withArguments()
            .withPluginClasspath(cp)
            .buildAndFail()
    }
}