package community.flock.wirespec.integration.kotest

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class KotestWirespecGeneratorJsTest {

    @Test
    fun `kotestWirespecGeneratorJs returns a working generator`() {
        val gen = kotestWirespecGeneratorJs(seed = 1)
        val field: dynamic = js("({kind: 'string', regex: undefined, annotations: []})")
        val value = gen.generate(arrayOf("a"), field) as String
        assertNotNull(value)
        assertTrue(value.isNotEmpty(), "expected non-empty string, got '$value'")
    }

    @Test
    fun `same seed produces identical output across two invocations`() {
        val field1: dynamic = js("({kind: 'string', regex: undefined, annotations: []})")
        val field2: dynamic = js("({kind: 'string', regex: undefined, annotations: []})")
        val a = kotestWirespecGeneratorJs(seed = 42).generate(arrayOf("a"), field1) as String
        val b = kotestWirespecGeneratorJs(seed = 42).generate(arrayOf("a"), field2) as String
        assertEquals(a, b)
    }

    @Test
    fun `shape kind translates to Wirespec_GeneratorFieldShape and invokes the JS generate callback`() {
        val gen = kotestWirespecGeneratorJs(seed = 1)
        val field: dynamic = js("({kind: 'shape', annotations: {}, generate: function(p) { return { ok: true, path: p.join('/') }; }, type: 'X'})")
        val value: dynamic = gen.generate(arrayOf("root"), field)
        assertEquals(true, value.ok as Boolean)
        assertEquals("root", value.path as String)
    }

    @Test
    fun `dict kind translates to Wirespec_GeneratorFieldDict and produces a JS object`() {
        val gen = kotestWirespecGeneratorJs(seed = 1)
        val field: dynamic = js("({kind: 'dict', generate: function(p) { return 'v-' + p[p.length - 1]; }})")
        val value: dynamic = gen.generate(arrayOf("root"), field)
        // Algorithm produces a single-entry Map("a" -> generate(path + "a")); kotlinToJs converts to JS object.
        assertEquals("v-a", value.a as String)
    }
}
