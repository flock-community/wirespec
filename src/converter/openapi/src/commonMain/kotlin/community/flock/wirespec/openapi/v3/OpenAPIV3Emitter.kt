package community.flock.wirespec.openapi.v3

import arrow.core.NonEmptyList
import arrow.core.nonEmptyListOf
import community.flock.kotlinx.openapi.bindings.InfoObject
import community.flock.kotlinx.openapi.bindings.MediaType
import community.flock.kotlinx.openapi.bindings.OpenAPIV3Components
import community.flock.kotlinx.openapi.bindings.OpenAPIV3Header
import community.flock.kotlinx.openapi.bindings.OpenAPIV3HeaderOrReference
import community.flock.kotlinx.openapi.bindings.OpenAPIV3MediaType
import community.flock.kotlinx.openapi.bindings.OpenAPIV3Model
import community.flock.kotlinx.openapi.bindings.OpenAPIV3Operation
import community.flock.kotlinx.openapi.bindings.OpenAPIV3Parameter
import community.flock.kotlinx.openapi.bindings.OpenAPIV3ParameterLocation
import community.flock.kotlinx.openapi.bindings.OpenAPIV3PathItem
import community.flock.kotlinx.openapi.bindings.OpenAPIV3Reference
import community.flock.kotlinx.openapi.bindings.OpenAPIV3RequestBody
import community.flock.kotlinx.openapi.bindings.OpenAPIV3Response
import community.flock.kotlinx.openapi.bindings.OpenAPIV3Schema
import community.flock.kotlinx.openapi.bindings.OpenAPIV3SchemaOrReference
import community.flock.kotlinx.openapi.bindings.OpenAPIV3SchemaOrReferenceOrBoolean
import community.flock.kotlinx.openapi.bindings.OpenAPIV3Type
import community.flock.kotlinx.openapi.bindings.Path
import community.flock.kotlinx.openapi.bindings.Ref
import community.flock.kotlinx.openapi.bindings.StatusCode
import community.flock.wirespec.compiler.core.emit.Emitted
import community.flock.wirespec.compiler.core.emit.Emitter
import community.flock.wirespec.compiler.core.emit.FileExtension
import community.flock.wirespec.compiler.core.parse.ast.AST
import community.flock.wirespec.compiler.core.parse.ast.Channel
import community.flock.wirespec.compiler.core.parse.ast.Endpoint
import community.flock.wirespec.compiler.core.parse.ast.Enum
import community.flock.wirespec.compiler.core.parse.ast.Field
import community.flock.wirespec.compiler.core.parse.ast.Reference
import community.flock.wirespec.compiler.core.parse.ast.Refined
import community.flock.wirespec.compiler.core.parse.ast.Statements
import community.flock.wirespec.compiler.core.parse.ast.Type
import community.flock.wirespec.compiler.core.parse.ast.Union
import community.flock.wirespec.compiler.utils.Logger
import community.flock.wirespec.openapi.common.emitFormat
import community.flock.wirespec.openapi.common.findDescription
import community.flock.wirespec.openapi.common.json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonPrimitive

object OpenAPIV3Emitter : Emitter {
    data class Options(
        val title: String,
        val version: String,
    )

    override val extension = FileExtension.JSON

    override fun emit(
        ast: AST,
        logger: Logger,
    ): NonEmptyList<Emitted> = ast.modules
        .flatMap { module ->
            logger.info("Combining Nodes from ${module.fileUri.value} ")
            module.statements
        }
        .let {
            Emitted(
                "OpenAPI.${extension.value}",
                json.encodeToString(emitOpenAPIObject(it, null, logger)),
            )
        }
        .let { nonEmptyListOf(it) }

    fun emitOpenAPIObject(statements: Statements, options: Options? = null, logger: Logger) = OpenAPIV3Model(
        openapi = "3.0.0",
        info = InfoObject(
            title = options?.title ?: "Wirespec",
            version = options?.version ?: "0.0.0",
        ),
        paths = statements.emitPaths(logger),
        components = statements.emitComponents(logger),
    )

