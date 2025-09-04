package community.flock.wirespec.openapi.v3

import arrow.core.NonEmptyList
import arrow.core.nonEmptyListOf
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
import community.flock.kotlinx.openapi.bindings.v3.SchemaOrReferenceOrBooleanObject
import community.flock.kotlinx.openapi.bindings.v3.StatusCode
import community.flock.wirespec.compiler.core.emit.Emitted
import community.flock.wirespec.compiler.core.emit.Emitter
import community.flock.wirespec.compiler.core.emit.FileExtension
import community.flock.wirespec.compiler.core.parse.AST
import community.flock.wirespec.compiler.core.parse.Channel
import community.flock.wirespec.compiler.core.parse.Endpoint
import community.flock.wirespec.compiler.core.parse.Enum
import community.flock.wirespec.compiler.core.parse.Field
import community.flock.wirespec.compiler.core.parse.Reference
import community.flock.wirespec.compiler.core.parse.Refined
import community.flock.wirespec.compiler.core.parse.Statements
import community.flock.wirespec.compiler.core.parse.Type
import community.flock.wirespec.compiler.core.parse.Union
import community.flock.wirespec.compiler.utils.Logger
import community.flock.wirespec.openapi.Common.json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonPrimitive
import community.flock.kotlinx.openapi.bindings.v3.Type as OpenAPIType

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
        .flatMap { it.statements }
        .let {
            Emitted(
                "OpenAPI.${extension.value}",
                json.encodeToString(emitOpenAPIObject(it, null)),
            )
        }
        .let { nonEmptyListOf(it) }

    fun emitOpenAPIObject(statements: Statements, options: Options? = null) = OpenAPIObject(
        openapi = "3.0.0",
        info = InfoObject(
            title = options?.title ?: "Wirespec",
            version = options?.version ?: "0.0.0",
        ),
        paths = statements.emitPaths(),
        components = statements.emitComponents(),
    )

    private fun Statements.emitComponents() = this
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

    private fun Statements.emitPaths() = this
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

    private fun Refined.emit(): SchemaObject = when (val type = reference.type) {
        is Reference.Primitive.Type.Integer -> SchemaObject(
            type = OpenAPIType.STRING,
            minimum = type.constraint?.min?.toDouble(),
            maximum = type.constraint?.max?.toDouble(),
        )

        is Reference.Primitive.Type.Number -> SchemaObject(
            type = OpenAPIType.STRING,
            minimum = type.constraint?.min?.toDouble(),
            maximum = type.constraint?.max?.toDouble(),
        )

        is Reference.Primitive.Type.String -> when (val pattern = type.constraint) {
            is Reference.Primitive.Type.Constraint.RegExp -> SchemaObject(
                type = OpenAPIType.STRING,
                pattern = pattern.value,
            )

            null -> SchemaObject(
                type = OpenAPIType.STRING,
            )
        }

        Reference.Primitive.Type.Boolean -> SchemaObject(
            type = OpenAPIType.BOOLEAN,
        )

        Reference.Primitive.Type.Bytes -> SchemaObject(
            type = OpenAPIType.STRING,
        )
    }

    private fun Type.emit(): SchemaObject = SchemaObject(
        description = comment?.value,
        properties = shape.value.associate { it.emitSchema() },
        required = shape.value
            .filter { !it.reference.isNullable }
            .map { it.identifier.value }
            .takeIf { it.isNotEmpty() },
    )

    private fun Enum.emit(): SchemaObject = SchemaObject(
        description = comment?.value,
        type = OpenAPIType.STRING,
        enum = entries.map { JsonPrimitive(it) },
    )

    private fun Union.emit(): SchemaObject = SchemaObject(
        description = comment?.value,
        type = OpenAPIType.STRING,
        oneOf = entries.map { it.emitSchema() },
    )

    private fun List<Endpoint>.emit(method: Endpoint.Method): OperationObject? =
        filter { it.method == method }.map { it.emit() }.firstOrNull()

    private fun Endpoint.emit(): OperationObject = OperationObject(
        operationId = identifier.value,
        description = comment?.value,
        parameters = path.filterIsInstance<Endpoint.Segment.Param>()
            .map { it.emitParameter() } + queries.map { it.emitParameter(ParameterLocation.QUERY) } + headers.map {
            it.emitParameter(
                ParameterLocation.HEADER,
            )
        },
        requestBody = requests.mapNotNull { it.content?.emit() }
            .toMap()
            .takeIf { it.isNotEmpty() }
            ?.let { content ->
                RequestBodyObject(
                    content = content,
                    required = !requests.any { it.content?.reference?.isNullable == true },
                )
            },
        responses = responses
            .groupBy { it.status }
            .map { (statusCode, res) ->
                StatusCode(statusCode) to ResponseObject(
                    headers = res.flatMap { it.headers }.associate { it.emitHeader() },
                    description = "${identifier.value} $statusCode response",
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

    private fun Field.emitParameter(location: ParameterLocation): ParameterObject = ParameterObject(
        `in` = location,
        name = identifier.value,
        schema = reference.emitSchema(),
    )

    private fun Endpoint.Segment.Param.emitParameter(): ParameterObject = ParameterObject(
        `in` = ParameterLocation.PATH,
        name = identifier.value,
        schema = reference.emitSchema(),
    )

    private fun Field.emitHeader(): Pair<String, HeaderOrReferenceObject> = identifier.value to reference.emitHeader()

    private fun Field.emitSchema(): Pair<String, SchemaOrReferenceObject> = identifier.value to reference.emitSchema()

    private fun Reference.emitHeader() = when (this) {
        is Reference.Dict -> ReferenceObject(ref = Ref("#/components/headers/$value"))
        is Reference.Iterable -> ReferenceObject(ref = Ref("#/components/headers/$value"))
        is Reference.Custom -> ReferenceObject(ref = Ref("#/components/headers/$value"))
        is Reference.Primitive -> HeaderObject(schema = emitSchema())
        is Reference.Any -> error("Cannot map Any")
        is Reference.Unit -> error("Cannot map Unit")
    }

    private fun Reference.emitSchema(): SchemaOrReferenceObject = when (this) {
        is Reference.Dict -> SchemaObject(
            nullable = reference.isNullable,
            type = OpenAPIType.OBJECT,
            additionalProperties = reference.emitSchema() as SchemaOrReferenceOrBooleanObject,
        )

        is Reference.Iterable -> SchemaObject(
            nullable = reference.isNullable,
            type = OpenAPIType.ARRAY,
            items = reference.emitSchema(),
        )

        is Reference.Custom -> ReferenceObject(ref = Ref("#/components/schemas/$value"))
        is Reference.Primitive -> SchemaObject(
            type = type.emitType(),
            format = emitFormat(),
            pattern = emitPattern(),
            minimum = emitMinimum(),
            maximum = emitMaximum(),
        )

        is Reference.Any -> error("Cannot map Any")
        is Reference.Unit -> error("Cannot map Unit")
    }

    private fun Reference.Primitive.Type.emitType(): OpenAPIType = when (this) {
        is Reference.Primitive.Type.String -> OpenAPIType.STRING
        is Reference.Primitive.Type.Integer -> OpenAPIType.INTEGER
        is Reference.Primitive.Type.Number -> OpenAPIType.NUMBER
        is Reference.Primitive.Type.Boolean -> OpenAPIType.BOOLEAN
        is Reference.Primitive.Type.Bytes -> OpenAPIType.STRING
    }

    private fun Endpoint.Content.emit(): Pair<MediaType, MediaTypeObject> = MediaType(type) to MediaTypeObject(
        schema = reference.emitSchema(),
    )

    private fun Reference.emitFormat() = when (this) {
        is Reference.Primitive -> when (val t = type) {
            is Reference.Primitive.Type.Number -> when (t.precision) {
                Reference.Primitive.Type.Precision.P32 -> "float"
                Reference.Primitive.Type.Precision.P64 -> "double"
            }

            is Reference.Primitive.Type.Integer -> when (t.precision) {
                Reference.Primitive.Type.Precision.P32 -> "int32"
                Reference.Primitive.Type.Precision.P64 -> "int64"
            }

            is Reference.Primitive.Type.Bytes -> "binary"

            else -> null
        }

        else -> null
    }

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
