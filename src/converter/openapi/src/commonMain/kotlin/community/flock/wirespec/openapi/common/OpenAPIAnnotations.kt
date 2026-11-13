package community.flock.wirespec.openapi.common

import community.flock.kotlinx.openapi.bindings.ContactObject
import community.flock.kotlinx.openapi.bindings.InfoObject
import community.flock.kotlinx.openapi.bindings.LicenseObject
import community.flock.kotlinx.openapi.bindings.TagObject
import community.flock.wirespec.compiler.core.FileUri
import community.flock.wirespec.compiler.core.exceptions.EmitterException
import community.flock.wirespec.compiler.core.parse.ast.AST
import community.flock.wirespec.compiler.core.parse.ast.Annotation
import community.flock.wirespec.compiler.core.parse.ast.Endpoint
import community.flock.wirespec.compiler.core.parse.ast.HasAnnotations
import community.flock.wirespec.compiler.core.parse.ast.SpecAnnotation

/**
 * The annotations that carry what Wirespec has no syntax for, so an OpenAPI document survives the conversion.
 *
 * [INFO] and [SECURITY_SCHEME] describe the document and only go on the spec (`@spec:`); [NO_SECURITY] and [LINK]
 * only go on an endpoint or a response. [SERVER], [SECURITY] and [TAG] go on both: on the spec they describe the
 * document, on an endpoint the operation.
 */
internal object OpenAPIAnnotations {
    const val DESCRIPTION = "Description"
    const val INFO = "OpenAPIInfo"
    const val SERVER = "OpenAPIServer"
    const val SECURITY_SCHEME = "OpenAPISecurityScheme"
    const val SECURITY = "OpenAPISecurity"
    const val NO_SECURITY = "OpenAPINoSecurity"
    const val TAG = "OpenAPITag"
    const val LINK = "OpenAPILink"

    const val DEFAULT_PARAMETER = "default"

    /** The OpenAPI annotations that belong to the document: they are spec annotations or nothing. */
    val SPEC_ONLY: Set<String> = setOf(INFO, SECURITY_SCHEME)

    /** The OpenAPI annotations that belong to one operation or response: they are no spec annotations. */
    val NOT_ON_SPEC: Set<String> = setOf(NO_SECURITY, LINK)

    /** The scheme types whose requirements name scopes; for every other type the list of scopes is empty. */
    val SCOPED_SCHEME_TYPES: Set<String> = setOf("oauth2", "openIdConnect")
}

internal data class ServerInfo(val url: String, val description: String?, val variables: Map<String, ServerVariableInfo>)

internal data class ServerVariableInfo(val default: String?, val enum: List<String>?, val description: String?)

internal data class OAuthFlowInfo(val authorizationUrl: String?, val tokenUrl: String?, val refreshUrl: String?, val scopes: Map<String, String>)

internal data class SecuritySchemeInfo(
    val name: String,
    val type: String?,
    val description: String?,
    val scheme: String?,
    val bearerFormat: String?,
    val parameterName: String?,
    val location: String?,
    val flows: Map<String, OAuthFlowInfo>,
    val openIdConnectUrl: String?,
)

/** One way to meet the security of an operation: every scheme it names, each with the scopes it needs. */
internal typealias SecurityRequirement = Map<String, List<String>>

/** What the spec annotations say about the document, merged over the files in file order. */
internal data class SpecMetadata(
    val title: String?,
    val version: String?,
    val description: String?,
    val termsOfService: String?,
    val contact: ContactObject?,
    val license: LicenseObject?,
    val servers: List<ServerInfo>,
    val securitySchemes: List<SecuritySchemeInfo>,
    val security: List<SecurityRequirement>?,
    val tags: List<TagObject>,
) {
    fun toInfo(title: String? = null, version: String? = null): InfoObject = InfoObject(
        title = title ?: this.title ?: "Wirespec",
        version = version ?: this.version ?: "0.0.0",
        description = description,
        termsOfService = termsOfService,
        contact = contact,
        license = license,
    )
}

