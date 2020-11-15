import de.itemis.mps.gradle.generate.GenerateMpsProjectPlugin
import org.gradle.testkit.runner.GradleRunner
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File


class GenerateModelTest {

    @Rule
    @JvmField
    val testProjectDir: TemporaryFolder = TemporaryFolder()

    private lateinit var settingsFile: File
    private lateinit var buildFile: File
    private lateinit var cp: List<File>
    private lateinit var file: File

    @Before
    fun setup() {
        val pkgCount = GenerateMpsProjectPlugin::class.java.packageName.count { it == '.' }
        file = File(GenerateMpsProjectPlugin::class.java.getResource("").toURI())
        for (i in 0..pkgCount) {
            file = file.parentFile
        }

        settingsFile = testProjectDir.newFile("settings.gradle")
        buildFile = testProjectDir.newFile("build.gradle")
        cp = javaClass.classLoader.getResource(
                "plugin-classpath.txt")!!.readText().lines().map { File(it) }
        testProjectDir.newFolder("buildSrc")
        val buildSrcGradle = testProjectDir.newFile("buildSrc/build.gradle")
        buildSrcGradle.writeText("""
            plugins {
                id 'java-gradle-plugin'
            }
            dependencies {
                implementation files('${file.absolutePath}')
            }
            gradlePlugin {
                plugins {
                    myPlugins {
                        id = 'generate-models'
                        implementationClass = 'de.itemis.mps.gradle.generate.GenerateMpsProjectPlugin'
                    }
                }
            }
        """.trimIndent())
    }

    @Test
    fun `test generate model`() {
        val testProjFolder = File(javaClass.classLoader.getResource("test-project")!!.toURI())
        val langPlugin = File(javaClass.classLoader.getResource("zargari-lang.zip")!!.toURI())
        testProjFolder.copyRecursively(testProjectDir.root)
        settingsFile.writeText("""
            rootProject.name = "test-project"
        """.trimIndent())
        buildFile.writeText("""
            ext.mpsMajor = "2020.1"
            ext.mpsMinor = "6"

            buildscript {
                dependencies {
                    classpath files('${file.absolutePath}')
                    classpath files(${cp.map { """"${it.invariantSeparatorsPath}"""" }.joinToString()})
                }
            }
            
            repositories {
                maven { url 'https://projects.itemis.de/nexus/content/repositories/mbeddr' }
                jcenter()
                ivy {
                    url "https://download.jetbrains.com/mps/${'$'}mpsMajor/"
                    layout 'pattern', {
                        artifact "[module]-[revision].[ext]"
                    }
                    metadataSources { // skip downloading ivy.xml
                        artifact()
                    }
                }
            }

            apply plugin: 'generate-models'
            
            configurations {
                mps
                mpsPlugin
            }

            ext.mpsVersion = '2020.1.6'
            
            generate {
                projectLocation = new File("${testProjectDir.root.absolutePath}")
                mpsConfig = configurations.mps
                mpsPluginConfig = configurations.mpsPlugin
            }
            
            dependencies {
                mps "com.jetbrains:mps:${'$'}mpsVersion"
                mpsPlugin files('${langPlugin.absolutePath}')
            }
        """.trimIndent())

        GradleRunner.create()
                .withProjectDir(testProjectDir.root)
                .withArguments("generate")
                .withPluginClasspath(cp)
                .build()
        val generatedSrcDir = testProjectDir.root
                .resolve("solutions")
                .resolve("ZargariSolution")
                .resolve("source_gen")
                .resolve("ZargariSolution")
                .resolve("java")
        Assert.assertTrue(generatedSrcDir.exists())
        Assert.assertTrue(generatedSrcDir.resolve("MyClass.java").exists())
    }
}

