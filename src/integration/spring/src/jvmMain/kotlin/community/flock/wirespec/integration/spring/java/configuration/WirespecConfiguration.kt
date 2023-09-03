package community.flock.wirespec.integration.spring.java.configuration

import com.fasterxml.jackson.databind.ObjectMapper
import community.flock.wirespec.integration.jackson.java.WirespecModuleJava
import community.flock.wirespec.integration.spring.java.web.WirespecResponseBodyAdvice
import community.flock.wirespec.integration.spring.kotlin.configuration.WirespecWebMvcConfiguration
import community.flock.wirespec.java.Wirespec
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import java.lang.reflect.Type

@Configuration
@Import(WirespecResponseBodyAdvice::class, WirespecWebMvcConfiguration::class)
open class WirespecConfiguration {

    @Bean
    open fun wirespecSerialization(objectMapper: ObjectMapper) = object : Wirespec.Serialization<String> {

        private val wirespecObjectMapper = objectMapper.copy().registerModule(WirespecModuleJava())

        override fun <T : Any?> serialize(body: T, type: Type?): String {
            return wirespecObjectMapper.writeValueAsString(body)
        }

        override fun <T : Any?> deserialize(raw: String?, valueType: Type?): T {
            val type = wirespecObjectMapper.constructType(valueType)
            val obj: T = wirespecObjectMapper.readValue(raw, type)
            return obj
        }
    }
}
