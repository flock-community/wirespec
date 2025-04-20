package community.flock.wirespec.integration.jackson.java

import com.fasterxml.jackson.databind.ObjectMapper
import community.flock.wirespec.java.Wirespec.ParamSerialization
import community.flock.wirespec.java.Wirespec.Serialization
import java.lang.reflect.Type

/**
 * A reusable implementation of Wirespec.Serialization that uses Jackson for serialization and deserialization.
 * This class delegates parameter serialization and deserialization to a provided ParamSerialization implementation.
 */
class WirespecSerialization(
    objectMapper: ObjectMapper,
    paramSerde: ParamSerialization,
) : Serialization<String>,
    ParamSerialization by paramSerde {

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
