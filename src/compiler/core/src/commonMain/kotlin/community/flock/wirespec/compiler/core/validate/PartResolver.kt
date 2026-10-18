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
import community.flock.wirespec.compiler.core.parse.ast.Shape
import community.flock.wirespec.compiler.core.parse.ast.Spread
import community.flock.wirespec.compiler.core.parse.ast.Type
import community.flock.wirespec.compiler.core.parse.ast.Union

/**
 * Resolves every [Spread] to the fields of the part it names, so that everything downstream
 * of the parser reads plain fields while a shape still tells which parts it spreads. Each stage
 * assumes the previous one passed: resolving is only safe once every spread names an existing
 * part and no part reaches itself.
 */
internal object PartResolver {

    fun resolve(ast: AST): EitherNel<WirespecException, AST> = either {
        val parts = ast.definitions().filterIsInstance<Part>().associateBy { it.identifier.value }
        raiseAll(ast.duplicatePartErrors() + ast.spreadNonPartErrors(parts.keys) + ast.partAsReferenceErrors(parts.keys))
        raiseAll(listOfNotNull(parts.cycleError()))
        val (partShapes, otherShapes) = ast.shapes().partition { it.definition is Part }
        raiseAll(parts.duplicateFieldErrors(partShapes))
        raiseAll(parts.duplicateFieldErrors(otherShapes.filter { owned -> owned.shape.value.any { it is Spread } }))
        parts.resolveSpreads(ast)
    }

    private fun Raise<NonEmptyList<WirespecException>>.raiseAll(errors: List<WirespecException>) {
        errors.toNonEmptyListOrNull()?.let(::raise)
    }

    private fun AST.duplicatePartErrors(): List<WirespecException> = definitions()
        .filter { it is Model || it is Part }
        .groupBy { it.identifier.value }
        .filterValues { definitions -> definitions.size > 1 && definitions.any { it is Part } }
        .keys
        .map(::DuplicatePartError)

    private fun AST.spreadNonPartErrors(partNames: Set<String>): List<WirespecException> = shapes()
        .flatMap { owned ->
            owned.shape.value.filterIsInstance<Spread>()
                .filterNot { it.reference.value in partNames }
                .map { SpreadNonPartError(it.reference.value, owned.owner) }
        }

    // A spread names its part on purpose, so only the references of fields count here.
    private fun AST.partAsReferenceErrors(partNames: Set<String>): List<WirespecException> = mapShapes { Shape(it.value.filterIsInstance<Field>()) }
        .definitions()
        .flatMap { it.importReferences() }
        .map { it.value }
        .filter { it in partNames }
        .distinct()
        .map(::PartAsReferenceError)

    private fun Map<String, Part>.cycleError(): WirespecException? = keys
        .filter { name -> name in spreadsReachableFrom(name) }
        .toNonEmptyListOrNull()
        ?.let(::CyclicPartError)

    // Transitive closure: keep adding the spreads of what is reached until nothing new turns up.
    private fun Map<String, Part>.spreadsReachableFrom(name: String): Set<String> = generateSequence(directSpreads(name)) { reached ->
        (reached + reached.flatMap { directSpreads(it) }).takeIf { it.size > reached.size }
    }.last()

    private fun Map<String, Part>.directSpreads(name: String): Set<String> = get(name)?.shape?.value.orEmpty()
        .filterIsInstance<Spread>()
        .map { it.reference.value }
        .toSet()

    private fun Map<String, Part>.duplicateFieldErrors(shapes: List<OwnedShape>): List<WirespecException> = shapes
        .flatMap { owned ->
            fieldsOf(owned.shape)
                .groupBy { it.identifier.value }
                .filterValues { it.size > 1 }
                .keys
                .map { DuplicateFieldError(it, owned.owner) }
        }

    private fun Map<String, Part>.fieldsOf(shape: Shape): List<Field> = shape.value.flatMap { entry ->
        when (entry) {
            is Field -> listOf(entry)
            is Spread -> fieldsOf(getValue(entry.reference.value).shape)
        }
    }

    private fun Map<String, Part>.resolveSpreads(ast: AST): AST = ast.mapShapes { shape ->
        Shape(
            shape.value.map { entry ->
                when (entry) {
                    is Field -> entry
                    is Spread -> entry.copy(fields = fieldsOf(getValue(entry.reference.value).shape))
                }
            },
        )
    }
}

public fun unusedPartWarning(partName: String): String = "Part '$partName' is never spread"

internal fun AST.unusedParts(): List<Part> = shapes()
    .flatMap { owned -> owned.shape.value.filterIsInstance<Spread>().map { it.reference.value } }
    .toSet()
    .let { spread -> definitions().filterIsInstance<Part>().filterNot { it.identifier.value in spread } }

private fun AST.mapShapes(transform: (Shape) -> Shape): AST = copy(
    modules = modules.map { module -> module.copy(statements = module.statements.map { it.mapShapes(transform) }) },
)

private fun Definition.mapShapes(transform: (Shape) -> Shape): Definition = when (this) {
    is Type -> copy(shape = transform(shape))
    is Rpc -> copy(shape = transform(shape))
    is Part -> copy(shape = transform(shape))
    is Endpoint -> copy(
        queries = transform(queries),
        headers = transform(headers),
        responses = responses.map { it.copy(headers = transform(it.headers)) },
    )
    is Channel, is Enum, is Refined, is Union -> this
}

private data class OwnedShape(val definition: Definition, val owner: String, val shape: Shape)

private fun AST.definitions(): List<Definition> = modules.toList().flatMap { it.statements }

private fun AST.shapes(): List<OwnedShape> = definitions()
    .flatMap { definition ->
        val name = definition.identifier.value
        when (definition) {
            is Part -> listOf("part $name" to definition.shape)
            is Type -> listOf("type $name" to definition.shape)
            is Rpc -> listOf("rpc $name" to definition.shape)
            is Endpoint -> listOf(
                "query of endpoint $name" to definition.queries,
                "headers of endpoint $name" to definition.headers,
            ) + definition.responses.map { "headers of response ${it.status} of endpoint $name" to it.headers }
            is Channel, is Enum, is Refined, is Union -> emptyList()
        }.map { (owner, shape) -> OwnedShape(definition, owner, shape) }
    }
