package community.flock.wirespec.integration.kotest

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class KotestWirespecGeneratorJsTest {

    @Test
    fun `kotestWirespecGeneratorJs returns a working generator with no registrations`() {
        val gen = kotestWirespecGeneratorJs(seed = 1)
        val field: dynamic = js("({kind: 'string', regex: undefined, annotations: []})")
        val value = gen.generate(arrayOf("a"), field) as String
        assertNotNull(value)
        assertTrue(value.isNotEmpty(), "expected non-empty string, got '$value'")
    }

    @Test
    fun `dynamic registrations object routes named generators through user functions`() {
        val regs: dynamic = js("({orderId: function(s) { return 'ORD-' + s; }})")
        val gen = kotestWirespecGeneratorJs(seed = 1, registrations = regs)
        val field: dynamic = js("({kind: 'string', regex: undefined, annotations: [{ 'name': 'Generator', 'parameters': { 'default': 'orderId' } }]})")
        val value = gen.generate(arrayOf("a"), field) as String
        assertTrue(value.startsWith("ORD-"), "expected 'ORD-...', got '$value'")
    }

    @Test
    fun `registry name match is case-insensitive`() {
        val regs: dynamic = js("({Email: function(s) { return 'CASE-' + s + '@x'; }})")
        val gen = kotestWirespecGeneratorJs(seed = 1, registrations = regs)
        val field: dynamic = js("({kind: 'string', regex: undefined, annotations: [{ 'name': 'Generator', 'parameters': { 'default': 'email' } }]})")
        val value = gen.generate(arrayOf("b"), field) as String
        assertTrue(value.startsWith("CASE-"), "expected case-insensitive override, got '$value'")
    }

    @Test
    fun `same seed produces identical output across two invocations`() {
        val field1: dynamic = js("({kind: 'string', regex: undefined, annotations: []})")
        val field2: dynamic = js("({kind: 'string', regex: undefined, annotations: []})")
        val a = kotestWirespecGeneratorJs(seed = 42).generate(arrayOf("a"), field1) as String
        val b = kotestWirespecGeneratorJs(seed = 42).generate(arrayOf("a"), field2) as String
        assertEquals(a, b)
    }
}
