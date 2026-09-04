@file:OptIn(ExperimentalJsExport::class)

package community.flock.wirespec.compiler.lib

import arrow.core.toNonEmptyListOrNull
import community.flock.wirespec.compiler.core.FileUri
import community.flock.wirespec.compiler.core.parse.ast.AST
import community.flock.wirespec.compiler.core.parse.ast.Channel
import community.flock.wirespec.compiler.core.parse.ast.Comment
import community.flock.wirespec.compiler.core.parse.ast.Definition
import community.flock.wirespec.compiler.core.parse.ast.DefinitionIdentifier
import community.flock.wirespec.compiler.core.parse.ast.Endpoint
import community.flock.wirespec.compiler.core.parse.ast.Enum
import community.flock.wirespec.compiler.core.parse.ast.Field
import community.flock.wirespec.compiler.core.parse.ast.FieldIdentifier
import community.flock.wirespec.compiler.core.parse.ast.Module
import community.flock.wirespec.compiler.core.parse.ast.Reference
import community.flock.wirespec.compiler.core.parse.ast.Reference.Primitive.Type.Constraint
import community.flock.wirespec.compiler.core.parse.ast.Reference.Primitive.Type.Precision.P32
import community.flock.wirespec.compiler.core.parse.ast.Reference.Primitive.Type.Precision.P64
import community.flock.wirespec.compiler.core.parse.ast.Refined
import community.flock.wirespec.compiler.core.parse.ast.Rpc
import community.flock.wirespec.compiler.core.parse.ast.Type
import community.flock.wirespec.compiler.core.parse.ast.Union

public fun WsAST.consume(): AST = AST(
    modules = modules.map { it.consume() }.toNonEmptyListOrNull()!!,
)

public fun WsModule.consume(): Module = Module(
    fileUri = FileUri("unknown"),
    statements = statements.map { it.consume() }.toNonEmptyListOrNull()!!,
)

public fun WsDefinition.consume(): Definition = when (this) {
    is WsEndpoint -> consume()
    is WsEnum -> consume()
    is WsRefined -> consume()
    is WsType -> consume()
    is WsUnion -> consume()
    is WsChannel -> consume()
    is WsRpc -> consume()
}

public fun WsEndpoint.consume(): Endpoint = Endpoint(
    comment = comment?.let { Comment(it) },
    annotations = emptyList(),
    identifier = DefinitionIdentifier(identifier),
    method = method.consume(),
    path = path.map { it.consume() },
    queries = queries.map { it.consume() },
    headers = headers.map { it.consume() },
    requests = requests.map { it.consume() },
    responses = responses.map { it.consume() },
)

private fun WsSegment.consume() = when (this) {
    is WsLiteral -> Endpoint.Segment.Literal(value)
    is WsParam -> Endpoint.Segment.Param(
        identifier = identifier.consume(),
        reference = reference.consume(),
    )
}

private fun WsMethod.consume() = when (this) {
    WsMethod.GET -> Endpoint.Method.GET
    WsMethod.POST -> Endpoint.Method.POST
    WsMethod.PUT -> Endpoint.Method.PUT
    WsMethod.DELETE -> Endpoint.Method.DELETE
    WsMethod.OPTIONS -> Endpoint.Method.OPTIONS
    WsMethod.HEAD -> Endpoint.Method.HEAD
    WsMethod.PATCH -> Endpoint.Method.PATCH
    WsMethod.TRACE -> Endpoint.Method.TRACE
}

private fun WsFieldIdentifier.consume() = FieldIdentifier(value)

private fun WsEnum.consume() = Enum(
    comment = comment?.let { Comment(it) },
    annotations = emptyList(),
    identifier = DefinitionIdentifier(identifier),
    entries = entries.toSet(),
)

private fun WsRefined.consume() = Refined(
    comment = comment?.let { Comment(it) },
    annotations = emptyList(),
    identifier = DefinitionIdentifier(identifier),
    reference = reference.consume() as? Reference.Primitive ?: error("Cannot refine non-primitive type"),
)

