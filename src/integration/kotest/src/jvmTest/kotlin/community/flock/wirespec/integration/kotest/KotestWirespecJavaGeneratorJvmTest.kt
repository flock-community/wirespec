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

    // Mimics a Java-emitted Refined wrapper: single-arg primary ctor.
    data class FakeEmail(val value: String)

    data class FakeUser(val email: String)

    @Test
    fun `path override on a refined shape auto-wraps the drawn primitive`() {
        val gen = kotestWirespecJavaGenerator(seed = 0L) {
            registerPath("u", "email") { Arb.constant("a@b.com") }
        }
        // Java-emitted shapes carry the target class (e.g. `Email.class`); the
        // adapter must surface it so JvmRefinedWrapper can wrap the drawn String.
        val emailShape = Wirespec.GeneratorFieldShape<FakeEmail>(
            emptyMap(),
            Function { p ->
                FakeEmail(gen.generate(p + "value", Wirespec.GeneratorFieldString(Optional.empty(), emptyList())))
            },
            FakeEmail::class.java,
        )
        val v: Any? = gen.generate(listOf("u", "email"), emailShape)
        assertEquals(FakeEmail("a@b.com"), v)
    }

    @Test
    fun `registerField fires for Java-emitted shapes carrying the parent class`() {
        val gen = kotestWirespecJavaGenerator(seed = 0L) {
            registerField(FakeUser::email) { Arb.constant("a@b.com") }
        }
        val userShape = Wirespec.GeneratorFieldShape<FakeUser>(
            emptyMap(),
            Function { p ->
                FakeUser(gen.generate(p + "email", Wirespec.GeneratorFieldString(Optional.empty(), emptyList())))
            },
            FakeUser::class.java,
        )
        val v = gen.generate(listOf("u"), userShape)
        assertEquals("a@b.com", v.email)
    }

    @Test
    fun `factory returns a Java Wirespec_Generator`() {
        val gen: Wirespec.Generator = kotestWirespecJavaGenerator(seed = 1L)
        assertNotNull(gen)
    }

    @Test
    fun `adapter wraps GeneratorFieldNullable result in Optional`() {
        val gen = kotestWirespecJavaGenerator(seed = 0L)
        val nullableField = Wirespec.GeneratorFieldNullable<String>(
            Function { _ -> "value" },
        )
        // The algorithm draws null for ~20% of paths; every draw must come
        // back as an Optional (never a bare T?/null), and present draws must
        // carry the callback's value.
        val draws = (0 until 20).map { i ->
            val v: Optional<String> = gen.generate(listOf("n$i"), nullableField)
            v
        }
        assertTrue(draws.all { it != null }, "every draw must be an Optional")
        assertTrue(draws.any { it.isPresent }, "expected at least one present draw")
        assertTrue(draws.filter { it.isPresent }.all { it.get() == "value" })
    }

    @Test
    fun `adapter handles all Java Wirespec_GeneratorField variants without throwing`() {
        val gen = kotestWirespecJavaGenerator(seed = 0L)

        gen.generate(listOf("s"), Wirespec.GeneratorFieldString(Optional.empty(), emptyList()))
        gen.generate(listOf("i"), Wirespec.GeneratorFieldInteger64(Optional.empty(), Optional.empty(), emptyList()))
        gen.generate(listOf("nu"), Wirespec.GeneratorFieldNumber64(Optional.empty(), Optional.empty(), emptyList()))
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
        assertNotNull(nullable)
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
