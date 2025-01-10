package community.flock.wirespec.compiler.core.emit.shared

import community.flock.wirespec.compiler.core.emit.common.DEFAULT_SHARED_PACKAGE_STRING
import community.flock.wirespec.compiler.core.emit.common.Spacer

data object KotlinShared : Shared {
    override val packageString: String = "$DEFAULT_SHARED_PACKAGE_STRING.kotlin"

    override val source = """
        |package $packageString
        |
        |import kotlin.reflect.KType
        |
        |object Wirespec {
        |${Spacer}interface Enum { val label: String }
        |${Spacer}interface Endpoint
        |${Spacer}interface Refined { val value: String }
        |${Spacer}interface Path
        |${Spacer}interface Queries
        |${Spacer}interface Headers
        |${Spacer}interface Handler
        |${Spacer}interface ServerEdge<Req: Request<*>, Res: Response<*>> { 
        |${Spacer}fun from(request: RawRequest): Req  
        |${Spacer}fun to(response: Res): RawResponse
        |${Spacer}}
        |${Spacer}interface ClientEdge<Req: Request<*>, Res: Response<*>> { 
        |${Spacer(2)}fun to(request: Req): RawRequest
        |${Spacer(2)}fun from(response: RawResponse): Res    
        |${Spacer}}
        |${Spacer}interface Client<Req : Request<*>, Res : Response<*>> {
        |${Spacer(2)}val pathTemplate: String
        |${Spacer(2)}val method: String
        |${Spacer(2)}fun client(serialization: Serialization<String>): ClientEdge<Req, Res>
        |${Spacer}}
        |${Spacer}interface Server<Req : Request<*>, Res : Response<*>> {
        |${Spacer(2)}val pathTemplate: String
        |${Spacer(2)}val method: String
        |${Spacer(2)}fun server(serialization: Serialization<String>): ServerEdge<Req, Res>
        |${Spacer}}
        |${Spacer}enum class Method { GET, PUT, POST, DELETE, OPTIONS, HEAD, PATCH, TRACE }
        |${Spacer}interface Request<T : Any> { val path: Path; val method: Method; val queries: Queries; val headers: Headers; val body: T; interface Headers : Wirespec.Headers }
        |${Spacer}interface Response<T : Any> { val status: Int; val headers: Headers; val body: T; interface Headers : Wirespec.Headers }
        |${Spacer}interface Serialization<RAW : Any> : Serializer<RAW>, Deserializer<RAW>, ParamSerialization
        |${Spacer}interface ParamSerialization: ParamSerializer, ParamDeserializer
        |${Spacer}interface ParamSerializer { fun <T> serializeParam(value: T, kType: KType): List<String> }
        |${Spacer}interface Serializer<RAW : Any> : ParamSerializer { fun <T> serialize(t: T, kType: KType): RAW }
        |${Spacer}interface Deserializer<RAW: Any>: ParamDeserializer { fun <T> deserialize(raw: RAW, kType: KType): T }
        |${Spacer}interface ParamDeserializer { fun <T> deserializeParam(values: List<String>, kType: KType): T }
        |${Spacer}data class RawRequest(val method: String, val path: List<String>, val queries: Map<String, List<String>>, val headers: Map<String, List<String>>, val body: String?) 
        |${Spacer}data class RawResponse(val statusCode: Int, val headers: Map<String, String>, val body: String?)
        |}
    """.trimMargin()
}