internal fun List<SpecAnnotation>.toSpecMetadata(): SpecMetadata = map { it.annotation }.let { annotations ->
    annotations.find { it.name == OpenAPIAnnotations.INFO }.let { info ->
        SpecMetadata(
            title = info?.string("title"),
            version = info?.string("version"),
            description = annotations.findDescription(),
            termsOfService = info?.string("termsOfService"),
            contact = info?.dict("contact")?.let { ContactObject(name = it.string("name"), url = it.string("url"), email = it.string("email")) },
            license = info?.dict("license")?.let { license -> license.string("name")?.let { LicenseObject(name = it, identifier = license.string("identifier"), url = license.string("url")) } },
            servers = annotations.servers().distinctBy { it.url },
            securitySchemes = annotations.named(OpenAPIAnnotations.SECURITY_SCHEME).mapNotNull { it.toSecurityScheme() }.distinctBy { it.name },
            security = firstOrNull { it.annotation.name == OpenAPIAnnotations.SECURITY }
                ?.fileUri
                ?.let { file -> filter { it.fileUri == file }.map { it.annotation }.securityRequirements() },
            tags = annotations.named(OpenAPIAnnotations.TAG)
                .mapNotNull { tag -> tag.positionalString()?.let { TagObject(name = it, description = tag.string("description")) } }
                .distinctBy { it.name },
        )
    }
}

internal fun List<Annotation>.servers(): List<ServerInfo> = named(OpenAPIAnnotations.SERVER).mapNotNull { it.toServer() }

internal fun List<Annotation>.tags(): List<String> = named(OpenAPIAnnotations.TAG).mapNotNull { it.positionalString() }

/** The security the annotations ask for: null when they say nothing, and no requirement at all for `@OpenAPINoSecurity`. */
internal fun List<Annotation>.securityRequirements(): List<SecurityRequirement>? = when {
    any { it.name == OpenAPIAnnotations.NO_SECURITY } -> emptyList()
    else -> named(OpenAPIAnnotations.SECURITY).mapNotNull { it.toSecurityRequirement() }.ifEmpty { null }
}

/** `@OpenAPISecurity("oauth", scopes: [...])` names one scheme, `["mtls", "bearerAuth"]` several, and `{ oauth: [...], mtls: [] }` several with scopes. */
private fun Annotation.toSecurityRequirement(): SecurityRequirement? = when (val value = positional()) {
    is Annotation.Value.Single -> mapOf(value.value to parameters.strings("scopes").orEmpty())
    is Annotation.Value.Array -> value.value.associate { it.value to emptyList() }
    is Annotation.Value.Dict -> value.value.associate { it.name to it.value.strings() }
    null -> null
}

private fun Annotation.toServer(): ServerInfo? = positionalString()?.let { url ->
    ServerInfo(
        url = url,
        description = string("description"),
        variables = parameters.dict("variables").orEmpty()
            .mapNotNull { variable ->
                (variable.value as? Annotation.Value.Dict)?.value?.let {
                    variable.name to ServerVariableInfo(default = it.string("default"), enum = it.strings("enum"), description = it.string("description"))
                }
            }
            .toMap(),
    )
}

private fun Annotation.toSecurityScheme(): SecuritySchemeInfo? = positionalString()?.let { name ->
    SecuritySchemeInfo(
        name = name,
        type = string("type"),
        description = string("description"),
        scheme = string("scheme"),
        bearerFormat = string("bearerFormat"),
        parameterName = string("name"),
        location = string("in"),
        flows = parameters.dict("flows").orEmpty()
            .mapNotNull { flow ->
                (flow.value as? Annotation.Value.Dict)?.value?.let {
                    flow.name to OAuthFlowInfo(
                        authorizationUrl = it.string("authorizationUrl"),
                        tokenUrl = it.string("tokenUrl"),
                        refreshUrl = it.string("refreshUrl"),
                        scopes = it.dict("scopes").orEmpty().mapNotNull { scope -> (scope.value as? Annotation.Value.Single)?.let { s -> scope.name to s.value } }.toMap(),
                    )
                }
            }
            .toMap(),
        openIdConnectUrl = string("openIdConnectUrl"),
    )
}

