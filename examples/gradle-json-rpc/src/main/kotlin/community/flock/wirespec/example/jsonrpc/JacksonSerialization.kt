package community.flock.wirespec.example.jsonrpc

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import community.flock.wirespec.kotlin.Wirespec
import community.flock.wirespec.kotlin.serde.DefaultParamSerialization
import community.flock.wirespec.kotlin.serde.DefaultPathSerialization
import kotlin.reflect.KType
import kotlin.reflect.javaType

/**
 * Reads and writes the params, results and errors of the rpcs with Jackson. The generated code writes the JSON-RPC
 * envelope itself, so this is all the serialization a JSON-RPC client or server needs. JSON-RPC has no paths or query
 * parameters; the defaults of the Wirespec runtime cover them for the endpoints.
 */
object JacksonSerialization :
    Wirespec.Serialization,
    Wirespec.ParamSerialization by DefaultParamSerialization(),
    Wirespec.PathSerialization by DefaultPathSerialization() {

    private val mapper = jacksonObjectMapper()

    override fun <T : Any> serializeBody(t: T, type: KType): ByteArray = mapper.writeValueAsBytes(t)

    @OptIn(ExperimentalStdlibApi::class)
    override fun <T : Any> deserializeBody(raw: ByteArray, type: KType): T = mapper.readValue(raw, mapper.constructType(type.javaType))
}
