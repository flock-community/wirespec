package community.flock.wirespec.examples.app.common

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import community.flock.wirespec.Wirespec
import kotlin.reflect.KType
import kotlin.reflect.javaType

object Serialization : Wirespec.Serialization<String> {

    private val mapper = jacksonObjectMapper()

    override fun <T : Any> serialize(t: T, kType: KType): String =
        mapper.writeValueAsString(t)

    @OptIn(ExperimentalStdlibApi::class)
    override fun <T : Any> deserialize(raw: String, kType: KType): T =
        mapper.constructType(kType.javaType)
            .let { mapper.readValue(raw, it) }

}
