package community.flock.wirespec.integration.spring.java.configuration

import com.fasterxml.jackson.databind.ObjectMapper
import community.flock.wirespec.integration.jackson.java.WirespecSerialization
import community.flock.wirespec.integration.spring.java.web.WirespecResponseBodyAdvice
import community.flock.wirespec.java.Wirespec.Serialization
import community.flock.wirespec.java.serde.DefaultParamSerialization
import community.flock.wirespec.java.serde.DefaultPathSerialization
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import java.lang.reflect.Type

@Configuration
@Import(WirespecResponseBodyAdvice::class, WirespecWebMvcConfiguration::class)
open class WirespecSerializationConfiguration {

    @Bean
    open fun wirespecSerializationMap(
        objectMapper: ObjectMapper,
    ): Map<MediaType, Serialization> = mapOf(
        MediaType.APPLICATION_JSON to WirespecSerialization(objectMapper),
        MediaType.TEXT_PLAIN to PlainTextDataSerialization(),
    )
}

class PlainTextDataSerialization :
    Serialization,
    DefaultParamSerialization,
    DefaultPathSerialization {
    override fun <T : Any?> serializeBody(t: T?, type: Type?): ByteArray {
        return when (t) {
            is ByteArray -> return t
            is String -> return t.toByteArray()
            else -> error("Unsupported type ${t?.let { it::class.simpleName }}")
        }
    }
    override fun <T : Any?> deserializeBody(raw: ByteArray?, type: Type?): T = raw as T
}