private fun WsType.consume() = Type(
    comment = comment?.let { Comment(it) },
    annotations = emptyList(),
    identifier = DefinitionIdentifier(identifier),
    shape = Type.Shape(shape.value.map { it.consume() }),
    extends = emptyList(),
)

private fun WsUnion.consume() = Union(
    comment = comment?.let { Comment(it) },
    annotations = emptyList(),
    identifier = DefinitionIdentifier(identifier),
    entries = entries.map { it.consume() }.toSet(),
)

private fun WsChannel.consume() = Channel(
    comment = comment?.let { Comment(it) },
    annotations = emptyList(),
    identifier = DefinitionIdentifier(identifier),
    reference = reference.consume(),
)

private fun WsRpc.consume() = Rpc(
    comment = comment?.let { Comment(it) },
    annotations = emptyList(),
    identifier = DefinitionIdentifier(identifier),
    shape = Type.Shape(shape.value.map { it.consume() }),
    result = result.consume(),
    error = error?.consume(),
)

private fun WsField.consume() = Field(
    identifier = identifier.consume(),
    annotations = emptyList(),
    reference = reference.consume(),
)

private fun WsRequest.consume() = Endpoint.Request(
    content = content?.consume(),
)

private fun WsResponse.consume() = Endpoint.Response(
    status = status,
    headers = headers.map { it.consume() },
    content = content?.consume(),
    annotations = emptyList(),
)

private fun WsContent.consume() = Endpoint.Content(
    type = type,
    reference = reference.consume(),
)

private fun WsReference.consume(): Reference = when (this) {
    is WsAny -> Reference.Any(
        isNullable = isNullable,
    )

    is WsUnit -> Reference.Unit(
        isNullable = isNullable,
    )

    is WsCustom -> Reference.Custom(
        value = value,
        isNullable = isNullable,
    )

    is WsPrimitive -> Reference.Primitive(
        type = consumeType(),
        isNullable = isNullable,
    )

    is WsDict -> Reference.Dict(
        reference = reference.consume(),
        isNullable = isNullable,
    )

    is WsIterable -> Reference.Iterable(
        reference = reference.consume(),
        isNullable = isNullable,
    )
}

private fun WsPrimitive.consumeType(): Reference.Primitive.Type {
    val regExp = (constraint as? WsRegExpConstraint)?.let { Constraint.RegExp(it.value) }
    val bound = (constraint as? WsBoundConstraint)?.let { Constraint.Bound(it.min, it.max) }
    return when (type) {
        WsPrimitiveType.String -> Reference.Primitive.Type.String(constraint = regExp)
        WsPrimitiveType.Integer -> Reference.Primitive.Type.Integer(precision = P64, constraint = bound)
        WsPrimitiveType.Integer32 -> Reference.Primitive.Type.Integer(precision = P32, constraint = bound)
        WsPrimitiveType.Number -> Reference.Primitive.Type.Number(precision = P64, constraint = bound)
        WsPrimitiveType.Number32 -> Reference.Primitive.Type.Number(precision = P32, constraint = bound)
        WsPrimitiveType.Boolean -> Reference.Primitive.Type.Boolean
        WsPrimitiveType.Bytes -> Reference.Primitive.Type.Bytes
    }
}

public fun AST.produce(): WsAST = WsAST(modules.map { it.produce() }.toTypedArray())

public fun Module.produce(): WsModule = WsModule(statements.map { it.produce() }.toTypedArray())

