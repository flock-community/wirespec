package community.flock.wirespec.examples.openapi.app

import com.fasterxml.jackson.databind.ObjectMapper
import community.flock.wirespec.kotlin.Wirespec
import org.springframework.stereotype.Component
import kotlin.reflect.KType
import kotlin.reflect.javaType

@Component
@OptIn(ExperimentalStdlibApi::class)
class Serialization(private val objectMapper: ObjectMapper) : Wirespec.Serialization<String> {
    override fun <T> serialize(t: T, kType: KType): String = objectMapper.writeValueAsString(t)

    override fun <T> deserialize(raw: String, kType: KType): T = objectMapper
        .constructType(kType.javaType)
        .let { objectMapper.readValue(raw, it) }
}
