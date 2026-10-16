package test.others

import de.itemis.mps.gradle.tasks.mpsClasspathJars
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class MpsClasspathJarsTest {
    @Rule
    @JvmField
    val tempDir: TemporaryFolder = TemporaryFolder()

    private fun fakeMpsHome(vararg jars: String): File {
        val mpsHome = tempDir.newFolder("mps")
        for (jar in jars) {
            mpsHome.resolve(jar).apply { parentFile.mkdirs() }.writeText("")
        }
        return mpsHome
    }

    private fun relativeJars(mpsHome: File, vararg includes: String): Set<String> {
        val project = ProjectBuilder.builder().withProjectDir(tempDir.root.resolve("project")).build()
        val mpsHomeProperty = project.objects.directoryProperty().fileValue(mpsHome)
        return mpsClasspathJars(mpsHomeProperty, *includes).files.map { it.relativeTo(mpsHome).invariantSeparatorsPath }.toSet()
    }

    @Test
    fun `platform kotlin metadata jar is excluded`() {
        val mpsHome = fakeMpsHome(
            "lib/ant/lib/ant.jar",
            "lib/app.jar",
            "lib/intellij.libraries.kotlin.metadata.jar",
            "lib/kotlin-metadata-jvm-2.4.20-mps.jar",
            "lib/sub/other.jar",
        )

        Assert.assertEquals(
            setOf("lib/ant/lib/ant.jar", "lib/app.jar", "lib/kotlin-metadata-jvm-2.4.20-mps.jar"),
            relativeJars(mpsHome, "lib/ant/lib/*.jar", "lib/*.jar"))

        Assert.assertEquals(
            setOf("lib/ant/lib/ant.jar", "lib/app.jar", "lib/kotlin-metadata-jvm-2.4.20-mps.jar", "lib/sub/other.jar"),
            relativeJars(mpsHome, "lib/**/*.jar"))
    }

    @Test
    fun `MPS home without platform kotlin metadata jar is unaffected`() {
        val mpsHome = fakeMpsHome("lib/app.jar", "lib/sub/other.jar")

        Assert.assertEquals(setOf("lib/app.jar", "lib/sub/other.jar"), relativeJars(mpsHome, "lib/**/*.jar"))
    }
}
