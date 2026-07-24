package community.flock.wirespec.integration.kotest

import community.flock.wirespec.integration.kotest.generator.FieldKey
import community.flock.wirespec.integration.kotest.generator.IdentityRefinedWrapper
import community.flock.wirespec.integration.kotest.generator.KotestFieldString
import community.flock.wirespec.integration.kotest.generator.OverrideRegistry
import community.flock.wirespec.integration.kotest.generator.PathPattern
import io.kotest.property.Arb
import io.kotest.property.arbitrary.constant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class KotestOverrideRegistryTest {

    @Test
    fun `PathPattern of literals matches the exact path only`() {
        val pat = PathPattern.compile(arrayOf("users", "0", "id"))
        assertEquals(true, pat.matches(listOf("users", "0", "id")))
        assertEquals(false, pat.matches(listOf("users", "1", "id")))
        assertEquals(false, pat.matches(listOf("users", "0")))
        assertEquals(false, pat.matches(listOf("users", "0", "id", "x")))
    }

    @Test
    fun `PathPattern wildcard matches any single segment`() {
        val pat = PathPattern.compile(arrayOf("users", "*", "id"))
        assertEquals(true, pat.matches(listOf("users", "0", "id")))
        assertEquals(true, pat.matches(listOf("users", "42", "id")))
        assertEquals(true, pat.matches(listOf("users", "anything", "id")))
        assertEquals(false, pat.matches(listOf("users", "0", "name")))
        assertEquals(false, pat.matches(listOf("orders", "0", "id")))
    }

    @Test
    fun `PathPattern specificity counts literals`() {
        val literal = PathPattern.compile(arrayOf("users", "0", "id"))
        val wild = PathPattern.compile(arrayOf("users", "*", "id"))
        val allWild = PathPattern.compile(arrayOf("*", "*", "*"))
        assertEquals(3, literal.specificity)
        assertEquals(2, wild.specificity)
        assertEquals(0, allWild.specificity)
    }

    @Test
    fun `OverrideRegistry returns the most specific path factory`() {
        val registry = OverrideRegistry()
        registry.addPath(arrayOf("users", "*", "id")) { Arb.constant("WILD") }
        registry.addPath(arrayOf("users", "0", "id")) { Arb.constant("EXACT") }

        val factory = registry.findPath(listOf("users", "0", "id"))
        assertNotNull(factory)
        assertEquals("EXACT", factory().generate(io.kotest.property.RandomSource.default()).first().value)
    }

    @Test
    fun `OverrideRegistry returns wildcard match when no literal exists`() {
        val registry = OverrideRegistry()
        registry.addPath(arrayOf("users", "*", "id")) { Arb.constant("WILD") }

        val factory = registry.findPath(listOf("users", "42", "id"))
        assertNotNull(factory)
        assertEquals("WILD", factory().generate(io.kotest.property.RandomSource.default()).first().value)
    }

    @Test
    fun `OverrideRegistry returns null when no pattern matches`() {
        val registry = OverrideRegistry()
        registry.addPath(arrayOf("users", "*", "id")) { Arb.constant("WILD") }
        assertNull(registry.findPath(listOf("orders", "0", "total")))
    }

    @Test
    fun `OverrideRegistry rejects two equally-specific patterns matching same path`() {
        val registry = OverrideRegistry()
        registry.addPath(arrayOf("users", "*", "id")) { Arb.constant("A") }
        registry.addPath(arrayOf("*", "0", "id")) { Arb.constant("B") }

        val ex = assertFailsWith<IllegalStateException> {
            registry.findPath(listOf("users", "0", "id"))
        }
        assertEquals(true, ex.message!!.contains("Ambiguous"))
    }

    @Test
    fun `OverrideRegistry stores and retrieves field overrides by FieldKey`() {
        val registry = OverrideRegistry()
        val key = FieldKey("com.example.User", "email")
        registry.addField(key) { Arb.constant("a@b.com") }
        val factory = registry.findField(key)
        assertNotNull(factory)
        assertEquals("a@b.com", factory().generate(io.kotest.property.RandomSource.default()).first().value)
    }

    @Test
    fun `OverrideRegistry rejects duplicate path pattern registration`() {
        val registry = OverrideRegistry()
        registry.addPath(arrayOf("users", "*", "id")) { Arb.constant("A") }
        val ex = assertFailsWith<IllegalStateException> {
            registry.addPath(arrayOf("users", "*", "id")) { Arb.constant("B") }
        }
        assertEquals(true, ex.message!!.contains("already registered"))
    }

    @Test
    fun `OverrideRegistry rejects duplicate FieldKey registration`() {
        val registry = OverrideRegistry()
        val key = FieldKey("com.example.User", "email")
        registry.addField(key) { Arb.constant("a@b.com") }
        val ex = assertFailsWith<IllegalStateException> {
            registry.addField(key) { Arb.constant("c@d.com") }
        }
        assertEquals(true, ex.message!!.contains("already registered"))
    }

    @Test
    fun `IdentityRefinedWrapper passes the drawn value through unchanged`() {
        val drawn = "hello"
        val out = IdentityRefinedWrapper.wrap(drawn, KotestFieldString(regex = null, annotations = emptyList()), listOf("a"))
        assertEquals(drawn, out)
    }
}
