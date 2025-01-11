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
class Serialization(private val objectMapper: ObjectMapper)
    : Wirespec.Serialization<String>, Wirespec.ParamSerialization by DefaultParamSerialization() {

    override fun <T> serialize(t: T, kType: KType): String = objectMapper.writeValueAsString(t)

    override fun <T> deserialize(raw: String, kType: KType): T = objectMapper
        .constructType(kType.javaType)
        .let { objectMapper.readValue(raw, it) }
}
