package community.flock.wirespec.integration.graphql.extension

import community.flock.wirespec.compiler.core.ir.BinaryOp
import community.flock.wirespec.compiler.core.ir.ConstructorStatement
import community.flock.wirespec.compiler.core.ir.Expression
import community.flock.wirespec.compiler.core.ir.Field
import community.flock.wirespec.compiler.core.ir.FieldCall
import community.flock.wirespec.compiler.core.ir.Function
import community.flock.wirespec.compiler.core.ir.FunctionCall
import community.flock.wirespec.compiler.core.ir.Literal
import community.flock.wirespec.compiler.core.ir.Name
import community.flock.wirespec.compiler.core.ir.Parameter
import community.flock.wirespec.compiler.core.ir.RawElement
import community.flock.wirespec.compiler.core.ir.RawExpression
import community.flock.wirespec.compiler.core.ir.Struct
import community.flock.wirespec.compiler.core.ir.StructBuilder
import community.flock.wirespec.compiler.core.ir.Type
import community.flock.wirespec.compiler.core.ir.VariableReference
import community.flock.wirespec.compiler.core.ir.Visibility
import community.flock.wirespec.compiler.core.ir.converter.convert
import community.flock.wirespec.compiler.core.ir.generator.JavaGenerator
import community.flock.wirespec.compiler.core.ir.generator.ScalaGenerator
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
 * val request = QueryTodoGraphQL.request(ID("1")) {
 *     id()
 *     owner { name() }
 * }
 * ```
 *
 * Every type and union a request can return gets a `<Type>Selection` that collects the selected fields as GraphQL text.
 * Each field of a type is a method that adds the field and returns the selection, so Java chains them in a lambda:
 * `t -> t.id().owner(o -> o.name())`. A field of a type or union takes a block that selects its fields, and a union has
 * an `on<Member>` block per member. The builder of each rpc, `<Rpc>GraphQL`, runs a block on a new selection and puts it
 * in the operation of the rpc, with the parameters as its variables. The clients send it for a call that takes a block.
 * The selections and results of an rpc are nested in its builder, so its file holds everything the rpc needs.
 */
internal class GraphQLQueryBuilder(private val operations: GraphQLOperations, private val language: Language) {

    enum class Language { Kotlin, Java, Scala }

    /** An rpc: its `Service` function, its document up to the selection, its result and its variables. */
    class Request(val rpc: String, val function: Function, val open: String, val result: Reference, val variables: List<Pair<Field, Parameter>>) {
        val builder: String = builder(rpc)
    }

    /** The selection of every type and union that [results] return, through all their fields. */
    fun selections(results: List<Reference>): List<Struct> = reachable(results).map { model ->
        struct(model.selection()) {
            visibility(Visibility.PUBLIC)
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
                returns(concat(Literal(if (model is Union) "{ $TYPENAME" else "{", Type.String), FieldCall(field = FIELDS), Literal(" }", Type.String)))
            }
        }
    }

    /** A partial result: its struct, and the models its fields refer to, which its file imports. */
    class Result(val struct: Struct, val models: List<String>)

    /**
     * The result of every type and union that [results] return, with every field nullable: a field that was not
     * selected is null. A field of a type or union is its result, and a union has `__typename` and the fields of all its
     * members, but for a field that members declare with different types, which GraphQL cannot select in both.
     */
    fun results(results: List<Reference>): List<Result> = reachable(results).map { model ->
        model.resultFields().let { fields ->
            Result(
                struct = Struct(
                    name = Name.of(model.result()),
                    fields = fields.map { (name, reference) -> Field(Name(listOf(name)), Type.Nullable(resultType(reference.copy(isNullable = false)))) },
                    visibility = Visibility.PUBLIC,
                ),
                models = fields.mapNotNull { (_, reference) -> reference.model() }.distinct(),
            )
        }
    }

    /** The type a [reference] reads as in a partial result: a type or union is its result, through any list. */
    fun resultType(reference: Reference): Type = when (reference) {
        is Reference.Iterable -> Type.Array(resultType(reference.reference))
        is Reference.Custom -> operations.composite(reference)?.let { Type.Custom(it.result()) } ?: reference.copy(isNullable = false).convert()
        is Reference.Any, is Reference.Unit, is Reference.Dict, is Reference.Primitive -> reference.copy(isNullable = false).convert()
    }.let { if (reference.isNullable) Type.Nullable(it) else it }

    private fun Definition.resultFields(): List<Pair<String, Reference>> = when (this) {
        is Union -> listOf(TYPENAME to Reference.Primitive(Reference.Primitive.Type.String(null), isNullable = false)) +
            entries.mapNotNull { operations.composite(it) as? TypeDefinition }
                .flatMap { member -> member.resultFields() }
                .groupBy({ it.first }, { it.second })
                .filterValues { references -> references.distinct().size == 1 }
                .map { (name, references) -> name to references.first() }
        is TypeDefinition -> shape.fields.filter { operations.isSelectable(it) }.map { it.identifier.value to it.reference }
        is Endpoint, is Channel, is Rpc, is Enum, is Refined, is Part -> emptyList()
    }

    /** The model a field of a partial result refers to, which is not a result itself. */
    private fun Reference.model(): String? = when (this) {
        is Reference.Iterable -> reference.model()
        is Reference.Custom -> value.takeIf { operations.composite(this) == null }?.let { Name.of(it).pascalCase() }
        is Reference.Any, is Reference.Unit, is Reference.Dict, is Reference.Primitive -> null
    }

    /** The block that selects the fields of the result of [reference], or null for a scalar or enum. */
    fun block(reference: Reference): Type? = operations.composite(reference)?.block()

    /**
     * Kotlin resolves a call in a block against every enclosing receiver, so `owner { title() }` would select the
     * `title` of the todo. A `@DslMarker` on the selections limits a block to its own selection.
     */
    val dslMarker: RawElement? = RawElement("@$DSL_MARKER").takeIf { language == Language.Kotlin }

    val dslMarkerDefinition: RawElement? = RawElement("@DslMarker\nannotation class $DSL_MARKER").takeIf { language == Language.Kotlin }

    /**
     * `QueryTodoGraphQL.request(id) { ... }` writes the request of an rpc with the fields of its result that the block
     * selects, as a `GraphQLRequest` with the query and its `GraphQLVariables`. It has no fields, so it is an `object` in
     * Kotlin and a record without components in Java.
     */
    fun builder(request: Request): Struct = struct(request.builder) {
        val selection = operations.composite(request.result)
        function(REQUEST) {
            visibility(Visibility.PUBLIC)
            request.function.parameters.forEach { arg(it.name, it.type) }
            selection?.let { arg(SELECTION, it.block()) }
            returnType(type(GRAPHQL_REQUEST))
            returns(
                construct(type(GRAPHQL_REQUEST)) {
                    arg(
                        "query",
                        selection
                            ?.let { concat(Literal("${request.open} ", Type.String), it.selected(), Literal(GraphQLOperations.CLOSE, Type.String)) }
                            ?: Literal(request.open + GraphQLOperations.CLOSE, Type.String),
                    )
                    request.variables.takeIf { it.isNotEmpty() }?.let { variables ->
                        arg("variables", ConstructorStatement(Type.Custom(GRAPHQL_VARIABLES), variables.associate { (field, parameter) -> field.name to VariableReference(parameter.name) }))
                    }
                },
            )
        }
        // A record nested in a Java record is only public and static when it says so.
        struct(GRAPHQL_REQUEST) {
            visibility(Visibility.PUBLIC)
            field("query", Type.String)
            if (request.variables.isNotEmpty()) field("variables", type(GRAPHQL_VARIABLES))
        }
        request.variables.takeIf { it.isNotEmpty() }?.let { variables ->
            struct(GRAPHQL_VARIABLES) {
                visibility(Visibility.PUBLIC)
                variables.forEach { (field, _) -> field(field.name, field.type) }
            }
        }
    }

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

    /** The block that selects the fields of a type: on `this` in Kotlin, a function of the selection in Java and Scala. */
    private fun Definition.block(): Type = Type.Custom(selection()).let { selection ->
        when (language) {
            Language.Kotlin -> Type.Function(parameterTypes = emptyList(), returnType = Type.Unit, receiver = selection)
            Language.Java, Language.Scala -> Type.Function(parameterTypes = listOf(selection), returnType = selection)
        }
    }

    /** Runs the `selection` block on a new selection of this type, and gives its fields between braces. */
    private fun Definition.selected(): Expression = ConstructorStatement(Type.Custom(selection()), mapOf(FIELDS to newStringBuilder()))
        .let { empty ->
            when (language) {
                Language.Kotlin -> FunctionCall(receiver = empty, name = APPLY, arguments = mapOf(Name.of("block") to VariableReference(SELECTION)))
                Language.Java, Language.Scala -> FunctionCall(receiver = VariableReference(SELECTION), name = APPLY, arguments = mapOf(Name.of("t") to empty))
            }
        }
        .let { FunctionCall(receiver = it, name = TO_GRAPHQL) }

    // An IR constructor call without arguments has no parentheses in Kotlin.
    private fun newStringBuilder(): Expression = when (language) {
        Language.Kotlin -> FunctionCall(name = Name(listOf(STRING_BUILDER)))
        Language.Java, Language.Scala -> ConstructorStatement(Type.Custom(STRING_BUILDER))
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

    private fun Definition.result(): String = "${Name.of(identifier.value).pascalCase()}Result"

    /** Java has no escape for a keyword; Kotlin backticks it itself, and Scala needs the backticks written. */
    private fun String.sanitized(): String = when (language) {
        Language.Kotlin -> this
        Language.Java -> if (this in JavaGenerator.reservedKeywords) "_$this" else this
        Language.Scala -> if (this in ScalaGenerator.reservedKeywords) "`$this`" else this
    }

    companion object {
        fun builder(rpc: String): String = "${rpc}GraphQL"

        const val DSL_MARKER = "GraphQLDsl"
        const val GRAPHQL_REQUEST = "GraphQLRequest"
        const val GRAPHQL_VARIABLES = "GraphQLVariables"

        private val FIELDS = Name(listOf("graphQLFields"))
        private val SELECTION = Name.of("selection")
        val REQUEST = Name(listOf("request"))
        private val TO_GRAPHQL = Name(listOf("toGraphQL"))
        private val APPEND = Name(listOf("append"))
        private val APPLY = Name(listOf("apply"))
        private val VALUE = Name.of("value")
        private const val STRING_BUILDER = "StringBuilder"
        private const val TYPENAME = "__typename"
    }
}
