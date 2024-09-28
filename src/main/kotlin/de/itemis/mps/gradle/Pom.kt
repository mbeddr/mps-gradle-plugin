package de.itemis.mps.gradle

import org.gradle.api.artifacts.Configuration
import org.gradle.api.publish.maven.MavenPom

class Pom {

    /**
     * Add all top level dependencies of a configuration to a pom
     * @param pom the POM node where to add the dependencies
     * @param config the configuration where to get the dependencies from
     */
    fun withDep(pom: MavenPom, config: Configuration) {
        pom.withXml {
            val dependenciesNode = asNode().appendNode("dependencies")
            config.resolvedConfiguration.firstLevelModuleDependencies.forEach {
                val dependencyNode = dependenciesNode.appendNode("dependency")
                dependencyNode.appendNode("groupId", it.moduleGroup)
                dependencyNode.appendNode("artifactId", it.moduleName)
                dependencyNode.appendNode("version", it.moduleVersion)
                dependencyNode.appendNode("type", it.moduleArtifacts.first().type)
            }
        }
    }

    /**
     * Add all top level dependencies of a configuration to a pom with provided scope
     * @param pom the POM node where to add the dependencies
     * @param config the configuration where to get the dependencies from
     */
    fun withProvidedDep(pom: MavenPom, config: Configuration) {
        pom.withXml {
            val dependenciesNode = asNode().appendNode("dependencies")
            config.resolvedConfiguration.firstLevelModuleDependencies.forEach {
                val dependencyNode = dependenciesNode.appendNode("dependency")
                dependencyNode.appendNode("groupId", it.moduleGroup)
                dependencyNode.appendNode("artifactId", it.moduleName)
                dependencyNode.appendNode("version", it.moduleVersion)
                dependencyNode.appendNode("type", it.moduleArtifacts.first().type)
                dependencyNode.appendNode("scope", "provided")
            }
        }
    }
}