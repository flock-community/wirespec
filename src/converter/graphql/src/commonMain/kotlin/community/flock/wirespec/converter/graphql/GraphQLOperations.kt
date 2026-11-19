package community.flock.wirespec.converter.graphql

import community.flock.wirespec.compiler.core.parse.ast.AST
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
 * [ast]:
 *
 * ```graphql
 * query QueryTodo($id: ID!) { result: todo(id: $id) { id title owner { name } } }
 * ```
 *
 * An rpc that names another type than the root operation type, `@GraphQLQuery("NotificationsQuery")`, is a field of that
 * type, so its document nests it in the fields that lead there: `query Inbox { notifications { result: inbox { id } } }`.
 *
 * Every parameter of the rpc is a variable, typed and defaulted as the field argument it is passed to. The field is
 * aliased, so a client reads it under the same name whatever the field is called, and selects every field of its result,
 * all the way down. A union or interface selects `__typename` and one inline fragment for each member.
 *
 * GraphQL selects a finite set of fields, so a field that refers back to a type it is selected in, or that takes a
 * required argument, cannot be selected. Such a field is left out when it is nullable; when it is not, the rpc has no
 * document and [document] throws an [IllegalStateException] that names the field. [unselectable] tells beforehand, for a
 * caller that then selects the fields itself and passes the [arguments] they take.
 */
public class GraphQLOperations(ast: AST) {

    private val schema = WirespecToGraphQL(ast)

    private val models: Map<String, Definition> = ast.modules.flatMap { it.statements }
        .filter { it is Type || it is Union || it is Enum || it is Refined }
        .associateBy { it.identifier.value }

    /** The document that selects the result of [rpc] under [alias]; null for a subscription, which is no request and response. */
    public fun document(rpc: Rpc, alias: String): String? = open(rpc, alias)
        ?.let { "$it${rpc.result.selection(Trail(rpc.identifier.value))}${close(rpc)}" }

    /**
     * The document of [rpc] up to where the selection of its result goes, for a caller that selects the fields itself:
     * the document is this text, a space and the selection between braces, and [close]. Null for a subscription.
     */
    public fun open(rpc: Rpc, alias: String): String? = operation(rpc)
        ?.let { operation -> "$operation${arguments(rpc).declared()}${field(rpc, alias)}" }

    /** How the document of [rpc] starts, before its variables: `query QueryTodo`. Null for a subscription. */
    public fun operation(rpc: Rpc): String? = schema.operation(rpc)
        .takeUnless { it == Operation.SUBSCRIPTION }
        ?.let { "${it.keyword} ${rpc.identifier.value}" }

