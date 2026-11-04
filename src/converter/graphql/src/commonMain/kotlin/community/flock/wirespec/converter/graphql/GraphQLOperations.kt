package community.flock.wirespec.converter.graphql

import community.flock.wirespec.compiler.core.parse.ast.Definition
import community.flock.wirespec.compiler.core.parse.ast.Enum
import community.flock.wirespec.compiler.core.parse.ast.Field
import community.flock.wirespec.compiler.core.parse.ast.Reference
import community.flock.wirespec.compiler.core.parse.ast.Refined
import community.flock.wirespec.compiler.core.parse.ast.Rpc
import community.flock.wirespec.compiler.core.parse.ast.Type
import community.flock.wirespec.compiler.core.parse.ast.Union
import community.flock.wirespec.compiler.core.parse.ast.fields
import community.flock.wirespec.converter.graphql.GraphQLModel.InputValueDefinition
import community.flock.wirespec.converter.graphql.GraphQLModel.Operation
import community.flock.wirespec.converter.graphql.GraphQLModel.TypeRef
import community.flock.wirespec.converter.graphql.GraphQLPrinter.print

/**
 * Writes the executable GraphQL document that calls an rpc, against the schema that [GraphQLEmitter] emits for the same
 * [definitions]:
 *
 * ```graphql
 * query QueryTodo($id: ID!) { result: todo(id: $id) { id title owner { name } } }
 * ```
 *
 * Every parameter of the rpc is a variable, typed and defaulted as the field argument it is passed to. The field is
 * aliased, so a client reads it under the same name whatever the field is called, and selects every field of its result,
 * all the way down. A union or interface selects `__typename` and one inline fragment for each member.
 *
 * GraphQL selects a finite set of fields, so a field that refers back to a type it is selected in, or that takes a
 * required argument, cannot be selected. Such a field is left out when it is nullable; when it is not, the rpc has no
 * document and [document] throws an [IllegalStateException] that names the field.
 */
public class GraphQLOperations(definitions: List<Definition>) {

    private val schema = WirespecToGraphQL(definitions)

    private val models: Map<String, Definition> = definitions
        .filter { it is Type || it is Union || it is Enum || it is Refined }
        .associateBy { it.identifier.value }

    /** The document that selects the result of [rpc] under [alias]; null for a subscription, which is no request and response. */
    public fun document(rpc: Rpc, alias: String): String? = schema.operation(rpc)
        .takeUnless { it == Operation.SUBSCRIPTION }
        ?.let { operation ->
            val field = schema.fieldDefinition(rpc)
            "${operation.keyword} ${rpc.identifier.value}${field.arguments.variables()} " +
                "{ $alias: ${field.name}${field.arguments.passed()}${rpc.result.selection(Trail(rpc.identifier.value))} }"
        }

    private fun List<InputValueDefinition>.variables(): String = takeIf { it.isNotEmpty() }
        ?.joinToString(", ", "(", ")") { "$${it.name}: ${it.type.print()}${it.defaultValue?.let { value -> " = ${value.print()}" }.orEmpty()}" }
        .orEmpty()

    private fun List<InputValueDefinition>.passed(): String = takeIf { it.isNotEmpty() }
        ?.joinToString(", ", "(", ")") { "${it.name}: $${it.name}" }
        .orEmpty()

    private fun Reference.selection(trail: Trail): String = when (val model = composite()) {
        is Type -> model.selection(trail.enter(model))
        is Union -> model.selection(trail.enter(model))
        else -> ""
    }

    private fun Type.selection(trail: Trail): String = when {
        annotations.any { it.name == GraphQLAnnotations.ENUM } -> ""
        annotations.any { it.name == GraphQLAnnotations.UNION } -> listOf(TYPENAME).block()
        else -> shape.fields.mapNotNull { it.selection(trail) }.ifEmpty { listOf(TYPENAME) }.block()
    }

    private fun Union.selection(trail: Trail): String = entries
        .mapNotNull { member -> models[(member as? Reference.Custom)?.value] as? Type }
        .map { member -> "... on ${schema.graphQLName(member.identifier.value)}${member.selection(trail.enter(member))}" }
        .let { fragments -> (listOf(TYPENAME) + fragments).block() }

    private fun Field.selection(trail: Trail): String? = trail.field(identifier.value).let { path ->
        when (val reason = unselectable(trail)) {
            null -> "${identifier.value}${reference.selection(path)}"
            else -> null.also {
                check(reference.isNullable) {
                    "Cannot write a GraphQL query for rpc ${path.rpc}: field ${path.text} of its result $reason. " +
                        "GraphQL selects a finite set of fields, so make the field nullable to leave it out of the query."
                }
            }
        }
    }

    private fun Field.unselectable(trail: Trail): String? = when {
        reference.selected().any { it in trail.types } -> "refers back to a type it is selected in"
        schema.fieldDefinition(this).arguments.any { it.type is TypeRef.NonNull && it.defaultValue == null } -> "takes a required argument"
        else -> null
    }

    /** The type or union a reference selects fields of, through any list; null for a scalar or enum. */
    private fun Reference.composite(): Definition? = when (this) {
        is Reference.Iterable -> reference.composite()
        is Reference.Custom -> models[value]?.takeIf { it is Type || it is Union }
        is Reference.Any, is Reference.Unit, is Reference.Dict, is Reference.Primitive -> null
    }

    /** The types whose fields selecting this reference selects right away: a type, or a union and its members. */
    private fun Reference.selected(): List<String> = when (val model = composite()) {
        is Union -> listOf(model.identifier.value) + model.entries.mapNotNull { (it as? Reference.Custom)?.value }
        null -> emptyList()
        else -> listOf(model.identifier.value)
    }

    private fun List<String>.block(): String = joinToString(" ", " { ", " }")

    /** Where a selection is: the types it is inside of, and the fields that lead to it from the result. */
    private data class Trail(val rpc: String, val types: Set<String> = emptySet(), val fields: List<String> = emptyList()) {
        val text: String get() = fields.joinToString(".")

        fun enter(model: Definition): Trail = copy(types = types + model.identifier.value)

        fun field(name: String): Trail = copy(fields = fields + name)
    }

    private companion object {
        const val TYPENAME = "__typename"
    }
}
