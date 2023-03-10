package community.flock.wirespec.compiler.core.parse

sealed interface Node

sealed interface Definition : Node

sealed interface Type

data class TypeDefinition(val name: Name, val shape: Type) : Definition {
    data class Name(val value: String)
}

data class EndpointDefinition(
    val name: Name,
    val method: Method,
    val path: List<Segment>,
    val responses: List<Response>
) : Definition {
    data class Name(val value: String)
    enum class Method { GET, POST, PUT, DELETE, OPTIONS, HEAD, PATCH, TRACE }
    sealed interface Segment {
        data class Literal(val value: String) : Segment
        data class Param(val key: String, val type: Type) : Segment
    }

    data class Response(val status: String, val contentType: String, val type: Shape.Field.Value)
}

data class Shape(val value: List<Field>) : Type {
    data class Field(val key: Key, val value: Value, val isNullable: Boolean) {
        data class Key(val value: String)
        sealed class Value(open val isIterable: Boolean) : Type {
            abstract fun toName():String
            data class Custom(val value: String, override val isIterable: Boolean) : Value(isIterable) {
                override fun toName() = value
            }
            data class Primitive(val value: PrimitiveType, override val isIterable: Boolean = false) : Value(isIterable) {
                enum class PrimitiveType { String, Integer, Boolean }
                override fun toName() = value.name
            }
        }
    }
}
