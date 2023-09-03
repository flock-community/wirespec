package community.flock.wirespec.kotlin

import java.lang.reflect.Type

interface Wirespec {
  enum class Method { GET, PUT, POST, DELETE, OPTIONS, HEAD, PATCH, TRACE }
  data class Content<T> (val type:String, val body:T )
  interface Request<T> { val path:String; val method: Method; val query: Map<String, List<Any?>>; val headers: Map<String, List<Any?>>; val content:Content<T>? }
  interface Response<T> { val status:Int; val headers: Map<String, List<Any?>>; val content:Content<T>? }
  interface ContentMapper<B> { fun <T> read(content: Content<B>, valueType: Type): Content<T> fun <T> write(content: Content<T>): Content<B> }
}