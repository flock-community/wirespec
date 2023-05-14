package community.flock.wirespec.compiler.core.parse

sealed interface Node

sealed interface Definition : Node

data class Type(val name: String, val shape: Shape) : Definition {
    data class Shape(val value: List<Field>) {
        data class Field(val identifier: Identifier, val reference: Reference, val isNullable: Boolean) {
            data class Identifier(val value: String)
            sealed interface Reference {
                val isIterable: Boolean
                data class Custom(val value: String, override val isIterable: Boolean) : Reference
                data class Primitive(val type: Type, override val isIterable: Boolean) : Reference {
                    enum class Type { String, Integer, Boolean }
                }
            }
        }
    }
}

data class Refined(val name: String, val validator: Validator) : Definition {
    data class Validator(val value: String)
}

data class Endpoint(
    val name: String,
    val method: Method,
    val path: List<Segment>,
    val query:List<Type.Shape.Field>,
    val headers:List<Type.Shape.Field>,
    val cookies:List<Type.Shape.Field>,
    val requests: List<Request>,
    val responses: List<Response>
) : Definition {
    enum class Method { GET, POST, PUT, DELETE, OPTIONS, HEAD, PATCH, TRACE }
    sealed interface Segment {
        data class Literal(val value: String) : Segment
        data class Param(val identifier: Type.Shape.Field.Identifier, val reference: Type.Shape.Field.Reference) : Segment
    }
    data class Request(val content:Content?)
    data class Response(val status: String, val content:Content?)
    data class Content(val type: String, val reference: Type.Shape.Field.Reference)
}


