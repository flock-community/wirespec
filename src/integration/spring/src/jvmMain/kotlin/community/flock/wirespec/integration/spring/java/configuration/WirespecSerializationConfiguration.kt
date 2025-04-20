package community.flock.wirespec.integration.spring.java.configuration

import com.fasterxml.jackson.databind.ObjectMapper
import community.flock.wirespec.integration.jackson.java.WirespecSerialization
import community.flock.wirespec.integration.spring.java.web.WirespecResponseBodyAdvice
import community.flock.wirespec.java.Wirespec.ParamSerialization
import community.flock.wirespec.java.Wirespec.Serialization
import community.flock.wirespec.java.serde.DefaultParamSerialization
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
    open fun queryParamSerde(): ParamSerialization = DefaultParamSerialization.create()

    @Bean
    open fun wirespecSerialization(
        objectMapper: ObjectMapper,
        paramSerde: ParamSerialization,
    ): Serialization<String> = WirespecSerialization(objectMapper, paramSerde)
}
