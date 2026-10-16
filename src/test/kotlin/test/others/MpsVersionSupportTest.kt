package test.others

import de.itemis.mps.gradle.isMpsVersionSupported
import org.junit.Assert
import org.junit.Test

class MpsVersionSupportTest {
    @Test
    fun `MPS 2023_2 and later are supported`() {
        for (version in listOf("2023.2", "2023.2.2", "2023.3", "2024.1.6", "2025.3-EAP1", "2026.1")) {
            Assert.assertTrue(version, isMpsVersionSupported(version))
        }
    }

    @Test
    fun `MPS before 2023_2 is not supported`() {
        for (version in listOf("2019.2.2", "2022.3.3", "2023.1", "2023")) {
            Assert.assertFalse(version, isMpsVersionSupported(version))
        }
    }
}
