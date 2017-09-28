package de.itemis.mps.gradle

class TestLanguages extends RunAntScript {
    TestLanguages() {
        targets 'clean', 'generate', 'assemble', 'check'
    }
}
