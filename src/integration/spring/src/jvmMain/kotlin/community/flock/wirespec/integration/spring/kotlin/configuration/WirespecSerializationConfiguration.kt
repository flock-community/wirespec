package community.flock.wirespec.integration.spring.kotlin.configuration

import community.flock.wirespec.integration.spring.kotlin.web.WirespecResponseBodyAdvice
import community.flock.wirespec.kotlin.serde.DefaultParamSerialization
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

/**
 * Entry point imported by `@EnableWirespecController`. The Wirespec [Serialization] bean
 * and the multipart `WirespecJsonMapper` bean are contributed by one of the two
 * version-conditional configurations below, so the integration runs on both Spring Boot 3
 * (Jackson 2) and Spring Boot 4 (Jackson 3). Jackson 3 takes precedence when both are present.
 */
@Configuration
@Import(
    WirespecResponseBodyAdvice::class,
    WirespecWebMvcConfiguration::class,
    WirespecNativeConfiguration::class,
    WirespecJackson3Configuration::class,
    WirespecJackson2Configuration::class,
)
open class WirespecSerializationConfiguration {

    @Bean
    open fun queryParamSerde() = DefaultParamSerialization()
}