public fun Definition.produce(): WsDefinition = when (this) {
    is Type -> WsType(
        identifier = identifier.value,
        comment = comment?.value,
        shape = shape.produce(),
    )

    is Endpoint -> WsEndpoint(
        identifier = identifier.value,
        comment = comment?.value,
        method = method.produce(),
        path = path.produce(),
        queries = queries.produce(),
        headers = headers.produce(),
        requests = requests.produce(),
        responses = responses.produce(),
    )

    is Enum -> WsEnum(
        identifier = identifier.value,
        comment = comment?.value,
        entries = entries.toTypedArray(),
    )

    is Refined -> WsRefined(
        identifier = identifier.value,
        comment = comment?.value,
        reference = reference.produce(),
    )

    is Union -> WsUnion(
        identifier = identifier.value,
        comment = comment?.value,
        entries = entries
            .map { it.produce() }
            .toTypedArray(),
    )

    is Channel -> WsChannel(
        identifier = identifier.value,
        comment = comment?.value,
        reference = reference.produce(),
    )

    is Rpc -> WsRpc(
        identifier = identifier.value,
        comment = comment?.value,
        shape = shape.produce(),
        result = result.produce(),
        error = error?.produce(),
    )
}

private fun Type.Shape.produce() = WsShape(
    value.map { it.produce() }.toTypedArray(),
)

private fun List<Endpoint.Segment>.produce(): Array<WsSegment> = map {
    when (it) {
        is Endpoint.Segment.Literal -> WsLiteral(it.value)
        is Endpoint.Segment.Param -> WsParam(it.identifier.produce(), it.reference.produce())
    }
}.toTypedArray()

private fun Field.produce() = WsField(
    identifier = identifier.produce(),
    reference = reference.produce(),
)

private fun List<Field>.produce() = map { it.produce() }.toTypedArray()

private fun FieldIdentifier.produce() = WsFieldIdentifier(value)

private fun Reference.produce(): WsReference = when (this) {
    is Reference.Any -> WsAny(isNullable)
    is Reference.Unit -> WsUnit(isNullable)
    is Reference.Custom -> WsCustom(value, isNullable)
    is Reference.Primitive -> WsPrimitive(type.produce(), isNullable, type.produceConstraint())
    is Reference.Dict -> WsDict(reference.produce(), isNullable)
    is Reference.Iterable -> WsIterable(reference.produce(), isNullable)
}

private fun Reference.Primitive.Type.produce() = when (this) {
    is Reference.Primitive.Type.String -> WsPrimitiveType.String
    is Reference.Primitive.Type.Integer -> when (precision) {
        P32 -> WsPrimitiveType.Integer32
        P64 -> WsPrimitiveType.Integer
    }

    is Reference.Primitive.Type.Number -> when (precision) {
        P32 -> WsPrimitiveType.Number32
        P64 -> WsPrimitiveType.Number
    }

    is Reference.Primitive.Type.Boolean -> WsPrimitiveType.Boolean
    is Reference.Primitive.Type.Bytes -> WsPrimitiveType.Bytes
}

private fun Reference.Primitive.Type.produceConstraint(): WsConstraint? = when (this) {
    is Reference.Primitive.Type.String -> constraint?.let { WsRegExpConstraint(it.value) }
    is Reference.Primitive.Type.Integer -> constraint?.let { WsBoundConstraint(it.min, it.max) }
    is Reference.Primitive.Type.Number -> constraint?.let { WsBoundConstraint(it.min, it.max) }
    is Reference.Primitive.Type.Boolean -> null
    is Reference.Primitive.Type.Bytes -> null
}

private fun Endpoint.Method.produce() = when (this) {
    Endpoint.Method.GET -> WsMethod.GET
    Endpoint.Method.POST -> WsMethod.POST
    Endpoint.Method.PUT -> WsMethod.PUT
    Endpoint.Method.DELETE -> WsMethod.DELETE
    Endpoint.Method.OPTIONS -> WsMethod.OPTIONS
    Endpoint.Method.HEAD -> WsMethod.HEAD
    Endpoint.Method.PATCH -> WsMethod.PATCH
    Endpoint.Method.TRACE -> WsMethod.TRACE
}

private fun Endpoint.Content.produce() = WsContent(type, reference.produce())

private fun Endpoint.Request.produce() = WsRequest(content?.produce())

private fun List<Endpoint.Request>.produce() = map { it.produce() }.toTypedArray()

private fun Endpoint.Response.produce() = WsResponse(
    status = status,
    headers = headers.map { it.produce() }.toTypedArray(),
    content = content?.produce(),
)

