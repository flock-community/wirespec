package community.flock.wirespec.openapi.v2

import community.flock.kotlinx.openapi.bindings.v2.HeaderObject
import community.flock.kotlinx.openapi.bindings.v2.InfoObject
import community.flock.kotlinx.openapi.bindings.v2.OperationObject
import community.flock.kotlinx.openapi.bindings.v2.ParameterLocation
import community.flock.kotlinx.openapi.bindings.v2.ParameterObject
import community.flock.kotlinx.openapi.bindings.v2.Path
import community.flock.kotlinx.openapi.bindings.v2.PathItemObject
import community.flock.kotlinx.openapi.bindings.v2.Ref
import community.flock.kotlinx.openapi.bindings.v2.ReferenceObject
import community.flock.kotlinx.openapi.bindings.v2.ResponseObject
import community.flock.kotlinx.openapi.bindings.v2.SchemaObject
import community.flock.kotlinx.openapi.bindings.v2.SchemaOrReferenceObject
import community.flock.kotlinx.openapi.bindings.v2.StatusCode
import community.flock.kotlinx.openapi.bindings.v2.SwaggerObject
import community.flock.wirespec.compiler.core.emit.common.Emitted
import community.flock.wirespec.compiler.core.emit.common.Emitter
import community.flock.wirespec.compiler.core.parse.AST
import community.flock.wirespec.compiler.core.parse.Channel
import community.flock.wirespec.compiler.core.parse.Endpoint
import community.flock.wirespec.compiler.core.parse.Enum
import community.flock.wirespec.compiler.core.parse.Field
import community.flock.wirespec.compiler.core.parse.Identifier
import community.flock.wirespec.compiler.core.parse.Reference
import community.flock.wirespec.compiler.core.parse.Refined
import community.flock.wirespec.compiler.core.parse.Type
import community.flock.wirespec.compiler.core.parse.Union
import community.flock.wirespec.compiler.utils.noLogger
import community.flock.wirespec.openapi.Common.json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonPrimitive
import community.flock.kotlinx.openapi.bindings.v2.Type as OpenApiType

object OpenApiV2Emitter: Emitter(noLogger) {

    override val singleLineComment = ""

    override fun emit(ast: AST): List<Emitted> =
        listOf(Emitted("SwaggerObject", json.encodeToString(emitSwaggerObject(ast))))

    override fun emit(type: Type, ast: AST) = notYetImplemented()

    override fun emit(enum: Enum) = notYetImplemented()

    override fun emit(refined: Refined) = notYetImplemented()

    override fun emit(endpoint: Endpoint) = notYetImplemented()

    override fun emit(union: Union) = notYetImplemented()

    override fun emit(identifier: Identifier) = notYetImplemented()

    override fun emit(channel: Channel) = notYetImplemented()

    fun emitSwaggerObject(ast: AST): SwaggerObject =
        SwaggerObject(
            swagger = "2.0",
            info = InfoObject(
                title = "Wirespec",
                version = "0.0.0"
            ),
            consumes = listOf("application/json"),
            produces = listOf("application/json"),
            paths = ast.filterIsInstance<Endpoint>().groupBy { it.path }.map { (segments, endpoints) ->
                Path(segments.emitSegment()) to PathItemObject(
                    parameters = segments.filterIsInstance<Endpoint.Segment.Param>().map {
                        ParameterObject(
                            `in` = ParameterLocation.PATH,
                            name = it.identifier.value,
                            type = it.reference.emitType(),
                            format = it.reference.emitFormat(),
                        )
                    },
                    get = endpoints.emit(Endpoint.Method.GET),
                    post = endpoints.emit(Endpoint.Method.POST),
                    put = endpoints.emit(Endpoint.Method.PUT),
                    delete = endpoints.emit(Endpoint.Method.DELETE),
                    patch = endpoints.emit(Endpoint.Method.PATCH),
                    options = endpoints.emit(Endpoint.Method.OPTIONS),
                    trace = endpoints.emit(Endpoint.Method.TRACE),
                    head = endpoints.emit(Endpoint.Method.HEAD),
                )
            }.toMap(),
            definitions = ast
                .filterIsInstance<Refined>().associate { type ->
                    type.identifier.value to SchemaObject(
                        type = OpenApiType.STRING,
                        pattern = type.validator.value
                    )
                } + ast
                .filterIsInstance<Type>().associate { type ->
                    type.identifier.value to SchemaObject(
                        properties = type.shape.value.associate { it.emit() },
                        required = type.shape.value
                            .filter { !it.isNullable }
                            .map { it.identifier.value }
                            .takeIf { it.isNotEmpty() }
                    )
                } + ast
                .filterIsInstance<Enum>().associate { enum ->
                    enum.identifier.value to SchemaObject(
                        type = OpenApiType.STRING,
                        enum = enum.entries.map { JsonPrimitive(it) }
                    )
                }
        )