    /**
     * The document of [rpc] after its variables, up to where the selection of its result goes: the fields that lead to
     * its field, and the field under [alias] with the variables it is passed. A caller that adds variables of its own,
     * for the arguments of the fields it selects, declares them between [operation] and this.
     */
    public fun field(rpc: Rpc, alias: String): String = schema.fieldDefinition(rpc).let { field ->
        " { ${path(rpc).joinToString("") { "$it { " }}$alias: ${field.name}${field.arguments.passed()}"
    }

    /** The arguments of the field that [rpc] is, which are the variables of its document. */
    public fun arguments(rpc: Rpc): List<Argument> = schema.fieldDefinition(rpc).arguments.map { it.toArgument() }

    /** The arguments that [field] takes. */
    public fun arguments(field: Field): List<Argument> = schema.fieldDefinition(field).arguments.map { it.toArgument() }

    /**
     * An argument of a field: its [name], its GraphQL [type] with its default, as a variable is declared with, and the
     * Wirespec type a caller passes for it.
     */
    public class Argument(public val name: String, public val type: String, public val reference: Reference)

    /**
     * Why no document selects every field of the result of [rpc], which leaves a caller to select the fields itself;
     * null when [document] writes one.
     */
    public fun unselectable(rpc: Rpc): String? = rpc.result.unselectable(Trail(rpc.identifier.value))

    /** What closes the document of [rpc] after the selection of its result. */
    public fun close(rpc: Rpc): String = " }".repeat(path(rpc).size + 1)

    /**
     * The fields that lead from the root operation type to the type [rpc] is a field of, which its document nests the
     * field in and its response nests the result in: `notifications` for a field of the `NotificationsQuery` that
     * `Query.notifications` returns. None for a field of the root operation type itself.
     */
    public fun path(rpc: Rpc): List<String> = checkNotNull(schema.path(rpc)) {
        schema.rootType(schema.operation(rpc)).let { root ->
            "Cannot write a GraphQL query for rpc ${rpc.identifier.value}: it is a field of ${schema.parentType(rpc)}, but no field leads from $root to that type. " +
                "Give $root a field that returns it, like `rpc Namespace {} -> ${schema.parentType(rpc)}`."
        }
    }

    /** Whether [field] can be selected without passing it an argument. */
    public fun isSelectable(field: Field): Boolean = schema.fieldDefinition(field).arguments.none { it.type is TypeRef.NonNull && it.defaultValue == null }

    /** The name of [definition] in the GraphQL schema, which an inline fragment names. */
    public fun graphQLName(definition: Definition): String = schema.graphQLName(definition.identifier.value)

    private fun InputValueDefinition.toArgument(): Argument = Argument(
        name = name,
        type = "${type.print()}${defaultValue?.let { " = ${it.print()}" }.orEmpty()}",
        reference = schema.reference(type),
    )

    private fun List<Argument>.declared(): String = takeIf { it.isNotEmpty() }
        ?.joinToString(", ", "(", ")") { "$${it.name}: ${it.type}" }
        .orEmpty()

    private fun List<InputValueDefinition>.passed(): String = takeIf { it.isNotEmpty() }
        ?.joinToString(", ", "(", ")") { "${it.name}: $${it.name}" }
        .orEmpty()

    private fun Reference.selection(trail: Trail): String = when (val model = composite(this)) {
        is Type -> model.selection(trail.enter(model))
        is Union -> model.selection(trail.enter(model))
        else -> ""
    }

    private fun Type.selection(trail: Trail): String = when {
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
            else -> null.also { check(reference.isNullable) { path.cannotSelect(reason) } }
        }
    }

    private fun Trail.cannotSelect(reason: String): String = "Cannot write a GraphQL query for rpc $rpc: field $text of its result $reason. " +
        "GraphQL selects a finite set of fields, so make the field nullable to leave it out of the query."

    /** What [selection] fails on, found without failing: it visits the same fields. */
    private fun Reference.unselectable(trail: Trail): String? = when (val model = composite(this)) {
        is Type -> model.unselectable(trail.enter(model))
        is Union ->
            model.entries
                .mapNotNull { member -> models[(member as? Reference.Custom)?.value] as? Type }
                .firstNotNullOfOrNull { member -> member.unselectable(trail.enter(model).enter(member)) }
        else -> null
    }

    private fun Type.unselectable(trail: Trail): String? = shape.fields
        .takeUnless { annotations.any { it.name == GraphQLAnnotations.UNION } }
        ?.firstNotNullOfOrNull { field ->
            trail.field(field.identifier.value).let { path ->
                when (val reason = field.unselectable(trail)) {
                    null -> field.reference.unselectable(path)
                    else -> path.cannotSelect(reason).takeUnless { field.reference.isNullable }
                }
            }
        }

    private fun Field.unselectable(trail: Trail): String? = when {
        reference.selected().any { it in trail.types } -> "refers back to a type it is selected in"
        !isSelectable(this) -> "takes a required argument"
        else -> null
    }

    /** The type or union whose fields [reference] selects, through any list; null for a scalar or an enum. */
    public fun composite(reference: Reference): Definition? = when (reference) {
        is Reference.Iterable -> composite(reference.reference)
        is Reference.Custom -> models[reference.value]
            ?.takeIf { it is Union || (it is Type && it.annotations.none { annotation -> annotation.name == GraphQLAnnotations.ENUM }) }
        is Reference.Any, is Reference.Unit, is Reference.Dict, is Reference.Primitive -> null
    }

    /** The types whose fields selecting this reference selects right away: a type, or a union and its members. */
    private fun Reference.selected(): List<String> = when (val model = composite(this)) {
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
