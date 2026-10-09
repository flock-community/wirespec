package community.flock.wirespec.compiler.core.validate

import arrow.core.EitherNel
import arrow.core.NonEmptyList
import arrow.core.raise.Raise
import arrow.core.raise.either
import arrow.core.toNonEmptyListOrNull
import community.flock.wirespec.compiler.core.emit.importReferences
import community.flock.wirespec.compiler.core.exceptions.CyclicPartError
import community.flock.wirespec.compiler.core.exceptions.DuplicateFieldError
import community.flock.wirespec.compiler.core.exceptions.DuplicatePartError
import community.flock.wirespec.compiler.core.exceptions.PartAsReferenceError
import community.flock.wirespec.compiler.core.exceptions.SpreadNonPartError
import community.flock.wirespec.compiler.core.exceptions.WirespecException
import community.flock.wirespec.compiler.core.parse.ast.AST
import community.flock.wirespec.compiler.core.parse.ast.Channel
import community.flock.wirespec.compiler.core.parse.ast.Definition
import community.flock.wirespec.compiler.core.parse.ast.Endpoint
import community.flock.wirespec.compiler.core.parse.ast.Enum
import community.flock.wirespec.compiler.core.parse.ast.Field
import community.flock.wirespec.compiler.core.parse.ast.Model
import community.flock.wirespec.compiler.core.parse.ast.Part
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
        val parts = ast.definitions().filterIsInstance<Part>().associateBy { it.identifier.value }
        raiseAll(ast.duplicatePartErrors() + ast.spreadNonPartErrors(parts.keys) + ast.partAsReferenceErrors(parts.keys))
        raiseAll(listOfNotNull(parts.cycleError()))
        val (partShapes, otherShapes) = ast.shapes().partition { it.definition is Part }
        raiseAll(parts.duplicateFieldErrors(partShapes))
        raiseAll(parts.duplicateFieldErrors(otherShapes))
        parts.flatten(ast)
    }

    private fun Raise<NonEmptyList<WirespecException>>.raiseAll(errors: List<WirespecException>) {
        errors.toNonEmptyListOrNull()?.let { raise(it) }
    }

    private fun AST.duplicatePartErrors(): List<WirespecException> = definitions()
        .filter { it is Model || it is Part }
        .groupBy { it.identifier.value }
        .filterValues { definitions -> definitions.size > 1 && definitions.any { it is Part } }
        .keys
        .map(::DuplicatePartError)

    private fun AST.spreadNonPartErrors(partNames: Set<String>): List<WirespecException> = shapes()
        .flatMap { shape ->
            shape.entries.filterIsInstance<Spread>()
                .filterNot { it.identifier.value in partNames }
                .map { SpreadNonPartError(it.identifier.value, shape.owner) }
        }

    private fun AST.partAsReferenceErrors(partNames: Set<String>): List<WirespecException> = definitions()
        .flatMap { it.importReferences() }
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
        ?.shape?.value
        ?.filterIsInstance<Spread>()
        ?.map { it.identifier.value }
        ?.toSet()
        .orEmpty()

    private fun Map<String, Part>.duplicateFieldErrors(shapes: List<OwnedShape>): List<WirespecException> = shapes
        .filter { shape -> shape.entries.any { it is Spread } }
        .flatMap { shape ->
            fieldsOf(shape.entries)
                .groupBy { it.identifier.value }
                .filterValues { it.size > 1 }
                .keys
                .map { DuplicateFieldError(it, shape.owner) }
        }

    private fun Map<String, Part>.fieldsOf(entries: List<ShapeEntry>): List<Field> = entries.flatMap { entry ->
        when (entry) {
            is Field -> listOf(entry)
            is Spread -> fieldsOf(getValue(entry.identifier.value).shape.value)
        }
    }

    private fun Map<String, Part>.flatten(ast: AST): AST = ast.copy(
        modules = ast.modules.map { module ->
            module.copy(
                statements = module.statements.map { definition ->
                    when (definition) {
                        is Type -> definition.copy(shape = flatten(definition.shape))
                        is Rpc -> definition.copy(shape = flatten(definition.shape))
                        is Part -> definition.copy(shape = flatten(definition.shape))
                        is Endpoint -> definition.copy(
                            queries = fieldsOf(definition.queries),
                            headers = fieldsOf(definition.headers),
                            responses = definition.responses.map { it.copy(headers = fieldsOf(it.headers)) },
                        )
                        is Channel, is Enum, is Refined, is Union -> definition
                    }
                },
            )
        },
    )

    private fun Map<String, Part>.flatten(shape: Type.Shape): Type.Shape = Type.Shape(fieldsOf(shape.value))
}

internal fun AST.unusedParts(): List<Part> = shapes()
    .flatMap { shape -> shape.entries.filterIsInstance<Spread>().map { it.identifier.value } }
    .toSet()
    .let { spread -> definitions().filterIsInstance<Part>().filterNot { it.identifier.value in spread } }

private data class OwnedShape(val definition: Definition, val owner: String, val entries: List<ShapeEntry>)

private fun AST.definitions(): List<Definition> = modules.toList().flatMap { it.statements }

private fun AST.shapes(): List<OwnedShape> = definitions()
    .flatMap { definition ->
        val name = definition.identifier.value
        when (definition) {
            is Part -> listOf("part $name" to definition.shape.value)
            is Type -> listOf("type $name" to definition.shape.value)
            is Rpc -> listOf("rpc $name" to definition.shape.value)
            is Endpoint -> listOf(
                "query of endpoint $name" to definition.queries,
                "headers of endpoint $name" to definition.headers,
            ) + definition.responses.map { "headers of response ${it.status} of endpoint $name" to it.headers }
            is Channel, is Enum, is Refined, is Union -> emptyList()
        }.map { (owner, entries) -> OwnedShape(definition, owner, entries) }
    }
