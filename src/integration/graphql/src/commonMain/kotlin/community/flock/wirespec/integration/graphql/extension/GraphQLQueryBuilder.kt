package community.flock.wirespec.integration.graphql.extension

import community.flock.wirespec.compiler.core.emit.FileExtension
import community.flock.wirespec.compiler.core.ir.BinaryOp
import community.flock.wirespec.compiler.core.ir.ConstructorStatement
import community.flock.wirespec.compiler.core.ir.Expression
import community.flock.wirespec.compiler.core.ir.FieldCall
import community.flock.wirespec.compiler.core.ir.Function
import community.flock.wirespec.compiler.core.ir.FunctionCall
import community.flock.wirespec.compiler.core.ir.Literal
import community.flock.wirespec.compiler.core.ir.Name
import community.flock.wirespec.compiler.core.ir.Namespace
import community.flock.wirespec.compiler.core.ir.NamespaceBuilder
import community.flock.wirespec.compiler.core.ir.Parameter
import community.flock.wirespec.compiler.core.ir.RawElement
import community.flock.wirespec.compiler.core.ir.RawExpression
import community.flock.wirespec.compiler.core.ir.Struct
import community.flock.wirespec.compiler.core.ir.StructBuilder
import community.flock.wirespec.compiler.core.ir.Type
import community.flock.wirespec.compiler.core.ir.VariableReference
import community.flock.wirespec.compiler.core.ir.Visibility
import community.flock.wirespec.compiler.core.ir.generator.JavaGenerator
import community.flock.wirespec.compiler.core.ir.struct
import community.flock.wirespec.compiler.core.parse.ast.Channel
import community.flock.wirespec.compiler.core.parse.ast.Definition
import community.flock.wirespec.compiler.core.parse.ast.Endpoint
import community.flock.wirespec.compiler.core.parse.ast.Enum
import community.flock.wirespec.compiler.core.parse.ast.Part
import community.flock.wirespec.compiler.core.parse.ast.Reference
import community.flock.wirespec.compiler.core.parse.ast.Refined
import community.flock.wirespec.compiler.core.parse.ast.Rpc
import community.flock.wirespec.compiler.core.parse.ast.Union
import community.flock.wirespec.compiler.core.parse.ast.fields
import community.flock.wirespec.converter.graphql.GraphQLOperations
import community.flock.wirespec.compiler.core.parse.ast.Type as TypeDefinition

/**
 * Builds, in the IR DSL, the query builder that lets a caller pick the fields of a result on `this`:
 *
 * ```kotlin
 * val request = GraphQLRequests.queryTodo(ID("1")) {
 *     id()
 *     owner { name() }
 * }
 * ```
 *
 * Every type and union a request can return gets a `<Type>Selection` that collects the selected fields as GraphQL text.
 * Each field of a type is a method that adds the field and returns the selection, so Java chains them in a lambda:
 * `t -> t.id().owner(o -> o.name())`. A field of a type or union takes a block that selects its fields, and a union has
 * an `on<Member>` block per member. `GraphQLRequests` runs a block on a new selection and puts it in the operation of an
 * rpc, with the parameters as its variables.
 */
internal class GraphQLQueryBuilder(private val operations: GraphQLOperations, private val language: FileExtension) {

    class Request(val function: Function, val open: String, val result: Reference, val client: String, val variables: List<Pair<Name, Parameter>>)

    /** The selection of every type and union that [results] return, through all their fields. */
    fun selections(results: List<Reference>): List<Struct> = reachable(results).map { model ->
        struct(model.selection()) {
            field(FIELDS, Type.Custom(STRING_BUILDER))
            when (model) {
                is Union -> model.entries.mapNotNull { operations.composite(it) as? TypeDefinition }.forEach { member ->
                    method(model, "on${Name.of(member.identifier.value).pascalCase()}", " ... on ${operations.graphQLName(member)} ", member)
                }
                is TypeDefinition -> model.shape.fields.filter { operations.isSelectable(it) }.forEach { field ->
                    when (val nested = operations.composite(field.reference)) {
                        null -> method(model, field.identifier.value, " ${field.identifier.value}")
                        else -> method(model, field.identifier.value, " ${field.identifier.value} ", nested)
                    }
                }
                is Endpoint, is Channel, is Rpc, is Enum, is Refined, is Part -> Unit
            }
            function(TO_GRAPHQL) {
                visibility(Visibility.PUBLIC)
                returnType(Type.String)
                returns(concat(Literal(if (model is Union) "{ __typename" else "{", Type.String), FieldCall(field = FIELDS), Literal(" }", Type.String)))
            }
        }
    }

    /**
     * Kotlin resolves a call in a block against every enclosing receiver, so `owner { title() }` would select the
     * `title` of the todo. A `@DslMarker` on the selections limits a block to its own selection.
     */
    val dslMarker: RawElement? = RawElement("@$DSL_MARKER").takeIf { language == FileExtension.Kotlin }

    val dslMarkerDefinition: RawElement? = RawElement("@DslMarker\nannotation class $DSL_MARKER").takeIf { language == FileExtension.Kotlin }