/** Why the spec cannot be emitted as an OpenAPI document; empty when it can. */
internal fun AST.openAPIProblems(): List<EmitterException> = annotations.specProblems() + annotations.securitySchemes().let { schemes ->
    annotations.flatMap { it.annotation.requirementProblems(schemes, it.fileUri, "the spec") } +
        modules.toList().flatMap { module ->
            module.annotations.filter { it.name.startsWith(OPENAPI_PREFIX) }.map {
                EmitterException(module.fileUri, "@file:${it.name} in ${module.fileUri.value}: an OpenAPI document has no metadata per file, so write @spec:${it.name}")
            } + module.statements.toList().flatMap { definition ->
                (definition as? HasAnnotations)?.annotations.orEmpty().definitionProblems(schemes, module.fileUri, definition.identifier.value, definition is Endpoint)
            }
        }
}

private const val OPENAPI_PREFIX = "OpenAPI"

private fun List<SpecAnnotation>.securitySchemes(): Map<String, SecuritySchemeInfo> = map { it.annotation }
    .named(OpenAPIAnnotations.SECURITY_SCHEME)
    .mapNotNull { it.toSecurityScheme() }
    .associateBy { it.name }

private fun List<SpecAnnotation>.specProblems(): List<EmitterException> = listOf(
    filter { it.annotation.name in OpenAPIAnnotations.NOT_ON_SPEC }.map {
        EmitterException(it.fileUri, "@spec:${it.annotation.name} in ${it.fileUri.value} does not apply to the spec: it goes on ${if (it.annotation.name == OpenAPIAnnotations.LINK) "a response" else "an endpoint"}")
    },
    listOf(OpenAPIAnnotations.INFO, OpenAPIAnnotations.DESCRIPTION).mapNotNull { name ->
        filter { it.annotation.name == name }.takeIf { it.size > 1 }?.let { declared ->
            EmitterException(declared.first().fileUri, "@spec:$name is declared ${declared.size} times, in ${declared.files()}: a spec has one")
        }
    },
    filter { it.annotation.name == OpenAPIAnnotations.SECURITY }.map { it.fileUri }.distinct().takeIf { it.size > 1 }?.let { files ->
        EmitterException(files.first(), "@spec:${OpenAPIAnnotations.SECURITY} is declared in ${files.joinToString(" and ") { it.value }}: the default security comes from one file, so that no file can add an alternative that weakens it")
    }.let { listOfNotNull(it) },
    declaredTwice(OpenAPIAnnotations.SECURITY_SCHEME, "security scheme"),
    declaredTwice(OpenAPIAnnotations.SERVER, "server"),
    declaredTwice(OpenAPIAnnotations.TAG, "tag"),
    filter { it.annotation.name == OpenAPIAnnotations.SECURITY_SCHEME }.mapNotNull { declared ->
        declared.annotation.schemeProblem()?.let { problem ->
            EmitterException(declared.fileUri, "@spec:${OpenAPIAnnotations.SECURITY_SCHEME}${declared.annotation.positionalString()?.let { "(\"$it\")" }.orEmpty()} in ${declared.fileUri.value} $problem")
        }
    },
    flatMap { declared -> declared.annotation.serverOrTagProblems().map { EmitterException(declared.fileUri, "@spec:$it in ${declared.fileUri.value}") } },
).flatten()

/** Spec annotations are unique, and so is what they declare by name: a server by its URL, a scheme or tag by its name. */
private fun List<SpecAnnotation>.declaredTwice(name: String, label: String): List<EmitterException> = filter { it.annotation.name == name }
    .groupBy { it.annotation.positionalString() }
    .filterKeys { it != null }
    .filterValues { it.size > 1 }
    .map { (key, declared) ->
        EmitterException(declared.last().fileUri, "The $label \"$key\" is declared ${declared.size} times, in ${declared.files()}: a spec declares each $label once")
    }

private fun List<SpecAnnotation>.files(): String = map { it.fileUri.value }.distinct().joinToString(" and ")

