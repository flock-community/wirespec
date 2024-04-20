package community.flock.wirespec.compiler.core.emit.shared

import community.flock.wirespec.compiler.core.emit.common.Emitter.Companion.SPACER

data object KotlinShared : Shared {
    override val source = """
        |package community.flock.wirespec
        |
        |import java.lang.reflect.Type
        |import java.lang.reflect.ParameterizedType
        |
        |object Wirespec {
        |${SPACER}interface Enum
        |${SPACER}interface Endpoint
        |${SPACER}interface Refined { val value: String }
        |${SPACER}enum class Method { GET, PUT, POST, DELETE, OPTIONS, HEAD, PATCH, TRACE }
        |$SPACER@JvmRecord data class Content<T> (val type:String, val body:T )
        |${SPACER}interface Request<T> { val path:String; val method: Method; val query: Map<String, List<Any?>>; val headers: Map<String, List<Any?>>; val content:Content<T>? }
        |${SPACER}interface Response<T> { val status:Int; val headers: Map<String, List<Any?>>; val content:Content<T>? }
        |${SPACER}interface ContentMapper<B> { fun <T> read(content: Content<B>, valueType: Type): Content<T> fun <T> write(content: Content<T>): Content<B> }
        |$SPACER$SPACER@JvmStatic fun getType(type: Class<*>, isIterable: Boolean): Type {
        |$SPACER${SPACER}return if (isIterable) {
        |$SPACER$SPACER${SPACER}object : ParameterizedType {
        |$SPACER$SPACER$SPACER${SPACER}override fun getRawType() = MutableList::class.java
        |$SPACER$SPACER$SPACER${SPACER}override fun getActualTypeArguments() = arrayOf(type)
        |$SPACER$SPACER$SPACER${SPACER}override fun getOwnerType() = null
        |$SPACER$SPACER$SPACER}
        |$SPACER$SPACER} else {
        |$SPACER$SPACER${SPACER}type
        |$SPACER$SPACER}
        |$SPACER}
        |}
    """.trimMargin()
}
