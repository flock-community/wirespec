package community.flock.wirespec.integration.kotest

import community.flock.wirespec.kotlin.Wirespec
import io.kotest.property.Arb
import io.kotest.property.arbitrary.constant
import kotlin.reflect.typeOf
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Pin the default `@Generator(...)` registrations shipped by the integration.
 * Each registered name should produce a non-empty `String` for any seed.
 */
class DefaultArbsTest {

    private val defaults = listOf(
        // core kotest-property
        "email",
        "uuid",
        "ipAddress",
        // kotest-property-arbs extras
        "firstName",
        "lastName",
        "fullName",
        "name",
        "username",
        "domain",
        "color",
    )

    @Test
    fun `every default registration produces a non-empty string`() {
        val gen = kotestWirespecGenerator(seed = 1L)
        defaults.forEach { name ->
            val v = gen.generate(
                path = listOf("x"),
                type = typeOf<String>(),
                field = Wirespec.GeneratorFieldString(
                    regex = null,
                    annotations = listOf(mapOf("name" to "Generator", "parameters" to mapOf("default" to name))),
                ),
            )
            assertNotNull(v, "$name returned null")
            assertTrue(v.isNotEmpty(), "$name returned empty string")
        }
    }

    @Test
    fun `default registrations are case-insensitive`() {
        val gen = kotestWirespecGenerator(seed = 1L)
        // "EMAIL" should match the "email" default registration.
        val v = gen.generate(
            path = listOf("x"),
            type = typeOf<String>(),
            field = Wirespec.GeneratorFieldString(
                regex = null,
                annotations = listOf(mapOf("name" to "Generator", "parameters" to mapOf("default" to "EMAIL"))),
            ),
        )
        assertTrue(v.contains("@"), "expected an email-shaped value, got '$v'")
    }

    @Test
    fun `user registration overrides default`() {
        val gen = kotestWirespecGenerator(seed = 1L) {
            register("email") { Arb.constant("override@example.com") }
        }
        val v = gen.generate(
            path = listOf("x"),
            type = typeOf<String>(),
            field = Wirespec.GeneratorFieldString(
                regex = null,
                annotations = listOf(mapOf("name" to "Generator", "parameters" to mapOf("default" to "email"))),
            ),
        )
        assertTrue(v == "override@example.com", "expected override, got '$v'")
    }
}
