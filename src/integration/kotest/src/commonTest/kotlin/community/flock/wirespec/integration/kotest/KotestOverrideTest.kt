package community.flock.wirespec.integration.kotest

import io.kotest.property.Arb
import io.kotest.property.arbitrary.constant
import kotlin.reflect.typeOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull

class KotestOverrideTest {

    // ---------- registerPath ----------

    @Test
    fun `registerPath fires at the exact path`() {
        val gen = kotestGenerator(seed = 0L) {
            registerPath("users", "0", "id") { Arb.constant("FIXED-ID") }
        }
        val v = gen.generate(
            path = listOf("users", "0", "id"),
            field = KotestFieldString(regex = null, annotations = emptyList()),
        )
        assertEquals("FIXED-ID", v)
    }

    @Test
    fun `registerPath with wildcard matches every array index`() {
        val gen = kotestGenerator(seed = 0L) {
            registerPath("users", "*", "id") { Arb.constant("WILD") }
        }
        repeat(5) { i ->
            val v = gen.generate(
                path = listOf("users", "$i", "id"),
                field = KotestFieldString(regex = null, annotations = emptyList()),
            )
            assertEquals("WILD", v, "expected wildcard match at index $i")
        }
    }

    @Test
    fun `registerPath does not fire on shorter or longer paths`() {
        val gen = kotestGenerator(seed = 0L) {
            registerPath("users", "0", "id") { Arb.constant("FIXED") }
        }
        val short = gen.generate(
            path = listOf("users", "0"),
            field = KotestFieldString(regex = null, annotations = emptyList()),
        )
        val long = gen.generate(
            path = listOf("users", "0", "id", "x"),
            field = KotestFieldString(regex = null, annotations = emptyList()),
        )
        assertNotEquals("FIXED", short)
        assertNotEquals("FIXED", long)
    }

    @Test
    fun `more specific path wins over wildcard`() {
        val gen = kotestGenerator(seed = 0L) {
            registerPath("users", "*", "id") { Arb.constant("WILD") }
            registerPath("users", "0", "id") { Arb.constant("EXACT") }
        }
        val v0 = gen.generate(
            path = listOf("users", "0", "id"),
            field = KotestFieldString(regex = null, annotations = emptyList()),
        )
        val v1 = gen.generate(
            path = listOf("users", "1", "id"),
            field = KotestFieldString(regex = null, annotations = emptyList()),
        )
        assertEquals("EXACT", v0)
        assertEquals("WILD", v1)
    }

    @Test
    fun `equally specific overlapping path patterns throw at lookup`() {
        val gen = kotestGenerator(seed = 0L) {
            registerPath("users", "*", "id") { Arb.constant("A") }
            registerPath("*", "0", "id") { Arb.constant("B") }
        }
        val ex = assertFailsWith<IllegalStateException> {
            gen.generate(
                path = listOf("users", "0", "id"),
                field = KotestFieldString(regex = null, annotations = emptyList()),
            )
        }
        assertNotNull(ex.message)
        assertEquals(true, ex.message!!.contains("Ambiguous"))
    }

    @Test
    fun `registerPath value shorthand wraps in Arb constant`() {
        val gen = kotestGenerator(seed = 0L) {
            registerPath("users", "0", "id", value = "VALUE-FORM")
        }
        val v = gen.generate(
            path = listOf("users", "0", "id"),
            field = KotestFieldString(regex = null, annotations = emptyList()),
        )
        assertEquals("VALUE-FORM", v)
    }

    // ---------- precedence ----------

    @Test
    fun `path override beats default leaf`() {
        val gen = kotestGenerator(seed = 0L) {
            registerPath("a", "b") { Arb.constant("OV") }
        }
        val v = gen.generate(
            path = listOf("a", "b"),
            field = KotestFieldString(regex = null, annotations = emptyList()),
        )
        assertEquals("OV", v)
    }

