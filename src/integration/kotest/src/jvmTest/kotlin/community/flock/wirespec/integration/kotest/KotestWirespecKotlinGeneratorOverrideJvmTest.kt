package community.flock.wirespec.integration.kotest

import community.flock.wirespec.kotlin.Wirespec
import io.kotest.property.Arb
import io.kotest.property.arbitrary.constant
import io.kotest.property.arbitrary.long
import kotlin.reflect.typeOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

class KotestWirespecKotlinGeneratorOverrideJvmTest {

    data class FakeUser(val email: String, val age: Long)
    data class FakeOrder(val email: String)

    // Mimics a Wirespec-emitted Refined wrapper: single-arg primary ctor.
    data class FakeEmailAddress(val value: String)

    @Test
    fun `registerField with reified Parent passes through to a String field`() {
        val gen = kotestWirespecKotlinGenerator(seed = 0L) {
            registerField<FakeUser>("email") { Arb.constant("a@b.com") }
        }
        val shape = Wirespec.GeneratorFieldShape<FakeUser>(
            annotations = emptyMap(),
            generate = { p ->
                val email = gen.generate(
                    p + "email",
                    Wirespec.GeneratorFieldString(regex = null, annotations = emptyList()),
                )
                FakeUser(email = email, age = 0L)
            },
            type = typeOf<FakeUser>(),
        )
        val v = gen.generate(listOf("u"), shape)
        assertEquals("a@b.com", v.email)
    }

    @Test
    fun `registerField with Long value works for primitive field`() {
        val gen = kotestWirespecKotlinGenerator(seed = 0L) {
            registerField<FakeUser>("age", value = 42L)
        }
        val shape = Wirespec.GeneratorFieldShape<FakeUser>(
            annotations = emptyMap(),
            generate = { p ->
                val age = gen.generate(
                    p + "age",
                    Wirespec.GeneratorFieldInteger64(min = null, max = null, annotations = emptyList()),
                )
                FakeUser(email = "x", age = age)
            },
            type = typeOf<FakeUser>(),
        )
        val v = gen.generate(listOf("u"), shape)
        assertEquals(42L, v.age)
    }

    @Test
    fun `registerField on Refined-typed field auto-wraps the drawn primitive`() {
        val gen = kotestWirespecKotlinGenerator(seed = 0L) {
            // Field's Kotlin type is FakeEmailAddress (a Refined wrapper of String).
            // User provides the inner primitive; the JVM RefinedWrapper wraps it.
            registerField<FakeUser>("email") { Arb.constant("auto@wrap.com") }
        }
        // The shape for FakeUser's email is a KotestFieldShape<FakeEmailAddress>
        // whose generate callback wraps a String into FakeEmailAddress. The
        // override fires before the callback runs, so the drawn String must
        // be passed through the RefinedWrapper.
        val emailFieldShape = Wirespec.GeneratorFieldShape<FakeEmailAddress>(
            annotations = emptyMap(),
            generate = { p ->
                val value = gen.generate(
                    p + "value",
                    Wirespec.GeneratorFieldString(regex = null, annotations = emptyList()),
                )
                FakeEmailAddress(value)
            },
            type = typeOf<FakeEmailAddress>(),
        )
        val userShape = Wirespec.GeneratorFieldShape<Pair<FakeEmailAddress, Long>>(
            annotations = emptyMap(),
            generate = { p ->
                val email = gen.generate(p + "email", emailFieldShape)
                email to 0L
            },
            type = typeOf<FakeUser>(),
        )
        val (email, _) = gen.generate(listOf("u"), userShape)
        assertEquals(FakeEmailAddress("auto@wrap.com"), email)
    }

    @Test
    fun `Refined auto-wrap type mismatch throws with documented message`() {
        val gen = kotestWirespecKotlinGenerator(seed = 0L) {
            registerField<FakeUser>("email") { Arb.long(0L..10L) }
        }
        val emailFieldShape = Wirespec.GeneratorFieldShape<FakeEmailAddress>(
            annotations = emptyMap(),
            generate = { p ->
                FakeEmailAddress(
                    gen.generate(
                        p + "value",
                        Wirespec.GeneratorFieldString(regex = null, annotations = emptyList()),
                    ),
                )
            },
            type = typeOf<FakeEmailAddress>(),
        )
        val userShape = Wirespec.GeneratorFieldShape<Pair<FakeEmailAddress, Long>>(
            annotations = emptyMap(),
            generate = { p ->
                gen.generate(p + "email", emailFieldShape) to 0L
            },
            type = typeOf<FakeUser>(),
        )
        val ex = assertFailsWith<IllegalStateException> {
            gen.generate(listOf("u"), userShape)
        }
        assertNotNull(ex.message)
        assertEquals(true, ex.message!!.contains("Override at"))
        assertEquals(true, ex.message!!.contains("FakeEmailAddress"))
    }

    @Test
    fun `registerField does not fire for the same field name on a different parent type`() {
        val gen = kotestWirespecKotlinGenerator(seed = 0L) {
            registerField<FakeUser>("email") { Arb.constant("user@x") }
        }
        // FakeOrder also has an `email` field; the override must not match.
        val shape = Wirespec.GeneratorFieldShape<FakeOrder>(
            annotations = emptyMap(),
            generate = { p ->
                FakeOrder(
                    gen.generate(
                        p + "email",
                        Wirespec.GeneratorFieldString(regex = null, annotations = emptyList()),
                    ),
                )
            },
            type = typeOf<FakeOrder>(),
        )
        val v = gen.generate(listOf("o"), shape)
        assertEquals(false, v.email == "user@x", "different parent must not match")
    }
}
