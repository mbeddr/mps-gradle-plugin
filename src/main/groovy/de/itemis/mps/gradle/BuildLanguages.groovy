package de.itemis.mps.gradle

class BuildLanguages extends RunAntScript {
    BuildLanguages() {
        targets 'clean', 'generate', 'assemble'
    }
}
