package test.de.itemis.mps.gradle
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File


fun getExpectedLibContent(vars: List<Pair<String, String>>): String {
    val entries = vars.map { """<entry key='${it.first}'>
          <value>
            <Library>
              <option name='name' value='${it.first}' />
              <option name='path' value='${it.second}' />
            </Library>
          </value>
        </entry>""" }.joinToString("\n")
    return """<project version='4'>
  <component name='ProjectLibraryManager'>
    <option name='libraries'>
      <map>
        $entries
      </map>
    </option>
  </component>
</project>"""
}

class GenerateProjectLibrariesXmlTest {

    @Rule @JvmField
    val testProjectDir: TemporaryFolder = TemporaryFolder()
    private lateinit var settingsFile: File
    private lateinit var buildFile: File

    @Before
    fun setup() {
        settingsFile = testProjectDir.newFile("settings.gradle.kts")
        buildFile = testProjectDir.newFile("build.gradle.kts")
    }

    @Test
    fun `test generate project libs without override`() {

        testProjectDir.newFile("projectlibraries.properties").writeText("""
            # To override values defined in this file create projectlibraries.overrides.properties and put overridden values there.
            mps.ex=${'$'}PROJECT_DIR/build/mps-ext
        """.trimIndent())

        settingsFile.writeText("""
            rootProject.name = "hello-world"
        """.trimIndent())
        buildFile.writeText("""
            import de.itemis.mps.gradle.GenerateLibrariesXml
            
            plugins {
                id("de.itemis.mps.gradle.common")
            }
            
            tasks.register<GenerateLibrariesXml>("generateLibs") {
                defaults = file("projectlibraries.properties")
                setOverrides(file("projectlibraries.overrides.properties"))
                destination = file(".mps/libraries.xml")
            }
        """.trimIndent())

        val result = GradleRunner.create()
                .withProjectDir(testProjectDir.root)
                .withArguments("generateLibs")
                .withPluginClasspath()
                .build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":generateLibs")?.outcome)
        val libs = File(testProjectDir.root, ".mps/libraries.xml")

        assertEquals(getExpectedLibContent(listOf(Pair("mps.ex", "${'$'}PROJECT_DIR/build/mps-ext"))),libs.readText())
    }

    @Test
    fun `test generate project libs with override`() {

        testProjectDir.newFile("projectlibraries.properties").writeText("""
            # To override values defined in this file create projectlibraries.overrides.properties and put overridden values there.
            mps.ex=${'$'}PROJECT_DIR/build/mps-ext
        """.trimIndent())

        testProjectDir.newFile("projectlibraries.overrides.properties").writeText("""
            mps.ex=my/awesome/override
        """.trimIndent())

        settingsFile.writeText("""
            rootProject.name = "hello-world"
        """.trimIndent())

        buildFile.writeText("""
            import de.itemis.mps.gradle.GenerateLibrariesXml
            
            plugins {
                id("de.itemis.mps.gradle.common")
            }
            
            tasks.register<GenerateLibrariesXml>("generateLibs") {
                defaults = file("projectlibraries.properties")
                setOverrides(file("projectlibraries.overrides.properties"))
                destination = file(".mps/libraries.xml")
            }
        """.trimIndent())

        val result = GradleRunner.create()
                .withProjectDir(testProjectDir.root)
                .withArguments("generateLibs")
                .withPluginClasspath()
                .build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":generateLibs")?.outcome)
        val libs = File(testProjectDir.root, ".mps/libraries.xml")

        assertEquals(getExpectedLibContent(listOf(Pair("mps.ex", "my/awesome/override"))),libs.readText())
    }
}

