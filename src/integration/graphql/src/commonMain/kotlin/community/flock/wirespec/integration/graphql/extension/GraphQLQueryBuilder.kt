package community.flock.wirespec.integration.graphql.extension

import community.flock.wirespec.compiler.core.ir.BinaryOp
import community.flock.wirespec.compiler.core.ir.ConstructorStatement
import community.flock.wirespec.compiler.core.ir.Expression
import community.flock.wirespec.compiler.core.ir.Field
import community.flock.wirespec.compiler.core.ir.FieldCall
import community.flock.wirespec.compiler.core.ir.Function
import community.flock.wirespec.compiler.core.ir.FunctionBuilder
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
 *
 * With a [batch], it builds the request that selects several rpcs at once instead:
 *
 * ```kotlin
 * val request = GraphQLQuery.request {
 *     queryTodo(ID("1")) { title() }
 *     todos(done = false) { id() }
 * }
 * ```
 *
 * There the rpcs are the fields of a `Selection` of the root operation type, and of the selection of the type they are
 * a field of. Each takes the parameters of its rpc, which it adds to the variables that all selections of a request
 * share, and is aliased to the name of its rpc, which is the field of the `Result` it is read into.
 */
internal class GraphQLQueryBuilder(private val operations: GraphQLOperations, private val language: Language, private val batch: Batch? = null) {

    enum class Language { Kotlin, Java, Scala }

    /**
     * An rpc as a field next to others: its `Service` function, the [alias] it is selected under, its [text] in a
     * selection, the [declarations] of its variables, each after a comma, and the variable each parameter is passed as.
     */
    class OperationField(val function: Function, val alias: String, val result: Reference, val text: String, val declarations: String, val variables: List<Pair<String, Parameter>>)

    /** The rpcs of a root operation type: those that are a field of it, and the others by the type they are a field of. */
    class Batch(val name: String, val keyword: String, val root: List<OperationField>, val nested: Map<String, List<OperationField>>) {
        val results: List<Reference> = root.map { it.result }
    }

    /** An rpc: its `Service` function, its document, also up to the selection and after it, its result and its variables. */
    class Request(val rpc: String, val function: Function, val document: String, val open: String, val close: String, val result: Reference, val variables: List<Pair<Field, Parameter>>) {
        val builder: String = builder(rpc)
    }

    /** The selection of every type and union that [results] return, through all their fields. */
    fun selections(results: List<Reference>): List<Struct> = reachable(results).map { model ->
        selection(model.selection(), if (model is Union) "{ $TYPENAME" else "{") {
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
            model.operationFields().forEach { method(model.selection(), it) }
        }
    }

    /** The selection of the root operation type of the [batch], whose fields are its rpcs. */
    fun rootSelection(batch: Batch): Struct = selection(ROOT_SELECTION, "{") { batch.root.forEach { method(ROOT_SELECTION, it) } }

    /** A selection collects its fields as text, and in a [batch] shares the variables of the request with the others. */
    private fun selection(name: String, open: String, methods: StructBuilder.() -> Unit): Struct = struct(name) {
        visibility(Visibility.PUBLIC)
        field(FIELDS, Type.Custom(STRING_BUILDER))
        batch?.let {
            field(DECLARATIONS, Type.Custom(STRING_BUILDER))
            field(VARIABLES, variablesType)
        }
        methods()
        function(TO_GRAPHQL) {
            visibility(Visibility.PUBLIC)
            returnType(Type.String)
            returns(concat(Literal(open, Type.String), FieldCall(field = FIELDS), Literal(" }", Type.String)))
        }
    }

    private fun Definition.operationFields(): List<OperationField> = batch?.nested?.get(identifier.value).orEmpty()

    /** A partial result: its struct, and the models its fields refer to, which its file imports. */
    class Result(val struct: Struct, val models: List<String>)

