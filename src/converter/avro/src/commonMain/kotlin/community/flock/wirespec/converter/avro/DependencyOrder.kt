package community.flock.wirespec.converter.avro

import community.flock.wirespec.compiler.core.parse.ast.Definition
import community.flock.wirespec.compiler.core.parse.ast.Module
import community.flock.wirespec.compiler.core.parse.ast.Reference
import community.flock.wirespec.compiler.core.parse.ast.Refined
import community.flock.wirespec.compiler.core.parse.ast.Type
import community.flock.wirespec.compiler.core.parse.ast.Union
import community.flock.wirespec.compiler.core.parse.ast.fields

/**
 * The module's definitions with every definition placed after the ones it refers to, otherwise in
 * source order: Avro only resolves a named type by name once it has been defined. A cycle keeps the
 * definition it was entered through first, which suits Avro's recursive records.
 */
internal fun Module.inDependencyOrder(): List<Definition> {
    val byName = statements.toList().associateBy { it.identifier.value }

    fun Definition.dependencies(): List<Definition> = when (this) {
        is Type -> shape.fields.flatMap { it.reference.customNames() }
        is Union -> entries.flatMap { it.customNames() }
        else -> emptyList()
    }.mapNotNull(byName::get)

    fun Ordering.visit(definition: Definition): Ordering = when (definition.identifier.value) {
        in seen -> this
        else -> definition.dependencies()
            .fold(copy(seen = seen + definition.identifier.value)) { ordering, dependency -> ordering.visit(dependency) }
            .let { it.copy(ordered = it.ordered + definition) }
    }

    return statements.toList().fold(Ordering()) { ordering, definition -> ordering.visit(definition) }.ordered
}

internal fun Module.findRefined(name: String): Refined? = statements.toList().filterIsInstance<Refined>().find { it.identifier.value == name }

private data class Ordering(val seen: Set<String> = emptySet(), val ordered: List<Definition> = emptyList())

private fun Reference.customNames(): List<String> = when (this) {
    is Reference.Custom -> listOf(value)
    is Reference.Iterable -> reference.customNames()
    is Reference.Dict -> reference.customNames()
    is Reference.Primitive, is Reference.Any, is Reference.Unit -> emptyList()
}
