package community.flock.wirespec.openapi.v3

import community.flock.kotlinx.openapi.bindings.v3.ComponentsObject
import community.flock.kotlinx.openapi.bindings.v3.HeaderObject
import community.flock.kotlinx.openapi.bindings.v3.HeaderOrReferenceObject
import community.flock.kotlinx.openapi.bindings.v3.InfoObject
import community.flock.kotlinx.openapi.bindings.v3.MediaType
import community.flock.kotlinx.openapi.bindings.v3.MediaTypeObject
import community.flock.kotlinx.openapi.bindings.v3.OpenAPIObject
import community.flock.kotlinx.openapi.bindings.v3.OperationObject
import community.flock.kotlinx.openapi.bindings.v3.ParameterLocation
import community.flock.kotlinx.openapi.bindings.v3.ParameterObject
import community.flock.kotlinx.openapi.bindings.v3.Path
import community.flock.kotlinx.openapi.bindings.v3.PathItemObject
import community.flock.kotlinx.openapi.bindings.v3.Ref
import community.flock.kotlinx.openapi.bindings.v3.ReferenceObject
import community.flock.kotlinx.openapi.bindings.v3.RequestBodyObject
import community.flock.kotlinx.openapi.bindings.v3.ResponseObject
import community.flock.kotlinx.openapi.bindings.v3.SchemaObject
import community.flock.kotlinx.openapi.bindings.v3.SchemaOrReferenceObject
import community.flock.kotlinx.openapi.bindings.v3.StatusCode
import community.flock.wirespec.compiler.core.parse.AST
import community.flock.wirespec.compiler.core.parse.Definition
import community.flock.wirespec.compiler.core.parse.Endpoint
import community.flock.wirespec.compiler.core.parse.Enum
import community.flock.wirespec.compiler.core.parse.Refined
import community.flock.wirespec.compiler.core.parse.Type
import community.flock.wirespec.compiler.core.parse.Union
import kotlinx.serialization.json.JsonPrimitive
import community.flock.kotlinx.openapi.bindings.v3.Type as OpenApiType

class OpenApiV3Emitter {
    data class Options(
        val title: String,
        val version: String
    )

    fun emit(ast: AST, options: Options? = null): OpenAPIObject {
        return OpenAPIObject(
            openapi = "3.0.0",
            info = InfoObject(
                title = options?.title ?: "Wirespec",
                version = options?.version ?: "0.0.0"
            ),
            paths = ast.emitPaths(),
            components = ast.emitComponents()
        )
    }

    private fun AST.emitComponents() = this
        .filterIsInstance<Definition>()
        .filter { it !is Endpoint }
        .associate { it.name to when (it) {
            is Enum -> it.emit()
            is Refined -> it.emit()
            is Type -> it.emit()
            is Union -> it.emit()
            is Endpoint -> error("Cannot emit endpoint")
        } }
        .let { ComponentsObject(it) }

    private fun AST.emitPaths() = this
        .filterIsInstance<Endpoint>()
        .groupBy { it.path }.map { (path, endpoints) ->
            Path(path.emitSegment()) to PathItemObject(
                parameters = path
                    .filterIsInstance<Endpoint.Segment.Param>()
                    .map { it.emitParameter() }
                    .ifEmpty { null },
                get = endpoints.emit(Endpoint.Method.GET),
                post = endpoints.emit(Endpoint.Method.POST),
                put = endpoints.emit(Endpoint.Method.PUT),
                delete = endpoints.emit(Endpoint.Method.DELETE),
                patch = endpoints.emit(Endpoint.Method.PATCH),
                options = endpoints.emit(Endpoint.Method.OPTIONS),
                trace = endpoints.emit(Endpoint.Method.TRACE),
                head = endpoints.emit(Endpoint.Method.HEAD),
            )
        }
        .toMap()

    private fun Refined.emit(): SchemaObject =
        SchemaObject(
            type = OpenApiType.STRING,
            pattern = validator.value
        )

    private fun Type.emit(): SchemaObject =
        SchemaObject(

            properties = shape.value.associate { it.emitSchema() },
            required = shape.value
                .filter { !it.isNullable }
                .map { it.identifier.value }
                .takeIf { it.isNotEmpty() }
        )

