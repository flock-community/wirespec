package community.flock.wirespec.openapi.v2

import arrow.core.NonEmptyList
import arrow.core.nonEmptyListOf
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
import community.flock.wirespec.compiler.core.emit.common.FileExtension
import community.flock.wirespec.compiler.core.parse.Channel
import community.flock.wirespec.compiler.core.parse.Endpoint
import community.flock.wirespec.compiler.core.parse.Enum
import community.flock.wirespec.compiler.core.parse.Field
import community.flock.wirespec.compiler.core.parse.Identifier
import community.flock.wirespec.compiler.core.parse.Module
import community.flock.wirespec.compiler.core.parse.Reference
import community.flock.wirespec.compiler.core.parse.Refined
import community.flock.wirespec.compiler.core.parse.Type
import community.flock.wirespec.compiler.core.parse.Union
import community.flock.wirespec.compiler.utils.Logger
import community.flock.wirespec.openapi.Common.json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonPrimitive
import community.flock.kotlinx.openapi.bindings.v2.Type as OpenAPIType

object OpenAPIV2Emitter : Emitter() {

    override val extension = FileExtension.JSON

    override val shared = null

    override val singleLineComment = ""

    override fun emit(module: Module, logger: Logger): NonEmptyList<Emitted> =
        nonEmptyListOf(Emitted("SwaggerObject", json.encodeToString(emitSwaggerObject(module))))

    override fun Type.Shape.emit() = notYetImplemented()

    override fun Field.emit() = notYetImplemented()

    override fun Reference.emit() = notYetImplemented()

    override fun Refined.emitValidator() = notYetImplemented()

    override fun Reference.Primitive.Type.Pattern.emit() = notYetImplemented()

    override fun Reference.Primitive.Type.Bound.emit() = notYetImplemented()

    override fun emit(type: Type, module: Module) = notYetImplemented()

    override fun emit(enum: Enum, module: Module) = notYetImplemented()

    override fun emit(refined: Refined) = notYetImplemented()

    override fun emit(endpoint: Endpoint) = notYetImplemented()

    override fun emit(union: Union) = notYetImplemented()

    override fun emit(identifier: Identifier) = notYetImplemented()

    override fun emit(channel: Channel) = notYetImplemented()

