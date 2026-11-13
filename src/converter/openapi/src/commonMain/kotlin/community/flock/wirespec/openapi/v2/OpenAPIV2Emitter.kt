package community.flock.wirespec.openapi.v2

import arrow.core.NonEmptyList
import arrow.core.nonEmptyListOf
import community.flock.kotlinx.openapi.bindings.OpenAPIV20Header
import community.flock.kotlinx.openapi.bindings.OpenAPIV20Model
import community.flock.kotlinx.openapi.bindings.OpenAPIV20Operation
import community.flock.kotlinx.openapi.bindings.OpenAPIV20Parameter
import community.flock.kotlinx.openapi.bindings.OpenAPIV20ParameterLocation
import community.flock.kotlinx.openapi.bindings.OpenAPIV20PathItem
import community.flock.kotlinx.openapi.bindings.OpenAPIV20Reference
import community.flock.kotlinx.openapi.bindings.OpenAPIV20Response
import community.flock.kotlinx.openapi.bindings.OpenAPIV20Schema
import community.flock.kotlinx.openapi.bindings.OpenAPIV20SchemaOrReference
import community.flock.kotlinx.openapi.bindings.OpenAPIV20SecurityScheme
import community.flock.kotlinx.openapi.bindings.OpenAPIV20SecuritySchemeType
import community.flock.kotlinx.openapi.bindings.OpenAPIV20Type
import community.flock.kotlinx.openapi.bindings.OpenAPIV2Model
import community.flock.kotlinx.openapi.bindings.Path
import community.flock.kotlinx.openapi.bindings.Ref
import community.flock.kotlinx.openapi.bindings.StatusCode
import community.flock.wirespec.compiler.core.emit.Emitted
import community.flock.wirespec.compiler.core.emit.Emitter
import community.flock.wirespec.compiler.core.emit.FileExtension
import community.flock.wirespec.compiler.core.emit.SpecInfo
import community.flock.wirespec.compiler.core.emit.SpecInfoEmitter
import community.flock.wirespec.compiler.core.exceptions.EmitterException
import community.flock.wirespec.compiler.core.parse.ast.AST
import community.flock.wirespec.compiler.core.parse.ast.Endpoint
import community.flock.wirespec.compiler.core.parse.ast.Enum
import community.flock.wirespec.compiler.core.parse.ast.Field
import community.flock.wirespec.compiler.core.parse.ast.Reference
import community.flock.wirespec.compiler.core.parse.ast.Refined
import community.flock.wirespec.compiler.core.parse.ast.SpecAnnotation
import community.flock.wirespec.compiler.core.parse.ast.Statements
import community.flock.wirespec.compiler.core.parse.ast.Type
import community.flock.wirespec.compiler.core.parse.ast.fields
import community.flock.wirespec.compiler.utils.Logger
import community.flock.wirespec.openapi.common.APPLICATION_JSON
import community.flock.wirespec.openapi.common.SecurityRequirement
import community.flock.wirespec.openapi.common.SecuritySchemeInfo
import community.flock.wirespec.openapi.common.ServerInfo
import community.flock.wirespec.openapi.common.SpecMetadata
import community.flock.wirespec.openapi.common.emitFormat
import community.flock.wirespec.openapi.common.emitPattern
import community.flock.wirespec.openapi.common.filterNotNullValues
import community.flock.wirespec.openapi.common.findDescription
import community.flock.wirespec.openapi.common.json
import community.flock.wirespec.openapi.common.openAPIProblems
import community.flock.wirespec.openapi.common.securityRequirements
import community.flock.wirespec.openapi.common.tags
import community.flock.wirespec.openapi.common.toSchemaDefault
import community.flock.wirespec.openapi.common.toSpecMetadata
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonPrimitive

public object OpenAPIV2Emitter : SpecInfoEmitter {

    override val extension: FileExtension = FileExtension.JSON

    override val supportsDefaults: Boolean = true

    override fun emit(
        ast: AST,
        logger: Logger,
    ): NonEmptyList<Emitted> = emit(ast, SpecInfo(), logger)

