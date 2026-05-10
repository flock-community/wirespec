package community.flock.wirespec.integration.kotest

import community.flock.wirespec.kotlin.Wirespec
import io.kotest.property.Arb
import io.kotest.property.arbitrary.constant
import kotlin.reflect.typeOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Cover the JVM-only `Wirespec.Generator` adapter — the rest of the algorithm
 * is exercised against the kotest-owned `KotestField*` types in commonTest.
 * Here we just confirm that:
 *   1. `kotestWirespecGenerator(...)` returns something assignable to the
 *      Wirespec contract IR-emitted callers depend on.
 *   2. Each `Wirespec.GeneratorField*` variant survives the round-trip
 *      through `WirespecGeneratorAdapter` into the commonMain algorithm.
 */
class KotestWirespecGeneratorJvmTest {

    @Test
    fun `factory returns a Wirespec_Generator`() {
        val gen: Wirespec.Generator = kotestWirespecGenerator(seed = 1L)
        assertNotNull(gen)
    }

    @Test
    fun `adapter routes Wirespec_GeneratorFieldString through the algorithm`() {
        val gen = kotestWirespecGenerator(seed = 0L) {
            register("orderId") { Arb.constant("ORD-JVM") }
        }
        val v = gen.generate(
            path = listOf("x"),
            field = Wirespec.GeneratorFieldString(
                regex = null,
                annotations = listOf(mapOf("name" to "Generator", "parameters" to mapOf("default" to "orderId"))),
            ),
        )
        assertEquals("ORD-JVM", v)
    }

    @Test
    fun `adapter handles Wirespec_GeneratorFieldShape with nested string`() {
        val gen = kotestWirespecGenerator(seed = 0L)
        val shape = Wirespec.GeneratorFieldShape<Map<String, String>>(
            annotations = emptyMap(),
            generate = { p ->
                val name = gen.generate(
                    p + "name",
                    Wirespec.GeneratorFieldString(regex = null, annotations = emptyList()),
                )
                mapOf("name" to name)
            },
            type = typeOf<Map<String, String>>(),
        )
        val result = gen.generate(listOf("x"), shape)
        assertNotNull(result["name"])
        assertTrue(result["name"]!!.isNotEmpty())
    }

    @Test
    fun `adapter handles all Wirespec_GeneratorField variants without throwing`() {
        val gen = kotestWirespecGenerator(seed = 0L)

        // Smoke: each variant gets dispatched through the adapter's `when`.
        gen.generate(listOf("s"), Wirespec.GeneratorFieldString(null, emptyList()))
        gen.generate(listOf("i"), Wirespec.GeneratorFieldInteger(null, null, emptyList()))
        gen.generate(listOf("n"), Wirespec.GeneratorFieldNumber(null, null, emptyList()))
        gen.generate(listOf("b"), Wirespec.GeneratorFieldBoolean(emptyList()))
        gen.generate(listOf("y"), Wirespec.GeneratorFieldBytes(emptyList()))
        gen.generate(
            listOf("e"),
            Wirespec.GeneratorFieldEnum(values = listOf("A", "B"), annotations = emptyList(), type = typeOf<String>()),
        )
        gen.generate(
            listOf("u"),
            Wirespec.GeneratorFieldUnion(variants = listOf("V1"), annotations = emptyList(), type = typeOf<String>()),
        )
        gen.generate(listOf("a"), Wirespec.GeneratorFieldArray<String> { _ -> "x" })
        gen.generate(listOf("nul"), Wirespec.GeneratorFieldNullable<String> { _ -> "y" })
        gen.generate(
            listOf("sh"),
            Wirespec.GeneratorFieldShape<Map<String, String>>(emptyMap(), { _ -> mapOf("k" to "v") }, typeOf<Map<String, String>>()),
        )
        gen.generate(listOf("d"), Wirespec.GeneratorFieldDict<String> { _ -> "v" })
    }
}
