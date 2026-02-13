package community.flock.wirespec.openapi.common

import community.flock.wirespec.compiler.core.emit.LanguageEmitter.Companion.firstToUpper
import community.flock.wirespec.compiler.core.parse.ast.Annotation
import community.flock.wirespec.compiler.core.parse.ast.Definition
import community.flock.wirespec.compiler.core.parse.ast.DefinitionIdentifier
import community.flock.wirespec.compiler.core.parse.ast.Endpoint
import kotlinx.serialization.json.Json

fun className(vararg arg: String) = arg
    .flatMap { it.split("-", "/") }
    .joinToString("") { it.firstToUpper() }

fun <K, V> Map<K, V?>.filterNotNullValues(): Map<K, V> = mapNotNull { (key, value) -> value?.let { key to it } }.toMap()

fun List<Annotation>.findDescription(): String? = find { it.name == "Description" }
    ?.parameters
    ?.find { it.name == "default" }
    ?.value
    ?.let { (it as? Annotation.Value.Single) }
    ?.value

fun String?.toDescriptionAnnotationList() = this?.let(::toDescriptionAnnotation)?.let(::listOf).orEmpty()

private fun toDescriptionAnnotation(description: String): Annotation = Annotation(
    "Description",
    Annotation.Parameter("default", Annotation.Value.Single(description)).let(::listOf),
)

fun List<Definition>.resolveEndpointNameCollisions(): List<Definition> {
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

val json = Json { prettyPrint = true }

const val APPLICATION_JSON = "application/json"
