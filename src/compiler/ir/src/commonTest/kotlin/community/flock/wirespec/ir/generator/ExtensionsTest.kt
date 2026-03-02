package community.flock.wirespec.ir.generator

import community.flock.wirespec.ir.core.File
import community.flock.wirespec.ir.core.Name
import kotlin.test.Test
import kotlin.test.assertNotNull

class ExtensionsTest {
    @Test
    fun testExtensions() {
        val file = File(Name.of(""), emptyList())
        assertNotNull(file.generateJava())
        assertNotNull(file.generatePython())
        assertNotNull(file.generateTypeScript())
    }
}
