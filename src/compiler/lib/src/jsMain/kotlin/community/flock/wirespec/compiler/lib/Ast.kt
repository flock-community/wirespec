@file:OptIn(ExperimentalJsExport::class)

package community.flock.wirespec.compiler.lib

import community.flock.wirespec.compiler.core.parse.Channel
import community.flock.wirespec.compiler.core.parse.Comment
import community.flock.wirespec.compiler.core.parse.DefinitionIdentifier
import community.flock.wirespec.compiler.core.parse.Endpoint
import community.flock.wirespec.compiler.core.parse.Enum
import community.flock.wirespec.compiler.core.parse.Field
import community.flock.wirespec.compiler.core.parse.FieldIdentifier
import community.flock.wirespec.compiler.core.parse.Node
import community.flock.wirespec.compiler.core.parse.Reference
import community.flock.wirespec.compiler.core.parse.Refined
import community.flock.wirespec.compiler.core.parse.Type
import community.flock.wirespec.compiler.core.parse.Union

fun WsNode.consume(): Node =
    when (this) {
        is WsEndpoint -> consume()
        is WsEnum -> consume()
        is WsRefined -> consume()
        is WsType -> consume()
        is WsUnion -> consume()
        is WsChannel -> consume()
    }

fun WsEndpoint.consume(): Endpoint =
    Endpoint(
        comment = comment?.let { Comment(it) },
        identifier = DefinitionIdentifier(identifier),
        method = method.consume(),
        path = path.map { it.consume() },
        queries = query.map { it.consume() },
        headers = query.map { it.consume() },
        cookies = query.map { it.consume() },
        requests = requests.map { it.consume() },
        responses = responses.map { it.consume() },
    )

