package test

import org.gradle.testkit.runner.GradleRunner
import org.hamcrest.CoreMatchers.containsString
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import support.FOOJAY_RESOLVER_CONVENTION_VERSION

class MpsTaskInterfaceTest {
    @Rule
    @JvmField
    val testProjectDir: TemporaryFolder = TemporaryFolder()

    @Test
    fun `withType MpsProjectTask configures all project tasks`() {
        testProjectDir.newFile("settings.gradle.kts").writeText(
            """
            plugins {
                id("org.gradle.toolchains.foojay-resolver-convention") version ("$FOOJAY_RESOLVER_CONVENTION_VERSION")
            }
            """.trimIndent()
        )

        testProjectDir.newFile("build.gradle.kts").writeText(
            """
            import de.itemis.mps.gradle.tasks.*

            plugins {
                id("de.itemis.mps.gradle.common")
            }

            val generate by tasks.registering(MpsGenerate::class)
            val check by tasks.registering(MpsCheck::class) {
                junitFile = layout.buildDirectory.file("output.xml")
            }
            val execute by tasks.registering(MpsExecute::class) {
                module = "test"
                className = "test.Class"
                method = "run"
            }

            tasks.withType<MpsProjectTask>().configureEach {
                mpsHome = layout.projectDirectory.dir("test-mps-home")
                projectLocation = layout.projectDirectory.dir("test-project")
            }

            tasks.register("printConfig") {
                doLast {
                    tasks.withType(MpsProjectTask::class.java).forEach {
                        println("TASK:${'$'}{it.name}:mpsHome=${'$'}{it.mpsHome.get()}:project=${'$'}{it.projectLocation.get()}")
                    }
                }
            }
            """.trimIndent()
        )

        val result = GradleRunner.create()
            .withProjectDir(testProjectDir.root)
            .withArguments("printConfig")
            .withPluginClasspath()
            .build()

        assertThat(result.output, containsString("TASK:generate:mpsHome="))
        assertThat(result.output, containsString("TASK:check:mpsHome="))
        assertThat(result.output, containsString("TASK:execute:mpsHome="))
        assertThat(result.output, containsString("test-mps-home"))
        assertThat(result.output, containsString("test-project"))
    }

    @Test
    fun `withType MpsTask configures RunAntScript tasks`() {
        testProjectDir.newFile("settings.gradle.kts")

        testProjectDir.newFile("build.gradle.kts").writeText(
            """
            import de.itemis.mps.gradle.BuildLanguages
            import de.itemis.mps.gradle.tasks.MpsTask
            import de.itemis.mps.gradle.tasks.MpsGenerate

            plugins {
                id("de.itemis.mps.gradle.common")
            }

            val generate by tasks.registering(MpsGenerate::class)
            val buildLangs by tasks.registering(BuildLanguages::class) {
                script = layout.projectDirectory.file("build.xml")
            }

            tasks.withType<MpsTask>().configureEach {
                mpsHome = layout.projectDirectory.dir("shared-mps")
            }

            tasks.register("printConfig") {
                doLast {
                    tasks.withType(MpsTask::class.java).forEach {
                        println("TASK:${'$'}{it.name}:mpsHome=${'$'}{it.mpsHome.get()}")
                    }
                }
            }
            """.trimIndent()
        )

        val result = GradleRunner.create()
            .withProjectDir(testProjectDir.root)
            .withArguments("printConfig")
            .withPluginClasspath()
            .build()

        assertThat(result.output, containsString("TASK:generate:mpsHome="))
        assertThat(result.output, containsString("TASK:buildLangs:mpsHome="))
        assertThat(result.output, containsString("shared-mps"))
    }
}
