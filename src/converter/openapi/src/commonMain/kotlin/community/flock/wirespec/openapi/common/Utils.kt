package community.flock.wirespec.openapi.common

import community.flock.wirespec.compiler.core.emit.LanguageEmitter.Companion.firstToUpper
import community.flock.wirespec.compiler.core.parse.ast.Annotation
import community.flock.wirespec.compiler.core.parse.ast.Definition
import community.flock.wirespec.compiler.core.parse.ast.DefinitionIdentifier
import community.flock.wirespec.compiler.core.parse.ast.Endpoint
import kotlinx.serialization.json.Json

internal fun className(vararg arg: String): String = arg
    .flatMap { it.split("-", "/") }
    .joinToString("") { it.firstToUpper() }

internal fun <K, V> Map<K, V?>.filterNotNullValues(): Map<K, V> = mapNotNull { (key, value) -> value?.let { key to it } }.toMap()

internal fun List<Annotation>.findDescription(): String? = find { it.name == "Description" }
    ?.parameters
    ?.find { it.name == "default" }
    ?.value
    ?.let { (it as? Annotation.Value.Single) }
    ?.value

internal fun String?.toDescriptionAnnotationList() = this?.let(::toDescriptionAnnotation)?.let(::listOf).orEmpty()

private fun toDescriptionAnnotation(description: String): Annotation = Annotation(
    "Description",
    Annotation.Parameter("default", Annotation.Value.Single(description)).let(::listOf),
)

private const val LINK_ANNOTATION_NAME = "Link"

internal data class LinkInfo(
    val name: String,
    val operationId: String?,
    val operationRef: String?,
    val parameters: Map<String, String>,
    val requestBody: String?,
    val description: String?,
    val serverUrl: String?,
)

internal fun List<Annotation>.findLinks(): List<LinkInfo> = filter { it.name == LINK_ANNOTATION_NAME }
    .mapNotNull { it.toLinkInfo() }

internal fun LinkInfo.toAnnotation(): Annotation {
    val params = mutableListOf(
        Annotation.Parameter("default", Annotation.Value.Single(name)),
    )
    operationId?.let { params += Annotation.Parameter("operationId", Annotation.Value.Single(it)) }
    operationRef?.let { params += Annotation.Parameter("operationRef", Annotation.Value.Single(it)) }
    if (parameters.isNotEmpty()) {
        params += Annotation.Parameter(
            "parameters",
            Annotation.Value.Dict(
                parameters.map { (k, v) -> Annotation.Parameter(k, Annotation.Value.Single(v)) },
            ),
        )
    }
    requestBody?.let { params += Annotation.Parameter("requestBody", Annotation.Value.Single(it)) }
    description?.let { params += Annotation.Parameter("description", Annotation.Value.Single(it)) }
    serverUrl?.let { params += Annotation.Parameter("server", Annotation.Value.Single(it)) }
    return Annotation(LINK_ANNOTATION_NAME, params)
}

private fun Annotation.toLinkInfo(): LinkInfo? {
    val name = parameters.singleParam("default") ?: return null
    return LinkInfo(
        name = name,
        operationId = parameters.singleParam("operationId"),
        operationRef = parameters.singleParam("operationRef"),
        parameters = parameters.dictParam("parameters"),
        requestBody = parameters.singleParam("requestBody"),
        description = parameters.singleParam("description"),
        serverUrl = parameters.singleParam("server"),
    )
}

private fun List<Annotation.Parameter>.singleParam(name: String): String? = find { it.name == name }
    ?.value
    ?.let { it as? Annotation.Value.Single }
    ?.value

private fun List<Annotation.Parameter>.dictParam(name: String): Map<String, String> = find { it.name == name }
    ?.value
    ?.let { it as? Annotation.Value.Dict }
    ?.value
    ?.mapNotNull { entry ->
        (entry.value as? Annotation.Value.Single)?.let { entry.name to it.value }
    }
    ?.toMap()
    .orEmpty()

internal fun List<Definition>.resolveEndpointNameCollisions(): List<Definition> {
    val nonEndpointNames = filterNot { it is Endpoint }
        .map { it.identifier.value }
        .toSet()
    return map { definition ->
        if (definition is Endpoint && definition.identifier.value in nonEndpointNames) {
            definition.copy(identifier = DefinitionIdentifier(definition.identifier.value + "Endpoint"))
        } else {
            definition
        }
    }
}

internal val json: Json = Json { prettyPrint = true }

internal const val APPLICATION_JSON: String = "application/json"