    override fun withSpecInfo(info: SpecInfo): Emitter = object : SpecInfoEmitter by this {
        override fun emit(ast: AST, logger: Logger): NonEmptyList<Emitted> = emit(ast, info, logger)
    }

    private fun emit(ast: AST, info: SpecInfo, logger: Logger): NonEmptyList<Emitted> = Emitted(
        "OpenAPI.${extension.value}",
        json.encodeToString(emitSwaggerObject(ast, logger, info)),
    ).let { nonEmptyListOf(it) }

    override fun validate(ast: AST): List<EmitterException> = ast.openAPIProblems()

    /** The document for every module of [ast], with the metadata of its `@spec:` annotations. */
    /** [info] sets the title and version instead of `@spec:OpenAPIInfo`. */
    public fun emitSwaggerObject(ast: AST, logger: Logger, info: SpecInfo = SpecInfo()): OpenAPIV2Model = ast.modules
        .flatMap { module ->
            logger.info("Combining Nodes from ${module.fileUri.value} ")
            module.statements
        }
        .let { emitSwaggerObject(it, ast.annotations.toSpecMetadata(), info, logger) }

    public fun emitSwaggerObject(statements: Statements, logger: Logger): OpenAPIV2Model = emitSwaggerObject(statements, emptyList<SpecAnnotation>().toSpecMetadata(), SpecInfo(), logger)

    private fun emitSwaggerObject(statements: Statements, spec: SpecMetadata, info: SpecInfo, logger: Logger): OpenAPIV2Model = spec.securitySchemes
        .associate { it.name to it.emit(logger) }
        .let { schemes ->
            val unsupported = schemes.filterValues { it == null }.keys
            spec.servers.emit(logger).let { server ->
                OpenAPIV20Model(
                    swagger = "2.0",
                    info = spec.toInfo(title = info.title, version = info.version),
                    host = server?.host,
                    basePath = server?.basePath,
                    schemes = server?.schemes,
                    consumes = listOf(APPLICATION_JSON),
                    produces = listOf(APPLICATION_JSON),
                    paths = statements.emitPaths(unsupported, logger),
                    definitions = statements.emitDefinitions(logger),
                    securityDefinitions = schemes.filterNotNullValues().ifEmpty { null },
                    security = spec.security?.withoutSchemes(unsupported, "the spec", logger),
                    tags = spec.tags.ifEmpty { null },
                )
            }
        }

    /** Swagger 2 has one host and base path, served over one or more schemes. */
    private data class SwaggerServer(val host: String?, val basePath: String?, val schemes: List<String>?)

    private fun List<ServerInfo>.emit(logger: Logger): SwaggerServer? = map { it.resolvedUrl() }.let { urls ->
        urls.firstOrNull()?.let { first ->
            urls.partition { it.substringAfter("://") == first.substringAfter("://") }.let { (sameHost, otherHosts) ->
                otherHosts.forEach { logger.warn("Swagger 2 has one host and base path, so the server $it is left out") }
                first.substringAfter("://", missingDelimiterValue = first).let { address ->
                    SwaggerServer(
                        host = address.substringBefore('/').ifEmpty { null },
                        basePath = address.drop(address.substringBefore('/').length).ifEmpty { null },
                        schemes = sameHost.mapNotNull { url -> url.substringBefore("://", missingDelimiterValue = "").ifEmpty { null } }.distinct().ifEmpty { null },
                    )
                }
            }
        }
    }

    /** Swagger 2 has no server variables, so a variable is replaced by its default. */
    private fun ServerInfo.resolvedUrl(): String = variables.entries.fold(url) { resolved, (name, variable) -> resolved.replace("{$name}", variable.default.orEmpty()) }