private fun Annotation.schemeProblem(): String? = when {
    positionalString() == null -> "has no name"
    else -> when (val type = string("type")) {
        null -> "has no type: use apiKey, http, oauth2 or openIdConnect"
        "http" -> "needs a scheme, such as bearer or basic".takeIf { string("scheme") == null }
        "apiKey" -> "needs a name and in: query, header or cookie".takeIf { string("name") == null || string("in") !in setOf("query", "header", "cookie") }
        "oauth2" -> "needs flows".takeIf { parameters.dict("flows").isNullOrEmpty() }
        "openIdConnect" -> "needs an openIdConnectUrl".takeIf { string("openIdConnectUrl") == null }
        else -> "has an unknown type $type: use apiKey, http, oauth2 or openIdConnect"
    }
}

private fun Annotation.serverOrTagProblems(): List<String> = when (name) {
    OpenAPIAnnotations.SERVER -> when (val server = toServer()) {
        null -> listOf("$name has no URL")
        else -> server.variables.filterValues { it.default == null }.keys.map { "$name(\"${server.url}\") has a variable $it without a default" }
    }
    OpenAPIAnnotations.TAG -> listOfNotNull("$name has no name".takeIf { positionalString() == null })
    else -> emptyList()
}

private fun Annotation.requirementProblems(schemes: Map<String, SecuritySchemeInfo>, file: FileUri, where: String): List<EmitterException> = takeIf { it.name == OpenAPIAnnotations.SECURITY }
    ?.toSecurityRequirement()
    .orEmpty()
    .mapNotNull { (scheme, scopes) ->
        when (val declared = schemes[scheme]) {
            null -> "@${OpenAPIAnnotations.SECURITY} on $where names the security scheme $scheme, which no @spec:${OpenAPIAnnotations.SECURITY_SCHEME} declares"
            else ->
                "@${OpenAPIAnnotations.SECURITY} on $where gives scopes for $scheme, but only oauth2 and openIdConnect schemes have scopes"
                    .takeIf { scopes.isNotEmpty() && declared.type !in OpenAPIAnnotations.SCOPED_SCHEME_TYPES }
        }
    }
    .map { EmitterException(file, "$it (${file.value})") }

private fun List<Annotation>.definitionProblems(schemes: Map<String, SecuritySchemeInfo>, file: FileUri, definition: String, isEndpoint: Boolean): List<EmitterException> = listOf(
    filter { it.name in OpenAPIAnnotations.SPEC_ONLY }.map { "@${it.name} on $definition describes the document: write it in the file header as @spec:${it.name}" },
    listOfNotNull(
        "$definition has both @${OpenAPIAnnotations.SECURITY} and @${OpenAPIAnnotations.NO_SECURITY}"
            .takeIf { isEndpoint && any { it.name == OpenAPIAnnotations.SECURITY } && any { it.name == OpenAPIAnnotations.NO_SECURITY } },
    ),
    flatMap { it.serverOrTagProblems() }.map { "@$it on $definition" },
).flatten().map { EmitterException(file, "$it (${file.value})") } +
    flatMap { it.requirementProblems(schemes, file, definition) }

private fun List<Annotation>.named(name: String): List<Annotation> = filter { it.name == name }

private fun Annotation.positional(): Annotation.Value? = parameters.find { it.name == OpenAPIAnnotations.DEFAULT_PARAMETER }?.value

private fun Annotation.positionalString(): String? = (positional() as? Annotation.Value.Single)?.value

private fun Annotation.string(name: String): String? = parameters.string(name)

private fun Annotation.dict(name: String): List<Annotation.Parameter>? = parameters.dict(name)

private fun List<Annotation.Parameter>.string(name: String): String? = (find { it.name == name }?.value as? Annotation.Value.Single)?.value

private fun List<Annotation.Parameter>.strings(name: String): List<String>? = find { it.name == name }?.value?.strings()

private fun List<Annotation.Parameter>.dict(name: String): List<Annotation.Parameter>? = (find { it.name == name }?.value as? Annotation.Value.Dict)?.value

private fun Annotation.Value.strings(): List<String> = when (this) {
    is Annotation.Value.Single -> listOf(value)
    is Annotation.Value.Array -> value.map { it.value }
    is Annotation.Value.Dict -> emptyList()
}

