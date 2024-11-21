package community.flock.wirespec.integration.spring.java.configuration

import com.fasterxml.jackson.databind.ObjectMapper
import community.flock.wirespec.integration.jackson.java.WirespecModuleJava
import community.flock.wirespec.integration.spring.java.web.WirespecResponseBodyAdvice
import community.flock.wirespec.java.Wirespec
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

@Configuration
@Import(WirespecResponseBodyAdvice::class, WirespecWebMvcConfiguration::class)
open class WirespecSerializationConfiguration {

    @Bean
    open fun wirespecSerialization(objectMapper: ObjectMapper) = object : Wirespec.Serialization<String> {

        private val wirespecObjectMapper = objectMapper.copy().registerModule(WirespecModuleJava())
        private val stringListDelimiter = ","

        override fun <T> serialize(body: T, type: Type?): String = when {
            body is String -> body
            isStringIterable(type) && body is Iterable<*> -> body.joinToString(stringListDelimiter)
            else -> wirespecObjectMapper.writeValueAsString(body)
        }

        override fun <T : Any> deserialize(raw: String?, valueType: Type?): T? = raw?.let {
            when {
                isStringIterable(valueType) -> {
                    @Suppress("UNCHECKED_CAST")
                    raw.split(stringListDelimiter) as T
                }
                else -> {
                    val type = wirespecObjectMapper.constructType(valueType)
                    wirespecObjectMapper.readValue(it, type)
                }
            }
        }

        private fun isStringIterable(type: Type?): Boolean {
            if (type !is ParameterizedType) return false

            val rawType = type.rawType as Class<*>
            if (!Iterable::class.java.isAssignableFrom(rawType)) return false

            val typeArgument = type.actualTypeArguments.firstOrNull()
            return typeArgument == String::class.java
        }
    }
}