    private fun SecuritySchemeInfo.emit(logger: Logger): OpenAPIV20SecurityScheme? = when (type) {
        "apiKey" -> when (location) {
            "query", "header" -> OpenAPIV20SecurityScheme(type = OpenAPIV20SecuritySchemeType.API_KEY, description = description, name = parameterName, `in` = location)
            else -> null.also { logger.warn("Swagger 2 has no API keys in a $location, so the security scheme $name is left out") }
        }

        "http" -> when (scheme?.lowercase()) {
            "basic" -> OpenAPIV20SecurityScheme(type = OpenAPIV20SecuritySchemeType.BASIC, description = description)
            // Swagger 2 has no bearer scheme; the usual stand-in is an API key in the Authorization header.
            else -> OpenAPIV20SecurityScheme(
                type = OpenAPIV20SecuritySchemeType.API_KEY,
                description = description ?: "A ${scheme ?: "bearer"} token${bearerFormat?.let { " ($it)" }.orEmpty()} in the Authorization header: ${scheme?.replaceFirstChar(Char::uppercase) ?: "Bearer"} <token>",
                name = "Authorization",
                `in` = "header",
            ).also { logger.warn("Swagger 2 has no ${scheme ?: "bearer"} scheme, so the security scheme $name is written as an API key in the Authorization header") }
        }

        "oauth2" -> OAUTH2_FLOWS.firstNotNullOfOrNull { (flow, swaggerFlow) -> flows[flow]?.let { swaggerFlow to it } }?.let { (swaggerFlow, flow) ->
            OpenAPIV20SecurityScheme(
                type = OpenAPIV20SecuritySchemeType.OAUTH2,
                description = description,
                flow = swaggerFlow,
                authorizationUrl = flow.authorizationUrl,
                tokenUrl = flow.tokenUrl,
                scopes = flow.scopes,
            ).also { if (flows.size > 1) logger.warn("Swagger 2 has one flow per security scheme, so the security scheme $name keeps its $swaggerFlow flow only") }
        }

        else -> null.also { logger.warn("Swagger 2 has no $type security schemes, so the security scheme $name is left out") }
    }

    /** OpenAPI 3 flows and their Swagger 2 names, in the order the first one present is chosen. */
    private val OAUTH2_FLOWS = listOf("implicit" to "implicit", "password" to "password", "clientCredentials" to "application", "authorizationCode" to "accessCode")

    /** Leaves out the requirements that need one of the [unsupportedSchemes], which Swagger 2 cannot describe. */
    private fun List<SecurityRequirement>.withoutSchemes(unsupportedSchemes: Set<String>, where: String, logger: Logger): List<SecurityRequirement>? = partition { it.keys.none(unsupportedSchemes::contains) }
        .let { (supported, unsupported) ->
            unsupported.forEach { logger.warn("The security requirement ${it.keys.joinToString(" and ")} of $where needs a scheme Swagger 2 cannot describe, so it is left out") }
            supported.takeIf { it.isNotEmpty() || unsupported.isEmpty() }
        }

