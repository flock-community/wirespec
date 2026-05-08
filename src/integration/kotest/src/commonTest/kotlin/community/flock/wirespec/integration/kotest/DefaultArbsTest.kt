package community.flock.wirespec.integration.kotest

import community.flock.wirespec.kotlin.Wirespec
import io.kotest.property.Arb
import io.kotest.property.arbitrary.constant
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Pin the cross-platform default `@Generator(...)` registrations shipped by
 * the integration (the kotest-property core; available on JVM and JS).
 * The JVM-only `kotest-property-arbs` extras are exercised by [DefaultArbsExtrasTest].
 */
class DefaultArbsTest {

    private val coreDefaults = listOf("email", "ipAddress")

    @Test
    fun `every core default registration produces a non-empty string`() {
        val gen = kotestWirespecGenerator(seed = 1L)
        coreDefaults.forEach { name ->
            val v = gen.generate(
                path = listOf("x"),
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
            field = Wirespec.GeneratorFieldString(
                regex = null,
                annotations = listOf(mapOf("name" to "Generator", "parameters" to mapOf("default" to "email"))),
            ),
        )
        assertTrue(v == "override@example.com", "expected override, got '$v'")
    }
}
