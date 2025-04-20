package community.flock.wirespec.integration.spring.kotlin.configuration

import com.fasterxml.jackson.databind.ObjectMapper
import community.flock.wirespec.integration.jackson.kotlin.WirespecSerialization
import community.flock.wirespec.integration.spring.kotlin.web.WirespecResponseBodyAdvice
import community.flock.wirespec.kotlin.Wirespec.ParamSerialization
import community.flock.wirespec.kotlin.Wirespec.Serialization
import community.flock.wirespec.kotlin.serde.DefaultParamSerialization
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

@Configuration
@Import(WirespecResponseBodyAdvice::class, WirespecWebMvcConfiguration::class)
open class WirespecSerializationConfiguration {

    @Bean
    @ConditionalOnMissingBean
    open fun objectMapper(): ObjectMapper = ObjectMapper()

    @Bean
    open fun queryParamSerde(): ParamSerialization = DefaultParamSerialization()

    @Bean
    open fun wirespecSerialization(
        objectMapper: ObjectMapper,
        queryParamSerde: ParamSerialization,
    ): Serialization<String> = WirespecSerialization(objectMapper, queryParamSerde)
}
