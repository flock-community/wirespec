package community.flock.wirespec.emitters.kotlin

import community.flock.wirespec.compiler.core.emit.DEFAULT_SHARED_PACKAGE_STRING
import community.flock.wirespec.compiler.core.emit.Shared
import community.flock.wirespec.compiler.core.emit.Spacer

data object KotlinShared : Shared {
    override val packageString: String = "$DEFAULT_SHARED_PACKAGE_STRING.kotlin"

    override val source = """
        |package $packageString
        |
        |import kotlin.reflect.KType
        |
        |object Wirespec {
        |${Spacer}interface Endpoint {}
        |${Spacer}interface Adapter<Req: Request<*>, Res: Response<*>> {
        |${Spacer(2)}fun toRawRequest(serialization: Wirespec.Serializer, request: Req): RawRequest
        |${Spacer(2)}fun fromRawRequest(serialization: Wirespec.Deserializer, request: RawRequest): Req
        |${Spacer(2)}fun toRawResponse(serialization: Wirespec.Serializer, response: Res): RawResponse
        |${Spacer(2)}fun fromRawResponse(serialization: Wirespec.Deserializer, response: RawResponse): Res
        |${Spacer(2)}val pathTemplate: String
        |${Spacer(2)}val method: String
        |$Spacer}
        |${Spacer}interface Enum { val label: String }
        |${Spacer}interface Refined { val value: String }
        |${Spacer}interface Path
        |${Spacer}interface Queries
        |${Spacer}interface Headers
        |${Spacer}interface Handler
        |${Spacer}enum class Method { GET, PUT, POST, DELETE, OPTIONS, HEAD, PATCH, TRACE }
        |${Spacer}interface Request<T : Any> { val path: Path; val method: Method; val queries: Queries; val headers: Headers; val body: T; interface Headers : Wirespec.Headers }
        |${Spacer}interface Response<T : Any> { val status: Int; val headers: Headers; val body: T; interface Headers : Wirespec.Headers }
        |${Spacer}interface Serialization : Serializer, Deserializer
        |${Spacer}interface Serializer : BodySerializer, PathSerializer, ParamSerializer
        |${Spacer}interface Deserializer : BodyDeserializer, PathDeserializer, ParamDeserializer
        |${Spacer}interface BodySerialization : BodySerializer, BodyDeserializer
        |${Spacer}interface BodySerializer { fun <T> serializeBody(t: T, kType: KType): ByteArray }
        |${Spacer}interface BodyDeserializer { fun <T> deserializeBody(raw: ByteArray, kType: KType): T }
        |${Spacer}interface PathSerialization : PathSerializer, PathDeserializer
        |${Spacer}interface PathSerializer { fun <T> serializePath(t: T, kType: KType): String }
        |${Spacer}interface PathDeserializer { fun <T> deserializePath(raw: String, kType: KType): T }
        |${Spacer}interface ParamSerialization : ParamSerializer, ParamDeserializer
        |${Spacer}interface ParamSerializer { fun <T> serializeParam(value: T, kType: KType): List<String> }
        |${Spacer}interface ParamDeserializer { fun <T> deserializeParam(values: List<String>, kType: KType): T }
        |${Spacer}data class RawRequest(val method: String, val path: List<String>, val queries: Map<String, List<String>>, val headers: Map<String, List<String>>, val body: ByteArray?) 
        |${Spacer}data class RawResponse(val statusCode: Int, val headers: Map<String, List<String>>, val body: ByteArray?)
        |}
    """.trimMargin()
}