    private fun Enum.emit(): SchemaObject =
        SchemaObject(
            type = OpenApiType.STRING,
            enum = entries.map { JsonPrimitive(it) }
        )

    private fun Union.emit(): SchemaObject =
        SchemaObject(
            type = OpenApiType.STRING,
            oneOf = entries.map { it.emitSchema() }
        )

    private fun List<Endpoint>.emit(method: Endpoint.Method): OperationObject? =
        filter { it.method == method }.map { it.emit() }.firstOrNull()

    private fun Endpoint.emit(): OperationObject = OperationObject(
        operationId = name,
        parameters = path.filterIsInstance<Endpoint.Segment.Param>()
            .map { it.emitParameter() } + query.map { it.emitParameter(ParameterLocation.QUERY) } + headers.map {
            it.emitParameter(
                ParameterLocation.HEADER
            )
        },
        requestBody = RequestBodyObject(
            content = requests.mapNotNull { it.content?.emit() }.toMap().ifEmpty { null },
            required = requests.any { it.content?.isNullable?:false }
        ),
        responses = responses
            .groupBy { it.status }
            .map { (statusCode, res) ->
                StatusCode(statusCode) to ResponseObject(
                    headers = res.flatMap { it.headers }.associate { it.emitHeader() },
                    description = "${name} $statusCode response",
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

    fun Type.Shape.Field.emitParameter(location: ParameterLocation): ParameterObject = ParameterObject(
        `in` = location,
        name = identifier.value,
        schema = reference.emitSchema()
    )

    fun Endpoint.Segment.Param.emitParameter(): ParameterObject = ParameterObject(
        `in` = ParameterLocation.PATH,
        name = identifier.value,
        schema = reference.emitSchema()
    )

    fun Type.Shape.Field.emitHeader(): Pair<String, HeaderOrReferenceObject> = identifier.value to reference.emitHeader()

    fun Type.Shape.Field.emitSchema(): Pair<String, SchemaOrReferenceObject> = identifier.value to reference.emitSchema()

    fun Type.Shape.Field.Reference.emitHeader(): HeaderOrReferenceObject {
        return when (this) {
            is Type.Shape.Field.Reference.Custom -> ReferenceObject(ref = Ref("#/components/headers/${value}"))
            is Type.Shape.Field.Reference.Primitive -> HeaderObject(schema = emitSchema())
            is Type.Shape.Field.Reference.Any -> error("Cannot map Any")
            is Type.Shape.Field.Reference.Unit -> error("Cannot map Unit")
        }
    }

    fun Type.Shape.Field.Reference.emitSchema(): SchemaOrReferenceObject {
        val ref = when (this) {
            is Type.Shape.Field.Reference.Custom -> ReferenceObject(ref = Ref("#/components/schemas/${value}"))
            is Type.Shape.Field.Reference.Primitive -> SchemaObject(type = type.emitType())
            is Type.Shape.Field.Reference.Any -> error("Cannot map Any")
            is Type.Shape.Field.Reference.Unit -> error("Cannot map Unit")
        }
        if(isMap){
            return SchemaObject(
                type = OpenApiType.OBJECT,
                additionalProperties = ref
            )
        }
        if(isIterable) {
            return SchemaObject(
                type = OpenApiType.ARRAY,
                items = ref,
            )
        }
        return ref
    }


    private fun Type.Shape.Field.Reference.Primitive.Type.emitType(): OpenApiType = when (this) {
        Type.Shape.Field.Reference.Primitive.Type.String -> OpenApiType.STRING
        Type.Shape.Field.Reference.Primitive.Type.Integer -> OpenApiType.INTEGER
        Type.Shape.Field.Reference.Primitive.Type.Number -> OpenApiType.NUMBER
        Type.Shape.Field.Reference.Primitive.Type.Boolean -> OpenApiType.BOOLEAN
    }

    private fun Endpoint.Content.emit(): Pair<MediaType, MediaTypeObject> =
        MediaType(type) to MediaTypeObject(
            schema = reference.emitSchema()
        )


}