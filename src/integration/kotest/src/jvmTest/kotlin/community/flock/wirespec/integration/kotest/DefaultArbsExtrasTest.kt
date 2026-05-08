package community.flock.wirespec.integration.kotest

import community.flock.wirespec.kotlin.Wirespec
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Pin the JVM-only `@Generator(...)` registrations sourced from
 * `kotest-property-arbs`. These are unavailable on JS because the published
 * artifact uses the legacy JS compiler.
 */
class DefaultArbsExtrasTest {

    private val extras = listOf(
        "firstName",
        "lastName",
        "fullName",
        "name",
        "username",
        "domain",
        "color",
    )

    @Test
    fun `every JVM-only extra default registration produces a non-empty string`() {
        val gen = kotestWirespecGenerator(seed = 1L)
        extras.forEach { name ->
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
}