    private fun Statements.emitComponents(logger: Logger) = this
        .filter { it !is Endpoint }
        .associate { definition ->
            definition.identifier.value to when (definition) {
                is Enum -> definition.emit()
                is Refined -> definition.emit()
                is Type -> definition.emit()
                is Union -> definition.emit()
                is Endpoint -> error("Cannot emit endpoint")
                is Channel -> error("Cannot emit channel")
            }
                .also { logger.info("Emitting ${definition::class.simpleName} ${definition.identifier.value}") }
        }
        .let { OpenAPIV3Components(it) }

    private fun Statements.emitPaths(logger: Logger) = filterIsInstance<Endpoint>()
        .groupBy { it.path }
        .map { (path, endpoints) ->
            logger.info("Emitting endpoints for path ${path.emitSegment()}")
            Path(path.emitSegment()) to OpenAPIV3PathItem(
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

    private fun Refined.emit(): OpenAPIV3Schema = when (val type = reference.type) {
        is Reference.Primitive.Type.Integer, is Reference.Primitive.Type.Number -> OpenAPIV3Schema(
            type = OpenAPIV3Type.STRING,
            minimum = type.constraint?.min?.toDouble(),
            maximum = type.constraint?.max?.toDouble(),
        )

        is Reference.Primitive.Type.String -> OpenAPIV3Schema(
            type = OpenAPIV3Type.STRING,
            pattern = type.constraint?.value,
        )

        Reference.Primitive.Type.Boolean -> OpenAPIV3Schema(
            type = OpenAPIV3Type.BOOLEAN,
        )

        Reference.Primitive.Type.Bytes -> OpenAPIV3Schema(
            type = OpenAPIV3Type.STRING,
        )
    }

    private fun Type.emit(): OpenAPIV3Schema = OpenAPIV3Schema(
        description = annotations.findDescription() ?: comment?.value,
        properties = shape.value.associate { it.emitSchema() },
        required = shape.value
            .filter { !it.reference.isNullable }
            .map { it.identifier.value }
            .takeIf { it.isNotEmpty() },
    )

    private fun Enum.emit(): OpenAPIV3Schema = OpenAPIV3Schema(
        description = annotations.findDescription() ?: comment?.value,
        type = OpenAPIV3Type.STRING,
        enum = entries.map { JsonPrimitive(it) },
    )

    private fun Union.emit(): OpenAPIV3Schema = OpenAPIV3Schema(
        description = annotations.findDescription() ?: comment?.value,
        type = OpenAPIV3Type.STRING,
        oneOf = entries.map { it.emitSchema() },
    )

    private fun List<Endpoint>.emit(method: Endpoint.Method): OpenAPIV3Operation? = filter { it.method == method }.map { it.emit() }.firstOrNull()

    private fun Endpoint.emit(): OpenAPIV3Operation = OpenAPIV3Operation(
        operationId = identifier.value,
        description = annotations.findDescription() ?: comment?.value,
        parameters = path.filterIsInstance<Endpoint.Segment.Param>()
            .map { it.emitParameter() } + queries.map { it.emitParameter(OpenAPIV3ParameterLocation.QUERY) } + headers.map {
            it.emitParameter(
                OpenAPIV3ParameterLocation.HEADER,
            )
        },
        requestBody = requests.mapNotNull { it.content?.emit() }
            .toMap()
            .takeIf { it.isNotEmpty() }
            ?.let { content ->
                OpenAPIV3RequestBody(
                    content = content,
                    required = !requests.any { it.content?.reference?.isNullable == true },
                )
            },
        responses = responses
            .groupBy { it.status }
            .map { (statusCode, res) ->
                StatusCode(statusCode) to OpenAPIV3Response(
                    headers = res.flatMap { it.headers }.associate { it.emitHeader() },
                    description = res.first().annotations.findDescription()
                        ?: "${identifier.value} $statusCode response",
                    content = res
                        .mapNotNull { it.content }
                        .associate { it.emit() }
                        .ifEmpty { null },
                )
            }
            .toMap(),
    )

    private fun List<Endpoint.Segment>.emitSegment() = "/" + joinToString("/") {
        when (it) {
            is Endpoint.Segment.Param -> "{${it.identifier.value}}"
            is Endpoint.Segment.Literal -> it.value
        }
    }

    private fun Field.emitParameter(location: OpenAPIV3ParameterLocation): OpenAPIV3Parameter = OpenAPIV3Parameter(
        `in` = location,
        name = identifier.value,
        schema = reference.emitSchema(),
        description = annotations.findDescription(),
        required = !reference.isNullable,
    )

    private fun Endpoint.Segment.Param.emitParameter(): OpenAPIV3Parameter = OpenAPIV3Parameter(
        `in` = OpenAPIV3ParameterLocation.PATH,
        name = identifier.value,
        schema = reference.emitSchema(),
        required = !reference.isNullable,
    )

    private fun Field.emitHeader(): Pair<String, OpenAPIV3HeaderOrReference> = identifier.value to reference.emitHeader(annotations.findDescription())

    private fun Field.emitSchema(): Pair<String, OpenAPIV3SchemaOrReference> = identifier.value to reference.emitSchema().let {
        when (it) {
            is OpenAPIV3Schema -> it.copy(description = annotations.findDescription())
            is OpenAPIV3Reference -> it
        }
    }

    private fun Reference.emitHeader(description: String?) = when (this) {
        is Reference.Dict -> OpenAPIV3Reference(ref = Ref("#/components/headers/$value"))
        is Reference.Iterable -> OpenAPIV3Reference(ref = Ref("#/components/headers/$value"))
        is Reference.Custom -> OpenAPIV3Reference(ref = Ref("#/components/headers/$value"))
        is Reference.Primitive -> OpenAPIV3Header(schema = emitSchema(), description = description)
        is Reference.Any -> error("Cannot map Any")
        is Reference.Unit -> error("Cannot map Unit")
    }

    private fun Reference.emitSchema(): OpenAPIV3SchemaOrReference = when (this) {
        is Reference.Dict -> OpenAPIV3Schema(
            nullable = reference.isNullable,
            type = OpenAPIV3Type.OBJECT,
            additionalProperties = reference.emitSchema() as OpenAPIV3SchemaOrReferenceOrBoolean,
        )

        is Reference.Iterable -> OpenAPIV3Schema(
            nullable = reference.isNullable,
            type = OpenAPIV3Type.ARRAY,
            items = reference.emitSchema(),
        )

        is Reference.Custom -> OpenAPIV3Reference(ref = Ref("#/components/schemas/$value"))
        is Reference.Primitive -> OpenAPIV3Schema(
            type = type.emitType(),
            format = emitFormat(),
            pattern = emitPattern(),
            minimum = emitMinimum(),
            maximum = emitMaximum(),
        )

        is Reference.Any -> error("Cannot map Any")
        is Reference.Unit -> error("Cannot map Unit")
    }

    private fun Reference.Primitive.Type.emitType(): OpenAPIV3Type = when (this) {
        is Reference.Primitive.Type.String -> OpenAPIV3Type.STRING
        is Reference.Primitive.Type.Integer -> OpenAPIV3Type.INTEGER
        is Reference.Primitive.Type.Number -> OpenAPIV3Type.NUMBER
        is Reference.Primitive.Type.Boolean -> OpenAPIV3Type.BOOLEAN
        is Reference.Primitive.Type.Bytes -> OpenAPIV3Type.STRING
    }

    private fun Endpoint.Content.emit(): Pair<MediaType, OpenAPIV3MediaType> = MediaType(type) to OpenAPIV3MediaType(
        schema = reference.emitSchema(),
    )

    private fun Reference.emitPattern() = when (this) {
        is Reference.Primitive -> when (val t = type) {
            is Reference.Primitive.Type.String -> when (val p = t.constraint) {
                is Reference.Primitive.Type.Constraint.RegExp -> p.value
                else -> null
            }

            else -> null
        }

        else -> null
    }

    private fun Reference.emitMinimum() = when (this) {
        is Reference.Primitive -> when (val t = type) {
            is Reference.Primitive.Type.Number -> t.constraint?.min?.toDouble()
            is Reference.Primitive.Type.Integer -> t.constraint?.min?.toDouble()
            else -> null
        }

        else -> null
    }

    private fun Reference.emitMaximum() = when (this) {
        is Reference.Primitive -> when (val t = type) {
            is Reference.Primitive.Type.Number -> t.constraint?.max?.toDouble()
            is Reference.Primitive.Type.Integer -> t.constraint?.max?.toDouble()
            else -> null
        }

        else -> null
    }
}
