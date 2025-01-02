package community.flock.wirespec.examples.openapi.app

import com.fasterxml.jackson.databind.ObjectMapper
import community.flock.wirespec.kotlin.Wirespec
import org.springframework.stereotype.Component
import kotlin.reflect.KType
import kotlin.reflect.javaType

/**
 * NOTE: this Serialization is quite primitive, a real-world Serialization would be more complex.
 * See community.flock.wirespec.integration.spring.kotlin.configuration.WirespecSerializationConfiguration for a more complete example
 */
@Component
@OptIn(ExperimentalStdlibApi::class)
class Serialization(private val objectMapper: ObjectMapper) : Wirespec.Serialization<String> {

    override fun <T> serialize(t: T, kType: KType): String = objectMapper.writeValueAsString(t)

    override fun <T> serializeQuery(name: String, value: T, kType: KType): Map<String, List<String>> =
        mapOf(name to listOf(objectMapper.writeValueAsString(value)))

    override fun <T> deserialize(raw: String, kType: KType): T = objectMapper
        .constructType(kType.javaType)
        .let { objectMapper.readValue(raw, it) }

    override fun <T> deserializeQuery(name: String, allQueryParams: Map<String, List<String>>, kType: KType): T {
        val firstParam = allQueryParams[name]?.firstOrNull()
        return objectMapper.constructType(kType.javaType).let { objectMapper.readValue(firstParam, it) }
    }
}
