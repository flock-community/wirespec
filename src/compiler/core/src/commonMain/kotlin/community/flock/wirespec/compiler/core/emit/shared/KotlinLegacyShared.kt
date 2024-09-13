package community.flock.wirespec.compiler.core.emit.shared

import community.flock.wirespec.compiler.core.emit.common.DEFAULT_SHARED_PACKAGE_STRING
import community.flock.wirespec.compiler.core.emit.common.Spacer

data object KotlinLegacyShared : Shared {
    override val source = """
        |package $DEFAULT_SHARED_PACKAGE_STRING
        |
        |import kotlin.reflect.KType
        |
        |object Wirespec {
        |${Spacer}interface Enum
        |${Spacer}interface Endpoint
        |${Spacer}interface Refined { val value: String }
        |${Spacer}enum class Method { GET, PUT, POST, DELETE, OPTIONS, HEAD, PATCH, TRACE }
        |${Spacer}data class Content<T> (val type:String, val body:T )
        |${Spacer}interface Request<T> { val path:String; val method: Method; val query: Map<String, List<Any?>>; val headers: Map<String, List<Any?>>; val content:Content<T>? }
        |${Spacer}interface Response<T> { val status:Int; val headers: Map<String, List<Any?>>; val content:Content<T>? }
        |${Spacer}interface ContentMapper<B> { fun <T> read(content: Content<B>, valueType: KType): Content<T> fun <T> write(content: Content<T>, valueType: KType): Content<B> }
        |}
    """.trimMargin()
}
