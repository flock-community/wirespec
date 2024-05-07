package community.flock.wirespec.compiler.core.parse

import community.flock.wirespec.compiler.core.Value
import community.flock.wirespec.compiler.core.removeBackticks

sealed interface Node

sealed interface Definition : Node {
    val identifier: Identifier
}

data class Type(override val identifier: Identifier, val shape: Shape) : Definition {
    data class Shape(override val value: List<Field>) : Value<List<Field>>
}

data class Field(val identifier: Identifier, val reference: Reference, val isNullable: Boolean) {
    sealed interface Reference : Value<String> {
        val isIterable: Boolean
        val isMap: Boolean

        data class Any(
            override val isIterable: Boolean,
            override val isMap: Boolean = false,
        ) : Reference {
            override val value = "Any"
        }

        data class Unit(
            override val isIterable: Boolean,
            override val isMap: Boolean = false,
        ) : Reference {
            override val value = "Unit"
        }

        data class Custom(
            override val value: String,
            override val isIterable: Boolean,
            override val isMap: Boolean = false
        ) : Reference

        data class Primitive(
            val type: Type,
            override val isIterable: Boolean = false,
            override val isMap: Boolean = false
        ) : Reference {
            enum class Type { String, Integer, Number, Boolean }

            override val value = type.name
        }
    }
}

data class Enum(override val identifier: Identifier, val entries: Set<String>) : Definition

data class Union(override val identifier: Identifier, val entries: Set<Field.Reference>) : Definition

data class Refined(override val identifier: Identifier, val validator: Validator) : Definition {
    data class Validator(override val value: String) : Value<String>
}

data class Endpoint(
    override val identifier: Identifier,
    val method: Method,
    val path: List<Segment>,
    val query: List<Field>,
    val headers: List<Field>,
    val cookies: List<Field>,
    val requests: List<Request>,
    val responses: List<Response>
) : Definition {
    enum class Method { GET, POST, PUT, DELETE, OPTIONS, HEAD, PATCH, TRACE }
    sealed interface Segment {
        data class Literal(override val value: String) : Value<String>, Segment
        data class Param(
            val identifier: Identifier,
            val reference: Field.Reference
        ) : Segment
    }

    data class Request(val content: Content?)
    data class Response(val status: String, val headers: List<Field>, val content: Content?)
    data class Content(val type: String, val reference: Field.Reference, val isNullable: Boolean = false)
}

class Identifier private constructor(override val value: String) : Value<String> {

    override fun toString(): String = value

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as Identifier

        return value == other.value
    }

    override fun hashCode(): Int = value.hashCode()

    companion object {
        operator fun invoke(value: String) = value
            .removeBackticks()
            .let(::Identifier)
    }
}
