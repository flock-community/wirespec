package community.flock.wirespec.compiler.core.parse.ast

import community.flock.wirespec.compiler.core.Value

public sealed interface Definition :
    HasComment,
    Node {
    public val identifier: Identifier
}

public sealed interface ShapeEntry

public data class Field(
    override val annotations: List<Annotation>,
    val identifier: FieldIdentifier,
    val reference: Reference,
    val defaultValue: DefaultValue? = null,
) : HasAnnotations,
    ShapeEntry

/** Validation resolves a spread to the [fields] of its part, so a validated shape still tells which parts it spreads. */
public data class Spread(val reference: Reference.Custom, val fields: List<Field>? = null) : ShapeEntry

public data class Shape(override val value: List<ShapeEntry>) : Value<List<ShapeEntry>>

/** The fields with every [Spread] written out; only valid on a validated AST, where every spread is resolved. */
public val Shape.fields: List<Field> get() = value.flatMap { entry ->
    when (entry) {
        is Field -> listOf(entry)
        is Spread -> entry.fields ?: error("Spread ...${entry.reference.value} is unresolved: read fields only from a validated AST")
    }
}

/**
 * A reusable set of fields that is only ever spread into shapes, never emitted on its own.
 * Validation resolves every [Spread] to the fields of its part, so after parsing [fields]
 * reads every shape as plain [Field]s.
 */
public data class Part(
    override val comment: Comment?,
    override val identifier: DefinitionIdentifier,
    val shape: Shape,
) : Definition

public data class Endpoint(
    override val comment: Comment?,
    override val annotations: List<Annotation>,
    override val identifier: DefinitionIdentifier,
    val method: Method,
    val path: List<Segment>,
    val queries: Shape,
    val headers: Shape,
    val requests: List<Request>,
    val responses: List<Response>,
) : Definition,
    HasMetaData {
    public enum class Method { GET, POST, PUT, DELETE, OPTIONS, HEAD, PATCH, TRACE }
    public sealed interface Segment {
        public data class Literal(override val value: String) :
            Value<String>,
            Segment
        public data class Param(
            val identifier: FieldIdentifier,
            val reference: Reference,
        ) : Segment
    }

    public data class Request(val content: Content?)
    public data class Response(val status: String, val headers: Shape, val content: Content?, val annotations: List<Annotation>)
    public data class Content(val type: String, val reference: Reference)
}

public data class Channel(
    override val comment: Comment?,
    override val annotations: List<Annotation>,
    override val identifier: DefinitionIdentifier,
    val reference: Reference,
) : Definition,
    HasMetaData

public data class Rpc(
    override val comment: Comment?,
    override val annotations: List<Annotation>,
    override val identifier: DefinitionIdentifier,
    val shape: Shape,
    val result: Reference,
    val error: Reference?,
) : Definition,
    HasMetaData

public sealed interface Model :
    Definition,
    HasMetaData

public data class Type(
    override val comment: Comment?,
    override val annotations: List<Annotation>,
    override val identifier: DefinitionIdentifier,
    val shape: Shape,
    val extends: List<Reference>,
) : Model

public data class Enum(
    override val comment: Comment?,
    override val annotations: List<Annotation>,
    override val identifier: DefinitionIdentifier,
    val entries: Set<String>,
) : Model

public data class Union(
    override val comment: Comment?,
    override val annotations: List<Annotation>,
    override val identifier: DefinitionIdentifier,
    val entries: Set<Reference>,
) : Model

public data class Refined(
    override val comment: Comment?,
    override val annotations: List<Annotation>,
    override val identifier: DefinitionIdentifier,
    val reference: Reference.Primitive,
) : Model
