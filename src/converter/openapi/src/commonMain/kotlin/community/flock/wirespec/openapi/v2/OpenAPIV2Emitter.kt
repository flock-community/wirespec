package community.flock.wirespec.openapi.v2

import arrow.core.NonEmptyList
import arrow.core.nonEmptyListOf
import community.flock.kotlinx.openapi.bindings.InfoObject
import community.flock.kotlinx.openapi.bindings.OpenAPIV2Header
import community.flock.kotlinx.openapi.bindings.OpenAPIV2Model
import community.flock.kotlinx.openapi.bindings.OpenAPIV2Operation
import community.flock.kotlinx.openapi.bindings.OpenAPIV2Parameter
import community.flock.kotlinx.openapi.bindings.OpenAPIV2ParameterLocation
import community.flock.kotlinx.openapi.bindings.OpenAPIV2PathItem
import community.flock.kotlinx.openapi.bindings.OpenAPIV2Reference
import community.flock.kotlinx.openapi.bindings.OpenAPIV2Response
import community.flock.kotlinx.openapi.bindings.OpenAPIV2Schema
import community.flock.kotlinx.openapi.bindings.OpenAPIV2SchemaOrReference
import community.flock.kotlinx.openapi.bindings.OpenAPIV2Type
import community.flock.kotlinx.openapi.bindings.Path
import community.flock.kotlinx.openapi.bindings.Ref
import community.flock.kotlinx.openapi.bindings.StatusCode
import community.flock.wirespec.compiler.core.emit.Emitted
import community.flock.wirespec.compiler.core.emit.Emitter
import community.flock.wirespec.compiler.core.emit.FileExtension
import community.flock.wirespec.compiler.core.parse.AST
import community.flock.wirespec.compiler.core.parse.Endpoint
import community.flock.wirespec.compiler.core.parse.Field
import community.flock.wirespec.compiler.core.parse.Reference
import community.flock.wirespec.compiler.core.parse.Refined
import community.flock.wirespec.compiler.core.parse.Statements
import community.flock.wirespec.compiler.core.parse.Type
import community.flock.wirespec.compiler.utils.Logger
import community.flock.wirespec.openapi.APPLICATION_JSON
import community.flock.wirespec.openapi.json
import community.flock.wirespec.openapi.v3.OpenAPIV3Emitter
import community.flock.wirespec.openapi.v3.OpenAPIV3Emitter.emitOpenAPIObject
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonPrimitive

object OpenAPIV2Emitter : Emitter {

    override val extension = FileExtension.JSON

    override fun emit(
        ast: AST,
        logger: Logger,
    ): NonEmptyList<Emitted> = ast.modules
        .flatMap { it.statements }
        .let {
            Emitted(
                "OpenAPI.${OpenAPIV3Emitter.extension.value}",
                json.encodeToString(emitOpenAPIObject(it, null)),
            )
        }
        .let { nonEmptyListOf(it) }

