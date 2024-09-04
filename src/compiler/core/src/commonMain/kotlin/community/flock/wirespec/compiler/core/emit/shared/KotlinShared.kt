package community.flock.wirespec.compiler.core.emit.shared

import community.flock.wirespec.compiler.core.emit.common.Spacer

data object KotlinShared : Shared {
    override val source = """
        |package community.flock.wirespec
        |
        |import kotlin.reflect.KType
        |
        |object Wirespec {
        |${Spacer}interface Enum
        |${Spacer}interface Endpoint
        |${Spacer}interface Refined { val value: String }
        |${Spacer}interface Path
        |${Spacer}interface Queries
        |${Spacer}interface Headers
        |${Spacer}interface Handler
        |interface ServerEdge<Req: Request<*>, Res: Response<*>> { 
        |  fun consume(request: RawRequest): Req  
        |  fun produce(response: Res): RawResponse
        |}
        |interface ClientEdge<Req: Request<*>, Res: Response<*>> { 
        |  fun internalize(response: RawResponse): Res  
        |  fun externalize(request: Req): RawRequest
        |}
        |interface Client<Req : Request<*>, Res : Response<*>> {
        |  val pathTemplate: String
        |  val method: String
        |  fun client(serialization: Serialization<String>): ClientEdge<Req, Res>
        |}
        |interface Server<Req : Request<*>, Res : Response<*>> {
        |  val pathTemplate: String
        |  val method: String
        |  fun server(serialization: Serialization<String>): ServerEdge<Req, Res>
        |}
        |${Spacer}enum class Method { GET, PUT, POST, DELETE, OPTIONS, HEAD, PATCH, TRACE }
        |${Spacer}interface Request<T : Any> { val path: Path; val method: Method; val queries: Queries; val headers: Headers; val body: T; interface Headers : Wirespec.Headers }
        |${Spacer}interface Response<T : Any> { val status: Int; val headers: Headers; val body: T; interface Headers : Wirespec.Headers }
        |${Spacer}interface Serialization<RAW : Any> : Serializer<RAW>, Deserializer<RAW>
        |${Spacer}interface Serializer<RAW : Any> { fun <T> serialize(t: T, kType: KType): RAW }
        |${Spacer}interface Deserializer<RAW: Any> { fun <T> deserialize(raw: RAW, kType: KType): T }
        |${Spacer}data class RawRequest(val method: String, val path: List<String>, val queries: Map<String, List<String>>, val headers: Map<String, List<String>>, val body: String?) 
        |${Spacer}data class RawResponse(val statusCode: Int, val headers: Map<String, List<String>>, val body: String?)
        |}
    """.trimMargin()
}
