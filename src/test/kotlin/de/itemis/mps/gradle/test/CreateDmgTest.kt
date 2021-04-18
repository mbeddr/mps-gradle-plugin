package de.itemis.mps.gradle.test

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Assert.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

const val JBR_REPOSITORY = """
    repositories {
        ivy {
            url = uri("https://dl.bintray.com/jetbrains/intellij-jdk/")
            content {
                includeGroupByRegex("com.jetbrains.jdk")
            }
            patternLayout {
                artifact("[module]-[revision]-[classifier].[ext]")
            }
            metadataSources { // skip downloading ivy.xml
                artifact()
            }
        }
    }
"""

class CreateDmgTest {
    @Rule
    @JvmField
    val testProjectDir: TemporaryFolder = TemporaryFolder()
    private lateinit var settingsFile: File
    private lateinit var buildFile: File
    private lateinit var backgroundImage: File
    private lateinit var rcpArchive: File
    private lateinit var gradleRunner: GradleRunner
    val jbrDependency = "com.jetbrains.jdk:jbr:11_0_4:osx-x64-b546.1@tar.gz"

    @Before
    fun setup() {
        assumeTrue(
            "This test uses macOS-specific commands, so it requires macOS for execution",
            System.getProperty("os.name").toLowerCase().contains("mac")
        )
        gradleRunner = GradleRunner.create()
            .withProjectDir(testProjectDir.root)
            .withPluginClasspath()
            .forwardOutput()
        settingsFile = testProjectDir.newFile("settings.gradle.kts").apply {
            val cp = gradleRunner.pluginClasspath.joinToString { "\"${it.absolutePath}\"" }
            writeText(
                """
                rootProject.name = "create-dmg-test"
                buildscript {
                  dependencies {
                    classpath(files($cp))
                  }
                }
            """.trimIndent()
            )
        }
        buildFile = testProjectDir.newFile("build.gradle.kts")
        val resources = testProjectDir.newFolder("resources")
        backgroundImage = File(resources, "background.png").apply {
            writeText("not a png file, however background is required")
        }
        // Generate a minimal RCP
        val appName = "hello world"
        rcpArchive = File(resources, "$appName.zip").apply {
            outputStream().use { os ->
                ZipOutputStream(os).use { zos ->
                    zos.putNextEntry(ZipEntry("$appName.app/Contents/Info.plist"))
                    CreateDmgTest::class.java.getResourceAsStream("Info.plist.xml").use {
                        it.copyTo(zos)
                    }
                    zos.putNextEntry(ZipEntry("$appName.app/Contents/Resources/"))
                    // This is a "mps launcher" binary, and mpssign.sh sets executable bit,
                    // so we create a dummy file here
                    zos.putNextEntry(ZipEntry("$appName.app/Contents/MacOS/mps"))
                    // required for chmod
                    zos.putNextEntry(ZipEntry("$appName.app/Contents/bin/printenv.py"))
                    // required for chmod
                    zos.putNextEntry(ZipEntry("$appName.app/Contents/bin/fsnotifier"))
                    // required for chmod
                    zos.putNextEntry(ZipEntry("$appName.app/Contents/bin/restarter"))
                }
            }
        }
    }

    @Test
    fun `CreateDmg, non signed dmg, kotlin dsl`() {
        buildFile.writeText(
            """
            import de.itemis.mps.gradle.CreateDmg

            $JBR_REPOSITORY

            val createDmg by tasks.registering(CreateDmg::class) {
                rcpArtifact.set(file("resources/${rcpArchive.name}"))
                jdkDependency("$jbrDependency")
                backgroundImage.set(file("resources/${backgroundImage.name}"))
                dmgFile.set(layout.buildDirectory.file("distributions/output.dmg"))
            }
        """.trimIndent()
        )

        val result = gradleRunner
            .withArguments("-i", "-s", "createDmg", "--warning-mode", "all")
            .build()

        assertEquals("createDmg task outcome", TaskOutcome.SUCCESS, result.task(":createDmg")?.outcome)
    }

    @Test
    fun `CreateDmg, non signed dmg, groovy dsl`() {
        buildFile.writeText(
            """
            import de.itemis.mps.gradle.CreateDmg

            $JBR_REPOSITORY

            tasks.register('createDmg', CreateDmg.class) {
                rcpArtifact = file("resources/${rcpArchive.name}")
                jdkDependency "$jbrDependency"
                backgroundImage = file("resources/${backgroundImage.name}")
                dmgFile = layout.buildDirectory.file("distributions/output.dmg")
            }
        """.trimIndent()
        )

        // Rename file, so Groovy DSL is used
        buildFile.renameTo(File(buildFile.parent, "build.gradle"))

        val result = gradleRunner
            .withArguments("-i", "-s", "createDmg", "--warning-mode", "all")
            .build()

        assertEquals("createDmg task outcome", TaskOutcome.SUCCESS, result.task(":createDmg")?.outcome)
    }

    @Test
    fun bundleMacosJdk() {
        buildFile.writeText(
            """
            import de.itemis.mps.gradle.BundleMacosJdk

            $JBR_REPOSITORY

            val bundleJdk by tasks.registering(BundleMacosJdk::class) {
                rcpArtifact.set(file("resources/${rcpArchive.name}"))
                jdkDependency("$jbrDependency")
                outputFile.set(layout.buildDirectory.file("distributions/output.tgz"))
            }
        """.trimIndent()
        )

        val result = gradleRunner
            .withArguments("-i", "-s", "bundleJdk", "--warning-mode", "all")
            .build()

        assertEquals("bundleJdk task outcome", TaskOutcome.SUCCESS, result.task(":bundleJdk")?.outcome)
    }
}

