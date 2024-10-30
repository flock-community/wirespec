package community.flock.wirespec.integration.spring.kotlin.configuration

import com.fasterxml.jackson.databind.ObjectMapper
import community.flock.wirespec.integration.jackson.kotlin.WirespecModuleKotlin
import community.flock.wirespec.integration.spring.kotlin.web.WirespecResponseBodyAdvice
import community.flock.wirespec.kotlin.Wirespec
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import kotlin.reflect.KType
import kotlin.reflect.javaType

@Configuration
@OptIn(ExperimentalStdlibApi::class)
@Import(WirespecResponseBodyAdvice::class, WirespecWebMvcConfiguration::class)
open class WirespecConfiguration {

    @Bean
    open fun wirespecSerialization(objectMapper: ObjectMapper) = object : Wirespec.Serialization<String> {

        private val wirespecObjectMapper = objectMapper.copy().registerModule(WirespecModuleKotlin())

        override fun <T> serialize(t: T, kType: KType): String = wirespecObjectMapper.writeValueAsString(t)

        override fun <T> deserialize(raw: String, kType: KType): T = wirespecObjectMapper
            .constructType(kType.javaType)
            .let { wirespecObjectMapper.readValue(raw, it) }
    }
}
