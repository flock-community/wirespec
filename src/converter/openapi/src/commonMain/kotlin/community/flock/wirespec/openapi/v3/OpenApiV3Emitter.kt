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
import community.flock.wirespec.compiler.core.emit.common.Emitted
import community.flock.wirespec.compiler.core.emit.common.Emitter
import community.flock.wirespec.compiler.core.parse.AST
import community.flock.wirespec.compiler.core.parse.Channel
import community.flock.wirespec.compiler.core.parse.Definition
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
import community.flock.kotlinx.openapi.bindings.v3.Type as OpenApiType

object OpenApiV3Emitter : Emitter(noLogger) {
    data class Options(
        val title: String,
        val version: String,
    )

    override val singleLineComment = ""

    override fun emit(ast: AST): List<Emitted> =
        listOf(Emitted("OpenAPIObject", json.encodeToString(emitOpenAPIObject(ast, null))))

    override fun emit(type: Type, ast: AST) = notYetImplemented()

    override fun emit(enum: Enum) = notYetImplemented()

    override fun emit(refined: Refined) = notYetImplemented()

    override fun emit(endpoint: Endpoint) = notYetImplemented()

    override fun emit(union: Union) = notYetImplemented()

    override fun emit(identifier: Identifier) = notYetImplemented()

    override fun emit(channel: Channel) = notYetImplemented()

    fun emitOpenAPIObject(ast: AST, options: Options? = null) = OpenAPIObject(
        openapi = "3.0.0",
        info = InfoObject(
            title = options?.title ?: "Wirespec",
            version = options?.version ?: "0.0.0"
        ),
        paths = ast.emitPaths(),
        components = ast.emitComponents()
    )

    private fun AST.emitComponents() = this
        .filterIsInstance<Definition>()
        .filter { it !is Endpoint }
        .associate {
            it.identifier.value to when (it) {
                is Enum -> it.emit()
                is Refined -> it.emit()
                is Type -> it.emit()
                is Union -> it.emit()
                is Endpoint -> error("Cannot emit endpoint")
                is Channel -> error("Cannot emit channel")
            }
        }
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
            description = comment?.value,
            properties = shape.value.associate { it.emitSchema() },
            required = shape.value
                .filter { !it.isNullable }
                .map { it.identifier.value }
                .takeIf { it.isNotEmpty() }
        )

    private fun Enum.emit(): SchemaObject =
        SchemaObject(
            description = comment?.value,
            type = OpenApiType.STRING,
            enum = entries.map { JsonPrimitive(it) }
        )

    private fun Union.emit(): SchemaObject =
        SchemaObject(
            description = comment?.value,
            type = OpenApiType.STRING,
            oneOf = entries.map { it.emitSchema() }
        )

    private fun List<Endpoint>.emit(method: Endpoint.Method): OperationObject? =
        filter { it.method == method }.map { it.emit() }.firstOrNull()

    private fun Endpoint.emit(): OperationObject = OperationObject(
        operationId = identifier.value,
        description = comment?.value,
        parameters = path.filterIsInstance<Endpoint.Segment.Param>()
            .map { it.emitParameter() } + queries.map { it.emitParameter(ParameterLocation.QUERY) } + headers.map {
            it.emitParameter(
                ParameterLocation.HEADER
            )
        },
        requestBody = RequestBodyObject(
            content = requests.mapNotNull { it.content?.emit() }.toMap().ifEmpty { null },
            required = !requests.any { it.content?.isNullable ?: false }
        ),
        responses = responses
            .groupBy { it.status }
            .map { (statusCode, res) ->
                StatusCode(statusCode) to ResponseObject(
                    headers = res.flatMap { it.headers }.associate { it.emitHeader() },
                    description = "${identifier.value} $statusCode response",
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

    private fun Field.emitParameter(location: ParameterLocation): ParameterObject = ParameterObject(
        `in` = location,
        name = identifier.value,
        schema = reference.emitSchema()
    )

    private fun Endpoint.Segment.Param.emitParameter(): ParameterObject = ParameterObject(
        `in` = ParameterLocation.PATH,
        name = identifier.value,
        schema = reference.emitSchema()
    )

    private fun Field.emitHeader(): Pair<String, HeaderOrReferenceObject> = identifier.value to reference.emitHeader()

    private fun Field.emitSchema(): Pair<String, SchemaOrReferenceObject> = identifier.value to reference.emitSchema()

    private fun Reference.emitHeader() = when (this) {
        is Reference.Custom -> ReferenceObject(ref = Ref("#/components/headers/${value}"))
        is Reference.Primitive -> HeaderObject(schema = emitSchema())
        is Reference.Any -> error("Cannot map Any")
        is Reference.Unit -> error("Cannot map Unit")
    }

    private fun Reference.emitSchema(): SchemaOrReferenceObject =
        when (this) {
            is Reference.Custom -> ReferenceObject(ref = Ref("#/components/schemas/${value}"))
            is Reference.Primitive -> SchemaObject(type = type.emitType())
            is Reference.Any -> error("Cannot map Any")
            is Reference.Unit -> error("Cannot map Unit")
        }.let {
            when {
                isDictionary -> SchemaObject(
                    type = OpenApiType.OBJECT,
                    additionalProperties = it
                )

                isIterable -> SchemaObject(
                    type = OpenApiType.ARRAY,
                    items = it,
                )

                else -> it
            }
        }

    private fun Reference.Primitive.Type.emitType(): OpenApiType = when (this) {
        Reference.Primitive.Type.String -> OpenApiType.STRING
        Reference.Primitive.Type.Integer -> OpenApiType.INTEGER
        Reference.Primitive.Type.Number -> OpenApiType.NUMBER
        Reference.Primitive.Type.Boolean -> OpenApiType.BOOLEAN
    }

    private fun Endpoint.Content.emit(): Pair<MediaType, MediaTypeObject> =
        MediaType(type) to MediaTypeObject(
            schema = reference.emitSchema()
        )
}
