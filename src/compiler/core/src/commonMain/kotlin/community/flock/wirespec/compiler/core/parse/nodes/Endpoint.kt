package community.flock.wirespec.compiler.core.parse.nodes

data class Endpoint(
    val name: String,
    val method: Method,
    val path: List<Segment>,
    val query: List<Type.Shape.Field>,
    val headers: List<Type.Shape.Field>,
    val cookies: List<Type.Shape.Field>,
    val requests: List<Request>,
    val responses: List<Response>
) : Definition {
    enum class Method { GET, POST, PUT, DELETE, OPTIONS, HEAD, PATCH, TRACE }
    sealed interface Segment {
        data class Literal(val value: String) : Segment
        data class Param(
            val identifier: Type.Shape.Field.Identifier,
            val reference: Type.Shape.Field.Reference
        ) : Segment
    }

    data class Request(val content: Content?)
    data class Response(val status: String, val content: Content?)
    data class Content(val type: String, val reference: Type.Shape.Field.Reference, val isNullable: Boolean = false)
}
