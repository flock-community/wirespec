package community.flock.wirespec.integration.spring.kotlin.jackson2

import community.flock.wirespec.integration.jackson.v2.kotlin.WirespecSerialization
import community.flock.wirespec.integration.spring.kotlin.configuration.WirespecJackson2Configuration
import community.flock.wirespec.integration.spring.kotlin.configuration.WirespecJackson3Configuration
import community.flock.wirespec.integration.spring.shared.Jackson2JsonMapper
import community.flock.wirespec.integration.spring.shared.WirespecJsonMapper
import community.flock.wirespec.kotlin.Wirespec
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Spring Boot 3 scenario: this suite runs on a Jackson-2-only classpath. With both
 * version-conditional configurations registered, it verifies that the Jackson 2 config
 * is selected and the Jackson 3 config backs off via `@ConditionalOnMissingClass` — and,
 * importantly, that the Jackson 3 config class still loads cleanly on a Jackson-3-free
 * classpath, as it must inside a real Spring Boot 3 app. The Jackson-3-only counterpart
 * lives in the `jvmJackson3Test` source set.
 */
class WirespecJackson2SelectionTest {

    private val runner = ApplicationContextRunner()
        .withUserConfiguration(
            WirespecJackson3Configuration::class.java,
            WirespecJackson2Configuration::class.java,
        )

    @Test
    fun `selects the Jackson 2 beans when Jackson 3 is absent`() {
        runner.run { context ->
            assertTrue(
                context.getBean(WirespecJsonMapper::class.java) is Jackson2JsonMapper,
                "expected the Jackson 2 multipart mapper to be selected",
            )
            assertTrue(
                context.getBean(Wirespec.Serialization::class.java) is WirespecSerialization,
                "expected the Jackson 2 Wirespec serialization to be selected",
            )
        }
    }
}
