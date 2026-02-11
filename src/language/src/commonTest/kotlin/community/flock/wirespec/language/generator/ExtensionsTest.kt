package community.flock.wirespec.language.generator

import community.flock.wirespec.language.core.File
import kotlin.test.Test
import kotlin.test.assertNotNull

class ExtensionsTest {
    @Test
    fun testExtensions() {
        val file = File("", emptyList())
        assertNotNull(file.generateJava())
        assertNotNull(file.generatePython())
        assertNotNull(file.generateTypeScript())
    }
}
