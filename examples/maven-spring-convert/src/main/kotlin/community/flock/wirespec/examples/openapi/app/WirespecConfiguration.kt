package community.flock.wirespec.examples.openapi.app

import com.fasterxml.jackson.databind.ObjectMapper
import community.flock.wirespec.kotlin.Wirespec
import community.flock.wirespec.kotlin.serde.DefaultParamSerialization
import org.springframework.stereotype.Component
import kotlin.reflect.KType
import kotlin.reflect.javaType

/**
 * Example implementation of Wirespec Serialization using DefaultParamSerialization
 * This class handles standard parameter serialization for headers and query parameters.
 * For custom serialization requirements, you can create your own implementation
 * of Wirespec.ParamSerialization instead of using DefaultParamSerialization.
 * In this case, you don't need the dependency on community.flock.wirespec.integration:wirespec
 */
@Component
@OptIn(ExperimentalStdlibApi::class)
class Serialization(private val objectMapper: ObjectMapper) :
    Wirespec.Serialization {

    private val defaultParamSerialization = DefaultParamSerialization()

    override fun <T : Any> serializeBody(t: T, kType: KType): ByteArray = objectMapper.writeValueAsBytes(t)

    override fun <T : Any> deserializeBody(raw: ByteArray, kType: KType): T = objectMapper
        .constructType(kType.javaType)
        .let { objectMapper.readValue(raw, it) }

    override fun <T : Any> serializePath(t: T, kType: KType): String = objectMapper.writeValueAsString(t)

    override fun <T : Any> deserializePath(raw: String, kType: KType): T = objectMapper
        .constructType(kType.javaType)
        .let { objectMapper.readValue(raw, it) }

    override fun <T : Any> serializeParam(value: T, kType: KType): List<String> =
        defaultParamSerialization.serializeParam(value, kType)

    override fun <T : Any> deserializeParam(values: List<String>, kType: KType): T =
        defaultParamSerialization.deserializeParam(values, kType)
}
