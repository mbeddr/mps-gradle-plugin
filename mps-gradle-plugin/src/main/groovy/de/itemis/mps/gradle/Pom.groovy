package de.itemis.mps.gradle

import org.gradle.api.artifacts.Configuration
import org.gradle.api.publish.maven.MavenPom

class Pom {

    /**
     * Add all top level dependencies of a configuration to a pom
     * @param pom the POM node where to add the dependencies
     * @param config the configuration where to get the dependencies from
     */
    def withDep(MavenPom pom, Configuration config) {
        pom.withXml {
            def dependenciesNode = asNode().appendNode('dependencies')
            config.resolvedConfiguration.firstLevelModuleDependencies.each {
                def dependencyNode = dependenciesNode.appendNode('dependency')
                dependencyNode.appendNode('groupId', it.moduleGroup)
                dependencyNode.appendNode('artifactId', it.moduleName)
                dependencyNode.appendNode('version', it.moduleVersion)
                dependencyNode.appendNode('type', it.moduleArtifacts[0].type)
            }
        }
    }
    /**
     * Add all top level dependencies of a configuration to a pom with provided scope
     * @param pom the POM node where to add the dependencies
     * @param config the configuration where to get the dependencies from
     */
    def withProvidedDep(MavenPom pom, Configuration config) {
        pom.withXml {
            def dependenciesNode = asNode().appendNode('dependencies')
            config.resolvedConfiguration.firstLevelModuleDependencies.each {
                def dependencyNode = dependenciesNode.appendNode('dependency')
                dependencyNode.appendNode('groupId', it.moduleGroup)
                dependencyNode.appendNode('artifactId', it.moduleName)
                dependencyNode.appendNode('version', it.moduleVersion)
                dependencyNode.appendNode('type', it.moduleArtifacts[0].type)
                dependencyNode.appendNode('scope', 'provided')
            }
        }
    }
}