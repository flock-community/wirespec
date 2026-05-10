package community.flock.wirespec.integration.kotest

import io.kotest.property.Arb
import io.kotest.property.arbitrary.constant
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Pin the default `@Generator(...)` registrations shipped by the integration.
 * All of these are now multiplatform: kotest-property core (`email`,
 * `ipAddress`, `uuid`) plus kotest-property-arbs (`firstName`, `lastName`,
 * `fullName`/`name`, `username`, `domain`, `color`).
 */
class DefaultArbsTest {

    private val defaults = listOf(
        "email",
        "ipAddress",
        "uuid",
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
        val gen = kotestGenerator(seed = 1L)
        defaults.forEach { name ->
            val v = gen.generate(
                path = listOf("x"),
                field = KotestFieldString(
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
        val gen = kotestGenerator(seed = 1L)
        // "EMAIL" should match the "email" default registration.
        val v = gen.generate(
            path = listOf("x"),
            field = KotestFieldString(
                regex = null,
                annotations = listOf(mapOf("name" to "Generator", "parameters" to mapOf("default" to "EMAIL"))),
            ),
        )
        assertTrue(v.contains("@"), "expected an email-shaped value, got '$v'")
    }

    @Test
    fun `user registration overrides default`() {
        val gen = kotestGenerator(seed = 1L) {
            register("email") { Arb.constant("override@example.com") }
        }
        val v = gen.generate(
            path = listOf("x"),
            field = KotestFieldString(
                regex = null,
                annotations = listOf(mapOf("name" to "Generator", "parameters" to mapOf("default" to "email"))),
            ),
        )
        assertTrue(v == "override@example.com", "expected override, got '$v'")
    }
}
