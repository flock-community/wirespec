@file:OptIn(ExperimentalJsExport::class)

package community.flock.wirespec.compiler.lib

import community.flock.wirespec.compiler.core.parse.nodes.Definition
import community.flock.wirespec.compiler.core.parse.nodes.Endpoint
import community.flock.wirespec.compiler.core.parse.nodes.Enum
import community.flock.wirespec.compiler.core.parse.nodes.Refined
import community.flock.wirespec.compiler.core.parse.nodes.Type

fun Definition.produce(): WsNode =
    when (this) {
        is Type -> WsType(name, shape.produce())
        is Endpoint -> WsEndpoint(
            name = name,
            method = method.produce(),
            path = path.produce(),
            query = query.produce(),
            headers = headers.produce(),
            cookies = cookies.produce(),
            requests = requests.produce(),
            responses = responses.produce(),
        )
        is Enum -> WsEnum(name, entries.toTypedArray())
        is Refined -> WsRefined(name, validator.value)
    }

fun List<Definition>.produce(): Array<WsNode> =
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

private fun Type.Shape.Field.produce() = WsField(identifier.produce(), reference.produce(), isNullable)

private fun List<Type.Shape.Field>.produce() = map { it.produce() }.toTypedArray()

private fun Type.Shape.Field.Identifier.produce() = WsIdentifier(this.value)

private fun Type.Shape.Field.Reference.produce() = when (this) {
    is Type.Shape.Field.Reference.Any -> WsAny(isIterable, isMap)
    is Type.Shape.Field.Reference.Unit -> WsUnit(isIterable, isMap)
    is Type.Shape.Field.Reference.Custom -> WsCustom(value, isIterable, isMap)
    is Type.Shape.Field.Reference.Primitive -> WsPrimitive(type.produce(), isIterable, isMap)
}

private fun Type.Shape.Field.Reference.Primitive.Type.produce() = when (this) {
    Type.Shape.Field.Reference.Primitive.Type.String -> WsPrimitiveType.String
    Type.Shape.Field.Reference.Primitive.Type.Integer -> WsPrimitiveType.Integer
    Type.Shape.Field.Reference.Primitive.Type.Number -> WsPrimitiveType.Number
    Type.Shape.Field.Reference.Primitive.Type.Boolean -> WsPrimitiveType.Boolean
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

private fun Endpoint.Response.produce() = WsResponse(status, content?.produce())

private fun List<Endpoint.Response>.produce() = map { it.produce() }.toTypedArray()

@JsExport
sealed interface WsNode

@JsExport
data class WsType(val name: String, val shape: WsShape) : WsNode

@JsExport
data class WsShape(val value: Array<WsField>)

@JsExport
data class WsEndpoint(
    val name: String,
    val method: WsMethod,
    val path: Array<WsSegment>,
    val query: Array<WsField>,
    val headers: Array<WsField>,
    val cookies: Array<WsField>,
    val requests: Array<WsRequest>,
    val responses: Array<WsResponse>
) : WsNode

@JsExport
data class WsEnum(val name: String, val entries: Array<String>) : WsNode

@JsExport
data class WsRefined(val name: String, val validator: String) : WsNode

@JsExport
enum class WsMethod { GET, POST, PUT, DELETE, OPTIONS, HEAD, PATCH, TRACE }

@JsExport
sealed interface WsSegment

@JsExport
data class WsLiteral(val value: String) : WsSegment

@JsExport
data class WsParam(
    val identifier: WsIdentifier,
    val reference: WsReference
) : WsSegment


@JsExport
data class Shape(val value: Array<WsField>)

@JsExport
data class WsField(val identifier: WsIdentifier, val reference: WsReference, val isNullable: Boolean)

@JsExport
data class WsIdentifier(val value: String)

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
data class WsResponse(val status: String, val content: WsContent?)

@JsExport
data class WsContent(val type: String, val reference: WsReference, val isNullable: Boolean = false)
