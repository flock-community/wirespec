package community.flock.wirespec.integration.kotest

import community.flock.wirespec.kotlin.Wirespec
import io.kotest.property.Arb
import io.kotest.property.arbitrary.constant
import kotlin.reflect.typeOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class KotestWirespecGeneratorTest {

    // ---------- String ----------

    @Test
    fun `plain GeneratorFieldString produces a non-empty deterministic string`() {
        val field = Wirespec.GeneratorFieldString(regex = null, annotations = emptyList())
        val a = generate(seed = 0L, field) as String
        val b = generate(seed = 0L, field) as String
        assertNotNull(a)
        assertTrue(a.isNotEmpty(), "expected non-empty string, got '$a'")
        assertEquals(a, b, "same seed and field should produce the same value")
    }

    @Test
    fun `GeneratorFieldString with regex matches the regex`() {
        val regex = "[A-Z]{3}-[0-9]{4}"
        val field = Wirespec.GeneratorFieldString(regex = regex, annotations = emptyList())
        val value = generate(seed = 1L, field) as String
        assertTrue(Regex(regex).matches(value), "expected '$value' to match '$regex'")
    }

    // ---------- Integer ----------

    @Test
    fun `GeneratorFieldInteger without bounds returns a deterministic Long`() {
        val field = Wirespec.GeneratorFieldInteger(min = null, max = null, annotations = emptyList())
        val a = generate(seed = 0L, field) as Long
        val b = generate(seed = 0L, field) as Long
        assertEquals(a, b)
    }

    @Test
    fun `GeneratorFieldInteger with bounds stays within bounds`() {
        repeat(20) { i ->
            val field = Wirespec.GeneratorFieldInteger(min = 10, max = 20, annotations = emptyList())
            val v = generate(seed = i.toLong(), field) as Long
            assertTrue(v in 10..20, "expected 10..20, got $v")
        }
    }

    // ---------- Number ----------

    @Test
    fun `GeneratorFieldNumber with bounds stays within bounds`() {
        repeat(20) { i ->
            val field = Wirespec.GeneratorFieldNumber(min = 1.0, max = 2.0, annotations = emptyList())
            val v = generate(seed = i.toLong(), field) as Double
            assertTrue(v in 1.0..2.0, "expected 1.0..2.0, got $v")
        }
    }

    // ---------- Boolean / Bytes ----------

    @Test
    fun `GeneratorFieldBoolean returns a Boolean deterministically`() {
        val field = Wirespec.GeneratorFieldBoolean(annotations = emptyList())
        val a = generate(seed = 0L, field) as Boolean
        val b = generate(seed = 0L, field) as Boolean
        assertEquals(a, b)
    }

    @Test
    fun `GeneratorFieldBytes returns a ByteArray`() {
        val field = Wirespec.GeneratorFieldBytes(annotations = emptyList())
        val a = generate(seed = 0L, field) as ByteArray
        assertNotNull(a)
    }

    // ---------- Enum / Union ----------

    @Test
    fun `GeneratorFieldEnum picks a value from the values list`() {
        val values = listOf("A", "B", "C")
        repeat(20) { i ->
            val field = Wirespec.GeneratorFieldEnum(values = values, annotations = emptyList(), type = typeOf<String>())
            val v = generate(seed = i.toLong(), field) as String
            assertTrue(v in values, "expected one of $values, got '$v'")
        }
    }

    @Test
    fun `GeneratorFieldUnion picks a variant from the variants list`() {
        val variants = listOf("V1", "V2")
        repeat(20) { i ->
            val field = Wirespec.GeneratorFieldUnion(variants = variants, annotations = emptyList(), type = typeOf<String>())
            val v = generate(seed = i.toLong(), field) as String
            assertTrue(v in variants, "expected one of $variants, got '$v'")
        }
    }

    // ---------- Array / Nullable / Shape / Dict (recursive) ----------

    @Test
    fun `GeneratorFieldArray invokes the inner generate callback at indexed paths`() {
        val capturedPaths = mutableListOf<List<String>>()
        val field = Wirespec.GeneratorFieldArray<String> { p ->
            capturedPaths += p
            "x"
        }

        @Suppress("UNCHECKED_CAST")
        val list = generate(seed = 0L, field) as List<String>
        assertTrue(list.isNotEmpty(), "expected non-empty list")
        assertTrue(list.all { it == "x" }, "every element should come from the callback")
        assertTrue(
            capturedPaths.all { it.last().toIntOrNull() != null },
            "expected indexed paths, got $capturedPaths",
        )
    }

    @Test
    fun `GeneratorFieldNullable invokes the inner generate callback`() {
        var calls = 0
        val field = Wirespec.GeneratorFieldNullable<String> {
            calls++
            "y"
        }
        val v = generate(seed = 0L, field) as String?
        // Either null or the callback's return value:
        assertTrue(v == null || v == "y")
        assertTrue(calls in 0..1)
    }

    @Test
    fun `GeneratorFieldShape invokes the inner generate callback with the same path`() {
        val captured = mutableListOf<List<String>>()
        val field = Wirespec.GeneratorFieldShape<String>(
            annotations = emptyMap(),
            generate = { p ->
                captured += p
                "shape"
            },
            type = typeOf<String>(),
        )
        val v = generate(seed = 0L, field, path = listOf("foo", "bar")) as String
        assertEquals("shape", v)
        assertEquals(listOf(listOf("foo", "bar")), captured)
    }

    @Test
    fun `GeneratorFieldDict invokes the callback once and wraps as map with one entry`() {
        var calls = 0
        val field = Wirespec.GeneratorFieldDict<String> {
            calls++
            "value"
        }

        @Suppress("UNCHECKED_CAST")
        val map = generate(seed = 0L, field) as Map<String, String>
        assertEquals(1, map.size)
        assertEquals("value", map.values.first())
        assertEquals(1, calls)
    }

    // ---------- Named generators (@Generator dispatch) ----------

    @Test
    fun `Generator annotation routes to a registered Arb`() {
        val gen = kotestWirespecGenerator(seed = 0L) {
            register("orderId") { Arb.constant("ORD-123") }
        }
        val v = gen.generate(
            path = listOf("orderId"),
            field = Wirespec.GeneratorFieldString(
                regex = null,
                annotations = listOf(mapOf("name" to "Generator", "parameters" to mapOf("default" to "orderId"))),
            ),
        )
        assertEquals("ORD-123", v)
    }

    @Test
    fun `Generator annotation lookup is case-insensitive`() {
        val gen = kotestWirespecGenerator(seed = 0L) {
            register("orderId") { Arb.constant("ORD-CASE") }
        }
        val v = gen.generate(
            path = listOf("x"),
            field = Wirespec.GeneratorFieldString(
                regex = null,
                annotations = listOf(mapOf("name" to "Generator", "parameters" to mapOf("default" to "ORDERID"))),
            ),
        )
        assertEquals("ORD-CASE", v)
    }

    // ---------- @Seed (deterministic-array regeneration) ----------

    @Test
    fun `Shape with @Seed string child receives the seed from the parent path`() {
        // Simulate an IR-emitted ProjectGenerator: Shape whose only field is
        // an @Seed-annotated string child.
        val gen = kotestWirespecGenerator(seed = 0L)
        val seedAnnotation = mapOf("name" to "Seed", "parameters" to emptyMap<String, Any>())
        val shape = Wirespec.GeneratorFieldShape<Map<String, String>>(
            annotations = mapOf("id" to listOf(seedAnnotation)),
            generate = { p ->
                val idField = Wirespec.GeneratorFieldString(
                    regex = null,
                    annotations = listOf(seedAnnotation),
                )
                val id = gen.generate(p + "id", idField)
                mapOf("id" to id)
            },
            type = typeOf<Map<String, String>>(),
        )
        val result = gen.generate(
            path = listOf("my-project-id"),
            field = shape,
        )
        assertEquals("my-project-id", result["id"])
    }

    @Test
    fun `nested Shape with @Seed does not capture the parent field name as seed`() {
        // Mirrors the IR-emitted shape for `Project { @Seed id: ProjectId,
        // owner: Member }` where Member also has an @Seed id. The inner
        // Member.id must NOT be set to the literal "owner" (the outer field
        // name) nor to the project's seed — it must fall through to a
        // deterministic random value.
        val gen = kotestWirespecGenerator(seed = 0L)
        val seedAnnotation = mapOf("name" to "Seed", "parameters" to emptyMap<String, Any>())

        // Refined-style wrapper: GeneratorFieldShape{value: [@Seed]} -> String value.
        fun refinedSeedShape() = Wirespec.GeneratorFieldShape<String>(
            annotations = mapOf("value" to listOf(seedAnnotation)),
            generate = { p ->
                gen.generate(
                    p + "value",
                    Wirespec.GeneratorFieldString(regex = null, annotations = listOf(seedAnnotation)),
                )
            },
            type = typeOf<String>(),
        )

        // Inner Member: Shape with @Seed id.
        val memberShape = Wirespec.GeneratorFieldShape<Map<String, Any>>(
            annotations = mapOf("id" to listOf(seedAnnotation), "ref" to emptyList<Map<String, Any>>()),
            generate = { memberPath ->
                val id = gen.generate(memberPath + "id", refinedSeedShape())
                val ref = gen.generate(
                    memberPath + "ref",
                    Wirespec.GeneratorFieldString(regex = null, annotations = emptyList()),
                )
                mapOf("id" to id, "ref" to ref)
            },
            type = typeOf<Map<String, Any>>(),
        )

        // Top level: `gen.generate(["my-project-id", "owner"], memberShape)`
        // is what `ProjectGenerator.generate(gen, ["my-project-id"])` emits
        // for the `owner` field.
        val owner = gen.generate(listOf("my-project-id", "owner"), memberShape)
        val ownerId = owner["id"] as String

        assertNotEquals("owner", ownerId, "inner @Seed must not capture the parent field name 'owner'")
        assertNotEquals("my-project-id", ownerId, "inner @Seed must not inherit the project's seed")
        assertTrue(ownerId.isNotEmpty(), "inner @Seed should still produce a non-empty deterministic value")
    }

    @Test
    fun `nested Shape with @Seed produces stable value for the same outer path`() {
        // Two separate generator instances with the same baseSeed and the
        // same outer path must produce the same nested @Seed value.
        val seedAnnotation = mapOf("name" to "Seed", "parameters" to emptyMap<String, Any>())

        fun buildOwner(gen: Wirespec.Generator): String {
            val refinedSeedShape = Wirespec.GeneratorFieldShape<String>(
                annotations = mapOf("value" to listOf(seedAnnotation)),
                generate = { p ->
                    gen.generate(
                        p + "value",
                        Wirespec.GeneratorFieldString(regex = null, annotations = listOf(seedAnnotation)),
                    )
                },
                type = typeOf<String>(),
            )
            val memberShape = Wirespec.GeneratorFieldShape<Map<String, Any>>(
                annotations = mapOf("id" to listOf(seedAnnotation)),
                generate = { memberPath ->
                    mapOf("id" to gen.generate(memberPath + "id", refinedSeedShape))
                },
                type = typeOf<Map<String, Any>>(),
            )
            val owner = gen.generate(listOf("my-project-id", "owner"), memberShape)
            return owner["id"] as String
        }

        val a = buildOwner(kotestWirespecGenerator(seed = 0L))
        val b = buildOwner(kotestWirespecGenerator(seed = 0L))
        assertEquals(a, b, "same baseSeed + same outer path must yield the same nested @Seed")

        val c = buildOwner(kotestWirespecGenerator(seed = 0L))
        // Vary just the outer seed via path[0]: different project ids should
        // reshuffle the nested owner.id even though Member.id has @Seed.
        val seedAnnotation2 = seedAnnotation
        val gen2 = kotestWirespecGenerator(seed = 0L)
        val refinedSeedShape2 = Wirespec.GeneratorFieldShape<String>(
            annotations = mapOf("value" to listOf(seedAnnotation2)),
            generate = { p ->
                gen2.generate(
                    p + "value",
                    Wirespec.GeneratorFieldString(regex = null, annotations = listOf(seedAnnotation2)),
                )
            },
            type = typeOf<String>(),
        )
        val memberShape2 = Wirespec.GeneratorFieldShape<Map<String, Any>>(
            annotations = mapOf("id" to listOf(seedAnnotation2)),
            generate = { memberPath ->
                mapOf("id" to gen2.generate(memberPath + "id", refinedSeedShape2))
            },
            type = typeOf<Map<String, Any>>(),
        )
        val d = (gen2.generate(listOf("a-different-project-id", "owner"), memberShape2))["id"] as String
        assertNotEquals(c, d, "different outer seed must produce a different nested @Seed value")
    }

    @Test
    fun `unknown Generator name throws a clear error`() {
        val gen = kotestWirespecGenerator(seed = 0L)
        val ex = runCatching {
            gen.generate(
                path = listOf("x"),
                field = Wirespec.GeneratorFieldString(
                    regex = null,
                    annotations = listOf(mapOf("name" to "Generator", "parameters" to mapOf("default" to "unregistered"))),
                ),
            )
        }.exceptionOrNull()
        assertNotNull(ex, "expected an exception for an unregistered @Generator name")
        assertTrue(
            ex.message!!.contains("unregistered", ignoreCase = true),
            "expected error to mention the missing name, got: ${ex.message}",
        )
    }

    // ---------- helpers ----------

    @Suppress("UNCHECKED_CAST")
    private fun generate(
        seed: Long,
        field: Wirespec.GeneratorField<*>,
        path: List<String> = listOf("x"),
    ): Any? = kotestWirespecGenerator(seed = seed)
        .generate(path, field as Wirespec.GeneratorField<Any?>)
}
