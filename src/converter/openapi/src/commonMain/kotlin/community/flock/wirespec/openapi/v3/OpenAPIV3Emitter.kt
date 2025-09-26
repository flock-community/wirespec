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
import community.flock.wirespec.openapi.json
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
        .flatMap { it.statements }
        .let {
            Emitted(
                "OpenAPI.${extension.value}",
                json.encodeToString(emitOpenAPIObject(it, null)),
            )
        }
        .let { nonEmptyListOf(it) }

    fun emitOpenAPIObject(statements: Statements, options: Options? = null) = OpenAPIV3Model(
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
        .let { OpenAPIV3Components(it) }

    private fun Statements.emitPaths() = this
        .filterIsInstance<Endpoint>()
        .groupBy { it.path }.map { (path, endpoints) ->
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
        is Reference.Primitive.Type.Integer -> OpenAPIV3Schema(
            type = OpenAPIV3Type.STRING,
            minimum = type.constraint?.min?.toDouble(),
            maximum = type.constraint?.max?.toDouble(),
        )

        is Reference.Primitive.Type.Number -> OpenAPIV3Schema(
            type = OpenAPIV3Type.STRING,
            minimum = type.constraint?.min?.toDouble(),
            maximum = type.constraint?.max?.toDouble(),
        )

        is Reference.Primitive.Type.String -> when (val pattern = type.constraint) {
            is Reference.Primitive.Type.Constraint.RegExp -> OpenAPIV3Schema(
                type = OpenAPIV3Type.STRING,
                pattern = pattern.value,
            )

            null -> OpenAPIV3Schema(
                type = OpenAPIV3Type.STRING,
            )
        }

        Reference.Primitive.Type.Boolean -> OpenAPIV3Schema(
            type = OpenAPIV3Type.BOOLEAN,
        )

        Reference.Primitive.Type.Bytes -> OpenAPIV3Schema(
            type = OpenAPIV3Type.STRING,
        )
    }

    private fun Type.emit(): OpenAPIV3Schema = OpenAPIV3Schema(
        description = comment?.value,
        properties = shape.value.associate { it.emitSchema() },
        required = shape.value
            .filter { !it.reference.isNullable }
            .map { it.identifier.value }
            .takeIf { it.isNotEmpty() },
    )

    private fun Enum.emit(): OpenAPIV3Schema = OpenAPIV3Schema(
        description = comment?.value,
        type = OpenAPIV3Type.STRING,
        enum = entries.map { JsonPrimitive(it) },
    )

    private fun Union.emit(): OpenAPIV3Schema = OpenAPIV3Schema(
        description = comment?.value,
        type = OpenAPIV3Type.STRING,
        oneOf = entries.map { it.emitSchema() },
    )

    private fun List<Endpoint>.emit(method: Endpoint.Method): OpenAPIV3Operation? = filter { it.method == method }.map { it.emit() }.firstOrNull()

    private fun Endpoint.emit(): OpenAPIV3Operation = OpenAPIV3Operation(
        operationId = identifier.value,
        description = comment?.value,
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

    private fun Field.emitParameter(location: OpenAPIV3ParameterLocation): OpenAPIV3Parameter = OpenAPIV3Parameter(
        `in` = location,
        name = identifier.value,
        schema = reference.emitSchema(),
        required = !reference.isNullable,
    )

    private fun Endpoint.Segment.Param.emitParameter(): OpenAPIV3Parameter = OpenAPIV3Parameter(
        `in` = OpenAPIV3ParameterLocation.PATH,
        name = identifier.value,
        schema = reference.emitSchema(),
        required = !reference.isNullable,
    )

    private fun Field.emitHeader(): Pair<String, OpenAPIV3HeaderOrReference> = identifier.value to reference.emitHeader()

    private fun Field.emitSchema(): Pair<String, OpenAPIV3SchemaOrReference> = identifier.value to reference.emitSchema()

    private fun Reference.emitHeader() = when (this) {
        is Reference.Dict -> OpenAPIV3Reference(ref = Ref("#/components/headers/$value"))
        is Reference.Iterable -> OpenAPIV3Reference(ref = Ref("#/components/headers/$value"))
        is Reference.Custom -> OpenAPIV3Reference(ref = Ref("#/components/headers/$value"))
        is Reference.Primitive -> OpenAPIV3Header(schema = emitSchema())
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