private fun List<Endpoint.Response>.produce() = map { it.produce() }.toTypedArray()

@JsExport
public sealed interface WsNode

@JsExport
public class WsAST(
    public val modules: Array<WsModule>,
) : WsNode

@JsExport
public class WsModule(
    public val statements: Array<WsDefinition>,
) : WsNode

@JsExport
public sealed interface WsDefinition : WsNode {
    public val identifier: String
    public val comment: String?
}

@JsExport
public class WsType(
    override val identifier: String,
    override val comment: String?,
    public val shape: WsShape,
) : WsDefinition

@JsExport
public class WsShape(
    public val value: Array<WsField>,
)

@JsExport
public class WsEndpoint(
    override val identifier: String,
    override val comment: String?,
    public val method: WsMethod,
    public val path: Array<WsSegment>,
    public val queries: Array<WsField>,
    public val headers: Array<WsField>,
    public val requests: Array<WsRequest>,
    public val responses: Array<WsResponse>,
) : WsDefinition

@JsExport
public class WsEnum(
    override val identifier: String,
    override val comment: String?,
    public val entries: Array<String>,
) : WsDefinition

@JsExport
public class WsUnion(
    override val identifier: String,
    override val comment: String?,
    public val entries: Array<WsReference>,
) : WsDefinition

@JsExport
public class WsChannel(
    override val identifier: String,
    override val comment: String?,
    public val reference: WsReference,
) : WsDefinition

@JsExport
public class WsRpc(
    override val identifier: String,
    override val comment: String?,
    public val shape: WsShape,
    public val result: WsReference,
    public val error: WsReference?,
) : WsDefinition

@JsExport
public class WsRefined(
    override val identifier: String,
    override val comment: String?,
    public val reference: WsReference,
) : WsDefinition

@JsExport
public enum class WsMethod { GET, POST, PUT, DELETE, OPTIONS, HEAD, PATCH, TRACE }

@JsExport
public sealed interface WsSegment

@JsExport
public class WsLiteral(public val value: String) : WsSegment

@JsExport
public class WsParam(
    public val identifier: WsFieldIdentifier,
    public val reference: WsReference,
) : WsSegment

@JsExport
public class Shape(public val value: Array<WsField>)

@JsExport
public class WsField(public val identifier: WsFieldIdentifier, public val reference: WsReference)

@JsExport
public sealed interface WsIdentifier

@JsExport
public class WsClassIdentifier(public val value: String) : WsIdentifier

@JsExport
public class WsFieldIdentifier(public val value: String) : WsIdentifier

@JsExport
public sealed interface WsReference {
    public val isNullable: Boolean
}

@JsExport
public class WsAny(override val isNullable: Boolean) : WsReference

@JsExport
public class WsUnit(override val isNullable: Boolean) : WsReference

@JsExport
public class WsIterable(public val reference: WsReference, override val isNullable: Boolean) : WsReference

@JsExport
public class WsDict(public val reference: WsReference, override val isNullable: Boolean) : WsReference

@JsExport
public class WsCustom(
    public val value: String,
    override val isNullable: Boolean,
) : WsReference

@JsExport
public class WsPrimitive(
    public val type: WsPrimitiveType,
    override val isNullable: Boolean,
    public val constraint: WsConstraint? = null,
) : WsReference

@JsExport
public enum class WsPrimitiveType { String, Integer, Integer32, Number, Number32, Boolean, Bytes }

@JsExport
public sealed interface WsConstraint

@JsExport
public class WsRegExpConstraint(public val value: String) : WsConstraint

@JsExport
public class WsBoundConstraint(public val min: String?, public val max: String?) : WsConstraint

@JsExport
public class WsRequest(public val content: WsContent?)

@JsExport
public class WsResponse(public val status: String, public val headers: Array<WsField>, public val content: WsContent?)

@JsExport
public class WsContent(public val type: String, public val reference: WsReference, public val isNullable: Boolean = false)
