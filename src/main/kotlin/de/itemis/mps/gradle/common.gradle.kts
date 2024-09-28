package de.itemis.mps.gradle

/**
 * A side effect of this plugin is that it lets us use `plugins` block rather than `buildscript` to put the task classes
 * ([RunAntScript], [BuildLanguages], etc.) onto the classpath.
 */

val modelcheckBackend = configurations.create(BackendConfigurations.MODELCHECK_BACKEND_CONFIGURATION_NAME)
val generateBackend = configurations.create(BackendConfigurations.GENERATE_BACKEND_CONFIGURATION_NAME)
val executeBackend = configurations.create(BackendConfigurations.EXECUTE_BACKEND_CONFIGURATION_NAME)

modelcheckBackend.defaultDependencies {
    add(dependencies.create("de.itemis.mps.build-backends:modelcheck:${MPS_BUILD_BACKENDS_VERSION}"))
}

generateBackend.defaultDependencies {
    add(dependencies.create("de.itemis.mps.build-backends:execute-generators:${MPS_BUILD_BACKENDS_VERSION}"))
}

executeBackend.defaultDependencies {
    add(dependencies.create("de.itemis.mps.build-backends:execute:${MPS_BUILD_BACKENDS_VERSION}"))
}

configurations.create(BackendConfigurations.REMIGRATE_BACKEND_CONFIGURATION_NAME) {
    isCanBeConsumed = false
    defaultDependencies {
        add(project.dependencies.create("de.itemis.mps.build-backends:remigrate:[0,2)"))
    }
}

extensions.create<CI>("ci", project)

abstract class CI(val _project: Project) {
    fun determineCI() {
        if(System.getenv("CI").toBoolean()) {
            _project.extra["ciBuild"] = true
        } else{
            _project.extra["ciBuild"] = _project.hasProperty("teamcity")
        }
    }
    
    fun isCI() = _project.extra["ciBuild"]
}

extensions.create<JDK>("jdk", project)

abstract class JDK(val _project: Project) {
    fun determine(javaVersion: JavaVersion) {
        if (_project.extra.has("java${javaVersion}_home")) {
            _project.extra["jdk_home"] = _project.extra.get("java${javaVersion}_home")
        } else if (System.getenv("JB_JAVA${javaVersion}_HOME") != null) {
            _project.extra["jdk_home"] = System.getenv("JB_JAVA${javaVersion}_HOME")
        } else {
            if (JavaVersion.current() != javaVersion) {
                throw GradleException("This build script requires Java ${javaVersion} but you are currently using ${JavaVersion.current()}.\nWhat you can do:\n"
                        + "  * Use project property java${javaVersion}_home to point to the Java ${javaVersion} JDK.\n"
                        + "  * Use environment variable JB_JAVA${javaVersion}_HOME to point to the Java ${javaVersion} JDK\n"
                        + "  * Run Gradle using Java ${javaVersion}")
            }
            _project.extra["jdk_home"] = System.getProperty("java.home")
        }

        val jdk_home = _project.extra["jdk_home"]
        // Check JDK location
        if (!File(jdk_home.toString(), "lib").exists()) {
            throw GradleException("Unable to locate JDK home folder. Detected folder is: ${jdk_home}")
        }
        _project.logger.info("Using JDK at ${jdk_home}")
    }
}

extensions.create<Itemis>("itemis",buildscript)

abstract class Itemis(val bs: ScriptHandler) {
    fun nexus() {
        bs.repositories {
            apply {  maven("https://artifacts.itemis.cloud/repository/maven-mps/") }
        }
        return 
    }
    
    
}

