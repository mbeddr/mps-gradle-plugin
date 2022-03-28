package test.de.itemis.mps.gradle

import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.streams.toList

class ProjectHelper {

    fun extractTestProject(projectName: String, to: File) {
        val url = this.javaClass.classLoader.getResource(projectName)
        val path = Paths.get(url.toURI())
        val files = Files.walk(path).filter { Files.isRegularFile(it) }.map { it.toFile() }.toList()
        files.forEach {
            val rel = path.relativize(it.toPath())
            it.copyTo(to.toPath().resolve(rel).toFile())
        }
    }

}