    /** `GraphQLRequests.queryTodo(id) { ... }` writes the request of an rpc with the fields of its result that the block selects. */
    fun requests(requests: List<Request>): Namespace = NamespaceBuilder(REQUESTS).apply {
        requests.forEach { request ->
            val selection = operations.composite(request.result)
            function(Name(listOf(request.function.name.camelCase()))) {
                request.function.parameters.forEach { arg(it.name, it.type) }
                selection?.let { arg(SELECTION, it.block()) }
                returnType(type("${request.client}.$GRAPHQL_REQUEST"))
                returns(
                    construct(type("${request.client}.$GRAPHQL_REQUEST")) {
                        arg(
                            "query",
                            selection
                                ?.let { concat(Literal("${request.open} ", Type.String), it.selected(), Literal(GraphQLOperations.CLOSE, Type.String)) }
                                ?: Literal(request.open + GraphQLOperations.CLOSE, Type.String),
                        )
                        request.variables.takeIf { it.isNotEmpty() }?.let { variables ->
                            arg(
                                "variables",
                                ConstructorStatement(
                                    Type.Custom("${request.client}.$GRAPHQL_VARIABLES"),
                                    variables.associate { (name, parameter) -> name to VariableReference(parameter.name) },
                                ),
                            )
                        }
                    },
                )
            }
        }
    }.build()

    /** Adds [text] to the selected fields, after the fields that the block selects of [nested] when there is one, and returns `this`. */
    private fun StructBuilder.method(model: Definition, name: String, text: String, nested: Definition? = null) = function(Name(listOf(Name.of(name).camelCase().sanitized()))) {
        visibility(Visibility.PUBLIC)
        nested?.let { arg(SELECTION, it.block()) }
        returnType(type(model.selection()))
        val appended = FunctionCall(receiver = FieldCall(field = FIELDS), name = APPEND, arguments = mapOf(VALUE to Literal(text, Type.String)))
        nested
            ?.let { functionCall(APPEND.value(), receiver = appended) { arg(VALUE, it.selected()) } }
            ?: functionCall(APPEND.value(), receiver = FieldCall(field = FIELDS)) { arg(VALUE, Literal(text, Type.String)) }
        returns(RawExpression("this"))
    }

    /** The block that selects the fields of a type: on `this` in Kotlin, a function of the selection in Java. */
    private fun Definition.block(): Type = Type.Custom(selection()).let { selection ->
        when (language) {
            FileExtension.Kotlin -> Type.Function(parameterTypes = emptyList(), returnType = Type.Unit, receiver = selection)
            else -> Type.Function(parameterTypes = listOf(selection), returnType = selection)
        }
    }

    /** Runs the `selection` block on a new selection of this type, and gives its fields between braces. */
    private fun Definition.selected(): Expression = ConstructorStatement(Type.Custom(selection()), mapOf(FIELDS to newStringBuilder()))
        .let { empty ->
            when (language) {
                FileExtension.Kotlin -> FunctionCall(receiver = empty, name = APPLY, arguments = mapOf(Name.of("block") to VariableReference(SELECTION)))
                else -> FunctionCall(receiver = VariableReference(SELECTION), name = APPLY, arguments = mapOf(Name.of("t") to empty))
            }
        }
        .let { FunctionCall(receiver = it, name = TO_GRAPHQL) }

    // An IR constructor call without arguments has no parentheses in Kotlin.
    private fun newStringBuilder(): Expression = when (language) {
        FileExtension.Kotlin -> FunctionCall(name = Name(listOf(STRING_BUILDER)))
        else -> ConstructorStatement(Type.Custom(STRING_BUILDER))
    }

    private fun concat(vararg parts: Expression): Expression = parts.reduce { left, right -> BinaryOp(left, BinaryOp.Operator.PLUS, right) }

    private fun reachable(results: List<Reference>): List<Definition> = generateSequence(results.mapNotNull { operations.composite(it) }.distinct()) { found ->
        (found + found.flatMap { it.nested() }).distinct().takeIf { it.size > found.size }
    }.last()

    private fun Definition.nested(): List<Definition> = when (this) {
        is Union -> entries.mapNotNull { operations.composite(it) }
        is TypeDefinition -> shape.fields.filter { operations.isSelectable(it) }.mapNotNull { operations.composite(it.reference) }
        is Endpoint, is Channel, is Rpc, is Enum, is Refined, is Part -> emptyList()
    }

    private fun Definition.selection(): String = "${Name.of(identifier.value).pascalCase()}Selection"

    /** Java has no escape for a keyword; Kotlin backticks it. */
    private fun String.sanitized(): String = when (language) {
        FileExtension.Java -> if (this in JavaGenerator.reservedKeywords) "_$this" else this
        else -> this
    }

    companion object {
        const val REQUESTS = "GraphQLRequests"
        const val DSL_MARKER = "GraphQLDsl"
        const val GRAPHQL_REQUEST = "GraphQLRequest"
        const val GRAPHQL_VARIABLES = "GraphQLVariables"

        private val FIELDS = Name(listOf("graphQLFields"))
        private val SELECTION = Name.of("selection")
        private val TO_GRAPHQL = Name(listOf("toGraphQL"))
        private val APPEND = Name(listOf("append"))
        private val APPLY = Name(listOf("apply"))
        private val VALUE = Name.of("value")
        private const val STRING_BUILDER = "StringBuilder"
    }
}
