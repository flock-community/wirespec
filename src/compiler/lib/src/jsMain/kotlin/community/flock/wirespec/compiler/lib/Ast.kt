@file:OptIn(ExperimentalJsExport::class)

package community.flock.wirespec.compiler.lib

import arrow.core.toNonEmptyListOrNull
import community.flock.wirespec.compiler.core.parse.AST
import community.flock.wirespec.compiler.core.parse.Channel
import community.flock.wirespec.compiler.core.parse.Comment
import community.flock.wirespec.compiler.core.parse.Definition
import community.flock.wirespec.compiler.core.parse.DefinitionIdentifier
import community.flock.wirespec.compiler.core.parse.Endpoint
import community.flock.wirespec.compiler.core.parse.Enum
import community.flock.wirespec.compiler.core.parse.Field
import community.flock.wirespec.compiler.core.parse.FieldIdentifier
import community.flock.wirespec.compiler.core.parse.Module
import community.flock.wirespec.compiler.core.parse.Reference
import community.flock.wirespec.compiler.core.parse.Refined
import community.flock.wirespec.compiler.core.parse.Type
import community.flock.wirespec.compiler.core.parse.Union

fun WsAST.consume(): AST = AST(
    modules = modules.map { it.consume() }.toNonEmptyListOrNull()!!,
)

fun WsModule.consume(): Module = Module(
    uri = "",
    statements = statements.map { it.consume() }.toNonEmptyListOrNull()!!,
)

fun WsDefinition.consume(): Definition = when (this) {
    is WsEndpoint -> consume()
    is WsEnum -> consume()
    is WsRefined -> consume()
    is WsType -> consume()
    is WsUnion -> consume()
    is WsChannel -> consume()
}

