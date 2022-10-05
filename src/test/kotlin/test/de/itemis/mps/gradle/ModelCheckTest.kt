package test.de.itemis.mps.gradle

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class ModelCheckTest {
    @Rule
    @JvmField
    val testProjectDir: TemporaryFolder = TemporaryFolder()
    private lateinit var settingsFile: File
    private lateinit var buildFile: File
    private lateinit var cp: List<File>
    private lateinit var mpsTestPrjLocation: File
    private lateinit var junitFile: File


    @Before
    fun setup() {
        settingsFile = testProjectDir.newFile("settings.gradle.kts")
        buildFile = testProjectDir.newFile("build.gradle.kts")
        cp = javaClass.classLoader.getResource(
            "plugin-classpath.txt"
        )!!.readText().lines().map { File(it) }
        mpsTestPrjLocation = testProjectDir.newFolder("mps-prj")
        junitFile = File(mpsTestPrjLocation, "junit.xml")
    }

    private fun extractProject(name: String) = ProjectHelper().extractTestProject(name, mpsTestPrjLocation)

    @Test
    fun `check model works with latest MPS`() {
        extractProject("test-project")

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
                id("modelcheck")
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
            .withPluginClasspath(cp)
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
            """
            import java.net.URI
            buildscript {
                dependencies {
                    "classpath"(files(${cp.map { """"${it.invariantSeparatorsPath}"""" }.joinToString()}))
                }
            }
            
            plugins {
                id("modelcheck")
            }
            
            repositories {
                mavenCentral()
                maven {
                    url = URI("https://artifacts.itemis.cloud/repository/maven-mps")
                }
            }
            
            val mps = configurations.create("mps")
            
            dependencies {
                mps("com.jetbrains:mps:2021.1.4")
            }
            
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
            .withPluginClasspath(cp)
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
            """
            import java.net.URI
            buildscript {
                dependencies {
                    "classpath"(files(${cp.map { """"${it.invariantSeparatorsPath}"""" }.joinToString()}))
                }
            }
            
            plugins {
                id("modelcheck")
            }
            
            repositories {
                mavenCentral()
                maven {
                    url = URI("https://artifacts.itemis.cloud/repository/maven-mps")
                }
            }
            
            val mps = configurations.create("mps")
            
            dependencies {
                mps("com.jetbrains:mps:2021.1.4")
            }
            
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
            .withPluginClasspath(cp)
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
            """
            import java.net.URI
            buildscript {
                dependencies {
                    "classpath"(files(${cp.map { """"${it.invariantSeparatorsPath}"""" }.joinToString()}))
                }
            }
            
            plugins {
                id("modelcheck")
            }
            
            repositories {
                mavenCentral()
                maven {
                    url = URI("https://artifacts.itemis.cloud/repository/maven-mps")
                }
            }
            
            val mps = configurations.create("mps")
            
            dependencies {
                mps("com.jetbrains:mps:2019.3.3")
            }
            
            modelcheck {
                projectLocation = file("${mpsTestPrjLocation.toPath()}")
                mpsConfig = mps
                junitFile = file("${junitFile.absolutePath}")

            }
        """.trimIndent()
        )

        GradleRunner.create()
            .withProjectDir(testProjectDir.root)
            .withArguments("checkmodels")
            .withPluginClasspath(cp)
            .buildAndFail()
        Assert.assertFalse(junitFile.exists())
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
            """
            import java.net.URI
            buildscript {
                dependencies {
                    "classpath"(files(${cp.map { """"${it.invariantSeparatorsPath}"""" }.joinToString()}))
                }
            }
            
            plugins {
                id("modelcheck")
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
            .withPluginClasspath(cp)
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
            """
            import java.net.URI
            buildscript {
                dependencies {
                    "classpath"(files(${cp.map { """"${it.invariantSeparatorsPath}"""" }.joinToString()}))
                }
            }
            
            plugins {
                id("modelcheck")
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
            .withPluginClasspath(cp)
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
            """
            import java.net.URI
            buildscript {
                dependencies {
                    "classpath"(files(${cp.map { """"${it.invariantSeparatorsPath}"""" }.joinToString()}))
                }
            }
            
            plugins {
                id("modelcheck")
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
            
            modelcheck {
                projectLocation = file("${mpsTestPrjLocation.toPath()}")
                mpsVersion = "2020.2.2"
                junitFile = file("${junitFile.absolutePath}")
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
    fun `check model fails with only MPS path set`() {
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
                id("modelcheck")
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
            
            modelcheck {
                projectLocation = file("${mpsTestPrjLocation.toPath()}")
                mpsLocation = file(".")
                junitFile = file("${junitFile.absolutePath}")
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
