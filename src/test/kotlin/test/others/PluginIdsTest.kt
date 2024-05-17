package test.others

import de.itemis.mps.gradle.tasks.readPluginId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.jar.JarEntry
import java.util.jar.JarOutputStream

class PluginIdsTest {

    @JvmField @Rule
    val folder = TemporaryFolder()

    @Test
    fun idInLibJar() {
        val pluginDir = folder.newFolder()
        writeJarWithPluginXml(pluginDir.resolve("lib/myjar.jar"), "<idea-plugin><id>jetbrains.jetpad</id></idea-plugin>")

        assertEquals("jetbrains.jetpad", readPluginId(pluginDir))
    }


    @Test
    fun noDescriptor() {
        val pluginDir = folder.newFolder()
        assertNull(readPluginId(pluginDir))
    }

    @Test
    fun idInMetaInf() {
        val pluginDir = folder.newFolder()
        writeTextFile(pluginDir.resolve("META-INF/plugin.xml"), "<idea-plugin><id>jetbrains.jetpad</id></idea-plugin>")

        assertEquals("jetbrains.jetpad", readPluginId(pluginDir))
    }

    @Test
    fun idInLibJarTakesPrecedence() {
        val pluginDir = folder.newFolder()
        writeJarWithPluginXml(pluginDir.resolve("lib/foo.jar"), "<idea-plugin><id>foo</id></idea-plugin>")
        writeTextFile(pluginDir.resolve("META-INF/plugin.xml"), "<idea-plugin><id>bar</id></idea-plugin>")

        assertEquals("foo", readPluginId(pluginDir))
    }

    @Test
    fun invalidXml() {
        val pluginDir = folder.newFolder()
        writeJarWithPluginXml(pluginDir.resolve("lib/foo.jar"), "INVALID")

        assertNull(readPluginId(pluginDir))
    }

    private fun writeTextFile(metaInfPluginXml: File, contents: String) {
        metaInfPluginXml.parentFile.mkdirs()
                || throw RuntimeException("Could not create parent directory for $metaInfPluginXml")

        metaInfPluginXml.writeText(contents)
    }

    private fun writeJarWithPluginXml(jarFile: File, contents: String) {
        jarFile.parentFile.mkdirs() || throw RuntimeException("Could not create parent directory for $jarFile")

        JarOutputStream(jarFile.outputStream()).use {
            it.putNextEntry(JarEntry("META-INF/plugin.xml"))
            it.write(contents.toByteArray())
        }
    }
}
