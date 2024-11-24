package community.flock.wirespec.compiler.core.parse

import community.flock.wirespec.compiler.core.Value
import community.flock.wirespec.compiler.core.removeBackticks
import community.flock.wirespec.compiler.core.removeCommentMarkers
import kotlin.jvm.JvmInline

sealed interface Node

sealed interface Definition : Node {
    val comment: Comment?
    val identifier: Identifier
}

data class Type(
    override val comment: Comment?,
    override val identifier: DefinitionIdentifier,
    val shape: Shape,
    val extends: List<Reference>,
) : Definition {
    data class Shape(override val value: List<Field>) : Value<List<Field>>
}

sealed interface Reference : Value<String> {
    val isIterable: Boolean
    val isDictionary: Boolean

    data class Any(
        override val isIterable: Boolean,
        override val isDictionary: Boolean = false,
    ) : Reference {
        override val value = "Any"
    }

    data class Unit(
        override val isIterable: Boolean,
        override val isDictionary: Boolean = false,
    ) : Reference {
        override val value = "Unit"
    }

    data class Custom(
        override val value: String,
        override val isIterable: Boolean,
        override val isDictionary: Boolean = false
    ) : Reference

    data class Primitive(
        val type: Type,
        override val isIterable: Boolean = false,
        override val isDictionary: Boolean = false
    ) : Reference {
        sealed interface Type{
            val name: kotlin.String; val subType: kotlin.String?
            data class String(override val subType: kotlin.String? = null):Type {
                override val name get() = "String"
            }
            data class Integer(override val subType: kotlin.String? = null):Type {
                override val name get() = "Integer"
            }
            data class Number(override val subType: kotlin.String? = null):Type {
                override val name get() = "Number"
            }
            data class Boolean(override val subType: kotlin.String? = null):Type {
                override val name get() = "Boolean"
            }
        }
        override val value = type.name
    }
}

data class Field(val identifier: FieldIdentifier, val reference: Reference, val isNullable: Boolean)

data class Enum(
    override val comment: Comment?,
    override val identifier: DefinitionIdentifier,
    val entries: Set<String>,
) : Definition

data class Union(
    override val comment: Comment?,
    override val identifier: DefinitionIdentifier,
    val entries: Set<Reference>,
) : Definition

data class Refined(
    override val comment: Comment?,
    override val identifier: DefinitionIdentifier,
    val validator: Validator,
) : Definition {
    data class Validator(override val value: String) : Value<String> {
        val expression = value.split("/").drop(1).dropLast(1).joinToString("/")
    }
}

data class Endpoint(
    override val comment: Comment?,
    override val identifier: DefinitionIdentifier,
    val method: Method,
    val path: List<Segment>,
    val queries: List<Field>,
    val headers: List<Field>,
    val cookies: List<Field>,
    val requests: List<Request>,
    val responses: List<Response>
) : Definition {
    enum class Method { GET, POST, PUT, DELETE, OPTIONS, HEAD, PATCH, TRACE }
    sealed interface Segment {
        data class Literal(override val value: String) : Value<String>, Segment
        data class Param(
            val identifier: FieldIdentifier,
            val reference: Reference
        ) : Segment
    }

    data class Request(val content: Content?)
    data class Response(val status: String, val headers: List<Field>, val content: Content?)
    data class Content(val type: String, val reference: Reference, val isNullable: Boolean = false)
}

data class Channel(
    override val comment: Comment?,
    override val identifier: DefinitionIdentifier,
    val isNullable: Boolean,
    val reference: Reference,
) : Definition

@JvmInline
value class Comment private constructor(override val value: String) : Value<String> {
    companion object {
        operator fun invoke(comment: String) = Comment(comment.removeCommentMarkers())
    }
}

sealed class Identifier(name: String) : Value<String> {
    override val value = name.removeBackticks()
}

data class DefinitionIdentifier(private val name: String) : Identifier(name)

data class FieldIdentifier(private val name: String) : Identifier(name)
