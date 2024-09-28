package de.itemis.mps.gradle

/**
 * A side effect of this plugin is that it lets us use `plugins` block rather than `buildscript` to put the task classes
 * ([RunAntScript], [BuildLanguages], etc.) onto the classpath.
 */

plugins { 
    id("base")
}

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
    
    fun buildNumber() = System.getenv("GITHUB_RUN_NUMBER").toIntOrNull() ?: System.getenv("BUILD_NUMBER").toInt()
    
    fun registerDependencyRepositories(repositories: ArrayList<String>) {
        for (repoUrl in repositories) {
            _project.repositories.maven {
                url = _project.uri(repoUrl)
                credentials {   
                    username = (_project.extensions.getByName("githubAuth") as GitHubAuth).user
                    password = (_project.extensions.getByName("githubAuth") as GitHubAuth).token
                }
            }
        }
    }
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
    fun mbeddrGitHub() = "https://maven.pkg.github.com/mbeddr/*"
    fun itemisNexus() = "https://artifacts.itemis.cloud/repository/maven-mps/"
}

project.configurations.create("mps") {
    description = "The MPS dependencies configuration"
}

project.configurations.create("languageLibs") {
    description = "The language dependencies configuration"
}

project.configurations.create("antLib") {
    description = "The JUnit dependencies configuration"
}

project.extra["skipResolveMps"] = project.hasProperty("mpsHomeDir")
project.extra["mpsHomeDir"] = rootProject.file(project.findProperty("mpsHomeDir") ?: "${layout.buildDirectory.toString()}/mps")

if (project.extra["skipResolveMps"].toString().toBoolean()) {
    tasks.register("resolveMps") {
        doLast {
            logger.info("MPS resolution skipped")
            logger.info("MPS home: " + (project.extra["mpsHomeDir"] as File).absolutePath)
        }
    }
} else {
    tasks.register<Sync>("resolveMps") {
        dependsOn(configurations.getByName("mps"))
        from(configurations.getByName("mps").resolve().map { zipTree(it) })
        project.extra["mpsHomeDir"]?.let { into(it) }
    }
}

tasks.register<Delete>("cleanMps") {
    delete(fileTree(mapOf(
        "dir" to projectDir,
        "include" to listOf("**/classes_gen/**", "**/source_gen/**", "**/source_gen.caches/**", "tmp/**")
    )))
}

tasks.named("clean") {
    dependsOn("cleanMPS")
}

extensions.create<GitHubAuth>("githubAuth", project)

abstract class GitHubAuth(val _project: Project) {
    val user:String = (_project.findProperty("github_username") ?: System.getenv("GITHUB_ACTOR")).toString()
    val token:String = (_project.findProperty("github_token") ?: System.getenv("GITHUB_TOKEN")).toString()
}

extensions.create<Directories>("directories",project)

abstract class Directories(val _project: Project) {
    fun artifactsDir() :File = _project.file("${_project.layout.projectDirectory}/artifacts")
        
    fun scriptFile(name:String):File = _project.file("${_project.layout.projectDirectory}/scripts/$name")
    
    fun jnLibraryPath():File = File(_project.extra["mpsHomeDir"].toString(), "lib/jna/${System.getProperty("os.arch")}")
}