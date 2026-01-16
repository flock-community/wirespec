package community.flock.wirespec.integration.spring.kotlin.configuration

import com.fasterxml.jackson.databind.ObjectMapper
import community.flock.wirespec.integration.jackson.kotlin.WirespecSerialization
import community.flock.wirespec.integration.spring.kotlin.web.WirespecResponseBodyAdvice
import community.flock.wirespec.kotlin.Wirespec
import community.flock.wirespec.kotlin.serde.DefaultParamSerialization
import community.flock.wirespec.kotlin.serde.DefaultPathSerialization
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import kotlin.reflect.KType

@Configuration
@Import(WirespecResponseBodyAdvice::class, WirespecWebMvcConfiguration::class)
open class WirespecSerializationConfiguration {

    @Bean
    open fun queryParamSerde() = DefaultParamSerialization()

    @Bean
    open fun wirespecSerializationMap(
        objectMapper: ObjectMapper,
    ): Map<MediaType, Wirespec.Serialization> = mapOf(
        MediaType.APPLICATION_JSON to WirespecSerialization(objectMapper),
        MediaType.TEXT_PLAIN to PlainTextDataSerialization(),
    )
}

class PlainTextDataSerialization :
    Wirespec.Serialization,
    Wirespec.ParamSerialization by DefaultParamSerialization(),
    Wirespec.PathSerialization by DefaultPathSerialization() {
    override fun <T : Any?> serializeBody(t: T, kType: KType): ByteArray {
        return when (t) {
            is ByteArray -> return t
            is String -> return t.toByteArray()
            else -> error("Unsupported type ${t?.let { it::class.simpleName }}")
        }
    }

    override fun <T : Any?> deserializeBody(raw: ByteArray, kType: KType): T = raw as T
}