    /**
     * The result of every type and union that [results] return, with every field nullable: a field that was not
     * selected is null. A field of a type or union is its result, and a union has `__typename` and the fields of all its
     * members, but for a field that members declare with different types, which GraphQL cannot select in both.
     */
    fun results(results: List<Reference>): List<Result> = reachable(results).map { model -> result(model.result(), model.resultFields()) }

    /** The result of the root operation type of the [batch]: the result of each of its rpcs, under the name of the rpc. */
    fun rootResult(batch: Batch): Result = result(ROOT_RESULT, batch.root.map { it.alias to it.result })

    private fun result(name: String, fields: List<Pair<String, Reference>>): Result = Result(
        struct = Struct(
            name = Name.of(name),
            fields = fields.map { (field, reference) -> Field(Name(listOf(field)), Type.Nullable(resultType(reference.copy(isNullable = false)))) },
            visibility = Visibility.PUBLIC,
        ),
        models = fields.mapNotNull { (_, reference) -> reference.model() }.distinct(),
    )

    /**
     * The type a [reference] reads as in a partial result: a type or union is its result, through any list. GraphQL has
     * no void, so an rpc without a result reads as a Boolean.
     */
    fun resultType(reference: Reference): Type = when (reference) {
        is Reference.Iterable -> Type.Array(resultType(reference.reference))
        is Reference.Custom -> operations.composite(reference)?.let { Type.Custom(it.result()) } ?: reference.copy(isNullable = false).convert()
        is Reference.Unit -> Type.Boolean
        is Reference.Any, is Reference.Dict, is Reference.Primitive -> reference.copy(isNullable = false).convert()
    }.let { if (reference.isNullable) Type.Nullable(it) else it }

