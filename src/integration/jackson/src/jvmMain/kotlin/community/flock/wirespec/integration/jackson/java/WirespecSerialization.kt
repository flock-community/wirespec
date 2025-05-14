package community.flock.wirespec.integration.jackson.java

import com.fasterxml.jackson.databind.ObjectMapper
import community.flock.wirespec.java.Wirespec.ParamSerialization
import community.flock.wirespec.java.Wirespec.Serialization
import community.flock.wirespec.java.serde.DefaultParamSerialization
import java.lang.reflect.Type

/**
 * A reusable implementation of Wirespec.Serialization that uses Jackson for serialization and deserialization.
 * This class implements parameter serialization and deserialization using a private ParamSerialization field.
 */
class WirespecSerialization(
    objectMapper: ObjectMapper,
) : Serialization<String> {

    private val paramSerde: ParamSerialization = DefaultParamSerialization.create()

    override fun <T> serializeParam(value: T, type: Type): List<String> = paramSerde.serializeParam(value, type)

    override fun <T> deserializeParam(values: List<String>, type: Type): T = paramSerde.deserializeParam(values, type)

    private val wirespecObjectMapper = objectMapper.copy().registerModule(WirespecModuleJava())

    override fun <T> serialize(body: T, type: Type?): String = when {
        body is String -> body
        else -> wirespecObjectMapper.writeValueAsString(body)
    }

    override fun <T : Any> deserialize(raw: String?, valueType: Type?): T? = raw?.let {
        when {
            valueType == String::class.java -> {
                @Suppress("UNCHECKED_CAST")
                raw as T
            }

            else -> {
                val type = wirespecObjectMapper.constructType(valueType)
                wirespecObjectMapper.readValue(it, type)
            }
        }
    }
}