    fun emitSwaggerObject(statements: Statements): OpenAPIV2Model = OpenAPIV2Model(
        swagger = "2.0",
        info = InfoObject(
            title = "Wirespec",
            version = "0.0.0",
        ),
        consumes = listOf(APPLICATION_JSON),
        produces = listOf(APPLICATION_JSON),
        paths = statements.filterIsInstance<Endpoint>().groupBy { it.path }
            .map { (segments, endpoints) ->
                Path(segments.emitSegment()) to OpenAPIV2PathItem(
                    parameters = segments.filterIsInstance<Endpoint.Segment.Param>().map {
                        OpenAPIV2Parameter(
                            `in` = OpenAPIV2ParameterLocation.PATH,
                            name = it.identifier.value,
                            type = it.reference.emitType(),
                            format = it.reference.emitFormat(),
                            pattern = it.reference.emitPattern(),
                            minimum = it.reference.emitMinimum(),
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
        definitions = statements
            .filterIsInstance<Refined>().associate { refined ->
                when (val type = refined.reference.type) {
                    Reference.Primitive.Type.Boolean ->
                        refined.identifier.value to OpenAPIV2Schema(
                            type = OpenAPIV2Type.BOOLEAN,
                        )

                    Reference.Primitive.Type.Bytes ->
                        refined.identifier.value to OpenAPIV2Schema(
                            type = OpenAPIV2Type.FILE,
                        )

                    is Reference.Primitive.Type.Integer ->
                        refined.identifier.value to OpenAPIV2Schema(
                            type = OpenAPIV2Type.INTEGER,
                            minimum = type.constraint?.min?.toDouble(),
                            maximum = type.constraint?.min?.toDouble(),
                        )

                    is Reference.Primitive.Type.Number ->
                        refined.identifier.value to OpenAPIV2Schema(
                            type = OpenAPIV2Type.NUMBER,
                            minimum = type.constraint?.min?.toDouble(),
                            maximum = type.constraint?.min?.toDouble(),
                        )

                    is Reference.Primitive.Type.String ->
                        refined.identifier.value to when (val pattern = type.constraint) {
                            is Reference.Primitive.Type.Constraint.RegExp -> OpenAPIV2Schema(
                                type = OpenAPIV2Type.STRING,
                                pattern = pattern.value,
                            )

                            null -> OpenAPIV2Schema(
                                type = OpenAPIV2Type.STRING,
                            )
                        }
                }
            } + statements
            .filterIsInstance<Type>().associate { type ->
                type.identifier.value to OpenAPIV2Schema(
                    properties = type.shape.value.associate { it.toProperties() },
                    required = type.shape.value
                        .filter { !it.reference.isNullable }
                        .map { it.identifier.value }
                        .takeIf { it.isNotEmpty() },
                )
            } + statements
            .filterIsInstance<community.flock.wirespec.compiler.core.parse.Enum>()
            .associate { enum ->
                enum.identifier.value to OpenAPIV2Schema(
                    type = OpenAPIV2Type.STRING,
                    enum = enum.entries.map { JsonPrimitive(it) },
                )
            },
    )

    private fun Field.toProperties(): Pair<String, OpenAPIV2SchemaOrReference> = identifier.value to reference.toSchemaOrReference()

    private fun List<Endpoint>.emit(method: Endpoint.Method): OpenAPIV2Operation? = filter { it.method == method }.map { it.emit() }.firstOrNull()

    private fun Endpoint.emit() = OpenAPIV2Operation(
        operationId = identifier.value,
        description = comment?.value,
        consumes = requests.mapNotNull { it.content?.type }.distinct().ifEmpty { null },
        produces = responses.mapNotNull { it.content?.type }.distinct().ifEmpty { null },
        parameters = requests
            .mapNotNull { it.content }
            .take(1)
            .map {
                OpenAPIV2Parameter(
                    `in` = OpenAPIV2ParameterLocation.BODY,
                    name = "RequestBody",
                    schema = it.reference.toSchemaOrReference(),
                    required = !it.reference.isNullable,
                )
            } + queries.map { it.emitParameter(OpenAPIV2ParameterLocation.QUERY) } + headers.map {
            it.emitParameter(
                OpenAPIV2ParameterLocation.HEADER,
            )
        },
        responses = responses
            .associate { response ->
                StatusCode(response.status) to OpenAPIV2Response(
                    description = comment?.value
                        ?: "${identifier.value} ${response.status} response",
                    headers = response.headers.associate {
                        it.identifier.value to OpenAPIV2Header(
                            type = it.reference.emitType(),
                            format = it.reference.emitFormat(),
                            pattern = it.reference.emitPattern(),
                            items = (it.reference as? Reference.Iterable)?.reference?.toSchemaOrReference(),
                        )
                    },
                    schema = response.content
                        ?.takeIf { content -> content.reference !is Reference.Unit }
                        ?.let { content ->
                            when (val ref = content.reference) {
                                is Reference.Iterable -> OpenAPIV2Schema(
                                    type = OpenAPIV2Type.ARRAY,
                                    items = ref.reference.toSchemaOrReference(),
                                )

                                else -> ref.toSchemaOrReference()
                            }
                        },
                )
            },
    )

    private fun List<Endpoint.Segment>.emitSegment() = "/" + joinToString("/") {
        when (it) {
            is Endpoint.Segment.Param -> "{${it.identifier.value}}"
            is Endpoint.Segment.Literal -> it.value
        }
    }

    private fun Field.emitParameter(location: OpenAPIV2ParameterLocation) = OpenAPIV2Parameter(
        `in` = location,
        name = identifier.value,
        type = reference.emitType(),
        format = reference.emitFormat(),
        pattern = reference.emitPattern(),
        items = when (val ref = reference) {
            is Reference.Iterable -> when (val emit = ref.toSchemaOrReference()) {
                is OpenAPIV2Reference -> emit
                is OpenAPIV2Schema -> emit.items
            }

            else -> null
        },
        required = !reference.isNullable,
    )

    private fun Reference.toSchemaOrReference(): OpenAPIV2SchemaOrReference = when (this) {
        is Reference.Dict -> OpenAPIV2Schema(
            type = OpenAPIV2Type.OBJECT,
            items = reference.toSchemaOrReference(),
        )

        is Reference.Iterable -> OpenAPIV2Schema(
            type = OpenAPIV2Type.ARRAY,
            items = reference.toSchemaOrReference(),
        )

        is Reference.Custom -> OpenAPIV2Reference(ref = Ref("#/definitions/$value"))
        is Reference.Primitive -> OpenAPIV2Schema(
            type = type.emitType(),
            format = emitFormat(),
            pattern = emitPattern(),
        )

        is Reference.Any -> error("Cannot map Any")
        is Reference.Unit -> error("Cannot map Unit")
    }

    private fun Reference.Primitive.Type.emitType(): OpenAPIV2Type = when (this) {
        is Reference.Primitive.Type.String -> OpenAPIV2Type.STRING
        is Reference.Primitive.Type.Integer -> OpenAPIV2Type.INTEGER
        is Reference.Primitive.Type.Number -> OpenAPIV2Type.NUMBER
        is Reference.Primitive.Type.Boolean -> OpenAPIV2Type.BOOLEAN
        is Reference.Primitive.Type.Bytes -> OpenAPIV2Type.STRING
    }

    private fun Reference.emitType(): OpenAPIV2Type = when (this) {
        is Reference.Dict -> OpenAPIV2Type.OBJECT
        is Reference.Iterable -> OpenAPIV2Type.ARRAY
        is Reference.Primitive -> type.emitType()
        is Reference.Custom -> OpenAPIV2Type.OBJECT
        is Reference.Any -> OpenAPIV2Type.OBJECT
        is Reference.Unit -> OpenAPIV2Type.OBJECT
    }

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