    private fun Definition.resultFields(): List<Pair<String, Reference>> = when (this) {
        is Union -> listOf(TYPENAME to Reference.Primitive(Reference.Primitive.Type.String(null), isNullable = false)) +
            entries.mapNotNull { operations.composite(it) as? TypeDefinition }
                .flatMap { member -> member.resultFields() }
                .groupBy({ it.first }, { it.second })
                .filterValues { references -> references.distinct().size == 1 }
                .map { (name, references) -> name to references.first() }
        is TypeDefinition -> shape.fields.filter { operations.isSelectable(it) }.map { it.identifier.value to it.reference } +
            operationFields().map { it.alias to it.result }
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

    /** The block that selects the rpcs of a [Batch]. */
    val rootBlock: Type get() = block(ROOT_SELECTION)

    /**
     * Kotlin resolves a call in a block against every enclosing receiver, so `owner { title() }` would select the
     * `title` of the todo. A `@DslMarker` on the selections limits a block to its own selection.
     */
    val dslMarker: RawElement? = RawElement("@$DSL_MARKER").takeIf { language == Language.Kotlin }

    val dslMarkerDefinition: RawElement? = RawElement("@DslMarker\nannotation class $DSL_MARKER").takeIf { language == Language.Kotlin }

    /**
     * `QueryTodoGraphQL.request(id)` writes the request of an rpc with every field of its result, and
     * `QueryTodoGraphQL.request(id) { ... }` with the fields that the block selects, as a `GraphQLRequest` with the query
     * and its `GraphQLVariables`. It has no fields, so it is an `object` in Kotlin and a record without components in Java.
     */
    fun builder(request: Request): Struct = struct(request.builder) {
        val selection = operations.composite(request.result)

        fun request(selection: Definition?, query: Expression) = function(REQUEST) {
            visibility(Visibility.PUBLIC)
            request.function.parameters.forEach { arg(it.name, it.type) }
            selection?.let { arg(SELECTION, it.block()) }
            returnType(type(GRAPHQL_REQUEST))
            returns(
                construct(type(GRAPHQL_REQUEST)) {
                    arg("query", query)
                    request.variables.takeIf { it.isNotEmpty() }?.let { variables ->
                        arg("variables", ConstructorStatement(Type.Custom(GRAPHQL_VARIABLES), variables.associate { (field, parameter) -> field.name to VariableReference(parameter.name) }))
                    }
                },
            )
        }

        request(null, Literal(request.document, Type.String))
        selection?.let { request(it, concat(Literal("${request.open} ", Type.String), it.selected(), Literal(request.close, Type.String))) }
        // A record nested in a Java record is only public and static when it says so.
        struct(GRAPHQL_REQUEST) {
            visibility(Visibility.PUBLIC)
            implements(Type.Custom("Wirespec.GraphQLRequest"))
            field("query", Type.String, isOverride = true)
            if (request.variables.isNotEmpty()) field("variables", type(GRAPHQL_VARIABLES))
        }
        request.variables.takeIf { it.isNotEmpty() }?.let { variables ->
            struct(GRAPHQL_VARIABLES) {
                visibility(Visibility.PUBLIC)
                variables.forEach { (field, _) -> field(field.name, field.type) }
            }
        }
    }

    /**
     * `GraphQLQuery.request { ... }` writes the request of the rpcs that the block selects, with the variables they
     * were passed. A variable is declared by the rpc that takes it, so the request declares those of its own rpcs only.
     */
    fun builder(batch: Batch): Struct = struct(batch.name) {
        function(REQUEST) {
            visibility(Visibility.PUBLIC)
            arg(SELECTION, rootBlock)
            returnType(type(GRAPHQL_REQUEST))
            assign(DECLARATIONS.value(), newStringBuilder())
            assign(VARIABLES.value(), newVariables())
            assign(FIELDS.value(), selected(ROOT_SELECTION, VariableReference(DECLARATIONS), VariableReference(VARIABLES)))
            returns(
                construct(type(GRAPHQL_REQUEST)) {
                    arg("query", concat(Literal(batch.keyword, Type.String), declared(), Literal(" ", Type.String), VariableReference(FIELDS)))
                    arg("variables", VariableReference(VARIABLES))
                },
            )
        }
        struct(GRAPHQL_REQUEST) {
            visibility(Visibility.PUBLIC)
            implements(Type.Custom("Wirespec.GraphQLRequest"))
            field("query", Type.String, isOverride = true)
            field("variables", variablesType)
        }
    }

    /** Adds [text] to the selected fields, after the fields that the block selects of [nested] when there is one, and returns `this`. */
    private fun StructBuilder.method(model: Definition, name: String, text: String, nested: Definition? = null) = function(Name(listOf(Name.of(name).camelCase().sanitized()))) {
        visibility(Visibility.PUBLIC)
        nested?.let { arg(SELECTION, it.block()) }
        returnType(type(model.selection()))
        select(text, nested)
        returns(RawExpression("this"))
    }

    /** Selects the rpc of [field] in the selection named [selection]: declares its variables, sets them and adds the field. */
    private fun StructBuilder.method(selection: String, field: OperationField) = function(Name(listOf(field.function.name.camelCase()))) {
        val nested = operations.composite(field.result)
        visibility(Visibility.PUBLIC)
        field.function.parameters.forEach { arg(it.name, it.type) }
        nested?.let { arg(SELECTION, it.block()) }
        returnType(type(selection))
        field.variables.takeIf { it.isNotEmpty() }?.let { variables ->
            functionCall(APPEND.value(), receiver = FieldCall(field = DECLARATIONS)) { arg(VALUE, Literal(field.declarations, Type.String)) }
            variables.forEach { (variable, parameter) ->
                functionCall("put", receiver = FieldCall(field = VARIABLES)) {
                    arg("key", Literal(variable, Type.String))
                    arg(VALUE, VariableReference(parameter.name))
                }
            }
        }
        select(if (nested == null) " ${field.text}" else " ${field.text} ", nested)
        returns(RawExpression("this"))
    }

    private fun FunctionBuilder.select(text: String, nested: Definition?) {
        val appended = FunctionCall(receiver = FieldCall(field = FIELDS), name = APPEND, arguments = mapOf(VALUE to Literal(text, Type.String)))
        nested
            ?.let { functionCall(APPEND.value(), receiver = appended) { arg(VALUE, it.selected()) } }
            ?: functionCall(APPEND.value(), receiver = FieldCall(field = FIELDS)) { arg(VALUE, Literal(text, Type.String)) }
    }

    /** The block that selects the fields of a type: on `this` in Kotlin, a function of the selection in Java and Scala. */
    private fun Definition.block(): Type = block(selection())

    private fun block(selection: String): Type = Type.Custom(selection).let {
        when (language) {
            Language.Kotlin -> Type.Function(parameterTypes = emptyList(), returnType = Type.Unit, receiver = it)
            Language.Java, Language.Scala -> Type.Function(parameterTypes = listOf(it), returnType = it)
        }
    }

    /** Runs the `selection` block on a new selection of this type, and gives its fields between braces. */
    private fun Definition.selected(): Expression = selected(selection(), FieldCall(field = DECLARATIONS), FieldCall(field = VARIABLES))

    /** In a [batch], the new selection adds to the [declarations] and [variables] of the request. */
    private fun selected(selection: String, declarations: Expression, variables: Expression): Expression = ConstructorStatement(
        Type.Custom(selection),
        mapOf(FIELDS to newStringBuilder()) + batch?.let { mapOf(DECLARATIONS to declarations, VARIABLES to variables) }.orEmpty(),
    )
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

    /** The values of the variables by name, which holds those of the selected rpcs only, so it is a map and no struct. */
    private val variablesType: Type = when (language) {
        Language.Kotlin -> Type.Custom("MutableMap", listOf(Type.String, Type.Nullable(Type.Any)))
        Language.Java -> Type.Custom("java.util.Map", listOf(Type.String, Type.Any))
        Language.Scala -> Type.Custom("scala.collection.mutable.Map", listOf(Type.String, Type.Any))
    }

    private fun newVariables(): Expression = when (language) {
        Language.Kotlin -> RawExpression("LinkedHashMap<String, Any?>()")
        Language.Java -> RawExpression("new java.util.LinkedHashMap<String, Object>()")
        Language.Scala -> RawExpression("scala.collection.mutable.LinkedHashMap[String, Any]()")
    }

    /** The declarations between parentheses, without the comma before the first, or nothing: GraphQL takes no empty list. */
    private fun declared(): Expression = when (language) {
        Language.Kotlin -> RawExpression("(if (graphQLDeclarations.isEmpty()) \"\" else \"(\" + graphQLDeclarations.substring(2) + \")\")")
        Language.Java -> RawExpression("(graphQLDeclarations.length() == 0 ? \"\" : \"(\" + graphQLDeclarations.substring(2) + \")\")")
        Language.Scala -> RawExpression("(if (graphQLDeclarations.isEmpty) \"\" else \"(\" + graphQLDeclarations.substring(2) + \")\")")
    }

    private fun concat(vararg parts: Expression): Expression = parts.reduce { left, right -> BinaryOp(left, BinaryOp.Operator.PLUS, right) }

    private fun reachable(results: List<Reference>): List<Definition> = generateSequence(results.mapNotNull { operations.composite(it) }.distinct()) { found ->
        (found + found.flatMap { it.nested() }).distinct().takeIf { it.size > found.size }
    }.last()

    private fun Definition.nested(): List<Definition> = when (this) {
        is Union -> entries.mapNotNull { operations.composite(it) }
        is TypeDefinition -> (shape.fields.filter { operations.isSelectable(it) }.map { it.reference } + operationFields().map { it.result })
            .mapNotNull { operations.composite(it) }
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

        const val ROOT_SELECTION = "Selection"
        const val ROOT_RESULT = "Result"

        private val FIELDS = Name(listOf("graphQLFields"))
        private val DECLARATIONS = Name(listOf("graphQLDeclarations"))
        private val VARIABLES = Name(listOf("graphQLVariables"))
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