private fun WsSegment.consume() =
    when (this) {
        is WsLiteral -> Endpoint.Segment.Literal(value)
        is WsParam -> Endpoint.Segment.Param(
            identifier = identifier.consume(),
            reference = reference.consume()
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
    entries = entries.toSet()
)

private fun WsRefined.consume() = Refined(
    identifier = DefinitionIdentifier(identifier),
    comment = comment?.let { Comment(it) },
    validator = Refined.Validator(validator)
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
    entries = entries.map { it.consume() }.toSet()
)

private fun WsChannel.consume() = Channel(
    identifier = DefinitionIdentifier(identifier),
    comment = comment?.let { Comment(it) },
    reference = reference.consume(),
    isNullable = isNullable
)


private fun WsField.consume() = Field(
    identifier = identifier.consume(),
    reference = reference.consume(),
    isNullable = isNullable
)

private fun WsRequest.consume() =
    Endpoint.Request(
        content = content?.consume()
    )

private fun WsResponse.consume() =
    Endpoint.Response(
        status = status,
        headers = headers.map { it.consume() },
        content = content?.consume()
    )

private fun WsContent.consume() =
    Endpoint.Content(
        type = type,
        reference = reference.consume(),
        isNullable = isNullable
    )

private fun WsReference.consume() =
    when (this) {
        is WsAny -> Reference.Any(
            isIterable = isIterable,
            isDictionary = isMap
        )

        is WsUnit -> Reference.Unit(
            isIterable = isIterable,
            isDictionary = isMap
        )

        is WsCustom -> Reference.Custom(
            value = value,
            isIterable = isIterable,
            isDictionary = isMap
        )

        is WsPrimitive -> Reference.Primitive(
            type = type.consume(),
            isIterable = isIterable,
            isDictionary = isMap
        )
    }

private fun WsPrimitiveType.consume() =
    when (this) {
        WsPrimitiveType.String -> Reference.Primitive.Type.String()
        WsPrimitiveType.Integer -> Reference.Primitive.Type.Integer()
        WsPrimitiveType.Number -> Reference.Primitive.Type.Number()
        WsPrimitiveType.Boolean -> Reference.Primitive.Type.Boolean()
    }


fun Node.produce(): WsNode =
    when (this) {
        is Type -> WsType(
            identifier = identifier.value,
            comment = comment?.value,
            shape = shape.produce()
        )

        is Endpoint -> WsEndpoint(
            identifier = identifier.value,
            comment = comment?.value,
            method = method.produce(),
            path = path.produce(),
            query = queries.produce(),
            headers = headers.produce(),
            cookies = cookies.produce(),
            requests = requests.produce(),
            responses = responses.produce(),
        )

        is Enum -> WsEnum(
            identifier = identifier.value,
            comment = comment?.value,
            entries = entries.toTypedArray()
        )

        is Refined -> WsRefined(
            identifier = identifier.value,
            comment = comment?.value,
            validator = validator.value
        )

        is Union -> WsUnion(
            identifier = identifier.value,
            comment = comment?.value,
            entries = entries
                .map { it.produce() }
                .toTypedArray())

        is Channel -> WsChannel(
            identifier = identifier.value,
            comment = comment?.value,
            reference = reference.produce(),
            isNullable = isNullable
        )
    }

fun List<Node>.produce(): Array<WsNode> =
    map { it.produce() }.toTypedArray()

private fun Type.Shape.produce() = WsShape(
    value.map { it.produce() }.toTypedArray()
)

private fun List<Endpoint.Segment>.produce(): Array<WsSegment> = map {
    when (it) {
        is Endpoint.Segment.Literal -> WsLiteral(it.value)
        is Endpoint.Segment.Param -> WsParam(it.identifier.produce(), it.reference.produce())
    }
}.toTypedArray()

private fun Field.produce() = WsField(identifier.produce(), reference.produce(), isNullable)

private fun List<Field>.produce() = map { it.produce() }.toTypedArray()

private fun DefinitionIdentifier.produce() = WsClassIdentifier(value)
private fun FieldIdentifier.produce() = WsFieldIdentifier(value)

private fun Reference.produce() = when (this) {
    is Reference.Any -> WsAny(isIterable, isDictionary)
    is Reference.Unit -> WsUnit(isIterable, isDictionary)
    is Reference.Custom -> WsCustom(value, isIterable, isDictionary)
    is Reference.Primitive -> WsPrimitive(type.produce(), isIterable, isDictionary)
}

private fun Reference.Primitive.Type.produce() = when (this) {
    is Reference.Primitive.Type.String -> WsPrimitiveType.String
    is Reference.Primitive.Type.Integer -> WsPrimitiveType.Integer
    is Reference.Primitive.Type.Number -> WsPrimitiveType.Number
    is Reference.Primitive.Type.Boolean -> WsPrimitiveType.Boolean
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

private fun Endpoint.Content.produce() = WsContent(type, reference.produce(), isNullable)

private fun Endpoint.Request.produce() = WsRequest(content?.produce())

private fun List<Endpoint.Request>.produce() = map { it.produce() }.toTypedArray()

private fun Endpoint.Response.produce() = WsResponse(
    status = status,
    headers = headers.map { it.produce() }.toTypedArray(),
    content = content?.produce()
)

private fun List<Endpoint.Response>.produce() = map { it.produce() }.toTypedArray()

@JsExport
sealed interface WsNode {
    val identifier: String
}

@JsExport
data class WsType(
    override val identifier: String,
    val comment: String?,
    val shape: WsShape
) : WsNode

@JsExport
data class WsShape(val value: Array<WsField>)

@JsExport
data class WsEndpoint(
    override val identifier: String,
    val comment: String?,
    val method: WsMethod,
    val path: Array<WsSegment>,
    val query: Array<WsField>,
    val headers: Array<WsField>,
    val cookies: Array<WsField>,
    val requests: Array<WsRequest>,
    val responses: Array<WsResponse>
) : WsNode

@JsExport
data class WsEnum(
    override val identifier: String,
    val comment: String?,
    val entries: Array<String>
) : WsNode

@JsExport
data class WsUnion(
    override val identifier: String,
    val comment: String?,
    val entries: Array<WsReference>
) : WsNode

@JsExport
data class WsChannel(
    override val identifier: String,
    val comment: String?,
    val reference: WsReference,
    val isNullable: Boolean
) : WsNode

@JsExport
data class WsRefined(
    override val identifier: String,
    val comment: String?,
    val validator: String
) : WsNode

@JsExport
enum class WsMethod { GET, POST, PUT, DELETE, OPTIONS, HEAD, PATCH, TRACE }

@JsExport
sealed interface WsSegment

@JsExport
data class WsLiteral(val value: String) : WsSegment

@JsExport
data class WsParam(
    val identifier: WsFieldIdentifier,
    val reference: WsReference
) : WsSegment


@JsExport
data class Shape(val value: Array<WsField>)

@JsExport
data class WsField(val identifier: WsFieldIdentifier, val reference: WsReference, val isNullable: Boolean)

@JsExport
sealed interface WsIdentifier

@JsExport
data class WsClassIdentifier(val value: String) : WsIdentifier

@JsExport
data class WsFieldIdentifier(val value: String) : WsIdentifier

@JsExport
sealed interface WsReference {
    val isIterable: Boolean
    val isMap: Boolean
}

@JsExport
data class WsAny(override val isIterable: Boolean, override val isMap: Boolean = false) : WsReference

@JsExport
data class WsUnit(override val isIterable: Boolean, override val isMap: Boolean = false) : WsReference

@JsExport
data class WsCustom(
    val value: String,
    override val isIterable: Boolean,
    override val isMap: Boolean = false
) : WsReference

@JsExport
data class WsPrimitive(
    val type: WsPrimitiveType,
    override val isIterable: Boolean,
    override val isMap: Boolean = false
) : WsReference

@JsExport
enum class WsPrimitiveType { String, Integer, Number, Boolean }

@JsExport
data class WsRequest(val content: WsContent?)

@JsExport
data class WsResponse(val status: String, val headers: Array<WsField>, val content: WsContent?)

@JsExport
data class WsContent(val type: String, val reference: WsReference, val isNullable: Boolean = false)
