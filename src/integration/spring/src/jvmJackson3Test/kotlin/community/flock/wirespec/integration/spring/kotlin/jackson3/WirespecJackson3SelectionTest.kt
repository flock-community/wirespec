package community.flock.wirespec.integration.spring.kotlin.jackson3

import community.flock.wirespec.integration.jackson.v3.kotlin.WirespecSerialization
import community.flock.wirespec.integration.spring.kotlin.configuration.WirespecJackson2Configuration
import community.flock.wirespec.integration.spring.kotlin.configuration.WirespecJackson3Configuration
import community.flock.wirespec.integration.spring.shared.Jackson3JsonMapper
import community.flock.wirespec.integration.spring.shared.WirespecJsonMapper
import community.flock.wirespec.kotlin.Wirespec
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Spring Boot 4 scenario: this test runs on a Jackson-3-only classpath (no Jackson 2),
 * with both version-conditional configurations registered. It verifies that:
 *  - Jackson 3 wins (the `@ConditionalOnClass` config is selected; the Jackson 2 config
 *    backs off via `@ConditionalOnMissingClass` and — importantly — its class still loads
 *    cleanly on a Jackson-2-free classpath, as it would inside a real Spring Boot 4 app),
 *  - the Jackson 3 mapper actually initializes (this is exactly what failed when Jackson 2
 *    and 3 collided on one classpath), and round-trips JSON.
 */
class WirespecJackson3SelectionTest {

    private val runner = ApplicationContextRunner()
        .withUserConfiguration(
            WirespecJackson3Configuration::class.java,
            WirespecJackson2Configuration::class.java,
        )

    @Test
    fun `selects the Jackson 3 beans when only Jackson 3 is present`() {
        runner.run { context ->
            assertTrue(
                context.getBean(WirespecJsonMapper::class.java) is Jackson3JsonMapper,
                "expected the Jackson 3 multipart mapper to be selected",
            )
            assertTrue(
                context.getBean(Wirespec.Serialization::class.java) is WirespecSerialization,
                "expected the Jackson 3 Wirespec serialization to be selected",
            )
        }
    }

    @Test
    fun `the selected Jackson 3 mapper initializes and round-trips json`() {
        runner.run { context ->
            val mapper = context.getBean(WirespecJsonMapper::class.java)
            val bytes = mapper.writeValueAsBytes(mapOf("number" to 1, "string" to "test"))
            val tree = mapper.readTree(bytes)
            assertTrue(bytes.isNotEmpty())
            assertTrue(tree.toString().contains("test"))
        }
    }
}