    @Test
    fun `Seed beats path override`() {
        // @Seed handling runs before path overrides; the captured seed value
        // takes precedence even when a path override is registered.
        val gen = kotestGenerator(seed = 0L) {
            registerPath("my-project-id", "id") { Arb.constant("FROM-PATH") }
        }
        val seedAnnotation = mapOf("name" to "Seed", "parameters" to emptyMap<String, Any>())
        val shape = KotestFieldShape<Map<String, String>>(
            annotations = mapOf("id" to listOf(seedAnnotation)),
            generate = { p ->
                val id = gen.generate(
                    p + "id",
                    KotestFieldString(regex = null, annotations = listOf(seedAnnotation)),
                )
                mapOf("id" to id)
            },
            type = typeOf<Map<String, String>>(),
        )
        val result = gen.generate(listOf("my-project-id"), shape)
        assertEquals("my-project-id", result["id"], "@Seed must win over path override")
    }

    // ---------- registerFieldByTypeName ----------

    @Test
    fun `registerFieldByTypeName fires when the leaf is a direct child of a matching shape`() {
        val gen = kotestGenerator(seed = 0L) {
            registerFieldByTypeName(typeOf<Map<String, String>>().toString(), "email") {
                Arb.constant("a@b.com")
            }
        }
        val shape = KotestFieldShape<Map<String, String>>(
            annotations = emptyMap(),
            generate = { p ->
                val email = gen.generate(
                    p + "email",
                    KotestFieldString(regex = null, annotations = emptyList()),
                )
                mapOf("email" to email)
            },
            type = typeOf<Map<String, String>>(),
        )
        val result = gen.generate(listOf("u"), shape)
        assertEquals("a@b.com", result["email"])
    }

    @Test
    fun `registerFieldByTypeName does not fire for a different parent type`() {
        val gen = kotestGenerator(seed = 0L) {
            registerFieldByTypeName(typeOf<Map<String, String>>().toString(), "email") {
                Arb.constant("a@b.com")
            }
        }
        // Parent is List<String>, not Map<String, String> — override must not fire.
        val shape = KotestFieldShape<List<String>>(
            annotations = emptyMap(),
            generate = { p ->
                val email = gen.generate(
                    p + "email",
                    KotestFieldString(regex = null, annotations = emptyList()),
                )
                listOf(email)
            },
            type = typeOf<List<String>>(),
        )
        val result = gen.generate(listOf("u"), shape)
        assertNotEquals("a@b.com", result.first(), "different parent type should not match")
    }

    @Test
    fun `registerFieldByTypeName does not fire at the top level (no enclosing shape)`() {
        val gen = kotestGenerator(seed = 0L) {
            registerFieldByTypeName(typeOf<Map<String, String>>().toString(), "email") {
                Arb.constant("a@b.com")
            }
        }
        val v = gen.generate(
            path = listOf("email"),
            field = KotestFieldString(regex = null, annotations = emptyList()),
        )
        assertNotEquals("a@b.com", v, "field override must require an enclosing shape")
    }

    @Test
    fun `path override beats field override`() {
        val gen = kotestGenerator(seed = 0L) {
            registerFieldByTypeName(typeOf<Map<String, String>>().toString(), "email") {
                Arb.constant("FIELD")
            }
            registerPath("u", "email") { Arb.constant("PATH") }
        }
        val shape = KotestFieldShape<Map<String, String>>(
            annotations = emptyMap(),
            generate = { p ->
                mapOf(
                    "email" to gen.generate(
                        p + "email",
                        KotestFieldString(regex = null, annotations = emptyList()),
                    ),
                )
            },
            type = typeOf<Map<String, String>>(),
        )
        val result = gen.generate(listOf("u"), shape)
        assertEquals("PATH", result["email"], "path override must win over field override")
    }

    @Test
    fun `field value shorthand wraps in Arb constant`() {
        val gen = kotestGenerator(seed = 0L) {
            registerFieldByTypeName(typeOf<Map<String, String>>().toString(), "email", value = "v@x")
        }
        val shape = KotestFieldShape<Map<String, String>>(
            annotations = emptyMap(),
            generate = { p ->
                mapOf(
                    "email" to gen.generate(
                        p + "email",
                        KotestFieldString(regex = null, annotations = emptyList()),
                    ),
                )
            },
            type = typeOf<Map<String, String>>(),
        )
        assertEquals("v@x", gen.generate(listOf("u"), shape)["email"])
    }
}
