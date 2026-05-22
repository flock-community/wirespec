package community.flock.wirespec.openapi.v3

import arrow.core.NonEmptyList
import arrow.core.nonEmptyListOf
import community.flock.kotlinx.openapi.bindings.InfoObject
import community.flock.kotlinx.openapi.bindings.MediaType
import community.flock.kotlinx.openapi.bindings.OpenAPIV30Components
import community.flock.kotlinx.openapi.bindings.OpenAPIV30Header
import community.flock.kotlinx.openapi.bindings.OpenAPIV30HeaderOrReference
import community.flock.kotlinx.openapi.bindings.OpenAPIV30Link
import community.flock.kotlinx.openapi.bindings.OpenAPIV30LinkOrReference
import community.flock.kotlinx.openapi.bindings.OpenAPIV30Links
import community.flock.kotlinx.openapi.bindings.OpenAPIV30MediaType
import community.flock.kotlinx.openapi.bindings.OpenAPIV30Model
import community.flock.kotlinx.openapi.bindings.OpenAPIV3Model
import community.flock.kotlinx.openapi.bindings.OpenAPIV30Operation
import community.flock.kotlinx.openapi.bindings.OpenAPIV30Parameter
import community.flock.kotlinx.openapi.bindings.OpenAPIV30ParameterLocation
import community.flock.kotlinx.openapi.bindings.OpenAPIV30PathItem
import community.flock.kotlinx.openapi.bindings.OpenAPIV30Reference
import community.flock.kotlinx.openapi.bindings.OpenAPIV30RequestBody
import community.flock.kotlinx.openapi.bindings.OpenAPIV30Response
import community.flock.kotlinx.openapi.bindings.OpenAPIV30Schema
import community.flock.kotlinx.openapi.bindings.OpenAPIV30SchemaOrReference
import community.flock.kotlinx.openapi.bindings.OpenAPIV30SchemaOrReferenceOrBoolean
import community.flock.kotlinx.openapi.bindings.OpenAPIV30SingleType
import community.flock.kotlinx.openapi.bindings.OpenAPIV30Type
import community.flock.kotlinx.openapi.bindings.OpenAPIV30TypeDefinition
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
import community.flock.wirespec.openapi.common.LinkInfo
import community.flock.wirespec.openapi.common.emitFormat
import community.flock.wirespec.openapi.common.findDescription
import community.flock.wirespec.openapi.common.findLinks
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

    fun emitOpenAPIObject(statements: Statements, options: Options? = null, logger: Logger): OpenAPIV3Model = OpenAPIV30Model(
        openapi = "3.0.0",
        info = InfoObject(
            title = options?.title ?: "Wirespec",
            version = options?.version ?: "0.0.0",
        ),
        paths = statements.emitPaths(logger),
        components = statements.emitComponents(logger),
    )

    private fun Statements.emitComponents(logger: Logger) = this
        .filter { it !is Endpoint && it !is Channel }
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
        .let { OpenAPIV30Components(it) }

    private fun Statements.emitPaths(logger: Logger) = filterIsInstance<Endpoint>()
        .groupBy { it.path }
        .map { (path, endpoints) ->
            logger.info("Emitting endpoints for path ${path.emitSegment()}")
            Path(path.emitSegment()) to OpenAPIV30PathItem(
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

    private fun Refined.emit(): OpenAPIV30Schema = when (val type = reference.type) {
        is Reference.Primitive.Type.Integer -> OpenAPIV30Schema(
            type = OpenAPIV30Type.INTEGER.asTypeDefinition(),
            minimum = type.constraint?.min?.toDouble(),
            maximum = type.constraint?.max?.toDouble(),
            format = "int32",
        )

        is Reference.Primitive.Type.Number -> OpenAPIV30Schema(
            type = OpenAPIV30Type.NUMBER.asTypeDefinition(),
            minimum = type.constraint?.min?.toDouble(),
            maximum = type.constraint?.max?.toDouble(),
            format = "float",
        )

        is Reference.Primitive.Type.String -> OpenAPIV30Schema(
            type = OpenAPIV30Type.STRING.asTypeDefinition(),
            pattern = type.constraint?.value,
        )

        Reference.Primitive.Type.Boolean -> OpenAPIV30Schema(
            type = OpenAPIV30Type.BOOLEAN.asTypeDefinition(),
        )

        Reference.Primitive.Type.Bytes -> OpenAPIV30Schema(
            type = OpenAPIV30Type.STRING.asTypeDefinition(),
        )
    }

    private fun OpenAPIV30Type.asTypeDefinition(): OpenAPIV30TypeDefinition = OpenAPIV30SingleType(this)

    private fun Type.emit(): OpenAPIV30Schema = OpenAPIV30Schema(
        description = annotations.findDescription() ?: comment?.value,
        properties = shape.value.associate { it.emitSchema() },
        required = shape.value
            .filter { !it.reference.isNullable }
            .map { it.identifier.value }
            .takeIf { it.isNotEmpty() },
    )

    private fun Enum.emit(): OpenAPIV30Schema = OpenAPIV30Schema(
        description = annotations.findDescription() ?: comment?.value,
        type = OpenAPIV30Type.STRING.asTypeDefinition(),
        enum = entries.map { JsonPrimitive(it) },
    )

    private fun Union.emit(): OpenAPIV30Schema = OpenAPIV30Schema(
        description = annotations.findDescription() ?: comment?.value,
        type = OpenAPIV30Type.STRING.asTypeDefinition(),
        oneOf = entries.map { it.emitSchema() },
    )

    private fun List<Endpoint>.emit(method: Endpoint.Method): OpenAPIV30Operation? = filter { it.method == method }.map { it.emit() }.firstOrNull()

    private fun Endpoint.emit(): OpenAPIV30Operation = OpenAPIV30Operation(
        operationId = identifier.value,
        description = annotations.findDescription() ?: comment?.value,
        parameters = path.filterIsInstance<Endpoint.Segment.Param>()
            .map { it.emitParameter() } + queries.map { it.emitParameter(OpenAPIV30ParameterLocation.QUERY) } + headers.map {
            it.emitParameter(
                OpenAPIV30ParameterLocation.HEADER,
            )
        },
        requestBody = requests.mapNotNull { it.content?.emit() }
            .toMap()
            .takeIf { it.isNotEmpty() }
            ?.let { content ->
                OpenAPIV30RequestBody(
                    content = content,
                    required = !requests.any { it.content?.reference?.isNullable == true },
                )
            },
        responses = responses
            .groupBy { it.status }
            .map { (statusCode, res) ->
                StatusCode(statusCode) to OpenAPIV30Response(
                    headers = res.flatMap { it.headers }.associate { it.emitHeader() },
                    description = res.first().annotations.findDescription()
                        ?: "${identifier.value} $statusCode response",
                    content = res
                        .mapNotNull { it.content }
                        .associate { it.emit() }
                        .ifEmpty { null },
                    links = res.flatMap { it.annotations.findLinks() }.toOpenAPIV30Links(),
                )
            }
            .toMap(),
    )

    private fun List<LinkInfo>.toOpenAPIV30Links(): OpenAPIV30Links? {
        if (isEmpty()) return null
        val byName = LinkedHashMap<String, OpenAPIV30LinkOrReference>()
        forEach { info ->
            byName[info.name] = OpenAPIV30Link(
                operationRef = info.operationRef,
                operationId = info.operationId,
                parameters = info.parameters
                    .mapValues<String, String, kotlinx.serialization.json.JsonElement> { JsonPrimitive(it.value) }
                    .ifEmpty { null },
                requestBody = info.requestBody?.let { JsonPrimitive(it) },
                description = info.description,
            )
        }
        return OpenAPIV30Links(byName.entries.toSet())
    }

    private fun List<Endpoint.Segment>.emitSegment() = "/" + joinToString("/") {
        when (it) {
            is Endpoint.Segment.Param -> "{${it.identifier.value}}"
            is Endpoint.Segment.Literal -> it.value
        }
    }

    private fun Field.emitParameter(location: OpenAPIV30ParameterLocation): OpenAPIV30Parameter = OpenAPIV30Parameter(
        `in` = location,
        name = identifier.value,
        schema = reference.emitSchema(),
        description = annotations.findDescription(),
        required = !reference.isNullable,
    )

    private fun Endpoint.Segment.Param.emitParameter(): OpenAPIV30Parameter = OpenAPIV30Parameter(
        `in` = OpenAPIV30ParameterLocation.PATH,
        name = identifier.value,
        schema = reference.emitSchema(),
        required = !reference.isNullable,
    )

    private fun Field.emitHeader(): Pair<String, OpenAPIV30HeaderOrReference> = identifier.value to reference.emitHeader(annotations.findDescription())

    private fun Field.emitSchema(): Pair<String, OpenAPIV30SchemaOrReference> = identifier.value to reference.emitSchema().let {
        when (it) {
            is OpenAPIV30Schema -> it.copy(description = annotations.findDescription())
            is OpenAPIV30Reference -> it
        }
    }

    private fun Reference.emitHeader(description: String?) = when (this) {
        is Reference.Dict -> OpenAPIV30Reference(ref = Ref("#/components/headers/$value"))
        is Reference.Iterable -> OpenAPIV30Reference(ref = Ref("#/components/headers/$value"))
        is Reference.Custom -> OpenAPIV30Reference(ref = Ref("#/components/headers/$value"))
        is Reference.Primitive -> OpenAPIV30Header(schema = emitSchema(), description = description)
        is Reference.Any -> error("Cannot map Any")
        is Reference.Unit -> error("Cannot map Unit")
    }

    private fun Reference.emitSchema(): OpenAPIV30SchemaOrReference = when (this) {
        is Reference.Dict -> OpenAPIV30Schema(
            nullable = reference.isNullable,
            type = OpenAPIV30Type.OBJECT.asTypeDefinition(),
            additionalProperties = reference.emitSchema() as OpenAPIV30SchemaOrReferenceOrBoolean,
        )

        is Reference.Iterable -> OpenAPIV30Schema(
            nullable = reference.isNullable,
            type = OpenAPIV30Type.ARRAY.asTypeDefinition(),
            items = reference.emitSchema(),
        )

        is Reference.Custom -> OpenAPIV30Reference(ref = Ref("#/components/schemas/$value"))
        is Reference.Primitive -> OpenAPIV30Schema(
            type = type.emitType().asTypeDefinition(),
            format = emitFormat(),
            pattern = emitPattern(),
            minimum = emitMinimum(),
            maximum = emitMaximum(),
        )

        is Reference.Any -> error("Cannot map Any")
        is Reference.Unit -> error("Cannot map Unit")
    }

    private fun Reference.Primitive.Type.emitType(): OpenAPIV30Type = when (this) {
        is Reference.Primitive.Type.String -> OpenAPIV30Type.STRING
        is Reference.Primitive.Type.Integer -> OpenAPIV30Type.INTEGER
        is Reference.Primitive.Type.Number -> OpenAPIV30Type.NUMBER
        is Reference.Primitive.Type.Boolean -> OpenAPIV30Type.BOOLEAN
        is Reference.Primitive.Type.Bytes -> OpenAPIV30Type.STRING
    }

    private fun Endpoint.Content.emit(): Pair<MediaType, OpenAPIV30MediaType> = MediaType(type) to OpenAPIV30MediaType(
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
