package community.flock.wirespec

import java.lang.reflect.Type
import java.lang.reflect.ParameterizedType

object Wirespec {
    interface Enum
    interface Endpoint
    interface Refined { val value: String }
    enum class Method { GET, PUT, POST, DELETE, OPTIONS, HEAD, PATCH, TRACE }
    @JvmRecord data class Content<T> (val type:String, val body:T )
    interface Request<T> { val path:String; val method: Method; val query: Map<String, List<Any?>>; val headers: Map<String, List<Any?>>; val content:Content<T>? }
    interface Response<T> { val status:Int; val headers: Map<String, List<Any?>>; val content:Content<T>? }
    interface ContentMapper<B> { fun <T> read(content: Content<B>, valueType: Type): Content<T> fun <T> write(content: Content<T>): Content<B> }
    @JvmStatic fun getType(type: Class<*>, isIterable: Boolean): Type {
        return if (isIterable) {
            object : ParameterizedType {
                override fun getRawType() = MutableList::class.java
                override fun getActualTypeArguments() = arrayOf(type)
                override fun getOwnerType() = null
            }
        } else {
            type
        }
    }
}
