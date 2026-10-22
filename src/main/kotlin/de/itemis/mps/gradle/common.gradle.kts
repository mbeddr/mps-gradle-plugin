package de.itemis.mps.gradle

// Register the configuration for each backend
for (backend in BuildBackend.values()) {
    configurations.register(backend.configurationName) {
        isCanBeConsumed = false
        defaultDependencies {
            add(project.dependencies.backendDependency(backend))
        }
    }
}
