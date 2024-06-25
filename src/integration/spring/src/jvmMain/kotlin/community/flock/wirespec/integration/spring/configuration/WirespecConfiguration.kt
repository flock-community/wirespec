package community.flock.wirespec.integration.spring.configuration

import com.fasterxml.jackson.databind.ObjectMapper
import community.flock.wirespec.Wirespec
import community.flock.wirespec.integration.jackson.WirespecModule
import community.flock.wirespec.integration.spring.web.WirespecResponseBodyAdvice
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import java.io.BufferedReader
import java.lang.reflect.Type

@Configuration
@Import(WirespecResponseBodyAdvice::class, WirespecWebMvcConfiguration::class)
open class WirespecConfiguration {

    @Bean
    open fun contentMapper(objectMapper: ObjectMapper) = object: Wirespec.ContentMapper<BufferedReader> {

        private val wirespecObjectMapper = objectMapper.copy().registerModule(WirespecModule())

        override fun <T> read(
            content: Wirespec.Content<BufferedReader>,
            valueType: Type,
        ) = content.let {
            val type = wirespecObjectMapper.constructType(valueType)
            val obj: T = wirespecObjectMapper.readValue(it.body, type)
            Wirespec.Content(it.type, obj)
        }

        override fun <T> write(
            content: Wirespec.Content<T>,
        ) = content.let {
            val bytes: ByteArray = wirespecObjectMapper.writeValueAsBytes(content.body)
            Wirespec.Content(it.type, bytes.inputStream().bufferedReader())
        }
    }
}
