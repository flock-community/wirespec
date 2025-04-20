package community.flock.wirespec.integration.jackson.kotlin

import com.fasterxml.jackson.databind.ObjectMapper
import community.flock.wirespec.kotlin.Wirespec
import community.flock.wirespec.kotlin.Wirespec.ParamSerialization
import community.flock.wirespec.kotlin.Wirespec.Serialization
import kotlin.reflect.KType
import kotlin.reflect.javaType

/**
 * A reusable implementation of Wirespec.Serialization that uses Jackson for serialization and deserialization.
 * This class delegates parameter serialization and deserialization to a provided ParamSerialization implementation.
 */
@OptIn(ExperimentalStdlibApi::class)
class WirespecSerialization(
    objectMapper: ObjectMapper,
    queryParamSerde: ParamSerialization
) : Serialization<String>, ParamSerialization by queryParamSerde {

    private val wirespecObjectMapper = objectMapper.copy().registerModule(WirespecModuleKotlin())

    override fun <T> serialize(t: T, kType: KType): String = when (t) {
        is String -> t
        else -> wirespecObjectMapper.writeValueAsString(t)
    }

    override fun <T> deserialize(raw: String, kType: KType): T = when {
        kType.classifier == String::class -> raw as T
        else ->
            wirespecObjectMapper
                .constructType(kType.javaType)
                .let { wirespecObjectMapper.readValue(raw, it) }
    }
}
