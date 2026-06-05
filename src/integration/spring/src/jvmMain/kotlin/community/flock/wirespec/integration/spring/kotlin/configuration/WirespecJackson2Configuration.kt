package community.flock.wirespec.integration.spring.kotlin.configuration

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import community.flock.wirespec.integration.jackson.v2.kotlin.WirespecSerialization
import community.flock.wirespec.integration.spring.shared.Jackson2JsonMapper
import community.flock.wirespec.integration.spring.shared.WirespecJsonMapper
import community.flock.wirespec.kotlin.Wirespec.Serialization
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Active when Jackson 3 is absent (Spring Boot 3, Jackson 2 only). Backs off whenever
 * Jackson 3 is present so [WirespecJackson3Configuration] wins.
 */
@Configuration
@ConditionalOnMissingClass("tools.jackson.databind.json.JsonMapper")
open class WirespecJackson2Configuration {

    private val objectMapper = jacksonObjectMapper()

    @Bean
    open fun wirespecSerialization(): Serialization = WirespecSerialization(objectMapper)

    @Bean
    open fun wirespecJsonMapper(): WirespecJsonMapper = Jackson2JsonMapper(objectMapper)
}
