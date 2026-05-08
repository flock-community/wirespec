package community.flock.wirespec.examples.spring.config

import com.fasterxml.jackson.databind.ObjectMapper
import community.flock.wirespec.kotlin.Wirespec
import kotlin.reflect.KType
import kotlin.reflect.javaType

/**
 * Self-contained `Wirespec.Serialization` implementation backed by Jackson.
 *
 * The example bypasses `community.flock.wirespec.integration:jackson-jvm` because that
 * artifact transitively pulls `wirespec-jvm`, whose `Wirespec` class collides with the
 * IR-emitted shared `Wirespec.kt` (a superset that adds `Generator`, `GeneratorField*`,
 * `Transportation`).
 */
@OptIn(ExperimentalStdlibApi::class)
class JacksonWirespecSerialization(
    objectMapper: ObjectMapper,
) : Wirespec.Serialization {

    private val objectMapper = objectMapper.copy().registerModule(WirespecJacksonModule())

    override fun <T : Any> serializeBody(t: T, type: KType): ByteArray = when (t) {
        is String -> t.toByteArray()
        else -> objectMapper.writeValueAsBytes(t)
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> deserializeBody(raw: ByteArray, type: KType): T = when {
        type.classifier == String::class -> raw as T
        else -> objectMapper.readValue(raw, objectMapper.constructType(type.javaType))
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> serializePath(t: T, type: KType): String = t.toString()

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> deserializePath(raw: String, type: KType): T = decodeScalar(raw, type)

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> serializeParam(value: T, type: KType): List<String> = listOf(value.toString())

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> deserializeParam(values: List<String>, type: KType): T =
        decodeScalar(values.first(), type)

    @Suppress("UNCHECKED_CAST")
    private fun <T : Any> decodeScalar(raw: String, type: KType): T {
        val classifier = type.classifier
        return when (classifier) {
            String::class -> raw as T
            Int::class -> raw.toInt() as T
            Long::class -> raw.toLong() as T
            Double::class -> raw.toDouble() as T
            Float::class -> raw.toFloat() as T
            Boolean::class -> raw.toBoolean() as T
            else -> {
                // Refined wrappers and enums delegate to Jackson.
                val javaType = objectMapper.constructType(type.javaType)
                objectMapper.readValue("\"$raw\"", javaType)
            }
        }
    }
}
