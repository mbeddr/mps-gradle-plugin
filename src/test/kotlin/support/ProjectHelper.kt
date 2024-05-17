package support

import org.gradle.api.GradleException
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.streams.toList

private class ProjectHelper {
    // Needed just to get its class loader
}

fun extractTestProject(projectName: String, to: File) {
    val url = ProjectHelper::class.java.classLoader.getResource(projectName) ?: throw GradleException("Project not found on class path: $projectName")
    val path = Paths.get(url.toURI())
    val files = Files.walk(path).filter { Files.isRegularFile(it) }.map { it.toFile() }.toList()
    files.forEach {
        val rel = path.relativize(it.toPath())
        it.copyTo(to.toPath().resolve(rel).toFile())
    }
}
