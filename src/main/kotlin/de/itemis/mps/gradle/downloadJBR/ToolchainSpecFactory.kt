package de.itemis.mps.gradle.downloadJBR

import org.gradle.api.internal.provider.PropertyFactory
import org.gradle.api.model.ObjectFactory
import org.gradle.jvm.toolchain.internal.SpecificInstallationToolchainSpec
import javax.inject.Inject

internal abstract class ToolchainSpecFactory {
    @get:Inject
    abstract val propertyFactory: PropertyFactory

    val method813 = try {
        SpecificInstallationToolchainSpec::class.java.getMethod("fromJavaExecutable",
            PropertyFactory::class.java, String::class.java)
    } catch (_: NoSuchMethodException) {
        null
    }

    @get:Inject
    abstract val objectFactory: ObjectFactory

    val method812 = try {
        SpecificInstallationToolchainSpec::class.java.getMethod("fromJavaExecutable",
            ObjectFactory::class.java, String::class.java)
    } catch (_: NoSuchMethodException) {
        null
    }

    fun fromJavaExecutable(javaExecutable: String): SpecificInstallationToolchainSpec =
        if (method813 != null) {
            method813.invoke(null, propertyFactory, javaExecutable) as SpecificInstallationToolchainSpec
        } else if (method812 != null) {
            method812.invoke(null, objectFactory, javaExecutable) as SpecificInstallationToolchainSpec
        } else  {
            throw IllegalStateException("Unsupported Gradle version, cannot create a SpecificInstallationToolchainSpec")
        }
}