    private fun List<Endpoint>.emit(method: Endpoint.Method): OperationObject? =
        filter { it.method == method }.map { it.emit() }.firstOrNull()

    private fun Endpoint.emit() = OperationObject(
        operationId = identifier.value,
        description = comment?.value,
        consumes = requests.mapNotNull { it.content?.type }.distinct().ifEmpty { null },
        produces = responses.mapNotNull { it.content?.type }.distinct().ifEmpty { null },
        parameters = requests
            .mapNotNull { it.content }
            .take(1)
            .map {
                ParameterObject(
                    `in` = ParameterLocation.BODY,
                    name = "RequestBody",
                    schema = it.reference.emit(),
                    required = !it.isNullable,
                )
            } + queries.map { it.emitParameter(ParameterLocation.QUERY) } + headers.map {
            it.emitParameter(
                ParameterLocation.HEADER
            )
        },
        responses = responses
            .associate { response ->
                StatusCode(response.status) to ResponseObject(
                    description = comment?.value ?: "${identifier.value} ${response.status} response",
                    headers = response.headers.associate {
                        it.identifier.value to HeaderObject(
                            type = it.reference.emitType().name,
                            format = it.reference.emitFormat(),
                            items = if (it.reference.isIterable) it.reference.emit() else null
                        )
                    },
                    schema = response.content
                        ?.takeIf { content -> content.reference !is Reference.Unit }
                        ?.let { content ->
                            when (content.reference.isIterable) {
                                false -> content.reference.emit()
                                true -> SchemaObject(
                                    type = OpenApiType.ARRAY,
                                    items = content.reference.emit()
                                )
                            }
                        }
                )
            }
    )

    private fun List<Endpoint.Segment>.emitSegment() = "/" + joinToString("/") {
        when (it) {
            is Endpoint.Segment.Param -> "{${it.identifier.value}}"
            is Endpoint.Segment.Literal -> it.value
        }
    }

    fun Field.emit(): Pair<String, SchemaOrReferenceObject> = identifier.value to reference.emit()

    private fun Field.emitParameter(location: ParameterLocation) =
        ParameterObject(
            `in` = location,
            name = identifier.value,
            type = reference.emitType(),
            format = reference.emitFormat(),
            items = when (reference.isIterable) {
                true -> when (val emit = reference.emit()) {
                    is ReferenceObject -> emit
                    is SchemaObject -> emit.items
                }

                false -> null
            },
            required = !isNullable

        )

    fun Reference.emit(): SchemaOrReferenceObject = when (this) {
        is Reference.Custom -> ReferenceObject(ref = Ref("#/definitions/${value}"))
        is Reference.Primitive -> SchemaObject(type = type.emitType(), format = type.subType)
        is Reference.Any -> error("Cannot map Any")
        is Reference.Unit -> error("Cannot map Unit")
    }.let { if (isIterable) SchemaObject(type = OpenApiType.ARRAY, items = it) else it }


    private fun Reference.Primitive.Type.emitType(): OpenApiType = when (this) {
        is Reference.Primitive.Type.String -> OpenApiType.STRING
        is Reference.Primitive.Type.Integer -> OpenApiType.INTEGER
        is Reference.Primitive.Type.Number -> OpenApiType.NUMBER
        is Reference.Primitive.Type.Boolean -> OpenApiType.BOOLEAN
    }

    private fun Reference.emitType() =
        if (isIterable) OpenApiType.ARRAY else when (this) {
            is Reference.Primitive -> type.emitType()
            is Reference.Custom -> OpenApiType.OBJECT
            is Reference.Any -> OpenApiType.OBJECT
            is Reference.Unit -> OpenApiType.OBJECT
        }

    private fun Reference.emitFormat() =
        when (this) {
            is Reference.Primitive -> this.type.subType
            else -> null
        }
}
