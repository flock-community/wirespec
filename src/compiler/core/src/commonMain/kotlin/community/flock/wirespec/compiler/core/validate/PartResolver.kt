package community.flock.wirespec.compiler.core.validate

import arrow.core.EitherNel
import arrow.core.NonEmptyList
import arrow.core.raise.Raise
import arrow.core.raise.either
import arrow.core.toNonEmptyListOrNull
import community.flock.wirespec.compiler.core.emit.flatten
import community.flock.wirespec.compiler.core.emit.importReferences
import community.flock.wirespec.compiler.core.exceptions.CyclicPartError
import community.flock.wirespec.compiler.core.exceptions.DuplicateFieldError
import community.flock.wirespec.compiler.core.exceptions.DuplicatePartError
import community.flock.wirespec.compiler.core.exceptions.PartAsReferenceError
import community.flock.wirespec.compiler.core.exceptions.SpreadNonPartError
import community.flock.wirespec.compiler.core.exceptions.WirespecException
import community.flock.wirespec.compiler.core.parse.ast.AST
import community.flock.wirespec.compiler.core.parse.ast.Channel
import community.flock.wirespec.compiler.core.parse.ast.Endpoint
import community.flock.wirespec.compiler.core.parse.ast.Enum
import community.flock.wirespec.compiler.core.parse.ast.Field
import community.flock.wirespec.compiler.core.parse.ast.Model
import community.flock.wirespec.compiler.core.parse.ast.Part
import community.flock.wirespec.compiler.core.parse.ast.Reference
import community.flock.wirespec.compiler.core.parse.ast.Refined
import community.flock.wirespec.compiler.core.parse.ast.Rpc
import community.flock.wirespec.compiler.core.parse.ast.ShapeEntry
import community.flock.wirespec.compiler.core.parse.ast.Spread
import community.flock.wirespec.compiler.core.parse.ast.Type
import community.flock.wirespec.compiler.core.parse.ast.Union

/**
 * Replaces every [Spread] with the fields of the part it names, so that everything downstream
 * of the parser only ever reads plain fields. Each stage assumes the previous one passed:
 * flattening is only safe once every spread names an existing part and no part reaches itself.
 */
internal object PartResolver {

    fun resolve(ast: AST): EitherNel<WirespecException, AST> = either {
        val parts = ast.parts.associateBy { it.identifier.value }
        raiseAll(ast.duplicatePartErrors() + ast.spreadNonPartErrors(parts.keys) + ast.partAsReferenceErrors(parts.keys))
        raiseAll(listOfNotNull(parts.cycleError()))
        raiseAll(ast.partShapes().flatMap { (owner, entries) -> parts.duplicateFieldErrors(owner, entries) })
        raiseAll(ast.definitionShapes().flatMap { (owner, entries) -> parts.duplicateFieldErrors(owner, entries) })
        parts.flatten(ast)
    }

    private fun Raise<NonEmptyList<WirespecException>>.raiseAll(errors: List<WirespecException>) {
        errors.toNonEmptyListOrNull()?.let { raise(it) }
    }

    private fun AST.duplicatePartErrors(): List<WirespecException> = modules.toList()
        .flatMap { it.statements.filterIsInstance<Model>() }
        .map { it.identifier.value }
        .plus(parts.map { it.identifier.value })
        .groupingBy { it }
        .eachCount()
        .filter { (name, count) -> count > 1 && parts.any { it.identifier.value == name } }
        .map { (name, _) -> DuplicatePartError(name) }

    private fun AST.spreadNonPartErrors(partNames: Set<String>): List<WirespecException> = shapes()
        .flatMap { (owner, entries) ->
            entries.filterIsInstance<Spread>()
                .filterNot { it.identifier.value in partNames }
                .map { SpreadNonPartError(it.identifier.value, owner) }
        }