    private fun Statements.emitDefinitions(logger: Logger): Map<String, OpenAPIV20Schema> {
        val refined = filterIsInstance<Refined>()
            .associate { refined ->
                when (val type = refined.reference.type) {
                    Reference.Primitive.Type.Boolean ->
                        refined.identifier.value to OpenAPIV20Schema(
                            type = OpenAPIV20Type.BOOLEAN,
                        )

                    Reference.Primitive.Type.Bytes ->
                        refined.identifier.value to OpenAPIV20Schema(
                            type = OpenAPIV20Type.FILE,
                        )

                    is Reference.Primitive.Type.Integer ->
                        refined.identifier.value to OpenAPIV20Schema(
                            type = OpenAPIV20Type.INTEGER,
                            minimum = type.constraint?.min?.toDouble(),
                            maximum = type.constraint?.max?.toDouble(),
                            format = "int32",
                        )

                    is Reference.Primitive.Type.Number ->
                        refined.identifier.value to OpenAPIV20Schema(
                            type = OpenAPIV20Type.NUMBER,
                            minimum = type.constraint?.min?.toDouble(),
                            maximum = type.constraint?.max?.toDouble(),
                            format = "float",
                        )

                    is Reference.Primitive.Type.String ->
                        refined.identifier.value to when (val pattern = type.constraint) {
                            is Reference.Primitive.Type.Constraint.RegExp -> OpenAPIV20Schema(
                                type = OpenAPIV20Type.STRING,
                                format = pattern.emitFormat(),
                                pattern = pattern.emitPattern(),
                            )

                            null -> OpenAPIV20Schema(
                                type = OpenAPIV20Type.STRING,
                            )
                        }
                }
                    .also { logger.info("Emitting Refined ${refined.identifier.value}") }
            }

        val types = filterIsInstance<Type>()
            .associate { type ->
                type.identifier.value to OpenAPIV20Schema(
                    description = type.annotations.findDescription() ?: type.comment?.value,
                    properties = type.shape.fields.associate { it.toProperties() },
                    required = type.shape.fields
                        .filter { !it.reference.isNullable }
                        .map { it.identifier.value }
                        .takeIf { it.isNotEmpty() },
                )
                    .also { logger.info("Emitting Type ${type.identifier.value}") }
            }

        val enums = filterIsInstance<Enum>()
            .associate { enum ->
                enum.identifier.value to OpenAPIV20Schema(
                    type = OpenAPIV20Type.STRING,
                    enum = enum.entries.map { JsonPrimitive(it) },
                    description = enum.annotations.findDescription(),
                )
                    .also { logger.info("Emitting Enum ${enum.identifier.value}") }
            }

        return refined + types + enums
    }

    private fun Statements.emitPaths(unsupportedSchemes: Set<String>, logger: Logger): Map<Path, OpenAPIV20PathItem> = filterIsInstance<Endpoint>()
        .groupBy { it.path }
        .map { (path, endpoints) ->
            logger.info("Emitting endpoints for path ${path.emitSegment()}")
            Path(path.emitSegment()) to OpenAPIV20PathItem(
                parameters = path.filterIsInstance<Endpoint.Segment.Param>().map {
                    OpenAPIV20Parameter(
                        `in` = OpenAPIV20ParameterLocation.PATH,
                        name = it.identifier.value,
                        type = it.reference.emitType(),
                        format = it.reference.emitFormat(),
                        pattern = it.reference.emitPattern(),
                        minimum = it.reference.emitMinimum(),
                        maximum = it.reference.emitMaximum(),
                    )
                },
                get = endpoints.emit(Endpoint.Method.GET, unsupportedSchemes, logger),
                post = endpoints.emit(Endpoint.Method.POST, unsupportedSchemes, logger),
                put = endpoints.emit(Endpoint.Method.PUT, unsupportedSchemes, logger),
                delete = endpoints.emit(Endpoint.Method.DELETE, unsupportedSchemes, logger),
                patch = endpoints.emit(Endpoint.Method.PATCH, unsupportedSchemes, logger),
                options = endpoints.emit(Endpoint.Method.OPTIONS, unsupportedSchemes, logger),
                trace = endpoints.emit(Endpoint.Method.TRACE, unsupportedSchemes, logger),
                head = endpoints.emit(Endpoint.Method.HEAD, unsupportedSchemes, logger),
            )
        }.toMap()

    private fun Field.toProperties(): Pair<String, OpenAPIV20SchemaOrReference> = identifier.value to reference.toSchemaOrReference().let {
        when (it) {
            is OpenAPIV20Schema -> it.copy(description = annotations.findDescription(), default = defaultValue.toSchemaDefault())
            is OpenAPIV20Reference -> it
        }
    }

    private fun List<Endpoint>.emit(method: Endpoint.Method, unsupportedSchemes: Set<String>, logger: Logger): OpenAPIV20Operation? = filter { it.method == method }.map { it.emit(unsupportedSchemes, logger) }.firstOrNull()

