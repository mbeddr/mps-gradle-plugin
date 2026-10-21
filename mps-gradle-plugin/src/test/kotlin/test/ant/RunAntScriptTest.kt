package test.ant

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.hamcrest.CoreMatchers.containsString
import org.hamcrest.CoreMatchers.not
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * Runs a [de.itemis.mps.gradle.BuildLanguages] task against a fake MPS home that contains only `build.properties` and
 * the Ant jars, so the test does not need an MPS distribution.
 */
class RunAntScriptTest {
    @Rule
    @JvmField
    val testProjectDir: TemporaryFolder = TemporaryFolder()

    private lateinit var mpsHome: File

    @Before
    fun setup() {
        testProjectDir.newFile("settings.gradle.kts")

        mpsHome = testProjectDir.newFolder("mps-home")
        mpsHome.resolve("build.properties").writeText("mps.build.number=MPS-251.23774.541\n")

        testProjectDir.newFile("build.xml").writeText(
            """
            <project name="probe" default="generate">
                <target name="clean"/>
                <target name="generate">
                    <echo message="mps.home=${'$'}{mps.home}"/>
                    <echo message="mps_home=${'$'}{mps_home}"/>
                    <echo message="mps.ant.log=${'$'}{mps.ant.log}"/>
                </target>
                <target name="assemble"/>
            </project>
            """.trimIndent()
        )

        testProjectDir.newFile("build.gradle.kts").writeText(
            """
            import de.itemis.mps.gradle.BuildLanguages

            plugins {
                id("de.itemis.mps.gradle.common")
            }

            repositories {
                mavenCentral()
            }

            val antRuntime = configurations.create("antRuntime")
            dependencies {
                antRuntime("org.apache.ant:ant:1.10.15")
            }

            val prepareMpsHome by tasks.registering(Sync::class) {
                from(antRuntime)
                into(layout.projectDirectory.dir("mps-home/lib/ant/lib"))
            }

            val buildLangs by tasks.registering(BuildLanguages::class) {
                dependsOn(prepareMpsHome)
                mpsHome = layout.projectDirectory.dir("mps-home")
                script = layout.projectDirectory.file("build.xml")
                doLast {
                    println("MPS_VERSION=" + mpsVersion.get())
                }
            }
            """.trimIndent()
        )
    }

    @Test
    fun `runs with only mpsHome and script configured`() {
        val result = gradleRunner().withArguments("buildLangs").build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":buildLangs")?.outcome)
        assertThat(result.output, containsString("mps.home=${mpsHome.canonicalPath}"))
        assertThat(result.output, containsString("mps_home=${mpsHome.canonicalPath}"))
        assertThat(result.output, containsString("MPS_VERSION=2025.1"))

        // At the default (lifecycle) level, no log level is passed and Ant leaves the property unset.
        assertThat(result.output, containsString("mps.ant.log=\${mps.ant.log}"))
    }

    @Test
    fun `quiet log level is mapped to a value accepted by MPS Ant tasks`() {
        val result = gradleRunner().withArguments("buildLangs", "--quiet").build()

        assertThat(result.output, containsString("mps.ant.log=error"))
        assertThat(result.output, not(containsString("mps.ant.log=quiet")))
    }

    @Test
    fun `info log level is passed on`() {
        val result = gradleRunner().withArguments("buildLangs", "--info").build()

        assertThat(result.output, containsString("mps.ant.log=info"))
    }

    private fun gradleRunner(): GradleRunner = GradleRunner.create()
        .withProjectDir(testProjectDir.root)
        .withPluginClasspath()
}