fun WsEndpoint.consume(): Endpoint = Endpoint(
    comment = comment?.let { Comment(it) },
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

private fun WsClassIdentifier.consume() = DefinitionIdentifier(value)
private fun WsFieldIdentifier.consume() = FieldIdentifier(value)

private fun WsEnum.consume() = Enum(
    identifier = DefinitionIdentifier(identifier),
    comment = comment?.let { Comment(it) },
    entries = entries.toSet(),
)

private fun WsRefined.consume() = Refined(
    identifier = DefinitionIdentifier(identifier),
    comment = comment?.let { Comment(it) },
    reference = reference.consume() as? Reference.Primitive ?: error("Cannot refine non-primitive type"),
)

private fun WsType.consume() = Type(
    identifier = DefinitionIdentifier(identifier),
    comment = comment?.let { Comment(it) },
    shape = Type.Shape(shape.value.map { it.consume() }),
    extends = emptyList(),
)

private fun WsUnion.consume() = Union(
    identifier = DefinitionIdentifier(identifier),
    comment = comment?.let { Comment(it) },
    entries = entries.map { it.consume() }.toSet(),
)

private fun WsChannel.consume() = Channel(
    identifier = DefinitionIdentifier(identifier),
    comment = comment?.let { Comment(it) },
    reference = reference.consume(),
)

private fun WsField.consume() = Field(
    identifier = identifier.consume(),
    reference = reference.consume(),
)

private fun WsRequest.consume() = Endpoint.Request(
    content = content?.consume(),
)

private fun WsResponse.consume() = Endpoint.Response(
    status = status,
    headers = headers.map { it.consume() },
    content = content?.consume(),
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
        type = type.consume(),
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

private fun WsPrimitiveType.consume() = when (this) {
    WsPrimitiveType.String -> Reference.Primitive.Type.String()
    WsPrimitiveType.Integer -> Reference.Primitive.Type.Integer()
    WsPrimitiveType.Number -> Reference.Primitive.Type.Number()
    WsPrimitiveType.Boolean -> Reference.Primitive.Type.Boolean
    WsPrimitiveType.Bytes -> Reference.Primitive.Type.Bytes
}

fun AST.produce(): WsAST = WsAST(modules.map { it.produce() }.toTypedArray())

fun Module.produce(): WsModule = WsModule(statements.map { it.produce() }.toTypedArray())

fun Definition.produce(): WsDefinition = when (this) {
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

private fun DefinitionIdentifier.produce() = WsClassIdentifier(value)
private fun FieldIdentifier.produce() = WsFieldIdentifier(value)

private fun Reference.produce(): WsReference = when (this) {
    is Reference.Any -> WsAny(isNullable)
    is Reference.Unit -> WsUnit(isNullable)
    is Reference.Custom -> WsCustom(value, isNullable)
    is Reference.Primitive -> WsPrimitive(type.produce(), isNullable)
    is Reference.Dict -> WsDict(reference.produce(), isNullable)
    is Reference.Iterable -> WsIterable(reference.produce(), isNullable)
}

private fun Reference.Primitive.Type.produce() = when (this) {
    is Reference.Primitive.Type.String -> WsPrimitiveType.String
    is Reference.Primitive.Type.Integer -> WsPrimitiveType.Integer
    is Reference.Primitive.Type.Number -> WsPrimitiveType.Number
    is Reference.Primitive.Type.Boolean -> WsPrimitiveType.Boolean
    is Reference.Primitive.Type.Bytes -> WsPrimitiveType.Bytes
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
sealed interface WsNode

@JsExport
data class WsAST(
    val modules: Array<WsModule>,
) : WsNode

@JsExport
data class WsModule(
    val statements: Array<WsDefinition>,
) : WsNode

@JsExport
sealed interface WsDefinition : WsNode {
    val identifier: String
    val comment: String?
}

@JsExport
data class WsType(
    override val identifier: String,
    override val comment: String?,
    val shape: WsShape,
) : WsDefinition

@JsExport
data class WsShape(val value: Array<WsField>)

@JsExport
data class WsEndpoint(
    override val identifier: String,
    override val comment: String?,
    val method: WsMethod,
    val path: Array<WsSegment>,
    val queries: Array<WsField>,
    val headers: Array<WsField>,
    val requests: Array<WsRequest>,
    val responses: Array<WsResponse>,
) : WsDefinition

@JsExport
data class WsEnum(
    override val identifier: String,
    override val comment: String?,
    val entries: Array<String>,
) : WsDefinition

@JsExport
data class WsUnion(
    override val identifier: String,
    override val comment: String?,
    val entries: Array<WsReference>,
) : WsDefinition

@JsExport
data class WsChannel(
    override val identifier: String,
    override val comment: String?,
    val reference: WsReference,
) : WsDefinition

@JsExport
data class WsRefined(
    override val identifier: String,
    override val comment: String?,
    val reference: WsReference,
) : WsDefinition

@JsExport
enum class WsMethod { GET, POST, PUT, DELETE, OPTIONS, HEAD, PATCH, TRACE }

@JsExport
sealed interface WsSegment

@JsExport
data class WsLiteral(val value: String) : WsSegment

@JsExport
data class WsParam(
    val identifier: WsFieldIdentifier,
    val reference: WsReference,
) : WsSegment

@JsExport
data class Shape(val value: Array<WsField>)

@JsExport
data class WsField(val identifier: WsFieldIdentifier, val reference: WsReference)

@JsExport
sealed interface WsIdentifier

@JsExport
data class WsClassIdentifier(val value: String) : WsIdentifier

@JsExport
data class WsFieldIdentifier(val value: String) : WsIdentifier

@JsExport
sealed interface WsReference {
    val isNullable: Boolean
}

@JsExport
data class WsAny(override val isNullable: Boolean) : WsReference

@JsExport
data class WsUnit(override val isNullable: Boolean) : WsReference

@JsExport
data class WsIterable(val reference: WsReference, override val isNullable: Boolean) : WsReference

@JsExport
data class WsDict(val reference: WsReference, override val isNullable: Boolean) : WsReference

@JsExport
data class WsCustom(
    val value: String,
    override val isNullable: Boolean,
) : WsReference

@JsExport
data class WsPrimitive(
    val type: WsPrimitiveType,
    override val isNullable: Boolean,
) : WsReference

@JsExport
enum class WsPrimitiveType { String, Integer, Number, Boolean, Bytes }

@JsExport
data class WsRequest(val content: WsContent?)

@JsExport
data class WsResponse(val status: String, val headers: Array<WsField>, val content: WsContent?)

@JsExport
data class WsContent(val type: String, val reference: WsReference, val isNullable: Boolean = false)
