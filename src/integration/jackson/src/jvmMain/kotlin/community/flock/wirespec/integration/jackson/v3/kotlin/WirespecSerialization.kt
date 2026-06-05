package community.flock.wirespec.integration.jackson.v3.kotlin

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.PropertyAccessor
import community.flock.wirespec.kotlin.Wirespec
import community.flock.wirespec.kotlin.Wirespec.Serialization
import community.flock.wirespec.kotlin.serde.DefaultParamSerialization
import community.flock.wirespec.kotlin.serde.DefaultPathSerialization
import tools.jackson.databind.json.JsonMapper
import kotlin.reflect.KType
import kotlin.reflect.javaType

/**
 * A reusable implementation of [Wirespec.Serialization] backed by Jackson 3.
 *
 * It rebuilds the supplied (immutable) [JsonMapper] with the [WirespecModuleKotlin]
 * registered and field-level visibility configured, so Wirespec models serialize by
 * field exactly like the Jackson 2 integration. Parameter and path serialization are
 * delegated to the default Wirespec serializers.
 */
class WirespecSerialization(
    jsonMapper: JsonMapper,
) : Serialization,
    Wirespec.ParamSerialization by DefaultParamSerialization(),
    Wirespec.PathSerialization by DefaultPathSerialization() {

    private val wirespecObjectMapper: JsonMapper = jsonMapper.rebuild()
        .changeDefaultVisibility {
            it
                .withVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE)
                .withFieldVisibility(JsonAutoDetect.Visibility.ANY)
        }
        .propertyNamingStrategy(KotlinReservedKeywordNamingStrategy())
        .addModule(WirespecModuleKotlin())
        .build()

    override fun <T : Any> serializeBody(t: T, kType: KType): ByteArray = when (t) {
        is String -> t.toByteArray()
        else -> wirespecObjectMapper.writeValueAsBytes(t)
    }

    @Suppress("UNCHECKED_CAST")
    @OptIn(ExperimentalStdlibApi::class)
    override fun <T : Any> deserializeBody(raw: ByteArray, kType: KType): T = when {
        kType.classifier == String::class -> raw as T
        else ->
            wirespecObjectMapper
                .constructType(kType.javaType)
                .let { wirespecObjectMapper.readValue(raw, it) }
    }
}
