package community.flock.wirespec.integration.spring

import com.fasterxml.jackson.databind.ObjectMapper
import community.flock.wirespec.Wirespec
import community.flock.wirespec.integration.jackson.WirespecModule
import java.io.BufferedReader
import java.lang.reflect.Type

class JacksonContentMapper(objectMapper: ObjectMapper) : Wirespec.ContentMapper<BufferedReader> {

    private val wirespecObjectMapper = objectMapper.copy().registerModule(WirespecModule())
    override fun <T> read(
        content: Wirespec.Content<BufferedReader>,
        valueType: Type,
    ) = content.let {
        val type = wirespecObjectMapper.constructType(valueType)
        val obj: T = wirespecObjectMapper.readValue(it.body, type)
        Wirespec.Content(it.type, obj)
    }

    override fun <T> write(
        content: Wirespec.Content<T>,
    ) = content.let {
        val bytes: ByteArray = wirespecObjectMapper.writeValueAsBytes(content.body)
        Wirespec.Content(it.type, bytes.inputStream().bufferedReader())
    }
}