    fun emitSwaggerObject(module: Module): SwaggerObject =
        SwaggerObject(
            swagger = "2.0",
            info = InfoObject(
                title = "Wirespec",
                version = "0.0.0"
            ),
            consumes = listOf("application/json"),
            produces = listOf("application/json"),
            paths = module.statements.filterIsInstance<Endpoint>().groupBy { it.path }.map { (segments, endpoints) ->
                Path(segments.emitSegment()) to PathItemObject(
                    parameters = segments.filterIsInstance<Endpoint.Segment.Param>().map {
                        ParameterObject(
                            `in` = ParameterLocation.PATH,
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
            definitions = module.statements
                .filterIsInstance<Refined>().associate { refined ->
                    when (val type = refined.reference.type) {
                        Reference.Primitive.Type.Boolean ->
                            refined.identifier.value to SchemaObject(
                                type = OpenAPIType.BOOLEAN,
                            )

                        Reference.Primitive.Type.Bytes ->
                            refined.identifier.value to SchemaObject(
                                type = OpenAPIType.FILE,
                            )

                        is Reference.Primitive.Type.Integer ->
                            refined.identifier.value to SchemaObject(
                                type = OpenAPIType.INTEGER,
                                minimum = type.bound?.min?.toDouble(),
                                maximum = type.bound?.min?.toDouble(),
                            )

                        is Reference.Primitive.Type.Number ->
                            refined.identifier.value to SchemaObject(
                                type = OpenAPIType.NUMBER,
                                minimum = type.bound?.min?.toDouble(),
                                maximum = type.bound?.min?.toDouble(),
                            )

                        is Reference.Primitive.Type.String ->
                            refined.identifier.value to when(val pattern = type.pattern){
                                is Reference.Primitive.Type.Pattern.RegExp -> SchemaObject(
                                    type = OpenAPIType.STRING,
                                    pattern = pattern.value,
                                )
                                is Reference.Primitive.Type.Pattern.Format -> SchemaObject(
                                    type = OpenAPIType.STRING,
                                    format = pattern.value,
                                )
                                null -> SchemaObject(
                                    type = OpenAPIType.STRING,
                                )
                            }
                    }

                } + module.statements
                .filterIsInstance<Type>().associate { type ->
                    type.identifier.value to SchemaObject(
                        properties = type.shape.value.associate { it.toProperties() },
                        required = type.shape.value
                            .filter { !it.reference.isNullable }
                            .map { it.identifier.value }
                            .takeIf { it.isNotEmpty() }
                    )
                } + module.statements
                .filterIsInstance<Enum>().associate { enum ->
                    enum.identifier.value to SchemaObject(
                        type = OpenAPIType.STRING,
                        enum = enum.entries.map { JsonPrimitive(it) }
                    )
                }
        )

    private fun Field.toProperties(): Pair<String, SchemaOrReferenceObject> =
        identifier.value to reference.toSchemaOrReference()

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
                    schema = it.reference.toSchemaOrReference(),
                    required = !it.reference.isNullable,
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
                            pattern = it.reference.emitPattern(),
                            items = (it.reference as? Reference.Iterable)?.reference?.toSchemaOrReference()
                        )
                    },
                    schema = response.content
                        ?.takeIf { content -> content.reference !is Reference.Unit }
                        ?.let { content ->
                            when (val ref = content.reference) {
                                is Reference.Iterable -> SchemaObject(
                                    type = OpenAPIType.ARRAY,
                                    items = ref.reference.toSchemaOrReference()
                                )

                                else -> ref.toSchemaOrReference()
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


    private fun Field.emitParameter(location: ParameterLocation) =
        ParameterObject(
            `in` = location,
            name = identifier.value,
            type = reference.emitType(),
            format = reference.emitFormat(),
            pattern = reference.emitPattern(),
            items = when (val ref = reference) {
                is Reference.Iterable -> when (val emit = ref.toSchemaOrReference()) {
                    is ReferenceObject -> emit
                    is SchemaObject -> emit.items
                }

                else -> null
            },
            required = !reference.isNullable,
        )

    private fun Reference.toSchemaOrReference(): SchemaOrReferenceObject = when (this) {
        is Reference.Dict -> SchemaObject(type = OpenAPIType.OBJECT, items = reference.toSchemaOrReference())
        is Reference.Iterable -> SchemaObject(type = OpenAPIType.ARRAY, items = reference.toSchemaOrReference())
        is Reference.Custom -> ReferenceObject(ref = Ref("#/definitions/${value}"))
        is Reference.Primitive -> SchemaObject(type = type.emitType(), format = emitFormat(), pattern = emitPattern())
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

    private fun Reference.emitType() =
        when (this) {
            is Reference.Dict -> OpenAPIType.OBJECT
            is Reference.Iterable -> OpenAPIType.ARRAY
            is Reference.Primitive -> type.emitType()
            is Reference.Custom -> OpenAPIType.OBJECT
            is Reference.Any -> OpenAPIType.OBJECT
            is Reference.Unit -> OpenAPIType.OBJECT
        }

    private fun Reference.emitFormat() =
        when (this) {
            is Reference.Primitive -> when (val t = type) {
                is Reference.Primitive.Type.String -> when (val p = t.pattern) {
                    is Reference.Primitive.Type.Pattern.Format -> p.value
                    else -> null
                }
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

    private fun Reference.emitPattern() =
        when (this) {
            is Reference.Primitive -> when (val t = type) {
                is Reference.Primitive.Type.String -> when (val p = t.pattern) {
                    is Reference.Primitive.Type.Pattern.RegExp -> p.value
                    else -> null
                }
                else -> null
            }
            else -> null
        }

    private fun Reference.emitMinimum() =
        when (this) {
            is Reference.Primitive -> when (val t = type) {
                is Reference.Primitive.Type.Number -> t.bound?.min?.toDouble()
                is Reference.Primitive.Type.Integer -> t.bound?.min?.toDouble()
                else -> null
            }
            else -> null
        }

    private fun Reference.emitMaximum() =
        when (this) {
            is Reference.Primitive -> when (val t = type) {
                is Reference.Primitive.Type.Number -> t.bound?.max?.toDouble()
                is Reference.Primitive.Type.Integer -> t.bound?.max?.toDouble()
                else -> null
            }
            else -> null
        }
}
