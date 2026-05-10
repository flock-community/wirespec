package community.flock.wirespec.integration.kotest

import community.flock.wirespec.java.Wirespec
import io.kotest.property.Arb
import io.kotest.property.arbitrary.constant
import java.util.Optional
import java.util.function.Function
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Cover the JVM-only Java `Wirespec.Generator` adapter — the rest of the
 * algorithm is exercised against the kotest-owned `KotestField*` types in
 * commonTest. Here we confirm:
 *   1. `kotestWirespecJavaGenerator(...)` returns something assignable to
 *      the Java Wirespec contract IR-emitted callers depend on.
 *   2. Each `Wirespec.GeneratorField*` Java record survives the round-trip
 *      through `WirespecJavaGeneratorAdapter` into the commonMain algorithm.
 *   3. `GeneratorFieldNullable<T>` returns `Optional<T>` (Java semantics)
 *      rather than the bare `T?` the commonMain algorithm produces.
 */
class KotestWirespecJavaGeneratorJvmTest {

    @Test
    fun `factory returns a Java Wirespec_Generator`() {
        val gen: Wirespec.Generator = kotestWirespecJavaGenerator(seed = 1L)
        assertNotNull(gen)
    }

    @Test
    fun `adapter routes Java Wirespec_GeneratorFieldString through the algorithm`() {
        val gen = kotestWirespecJavaGenerator(seed = 0L) {
            register("orderId") { Arb.constant("ORD-JAVA") }
        }
        val v: String = gen.generate(
            listOf("x"),
            Wirespec.GeneratorFieldString(
                Optional.empty(),
                listOf(mapOf("name" to "Generator", "parameters" to mapOf("default" to "orderId"))),
            ),
        )
        assertEquals("ORD-JAVA", v)
    }

    @Test
    fun `adapter wraps GeneratorFieldNullable result in Optional`() {
        val gen = kotestWirespecJavaGenerator(seed = 0L)
        val nullableField = Wirespec.GeneratorFieldNullable<String>(
            Function { _ -> "value" },
        )
        val v: Optional<String> = gen.generate(listOf("n"), nullableField)
        assertTrue(v.isPresent)
        assertEquals("value", v.get())
    }

    @Test
    fun `adapter handles all Java Wirespec_GeneratorField variants without throwing`() {
        val gen = kotestWirespecJavaGenerator(seed = 0L)

        gen.generate(listOf("s"), Wirespec.GeneratorFieldString(Optional.empty(), emptyList()))
        gen.generate(listOf("i"), Wirespec.GeneratorFieldInteger(Optional.empty(), Optional.empty(), emptyList()))
        gen.generate(listOf("nu"), Wirespec.GeneratorFieldNumber(Optional.empty(), Optional.empty(), emptyList()))
        gen.generate(listOf("b"), Wirespec.GeneratorFieldBoolean(emptyList()))
        gen.generate(listOf("y"), Wirespec.GeneratorFieldBytes(emptyList()))
        gen.generate(
            listOf("e"),
            Wirespec.GeneratorFieldEnum(listOf("A", "B"), emptyList(), String::class.java),
        )
        gen.generate(
            listOf("u"),
            Wirespec.GeneratorFieldUnion(listOf("V1"), emptyList(), String::class.java),
        )
        gen.generate(
            listOf("a"),
            Wirespec.GeneratorFieldArray<String>(Function { _ -> "x" }),
        )
        val nullable: Optional<String> = gen.generate(
            listOf("nul"),
            Wirespec.GeneratorFieldNullable<String>(Function { _ -> "y" }),
        )
        assertTrue(nullable.isPresent)
        gen.generate(
            listOf("sh"),
            Wirespec.GeneratorFieldShape<Map<String, String>>(
                emptyMap(),
                Function { _ -> mapOf("k" to "v") },
                Map::class.java,
            ),
        )
        gen.generate(
            listOf("d"),
            Wirespec.GeneratorFieldDict<String>(Function { _ -> "v" }),
        )
    }
}
