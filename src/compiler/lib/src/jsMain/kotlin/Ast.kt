import community.flock.wirespec.compiler.core.parse.nodes.Endpoint
import community.flock.wirespec.compiler.core.parse.nodes.Enum
import community.flock.wirespec.compiler.core.parse.nodes.Node
import community.flock.wirespec.compiler.core.parse.nodes.Refined
import community.flock.wirespec.compiler.core.parse.nodes.Type
import community.flock.wirespec.compiler.core.tokenize.types.WsUnit

@ExperimentalJsExport
internal fun List<Node>.produce(): Array<WsNode> = map {
    when (it) {
        is Type -> WsType(it.name, it.shape.produce())
        is Endpoint -> WsEndpoint(
            name = it.name,
             method = it.method.produce(),
             path= it.path.produce(),
             query= it.query.produce(),
             headers=  it.headers.produce(),
             cookies=  it.cookies.produce(),
             requests=  it.requests.produce(),
             responses=  it.responses.produce(),

        )
        is Enum -> WsEnum(it.name, it.entries.toTypedArray())
        is Refined -> WsRefined(it.name, it.validator.value)
    }
}.toTypedArray()

private fun Type.Shape.produce() = WsShape(
    value.map { it.produce() }.toTypedArray()
)

@ExperimentalJsExport
private fun List<Endpoint.Segment>.produce(): Array<WsSegment> = map{when(it){
    is Endpoint.Segment.Literal -> WsLiteral(it.value)
    is Endpoint.Segment.Param -> WsParam(it.identifier.produce(), it.reference.produce())
}}.toTypedArray()

@ExperimentalJsExport
private fun Type.Shape.Field.produce() = WsField(identifier.produce(), reference.produce(), isNullable)

@ExperimentalJsExport
private fun List<Type.Shape.Field>.produce() = map{it.produce()}.toTypedArray()

@ExperimentalJsExport
private fun Type.Shape.Field.Identifier.produce() = WsIdentifier(this.value)

@ExperimentalJsExport
private fun Type.Shape.Field.Reference.produce() = when(this){
    is Type.Shape.Field.Reference.Any -> WsAny(isIterable, isMap)
    is Type.Shape.Field.Reference.Unit -> WsUnit(isIterable, isMap)
    is Type.Shape.Field.Reference.Custom -> WsCustom(value, isIterable, isMap)
    is Type.Shape.Field.Reference.Primitive -> WsPrimitive(type.produce(), isIterable, isMap)
}

@ExperimentalJsExport
private fun Type.Shape.Field.Reference.Primitive.Type.produce() = when(this){
    Type.Shape.Field.Reference.Primitive.Type.String -> WsPrimitiveType.String
    Type.Shape.Field.Reference.Primitive.Type.Integer -> WsPrimitiveType.Integer
    Type.Shape.Field.Reference.Primitive.Type.Number -> WsPrimitiveType.Number
    Type.Shape.Field.Reference.Primitive.Type.Boolean -> WsPrimitiveType.Boolean
}

@ExperimentalJsExport
private fun Endpoint.Method.produce() = when (this){
    Endpoint.Method.GET -> WsMethod.GET
    Endpoint.Method.POST -> WsMethod.POST
    Endpoint.Method.PUT -> WsMethod.PUT
    Endpoint.Method.DELETE -> WsMethod.DELETE
    Endpoint.Method.OPTIONS -> WsMethod.OPTIONS
    Endpoint.Method.HEAD -> WsMethod.HEAD
    Endpoint.Method.PATCH -> WsMethod.PATCH
    Endpoint.Method.TRACE -> WsMethod.TRACE
}

@ExperimentalJsExport
private fun Endpoint.Content.produce() = WsContent(type, reference.produce(), isNullable)

@ExperimentalJsExport
private fun Endpoint.Request.produce() = WsRequest(content?.produce())

@ExperimentalJsExport
private fun List<Endpoint.Request>.produce() = map{it.produce()}.toTypedArray()


@ExperimentalJsExport
private fun Endpoint.Response.produce() = WsResponse(status, content?.produce())

@ExperimentalJsExport
private fun List<Endpoint.Response>.produce() = map{it.produce()}.toTypedArray()
@JsExport
@ExperimentalJsExport
sealed interface WsNode

@JsExport
@ExperimentalJsExport
data class WsType(val name: String, val shape: WsShape) : WsNode

@JsExport
@ExperimentalJsExport
data class WsShape(val value: Array<WsField>)


@JsExport
@ExperimentalJsExport
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
@ExperimentalJsExport
data class WsEnum(val name: String, val entries: Array<String>) : WsNode

@JsExport
@ExperimentalJsExport
data class WsRefined(val name: String, val validator: String) : WsNode

@JsExport
@ExperimentalJsExport
enum class WsMethod { GET, POST, PUT, DELETE, OPTIONS, HEAD, PATCH, TRACE }

@JsExport
@ExperimentalJsExport
sealed interface WsSegment

@JsExport
@ExperimentalJsExport
data class WsLiteral(val value: String) : WsSegment
@JsExport
@ExperimentalJsExport
data class WsParam(
    val identifier: WsIdentifier,
    val reference: WsReference
) : WsSegment


@JsExport
@ExperimentalJsExport
data class Shape(val value: Array<WsField>)

@JsExport
@ExperimentalJsExport
data class WsField(val identifier: WsIdentifier, val reference: WsReference, val isNullable: Boolean) {

}

@JsExport
@ExperimentalJsExport
data class WsIdentifier(val value: String)

@JsExport
@ExperimentalJsExport
sealed interface WsReference {
    val isIterable: Boolean
    val isMap: Boolean


}

@JsExport
@ExperimentalJsExport
data class WsAny(override val isIterable: Boolean, override val isMap: Boolean = false) : WsReference
@JsExport
@ExperimentalJsExport
data class WsUnit(override val isIterable: Boolean, override val isMap: Boolean = false) : WsReference
@JsExport
@ExperimentalJsExport
data class WsCustom(
    val value: String,
    override val isIterable: Boolean,
    override val isMap: Boolean = false
) : WsReference

@JsExport
@ExperimentalJsExport
data class WsPrimitive(
    val type: WsPrimitiveType,
    override val isIterable: Boolean,
    override val isMap: Boolean = false
) : WsReference

@JsExport
@ExperimentalJsExport
enum class WsPrimitiveType { String, Integer, Number, Boolean }

@JsExport
@ExperimentalJsExport
data class WsRequest(val content: WsContent?)

@JsExport
@ExperimentalJsExport
data class WsResponse(val status: String, val content: WsContent?)

@JsExport
@ExperimentalJsExport
data class WsContent(val type: String, val reference: WsReference, val isNullable: Boolean = false)