internal fun infoAnnotation(title: String, version: String, termsOfService: String?, contact: ContactObject?, license: LicenseObject?): Annotation = annotation(
    OpenAPIAnnotations.INFO,
    null,
    parameter("title", title),
    parameter("version", version),
    parameter("termsOfService", termsOfService),
    contact?.let { dict("contact", parameter("name", it.name), parameter("url", it.url), parameter("email", it.email)) },
    license?.let { dict("license", parameter("name", it.name), parameter("identifier", it.identifier), parameter("url", it.url)) },
)

internal fun ServerInfo.toAnnotation(): Annotation = annotation(
    OpenAPIAnnotations.SERVER,
    Annotation.Value.Single(url),
    parameter("description", description),
    variables.takeIf { it.isNotEmpty() }?.map { (name, variable) ->
        dict(
            name,
            parameter("default", variable.default),
            variable.enum?.let { Annotation.Parameter("enum", Annotation.Value.Array(it.map { Annotation.Value.Single(it) })) },
            parameter("description", variable.description),
        )
    }?.let { Annotation.Parameter("variables", Annotation.Value.Dict(it.filterNotNull())) },
)

internal fun SecuritySchemeInfo.toAnnotation(): Annotation = annotation(
    OpenAPIAnnotations.SECURITY_SCHEME,
    Annotation.Value.Single(name),
    parameter("type", type),
    parameter("description", description),
    parameter("scheme", scheme),
    parameter("bearerFormat", bearerFormat),
    parameter("name", parameterName),
    parameter("in", location),
    flows.takeIf { it.isNotEmpty() }?.map { (flow, info) ->
        dict(
            flow,
            parameter("authorizationUrl", info.authorizationUrl),
            parameter("tokenUrl", info.tokenUrl),
            parameter("refreshUrl", info.refreshUrl),
            Annotation.Parameter("scopes", Annotation.Value.Dict(info.scopes.map { (scope, description) -> Annotation.Parameter(scope, Annotation.Value.Single(description)) })),
        )
    }?.let { Annotation.Parameter("flows", Annotation.Value.Dict(it.filterNotNull())) },
    parameter("openIdConnectUrl", openIdConnectUrl),
)

/** One scheme is written by name, several without scopes as a list, and anything else as a dictionary of scopes. */
internal fun SecurityRequirement.toAnnotation(): Annotation = when {
    size == 1 -> entries.single().let { (scheme, scopes) ->
        annotation(
            OpenAPIAnnotations.SECURITY,
            Annotation.Value.Single(scheme),
            scopes.takeIf { it.isNotEmpty() }?.let { Annotation.Parameter("scopes", Annotation.Value.Array(it.map { Annotation.Value.Single(it) })) },
        )
    }
    values.all { it.isEmpty() } -> annotation(OpenAPIAnnotations.SECURITY, Annotation.Value.Array(keys.map { Annotation.Value.Single(it) }))
    else -> annotation(
        OpenAPIAnnotations.SECURITY,
        Annotation.Value.Dict(map { (scheme, scopes) -> Annotation.Parameter(scheme, Annotation.Value.Array(scopes.map { Annotation.Value.Single(it) })) }),
    )
}

/** The security of an operation: an empty list is a public operation, and null inherits the default of the document. */
internal fun List<SecurityRequirement>?.toOperationAnnotations(): List<Annotation> = when {
    this == null -> emptyList()
    isEmpty() -> listOf(Annotation(OpenAPIAnnotations.NO_SECURITY, emptyList()))
    else -> map { it.toAnnotation() }
}

internal fun tagAnnotation(name: String, description: String? = null): Annotation = annotation(OpenAPIAnnotations.TAG, Annotation.Value.Single(name), parameter("description", description))

private fun annotation(name: String, value: Annotation.Value?, vararg parameters: Annotation.Parameter?): Annotation = Annotation(
    name,
    listOfNotNull(value?.let { Annotation.Parameter(OpenAPIAnnotations.DEFAULT_PARAMETER, it) }) + parameters.filterNotNull(),
)

private fun parameter(name: String, value: String?): Annotation.Parameter? = value?.let { Annotation.Parameter(name, Annotation.Value.Single(it)) }

private fun dict(name: String, vararg entries: Annotation.Parameter?): Annotation.Parameter? = entries.filterNotNull()
    .takeIf { it.isNotEmpty() }
    ?.let { Annotation.Parameter(name, Annotation.Value.Dict(it)) }
