package community.flock.wirespec.compiler.core.parse

sealed interface Node

sealed interface Definition : Node

data class Type(val name: String, val shape: Shape) : Definition {
    data class Shape(val value: List<Field>) {
        data class Field(val identifier: Identifier, val reference: Reference, val isNullable: Boolean) {
            data class Identifier(val value: String)
            sealed class Reference(val isIterable: Boolean) {
                class Custom(val value: String, isIterable: Boolean) : Reference(isIterable)
                class Primitive(val type: Type, isIterable: Boolean) : Reference(isIterable) {
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
    val responses: List<Response>
) : Definition {
    enum class Method { GET, POST, PUT, DELETE, OPTIONS, HEAD, PATCH, TRACE }
    sealed interface Segment {
        data class Literal(val value: String) : Segment
        data class Param(val key: String, val reference: Type.Shape.Field.Reference) : Segment
    }
    data class Response(val status: String, val contentType: String, val type: Type.Shape.Field.Reference)
}


