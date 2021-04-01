package test.de.itemis.mps.gradle

import java.io.File
import java.net.URI
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.streams.toList

class ProjectHelper {

    fun extractTestProject(to: File) {
        val url = this.javaClass.classLoader.getResource("test-project")
        val path = Paths.get(url.toURI())
        val files = Files.walk(path).filter { Files.isRegularFile(it) }.map { it.toFile() }.toList()
        files.forEach {
            val rel = path.relativize(it.toPath())
            it.copyTo(to.toPath().resolve(rel).toFile())
        }
    }

}