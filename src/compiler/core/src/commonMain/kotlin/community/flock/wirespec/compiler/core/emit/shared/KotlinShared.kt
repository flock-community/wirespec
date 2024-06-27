package community.flock.wirespec.compiler.core.emit.shared

import community.flock.wirespec.compiler.core.emit.common.Emitter.Companion.SPACER

data object KotlinShared : Shared {
    override val source = """
        |package community.flock.wirespec
        |
        |import kotlin.reflect.KType
        |
        |object Wirespec {
        |${SPACER}interface Enum
        |${SPACER}interface Endpoint
        |${SPACER}interface Refined { val value: String }
        |${SPACER}enum class Method { GET, PUT, POST, DELETE, OPTIONS, HEAD, PATCH, TRACE }
        |${SPACER}data class Content<T> (val type:String, val body:T )
        |${SPACER}interface Request<T> { val path:String; val method: Method; val query: Map<String, List<Any?>>; val headers: Map<String, List<Any?>>; val content:Content<T>? }
        |${SPACER}interface Response<T> { val status:Int; val headers: Map<String, List<Any?>>; val content:Content<T>? }
        |${SPACER}interface ContentMapper<B> { fun <T> read(content: Content<B>, valueType: KType): Content<T> fun <T> write(content: Content<T>, valueType: KType): Content<B> }
        |}
    """.trimMargin()
}