    private fun AST.partAsReferenceErrors(partNames: Set<String>): List<WirespecException> = modules.toList()
        .flatMap { module -> module.statements.toList().flatMap { it.importReferences() } }
        .plus(parts.flatMap { part -> part.shape.value.map { it.reference.flatten() } }.filterIsInstance<Reference.Custom>())
        .map { it.value }
        .filter { it in partNames }
        .distinct()
        .map(::PartAsReferenceError)

    private fun Map<String, Part>.cycleError(): WirespecException? = keys
        .filter { name -> name in spreadsReachableFrom(name) }
        .takeIf { it.isNotEmpty() }
        ?.let(::CyclicPartError)

    private fun Map<String, Part>.spreadsReachableFrom(name: String): Set<String> = generateSequence(directSpreads(name)) { reached ->
        (reached + reached.flatMap { directSpreads(it) }).takeIf { it.size > reached.size }
    }.last()

    private fun Map<String, Part>.directSpreads(name: String): Set<String> = get(name)
        ?.shape?.entries
        ?.filterIsInstance<Spread>()
        ?.map { it.identifier.value }
        ?.toSet()
        .orEmpty()

    private fun Map<String, Part>.duplicateFieldErrors(owner: String, entries: List<ShapeEntry>): List<WirespecException> = entries
        .takeIf { it.any { entry -> entry is Spread } }
        ?.let { fieldsOf(it) }
        ?.groupBy { it.identifier.value }
        ?.filterValues { it.size > 1 }
        ?.keys
        ?.map { DuplicateFieldError(it, owner) }
        .orEmpty()

    private fun Map<String, Part>.fieldsOf(entries: List<ShapeEntry>): List<Field> = entries.flatMap { entry ->
        when (entry) {
            is Field -> listOf(entry)
            is Spread -> fieldsOf(getValue(entry.identifier.value).shape.entries)
        }
    }

    private fun Map<String, Part>.flatten(ast: AST): AST = ast.copy(
        modules = ast.modules.map { module ->
            module.copy(
                statements = module.statements.map { definition ->
                    when (definition) {
                        is Type -> definition.copy(shape = flatten(definition.shape))
                        is Rpc -> definition.copy(shape = flatten(definition.shape))
                        is Endpoint -> definition.copy(
                            queries = fieldsOf(definition.queryEntries),
                            headers = fieldsOf(definition.headerEntries),
                            responses = definition.responses.map { it.copy(headers = fieldsOf(it.headerEntries)) },
                        )
                        is Channel, is Enum, is Refined, is Union -> definition
                    }
                },
            )
        },
        parts = ast.parts.map { it.copy(shape = flatten(it.shape)) },
    )

    private fun Map<String, Part>.flatten(shape: Type.Shape): Type.Shape = shape.copy(value = fieldsOf(shape.entries))
}

internal fun AST.unusedParts(): List<Part> = shapes()
    .flatMap { (_, entries) -> entries.filterIsInstance<Spread>().map { it.identifier.value } }
    .toSet()
    .let { spread -> parts.filterNot { it.identifier.value in spread } }

private fun AST.shapes(): List<Pair<String, List<ShapeEntry>>> = partShapes() + definitionShapes()

private fun AST.partShapes(): List<Pair<String, List<ShapeEntry>>> = parts.map { "part ${it.identifier.value}" to it.shape.entries }

private fun AST.definitionShapes(): List<Pair<String, List<ShapeEntry>>> = modules.toList()
    .flatMap { it.statements }
    .flatMap { definition ->
        val name = definition.identifier.value
        when (definition) {
            is Type -> listOf("type $name" to definition.shape.entries)
            is Rpc -> listOf("rpc $name" to definition.shape.entries)
            is Endpoint -> listOf(
                "query of endpoint $name" to definition.queryEntries,
                "headers of endpoint $name" to definition.headerEntries,
            ) + definition.responses.map { "headers of response ${it.status} of endpoint $name" to it.headerEntries }
            is Channel, is Enum, is Refined, is Union -> emptyList()
        }
    }
