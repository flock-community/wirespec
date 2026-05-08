package community.flock.wirespec.integration.kotest

import community.flock.wirespec.kotlin.Wirespec
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class KotestWirespecGeneratorJsTest {

    @Test
    fun `kotestWirespecGeneratorJs returns a working generator with no registrations`() {
        val gen = kotestWirespecGeneratorJs(seed = 1)
        val field = Wirespec.GeneratorFieldString(regex = null, annotations = emptyList())
        @Suppress("UNCHECKED_CAST")
        val value = gen.generate(listOf("a"), field) as String
        assertNotNull(value)
        assertTrue(value.isNotEmpty(), "expected non-empty string, got '$value'")
    }
}
