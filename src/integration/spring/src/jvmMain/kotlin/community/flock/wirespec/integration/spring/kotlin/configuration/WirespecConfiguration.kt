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
@Import(WirespecResponseBodyAdvice::class, WirespecWebMvcConfiguration::class)
@OptIn(ExperimentalStdlibApi::class)
open class WirespecConfiguration {

    @Bean
    open fun wirespecSerialization(objectMapper: ObjectMapper) = object : Wirespec.Serialization<String> {

        private val wirespecObjectMapper = objectMapper.copy().registerModule(WirespecModuleKotlin())

        override fun <T> serialize(body: T, kType: KType): String {
            return wirespecObjectMapper.writeValueAsString(body)
        }

        override fun <T> deserialize(raw: String, kType: KType): T {
            val type = wirespecObjectMapper.constructType(kType.javaType)
            val obj: T = wirespecObjectMapper.readValue(raw, type)
            return obj
        }


    }
}
