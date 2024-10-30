package community.flock.wirespec.integration.spring.java.configuration

import com.fasterxml.jackson.databind.ObjectMapper
import community.flock.wirespec.integration.jackson.java.WirespecModuleJava
import community.flock.wirespec.integration.spring.java.web.WirespecResponseBodyAdvice
import community.flock.wirespec.java.Wirespec
import java.lang.reflect.Type
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

@Configuration
@Import(WirespecResponseBodyAdvice::class, WirespecWebMvcConfiguration::class)
open class WirespecConfiguration {

    @Bean
    open fun wirespecSerialization(objectMapper: ObjectMapper) = object : Wirespec.Serialization<String> {

        private val wirespecObjectMapper = objectMapper.copy().registerModule(WirespecModuleJava())

        override fun <T> serialize(body: T, type: Type?): String = wirespecObjectMapper.writeValueAsString(body)

        override fun <T : Any> deserialize(raw: String?, valueType: Type?): T? = raw?.let {
            val type = wirespecObjectMapper.constructType(valueType)
            wirespecObjectMapper.readValue(it, type)
        }
    }
}
