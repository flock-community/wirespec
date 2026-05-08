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

    @Test
    fun `dynamic registrations object routes named generators through user functions`() {
        val regs: dynamic = js("({orderId: function(s) { return 'ORD-' + s; }})")
        val gen = kotestWirespecGeneratorJs(seed = 1, registrations = regs)
        val field = Wirespec.GeneratorFieldString(
            regex = null,
            annotations = listOf(
                mapOf(
                    "name" to "Generator",
                    "parameters" to mapOf("default" to "orderId"),
                ),
            ),
        )
        @Suppress("UNCHECKED_CAST")
        val value = gen.generate(listOf("a"), field) as String
        assertTrue(value.startsWith("ORD-"), "expected 'ORD-...', got '$value'")
    }

    @Test
    fun `registry name match is case-insensitive`() {
        val regs: dynamic = js("({Email: function(s) { return 'CASE-' + s + '@x'; }})")
        val gen = kotestWirespecGeneratorJs(seed = 1, registrations = regs)
        val field = Wirespec.GeneratorFieldString(
            regex = null,
            annotations = listOf(
                mapOf(
                    "name" to "Generator",
                    "parameters" to mapOf("default" to "email"),
                ),
            ),
        )
        @Suppress("UNCHECKED_CAST")
        val value = gen.generate(listOf("b"), field) as String
        assertTrue(value.startsWith("CASE-"), "expected case-insensitive override, got '$value'")
    }
}