    private fun Endpoint.emit(unsupportedSchemes: Set<String>, logger: Logger) = OpenAPIV20Operation(
        tags = annotations.tags().ifEmpty { null },
        operationId = identifier.value,
        description = annotations.findDescription() ?: comment?.value,
        security = annotations.securityRequirements()?.withoutSchemes(unsupportedSchemes, identifier.value, logger),
        consumes = requests.mapNotNull { it.content?.type }.distinct().ifEmpty { null },
        produces = responses.mapNotNull { it.content?.type }.distinct().ifEmpty { null },
        parameters = requests
            .mapNotNull { it.content }
            .take(1)
            .map {
                OpenAPIV20Parameter(
                    `in` = OpenAPIV20ParameterLocation.BODY,
                    name = "RequestBody",
                    schema = it.reference.toSchemaOrReference(),
                    required = !it.reference.isNullable,
                )
            } + queries.fields.map { it.emitParameter(OpenAPIV20ParameterLocation.QUERY) } + headers.fields.map {
            it.emitParameter(
                OpenAPIV20ParameterLocation.HEADER,
            )
        },
        responses = responses
            .associate { response ->
                StatusCode(response.status) to OpenAPIV20Response(
                    description = response.annotations.findDescription()
                        ?: "${identifier.value} ${response.status} response",
                    headers = response.headers.fields.associate {
                        it.identifier.value to OpenAPIV20Header(
                            description = it.annotations.findDescription(),
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
                                is Reference.Iterable -> OpenAPIV20Schema(
                                    type = OpenAPIV20Type.ARRAY,
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

    private fun Field.emitParameter(location: OpenAPIV20ParameterLocation) = OpenAPIV20Parameter(
        `in` = location,
        name = identifier.value,
        type = reference.emitType(),
        format = reference.emitFormat(),
        pattern = reference.emitPattern(),
        items = when (val ref = reference) {
            is Reference.Iterable -> when (val emit = ref.toSchemaOrReference()) {
                is OpenAPIV20Reference -> emit
                is OpenAPIV20Schema -> emit.items
            }

            else -> null
        },
        required = !reference.isNullable,
        description = annotations.findDescription(),
    )

    private fun Reference.toSchemaOrReference(): OpenAPIV20SchemaOrReference = when (this) {
        is Reference.Dict -> OpenAPIV20Schema(
            type = OpenAPIV20Type.OBJECT,
            items = reference.toSchemaOrReference(),
        )

        is Reference.Iterable -> OpenAPIV20Schema(
            type = OpenAPIV20Type.ARRAY,
            items = reference.toSchemaOrReference(),
        )

        is Reference.Custom -> OpenAPIV20Reference(ref = Ref("#/definitions/$value"))
        is Reference.Primitive -> OpenAPIV20Schema(
            type = type.emitType(),
            format = emitFormat(),
            pattern = emitPattern(),
        )

        is Reference.Any -> error("Cannot map Any")
        is Reference.Unit -> error("Cannot map Unit")
    }

    private fun Reference.Primitive.Type.emitType(): OpenAPIV20Type = when (this) {
        is Reference.Primitive.Type.String -> OpenAPIV20Type.STRING
        is Reference.Primitive.Type.Integer -> OpenAPIV20Type.INTEGER
        is Reference.Primitive.Type.Number -> OpenAPIV20Type.NUMBER
        is Reference.Primitive.Type.Boolean -> OpenAPIV20Type.BOOLEAN
        is Reference.Primitive.Type.Bytes -> OpenAPIV20Type.STRING
    }

    private fun Reference.emitType(): OpenAPIV20Type = when (this) {
        is Reference.Dict -> OpenAPIV20Type.OBJECT
        is Reference.Iterable -> OpenAPIV20Type.ARRAY
        is Reference.Primitive -> type.emitType()
        is Reference.Custom -> OpenAPIV20Type.OBJECT
        is Reference.Any -> OpenAPIV20Type.OBJECT
        is Reference.Unit -> OpenAPIV20Type.OBJECT
    }

    private fun Reference.emitPattern() = when (this) {
        is Reference.Primitive -> when (val t = type) {
            is Reference.Primitive.Type.String -> when (val p = t.constraint) {
                is Reference.Primitive.Type.Constraint.RegExp -> p.emitPattern()
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
