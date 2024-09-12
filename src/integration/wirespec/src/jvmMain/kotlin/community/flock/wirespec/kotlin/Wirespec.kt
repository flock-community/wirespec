package community.flock.wirespec.kotlin

import kotlin.reflect.KType

object Wirespec {
    interface Enum
    interface Endpoint
    interface Refined {
        val value: String
    }

    interface Path
    interface Queries
    interface Headers
    interface Handler
    interface ServerEdge<Req : Request<*>, Res : Response<*>> {
        fun consume(request: RawRequest): Req
        fun produce(response: Res): RawResponse
    }

    interface ClientEdge<Req : Request<*>, Res : Response<*>> {
        fun internalize(response: RawResponse): Res
        fun externalize(request: Req): RawRequest
    }

    interface Client<Req : Request<*>, Res : Response<*>> {
        val pathTemplate: String
        val method: String
        fun client(serialization: Serialization<String>): ClientEdge<Req, Res>
    }

    interface Server<Req : Request<*>, Res : Response<*>> {
        val pathTemplate: String
        val method: String
        fun server(serialization: Serialization<String>): ServerEdge<Req, Res>
    }

    enum class Method { GET, PUT, POST, DELETE, OPTIONS, HEAD, PATCH, TRACE }
    interface Request<T : Any> {
        val path: Path;
        val method: Method;
        val queries: Queries;
        val headers: Headers;
        val body: T;

        interface Headers : Wirespec.Headers
    }

    interface Response<T : Any> {
        val status: Int;
        val headers: Headers;
        val body: T;

        interface Headers : Wirespec.Headers
    }

    interface Serialization<RAW : Any> : Serializer<RAW>, Deserializer<RAW>
    interface Serializer<RAW : Any> {
        fun <T> serialize(t: T, kType: KType): RAW
    }

    interface Deserializer<RAW : Any> {
        fun <T> deserialize(raw: RAW, kType: KType): T
    }

    data class RawRequest(
        val method: String,
        val path: List<String>,
        val queries: Map<String, List<String>>,
        val headers: Map<String, List<String>>,
        val body: String?
    )

    data class RawResponse(val statusCode: Int, val headers: Map<String, List<String>>, val body: String?)
}
