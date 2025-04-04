package community.flock.wirespec.integration.spring.java.configuration

import com.fasterxml.jackson.databind.ObjectMapper
import community.flock.wirespec.integration.jackson.java.WirespecModuleJava
import community.flock.wirespec.integration.spring.java.web.WirespecResponseBodyAdvice
import community.flock.wirespec.java.Wirespec.ParamSerialization
import community.flock.wirespec.java.Wirespec.Serialization
import community.flock.wirespec.java.serde.DefaultParamSerialization
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import java.lang.reflect.Type

@Configuration
@Import(WirespecResponseBodyAdvice::class, WirespecWebMvcConfiguration::class)
open class WirespecSerializationConfiguration {

    @Bean
    open fun queryParamSerde(): ParamSerialization = DefaultParamSerialization.create()

    @Bean
    open fun wirespecSerialization(
        objectMapper: ObjectMapper,
        paramSerde: ParamSerialization,
    ): Serialization<String> = object : Serialization<String>, ParamSerialization by paramSerde {
        private val wirespecObjectMapper = objectMapper.copy().registerModule(WirespecModuleJava())

        override fun <T> serialize(body: T, type: Type?): String = when {
            body is String -> body
            else -> wirespecObjectMapper.writeValueAsString(body)
        }

        override fun <T : Any> deserialize(raw: String?, valueType: Type?): T? = raw?.let {
            when {
                valueType == String::class.java -> {
                    @Suppress("UNCHECKED_CAST")
                    raw as T
                }

                else -> {
                    val type = wirespecObjectMapper.constructType(valueType)
                    wirespecObjectMapper.readValue(it, type)
                }
            }
        }
    }
}
