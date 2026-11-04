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
 * Builds, in the IR DSL, the query builder that lets a caller pick the fields of a result:
 *
 * ```kotlin
 * val request = GraphQLRequests.queryTodo(ID("1"), Select.todo().id().title().owner(Select.user().name()))
 * ```
 *
 * Every type and union a request can return gets an immutable `<Type>Selection` that holds the selected fields as
 * GraphQL text. Each field of a type is a method that returns a copy with the field added; a field of a type or union
 * takes the selection of that type, and a union has an `on<Member>` method per member. `Select` starts an empty
 * selection of every type, and `GraphQLRequests` puts a selection in the operation of an rpc, with the parameters as
 * its variables. Only strings are concatenated, so every generator renders the same builder.
 */
internal class GraphQLQueryBuilder(private val operations: GraphQLOperations, private val language: FileExtension) {

    class Request(val function: Function, val open: String, val result: Reference, val client: String, val variables: List<Pair<Name, Parameter>>)

    /** The selection of every type and union that [results] return, through all their fields. */
    fun selections(results: List<Reference>): List<Struct> = reachable(results).map { model ->
        struct(model.selection()) {
            field(FIELDS, Type.String)
            when (model) {
                is Union -> model.entries.mapNotNull { operations.composite(it) as? TypeDefinition }.forEach { member ->
                    val name = Name.of(member.identifier.value).pascalCase()
                    method(model, "on$name", concat(appended(" ... on ${operations.graphQLName(member)} "), toGraphQL()), member)
                }
                is TypeDefinition -> model.shape.fields.filter { operations.isSelectable(it) }.forEach { field ->
                    val name = field.identifier.value
                    when (val nested = operations.composite(field.reference)) {
                        null -> method(model, name, appended(" $name"))
                        else -> method(model, name, concat(appended(" $name "), toGraphQL()), nested)
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

    /** `Select.todo()` starts an empty `TodoSelection`. */
    fun select(results: List<Reference>): Namespace = NamespaceBuilder(SELECT).apply {
        reachable(results).forEach { model ->
            function(Name(listOf(Name.of(model.identifier.value).camelCase().sanitized()))) {
                returnType(type(model.selection()))
                returns(construct(type(model.selection())) { arg(FIELDS, Literal("", Type.String)) })
            }
        }
    }.build()

    /** `GraphQLRequests.queryTodo(id, selection)` writes the request of an rpc with the fields of its result that the selection picks. */
    fun requests(requests: List<Request>): Namespace = NamespaceBuilder(REQUESTS).apply {
        requests.forEach { request ->
            val selection = operations.composite(request.result)
            function(Name(listOf(request.function.name.camelCase()))) {
                request.function.parameters.forEach { arg(it.name, it.type) }
                selection?.let { arg(SELECTION, type(it.selection())) }
                returnType(type("${request.client}.$GRAPHQL_REQUEST"))
                returns(
                    construct(type("${request.client}.$GRAPHQL_REQUEST")) {
                        arg(
                            "query",
                            selection
                                ?.let { concat(Literal("${request.open} ", Type.String), toGraphQL(), Literal(GraphQLOperations.CLOSE, Type.String)) }
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

    private fun StructBuilder.method(model: Definition, name: String, value: Expression, nested: Definition? = null) = function(Name(listOf(Name.of(name).camelCase().sanitized()))) {
        visibility(Visibility.PUBLIC)
        nested?.let { arg(SELECTION, type(it.selection())) }
        returnType(type(model.selection()))
        returns(construct(type(model.selection())) { arg(FIELDS, value) })
    }

    private fun appended(text: String): Expression = concat(FieldCall(field = FIELDS), Literal(text, Type.String))

    /** The fields of the `selection` parameter, between braces. */
    private fun toGraphQL(): Expression = FunctionCall(receiver = VariableReference(SELECTION), name = TO_GRAPHQL)

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
        const val SELECT = "Select"
        const val REQUESTS = "GraphQLRequests"
        const val GRAPHQL_REQUEST = "GraphQLRequest"
        const val GRAPHQL_VARIABLES = "GraphQLVariables"

        private val FIELDS = Name(listOf("graphQLFields"))
        private val SELECTION = Name.of("selection")
        private val TO_GRAPHQL = Name(listOf("toGraphQL"))
    }
}
