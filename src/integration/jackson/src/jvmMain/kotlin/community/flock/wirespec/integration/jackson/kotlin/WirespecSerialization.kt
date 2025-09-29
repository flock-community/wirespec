package community.flock.wirespec.integration.jackson.kotlin

import com.fasterxml.jackson.databind.ObjectMapper
import community.flock.wirespec.kotlin.Wirespec
import community.flock.wirespec.kotlin.Wirespec.Serialization
import community.flock.wirespec.kotlin.serde.DefaultParamSerialization
import community.flock.wirespec.kotlin.serde.DefaultPathSerialization
import kotlin.reflect.KType
import kotlin.reflect.javaType

/**
 * A reusable implementation of Wirespec.Serialization that uses Jackson for serialization and deserialization.
 * This class implements parameter serialization and deserialization using a private ParamSerialization field.
 */
class WirespecSerialization(
    objectMapper: ObjectMapper,
) : Serialization,
    Wirespec.ParamSerialization by DefaultParamSerialization(),
    Wirespec.PathSerialization by DefaultPathSerialization() {

    private val wirespecObjectMapper = objectMapper.copy().registerModule(WirespecModuleKotlin())

    override fun <T> serializeBody(t: T, kType: KType): ByteArray = when (t) {
        is String -> t.toByteArray()
        else -> wirespecObjectMapper.writeValueAsBytes(t)
    }

    @Suppress("UNCHECKED_CAST")
    @OptIn(ExperimentalStdlibApi::class)
    override fun <T> deserializeBody(raw: ByteArray, kType: KType): T = when {
        kType.classifier == String::class -> raw as T
        else ->
            wirespecObjectMapper
                .constructType(kType.javaType)
                .let { wirespecObjectMapper.readValue(raw, it) }
    }
}
