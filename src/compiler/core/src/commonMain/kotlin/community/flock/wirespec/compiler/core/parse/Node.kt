package community.flock.wirespec.compiler.core.parse

import arrow.core.NonEmptyList
import community.flock.wirespec.compiler.core.FileUri
import community.flock.wirespec.compiler.core.Value
import community.flock.wirespec.compiler.core.parse.Reference.Primitive.Type.Precision.P64
import community.flock.wirespec.compiler.core.removeBackticks
import community.flock.wirespec.compiler.core.removeCommentMarkers
import kotlin.jvm.JvmInline

sealed interface Node

data class AST(
    val modules: NonEmptyList<Module>,
) : Node

data class Module(
    val fileUri: FileUri,
    val statements: Statements,
) : Node

typealias Statements = NonEmptyList<Definition>

sealed interface Definition : Node {
    val comment: Comment?
    val annotations: List<Annotation>
    val identifier: Identifier
}

sealed interface Model : Definition

data class Type(
    override val comment: Comment?,
    override val annotations: List<Annotation>,
    override val identifier: DefinitionIdentifier,
    val shape: Shape,
    val extends: List<Reference>,
) : Model {
    data class Shape(override val value: List<Field>) : Value<List<Field>>
}

sealed interface Reference : Value<String> {
    val isNullable: Boolean

    fun copy(isNullable: Boolean? = null) = when (this) {
        is Any -> copy(isNullable = isNullable ?: this.isNullable)
        is Custom -> copy(isNullable = isNullable ?: this.isNullable)
        is Dict -> copy(isNullable = isNullable ?: this.isNullable)
        is Iterable -> copy(isNullable = isNullable ?: this.isNullable)
        is Primitive -> copy(isNullable = isNullable ?: this.isNullable)
        is Unit -> copy(isNullable = isNullable ?: this.isNullable)
    }

    data class Any(
        override val isNullable: Boolean,
    ) : Reference {
        override val value = "Any"
    }

    data class Unit(
        override val isNullable: Boolean,
    ) : Reference {
        override val value = "Unit"
    }

    data class Dict(
        val reference: Reference,
        override val isNullable: Boolean,
    ) : Reference {
        override val value = "Dict"
    }

    data class Iterable(
        val reference: Reference,
        override val isNullable: Boolean,
    ) : Reference {
        override val value = "Iterable"
    }

    data class Custom(
        override val value: String,
        override val isNullable: Boolean,
    ) : Reference

    data class Primitive(
        val type: Type,
        override val isNullable: Boolean,
    ) : Reference {

        sealed interface Type {
            val name: kotlin.String

            enum class Precision { P32, P64 }

            sealed interface Constraint {
                @JvmInline
                value class RegExp(override val value: kotlin.String) :
                    Value<kotlin.String>,
                    Constraint

                data class Bound(val min: kotlin.String?, val max: kotlin.String?) : Constraint
            }

            data class String(val constraint: Constraint.RegExp?) : Type {
                override val name = "String"
            }

            data class Integer(val precision: Precision = P64, val constraint: Constraint.Bound?) : Type {
                override val name = "Integer"
            }

            data class Number(val precision: Precision = P64, val constraint: Constraint.Bound?) : Type {
                override val name = "Number"
            }

            data object Boolean : Type {
                override val name = "Boolean"
            }

            data object Bytes : Type {
                override val name = "Bytes"
            }
        }

        override val value = type.name
    }
}

data class Field(
    val identifier: FieldIdentifier,
    val reference: Reference,
    val annotations: List<Annotation>,
)

data class Enum(
    override val comment: Comment?,
    override val annotations: List<Annotation>,
    override val identifier: DefinitionIdentifier,
    val entries: Set<String>,
) : Model

data class Union(
    override val comment: Comment?,
    override val annotations: List<Annotation>,
    override val identifier: DefinitionIdentifier,
    val entries: Set<Reference>,
) : Model

data class Refined(
    override val comment: Comment?,
    override val annotations: List<Annotation>,
    override val identifier: DefinitionIdentifier,
    val reference: Reference.Primitive,
) : Model

data class Endpoint(
    override val comment: Comment?,
    override val annotations: List<Annotation>,
    override val identifier: DefinitionIdentifier,
    val method: Method,
    val path: List<Segment>,
    val queries: List<Field>,
    val headers: List<Field>,
    val requests: List<Request>,
    val responses: List<Response>,
) : Definition {
    enum class Method { GET, POST, PUT, DELETE, OPTIONS, HEAD, PATCH, TRACE }
    sealed interface Segment {
        data class Literal(override val value: String) :
            Value<String>,
            Segment
        data class Param(
            val identifier: FieldIdentifier,
            val reference: Reference,
        ) : Segment
    }

    data class Request(val content: Content?)
    data class Response(
        val status: String,
        val headers: List<Field>,
        val content: Content?,
        val annotations: List<Annotation> = emptyList(),
    )
    data class Content(val type: String, val reference: Reference)
}

data class Channel(
    override val comment: Comment?,
    override val annotations: List<Annotation>,
    override val identifier: DefinitionIdentifier,
    val reference: Reference,
) : Definition

@JvmInline
value class Comment private constructor(override val value: String) : Value<String> {
    companion object {
        operator fun invoke(comment: String) = Comment(comment.removeCommentMarkers())
    }
}

data class Annotation(
    val name: String,
    val parameters: List<Parameter>,
) : Node {
    data class Parameter(
        val name: String,
        val value: Value,
    ) : Node
    sealed interface Value {
        data class Single(val value: String) : Value
        data class Array(val value: List<Single>) : Value
        data class Dict(val value: List<Parameter>) : Value
    }
}

sealed class Identifier(name: String) : Value<String> {
    override val value = name.removeBackticks()
    override fun toString() = value

    abstract fun map(fn: (String) -> String): Identifier
}

data class DefinitionIdentifier(private val name: String) : Identifier(name) {
    override fun map(fn: (String) -> String) = DefinitionIdentifier(fn(value))
}

data class FieldIdentifier(private val name: String) : Identifier(name) {
    override fun map(fn: (String) -> String) = FieldIdentifier(fn(value))
}
