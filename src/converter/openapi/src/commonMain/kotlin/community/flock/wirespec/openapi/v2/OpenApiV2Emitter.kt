package community.flock.wirespec.openapi.v2

import community.flock.kotlinx.openapi.bindings.v2.InfoObject
import community.flock.kotlinx.openapi.bindings.v2.OpenAPI
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
import community.flock.kotlinx.openapi.bindings.v2.Type as OpenApiType

import community.flock.wirespec.compiler.core.emit.common.Emitted
import community.flock.wirespec.compiler.core.parse.AST
import community.flock.wirespec.compiler.core.parse.Definition
import community.flock.wirespec.compiler.core.parse.Endpoint
import community.flock.wirespec.compiler.core.parse.Refined
import community.flock.wirespec.compiler.core.parse.Type
import community.flock.wirespec.compiler.core.parse.Enum
import kotlinx.serialization.json.JsonPrimitive

class OpenApiV2Emitter {

     fun emit(ast: AST): SwaggerObject {
        return SwaggerObject(
            swagger = "2.0",
            info = InfoObject(
                title = "Wirespec",
                version = "0.0.0"
            ),
            consumes = listOf("application/json"),
            produces = listOf("application/json"),
            paths = ast.filterIsInstance<Endpoint>().groupBy { it.path }.map {
                Path(it.key.emitSegment()) to PathItemObject(
                    parameters = it.key.filterIsInstance<Endpoint.Segment.Param>().map {
                        ParameterObject(
                            `in` = ParameterLocation.PATH,
                            name = it.identifier.value,
                            type = OpenApiType.STRING
                        )
                    },
                    get = it.value.emit(Endpoint.Method.GET),
                    post = it.value.emit(Endpoint.Method.POST),
                    put = it.value.emit(Endpoint.Method.PUT),
                    delete = it.value.emit(Endpoint.Method.DELETE),
                    patch = it.value.emit(Endpoint.Method.PATCH),
                    options = it.value.emit(Endpoint.Method.OPTIONS),
                    trace = it.value.emit(Endpoint.Method.TRACE),
                    head = it.value.emit(Endpoint.Method.HEAD),
                )
            }.toMap(),
            definitions = ast
                .filterIsInstance<Refined>().associate { type ->
                    type.name to SchemaObject(
                        type = OpenApiType.STRING,
                        pattern = type.validator.value
                    )
                } + ast
                .filterIsInstance<Type>().associate { type ->
                    type.name to SchemaObject(
                        properties = type.shape.value.associate { it.emit() },
                        required = type.shape.value
                            .filter { !it.isNullable }
                            .map { it.identifier.value }
                            .takeIf { it.isNotEmpty() }
                    )
                } + ast
                .filterIsInstance<Enum>().associate { enum ->
                    enum.name to SchemaObject(
                        type = OpenApiType.STRING,
                        enum = enum.entries.map { JsonPrimitive(it) }
                    )
                }
        )
    }

    private fun List<Endpoint>.emit(method: Endpoint.Method): OperationObject? =
        filter { it.method == method }.map { it.emit() }.firstOrNull()

    private fun Endpoint.emit(): OperationObject = OperationObject(
        operationId = this.name,
        parameters = this.requests
            .mapNotNull { it.content }
            .take(1)
            .map {
                ParameterObject(
                    `in` = ParameterLocation.BODY,
                    name = "RequestBody",
                    schema = it.reference.emit()
                )
            } + query.map {
            ParameterObject(
                `in` = ParameterLocation.QUERY,
                name = it.identifier.value,
                type = it.reference.emitType()
            )

        }+headers.map {
            ParameterObject(
                `in` = ParameterLocation.HEADER,
                name = it.identifier.value,
                type = it.reference.emitType()
            )

        },
        responses = responses
            .associate {
                StatusCode(it.status) to ResponseObject(
                    description = "${this.name} ${it.status} response",
                    schema = it.content
                        ?.takeIf { content -> content.reference !is Type.Shape.Field.Reference.Unit }
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

    fun Type.Shape.Field.emit(): Pair<String, SchemaOrReferenceObject> = identifier.value to reference.emit()

    fun Type.Shape.Field.Reference.emit(): SchemaOrReferenceObject = when (this) {
        is Type.Shape.Field.Reference.Custom -> ReferenceObject(ref = Ref("#/definitions/${value}"))
        is Type.Shape.Field.Reference.Primitive -> SchemaObject(
            type = type.emitType()
        )

        is Type.Shape.Field.Reference.Any -> error("Cannot map Any")
        is Type.Shape.Field.Reference.Unit -> error("Cannot map Unit")
    }


    private fun Type.Shape.Field.Reference.Primitive.Type.emitType(): OpenApiType = when (this) {
        Type.Shape.Field.Reference.Primitive.Type.String -> OpenApiType.STRING
        Type.Shape.Field.Reference.Primitive.Type.Integer -> OpenApiType.INTEGER
        Type.Shape.Field.Reference.Primitive.Type.Number -> OpenApiType.NUMBER
        Type.Shape.Field.Reference.Primitive.Type.Boolean -> OpenApiType.BOOLEAN
    }

    private fun Type.Shape.Field.Reference.emitType(): OpenApiType = when (this) {
        is Type.Shape.Field.Reference.Primitive -> type.emitType()
        is Type.Shape.Field.Reference.Custom -> OpenApiType.OBJECT
        is Type.Shape.Field.Reference.Any -> OpenApiType.OBJECT
        is Type.Shape.Field.Reference.Unit -> OpenApiType.OBJECT
    }
}