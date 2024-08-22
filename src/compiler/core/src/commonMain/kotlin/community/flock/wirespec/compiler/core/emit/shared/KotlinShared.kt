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
        |${Spacer}enum class Method { GET, PUT, POST, DELETE, OPTIONS, HEAD, PATCH, TRACE }
        |${Spacer}interface Request<T : Any> { val path: Path; val method: Method; val queries: Queries; val headers: Headers; val body: T?; interface Headers : Wirespec.Headers }
        |${Spacer}interface Response<T : Any> { val status: Int; val headers: Headers; val body: T?; interface Headers : Wirespec.Headers }
        |${Spacer}interface Mapper<RAW : Any> : Writer<RAW>, Reader<RAW>
        |${Spacer}interface Writer<RAW : Any> { fun <T : Any> write(t: T, kType: KType): RAW }
        |${Spacer}interface Reader<RAW> { fun <T : Any> read(raw: RAW, kType: KType): T }
        |${Spacer}interface Http { val headers: Map<String, List<String>>; val body: String? }
        |${Spacer}data class RawRequest(val method: String, val path: List<String>, val queries: Map<String, List<String>>, override val headers: Map<String, List<String>>, override val body: String?, ) : Http
        |${Spacer}data class RawResponse(val statusCode: Int, override val headers: Map<String, List<String>>, override val body: String?, ) : Http
        |}
    """.trimMargin()
}
