package community.flock.wirespec.kotlin

import kotlin.reflect.KType

object Wirespec {
    interface Enum {
        val label: String
    }
    interface Endpoint
    interface Refined {
        val value: String
    }
    interface Path
    interface Queries
    interface Headers
    interface Handler
    interface ServerEdge<Req : Request<*>, Res : Response<*>> {
        fun from(request: RawRequest): Req
        fun to(response: Res): RawResponse
    }
    interface ClientEdge<Req : Request<*>, Res : Response<*>> {
        fun to(request: Req): RawRequest
        fun from(response: RawResponse): Res
    }
    interface Client<Req : Request<*>, Res : Response<*>> {
        val pathTemplate: String
        val method: String
        fun client(serialization: Serialization): ClientEdge<Req, Res>
    }
    interface Server<Req : Request<*>, Res : Response<*>> {
        val pathTemplate: String
        val method: String
        fun server(serialization: Serialization): ServerEdge<Req, Res>
    }
    enum class Method { GET, PUT, POST, DELETE, OPTIONS, HEAD, PATCH, TRACE }
    interface Request<T : Any> {
        val path: Path
        val method: Method
        val queries: Queries
        val headers: Headers
        val body: T
        interface Headers : Wirespec.Headers
    }
    interface Response<T : Any> {
        val status: Int
        val headers: Headers
        val body: T
        interface Headers : Wirespec.Headers
    }
    interface Serialization :
        Serializer,
        Deserializer
    interface Serializer :
        BodySerializer,
        PathSerializer,
        ParamSerializer
    interface Deserializer :
        BodyDeserializer,
        PathDeserializer,
        ParamDeserializer
    interface BodySerialization :
        BodySerializer,
        BodyDeserializer
    interface BodySerializer {
        fun <T> serializeBody(t: T, kType: KType): ByteArray
    }
    interface BodyDeserializer {
        fun <T> deserializeBody(raw: ByteArray, kType: KType): T
    }
    interface PathSerialization :
        PathSerializer,
        PathDeserializer
    interface PathSerializer {
        fun <T> serializePath(t: T, kType: KType): String
    }
    interface PathDeserializer {
        fun <T> deserializePath(raw: String, kType: KType): T
    }
    interface ParamSerialization :
        ParamSerializer,
        ParamDeserializer
    interface ParamSerializer {
        fun <T> serializeParam(value: T, kType: KType): List<String>
    }
    interface ParamDeserializer {
        fun <T> deserializeParam(values: List<String>, kType: KType): T
    }
    data class RawRequest(val method: String, val path: List<String>, val queries: Map<String, List<String>>, val headers: Map<String, List<String>>, val body: ByteArray?)
    data class RawResponse(val statusCode: Int, val headers: Map<String, List<String>>, val body: ByteArray?)
    interface Transportation { suspend fun transport(request: RawRequest): RawResponse }
}
