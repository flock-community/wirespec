package community.flock.wirespec.openapi.v3

import community.flock.kotlinx.openapi.bindings.v3.ComponentsObject
import community.flock.kotlinx.openapi.bindings.v3.InfoObject
import community.flock.kotlinx.openapi.bindings.v3.MediaType
import community.flock.kotlinx.openapi.bindings.v3.MediaTypeObject
import community.flock.kotlinx.openapi.bindings.v3.OpenAPI
import community.flock.kotlinx.openapi.bindings.v3.OpenAPIObject
import community.flock.kotlinx.openapi.bindings.v3.OperationObject
import community.flock.kotlinx.openapi.bindings.v3.ParameterLocation
import community.flock.kotlinx.openapi.bindings.v3.ParameterObject
import community.flock.kotlinx.openapi.bindings.v3.Path
import community.flock.kotlinx.openapi.bindings.v3.PathItemObject
import community.flock.kotlinx.openapi.bindings.v3.Ref
import community.flock.kotlinx.openapi.bindings.v3.ReferenceObject
import community.flock.kotlinx.openapi.bindings.v3.ResponseObject
import community.flock.kotlinx.openapi.bindings.v3.SchemaObject
import community.flock.kotlinx.openapi.bindings.v3.SchemaOrReferenceObject
import community.flock.kotlinx.openapi.bindings.v3.StatusCode
import community.flock.wirespec.compiler.core.emit.common.Emitted
import community.flock.wirespec.compiler.core.parse.Definition
import community.flock.wirespec.compiler.core.parse.Endpoint
import community.flock.wirespec.compiler.core.parse.Refined
import community.flock.wirespec.compiler.core.parse.Type
import community.flock.kotlinx.openapi.bindings.v3.Type as OpenApiType

class OpenApiV3Emitter {

    fun emit(ast: List<Definition>): List<Emitted> {
        val obj = OpenAPIObject(
            openapi = "3.0.0",
            info = InfoObject(
                title = "Wirespec",
                version = "0.0.0"
            ),
            paths = ast.filterIsInstance<Endpoint>().groupBy { it.path }.map {
                Path(it.key.emitSegment()) to PathItemObject(
                    parameters = it.key.filterIsInstance<Endpoint.Segment.Param>().map {
                        ParameterObject(
                            `in` = ParameterLocation.PATH,
                            name = it.identifier.value,
                            schema = it.reference.emit()
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
            components = ComponentsObject(
                schemas = ast
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
                    }
            )
        )
        val str = OpenAPI.encodeToString(obj)
        return listOf(Emitted("OpenApi", str))
    }

    private fun List<Endpoint>.emit(method: Endpoint.Method): OperationObject? =
        filter { it.method == method }.map { it.emit() }.firstOrNull()

    private fun Endpoint.emit(): OperationObject = OperationObject(
        operationId = this.name,
        parameters =  query.map {
            ParameterObject(
                `in` = ParameterLocation.QUERY,
                name = it.identifier.value,
                schema = it.reference.emit()
            )
        },
        responses = responses
            .groupBy{it.status}
            .map { (statusCode, res) ->
                StatusCode(statusCode) to ResponseObject(
                        description = "${this.name} $statusCode response",
                        content = res
                            .mapNotNull { it.content }
                            .associate { it.emit() }
                            .ifEmpty { null }
                )
            }
            .toMap()
    )

    private fun List<Endpoint.Segment>.emitSegment() = "/" + joinToString("/") {
        when (it) {
            is Endpoint.Segment.Param -> "{${it.identifier.value}}"
            is Endpoint.Segment.Literal -> it.value
        }
    }

    fun Type.Shape.Field.emit(): Pair<String, SchemaOrReferenceObject> = identifier.value to reference.emit()

    fun Type.Shape.Field.Reference.emit(): SchemaOrReferenceObject = when(isIterable){
        true -> SchemaObject(
                type = OpenApiType.ARRAY,
                items = emit()
            )
        false ->when (this) {
                is Type.Shape.Field.Reference.Custom -> ReferenceObject(ref = Ref("#/definitions/${value}"))
                is Type.Shape.Field.Reference.Primitive -> SchemaObject(
                    type = type.emitType()
                )
                is Type.Shape.Field.Reference.Any -> error("Cannot map Any")
                is Type.Shape.Field.Reference.Unit -> error("Cannot map Unit")
            }
        }


    private fun Type.Shape.Field.Reference.Primitive.Type.emitType(): OpenApiType = when (this) {
        Type.Shape.Field.Reference.Primitive.Type.String -> OpenApiType.STRING
        Type.Shape.Field.Reference.Primitive.Type.Integer -> OpenApiType.INTEGER
        Type.Shape.Field.Reference.Primitive.Type.Number -> OpenApiType.NUMBER
        Type.Shape.Field.Reference.Primitive.Type.Boolean -> OpenApiType.BOOLEAN
    }

    private fun Endpoint.Content.emit():Pair<MediaType, MediaTypeObject> =
        MediaType(type) to MediaTypeObject(schema =  reference.emit())


}