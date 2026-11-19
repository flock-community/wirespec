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
import community.flock.wirespec.compiler.core.parse.ast.Field as AstField
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

    /**
     * A target language, with its `GraphQLArguments`: the variables of a document whose selection passes arguments.
     * `declare` adds a variable of the rpc under its own name, and `pass` adds one for an argument of a selected field,
     * under a name it makes up from the prefix it is given. Both give the variable as the document refers to it.
     */
    enum class Language {
        Kotlin {
            override val variables = Type.Dict(Type.String, Type.Nullable(Type.Any))

            override fun arguments(prefix: String) = """
                |class $GRAPHQL_ARGUMENTS {
                |    private val declarations = mutableListOf<String>()
                |    private val variables = mutableMapOf<String, Any?>()
                |    fun declare(name: String, type: String, value: Any?): String {
                |        declarations.add("$" + name + ": " + type)
                |        variables[name] = value
                |        return "$" + name
                |    }
                |    fun pass(type: String, value: Any?): String = declare("$prefix" + (variables.size + 1), type, value)
                |    fun values(): Map<String, Any?> = variables
                |    fun toGraphQL(): String = if (declarations.isEmpty()) "" else declarations.joinToString(", ", "(", ")")
                |}
            """.trimMargin()
        },

        /** An absent value is an empty `Optional` in Java, and `null` as a variable. */
        Java {
            override val variables = Type.Dict(Type.String, Type.Any)

            override fun arguments(prefix: String) = """
                |public static final class $GRAPHQL_ARGUMENTS {
                |    private final java.util.List<String> declarations = new java.util.ArrayList<>();
                |    private final java.util.Map<String, Object> variables = new java.util.LinkedHashMap<>();
                |    public String declare(String name, String type, Object value) {
                |        declarations.add("$" + name + ": " + type);
                |        variables.put(name, value instanceof java.util.Optional<?> optional ? optional.orElse(null) : value);
                |        return "$" + name;
                |    }
                |    public String pass(String type, Object value) { return declare("$prefix" + (variables.size() + 1), type, value); }
                |    public java.util.Map<String, Object> values() { return variables; }
                |    public String toGraphQL() { return declarations.isEmpty() ? "" : "(" + String.join(", ", declarations) + ")"; }
                |}
                |
            """.trimMargin()
        },

        /** An absent value is `None` in Scala, and `null` as a variable. */
        Scala {
            override val variables = Type.Dict(Type.String, Type.Any)

            override fun arguments(prefix: String) = """
                |class $GRAPHQL_ARGUMENTS {
                |  private val declarations = scala.collection.mutable.ListBuffer[String]()
                |  private val variables = scala.collection.mutable.LinkedHashMap[String, Any]()
                |  def declare(name: String, graphQLType: String, value: Any): String = {
                |    declarations += "$" + name + ": " + graphQLType
                |    variables(name) = value match {
                |      case Some(some) => some
                |      case None => null
                |      case other => other
                |    }
                |    "$" + name
                |  }
                |  def pass(graphQLType: String, value: Any): String = declare("$prefix" + (variables.size + 1), graphQLType, value)
                |  def values(): Map[String, Any] = variables.toMap
                |  def toGraphQL(): String = if (declarations.isEmpty) "" else declarations.mkString("(", ", ", ")")
                |}
            """.trimMargin()
        },
        ;

        /** The type of the variables that a `GraphQLArguments` collected, by name. */
        abstract val variables: Type

        abstract fun arguments(prefix: String): String
    }

    /**
     * An rpc: its name, its `Service` function, the rpc itself, the alias its document selects its result under, and its
     * variables. [isTyped] says that a document selects every field of its result; without one, only a selection calls it.
     */
    class Request(val name: String, val function: Function, val rpc: Rpc, val alias: String, val variables: List<Variable>, val isTyped: Boolean) {
        val builder: String = builder(name)
        val result: Reference = rpc.result
    }

    /** A variable of a document: the [field] it is sent as, typed [type] in GraphQL, and the [parameter] that holds its value. */
    class Variable(val field: Field, val type: String, val parameter: Parameter)

    /**
     * The selection of every type and union that [results] return, through all their fields. A field that takes a required
     * argument takes its arguments as parameters, and passes each as a variable of the document.
     */
    fun selections(results: List<Reference>): List<Struct> = passesArguments(results).let { passes ->
        reachable(results).map { model ->
            struct(model.selection()) {
                visibility(Visibility.PUBLIC)
                field(FIELDS, Type.Custom(STRING_BUILDER))
                if (passes) field(ARGUMENTS, Type.Custom(GRAPHQL_ARGUMENTS))
                val arguments = FieldCall(field = ARGUMENTS).takeIf { passes }
                when (model) {
                    is Union -> model.entries.mapNotNull { operations.composite(it) as? TypeDefinition }.forEach { member ->
                        method(model, "on${Name.of(member.identifier.value).pascalCase()}", listOf(Literal(" ... on ${operations.graphQLName(member)} ", Type.String)), emptyList(), member, arguments)
                    }
                    is TypeDefinition -> model.shape.fields.forEach { field ->
                        operations.composite(field.reference).let { nested ->
                            method(model, field.identifier.value, field.selected(nested != null), field.required(), nested, arguments)
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
    }

    /**
     * Whether a selection of [results] passes arguments: a field they reach takes a required argument. Its selections
     * then share the `GraphQLArguments` that collects the variables of the document.
     */
    fun passesArguments(results: List<Reference>): Boolean = reachable(results).any { model -> model is TypeDefinition && model.shape.fields.any { it.required().isNotEmpty() } }

    /** The arguments a selection passes to a field: all of them when one is required, none when the field is selected without. */
    private fun AstField.required(): List<GraphQLOperations.Argument> = operations.arguments(this).takeUnless { operations.isSelectable(this) }.orEmpty()

    /** The GraphQL text that selects a field, with each argument it is passed as the variable `GraphQLArguments` gives for it. */
    private fun AstField.selected(isNested: Boolean): List<Expression> = required().let { arguments ->
        when {
            arguments.isEmpty() -> listOf(Literal(" ${identifier.value}${if (isNested) " " else ""}", Type.String))
            else -> arguments.flatMapIndexed { index, argument ->
                listOf(Literal("${if (index == 0) " ${identifier.value}(" else ", "}${argument.name}: ", Type.String), argument.passed())
            } + Literal(if (isNested) ") " else ")", Type.String)
        }
    }

    private fun GraphQLOperations.Argument.passed(): Expression = FunctionCall(
        receiver = FieldCall(field = ARGUMENTS),
        name = PASS,
        arguments = mapOf(Name.of("type") to Literal(type, Type.String), VALUE to VariableReference(parameter())),
    )

    private fun GraphQLOperations.Argument.parameter(): Name = Name(listOf(Name.of(name).camelCase().sanitized()))

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
                models = (fields.mapNotNull { (_, reference) -> reference.model() } + model.argumentModels()).distinct(),
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
        is TypeDefinition -> shape.fields.map { it.identifier.value to it.reference }
        is Endpoint, is Channel, is Rpc, is Enum, is Refined, is Part -> emptyList()
    }

    /** The model a field of a partial result refers to, which is not a result itself. */
    private fun Reference.model(): String? = takeIf { operations.composite(it) == null }?.custom()

    /** The models that the selection of a type takes as the arguments of its fields. */
    private fun Definition.argumentModels(): List<String> = (this as? TypeDefinition)?.shape?.fields.orEmpty()
        .flatMap { it.required() }
        .mapNotNull { it.reference.custom() }

    private fun Reference.custom(): String? = when (this) {
        is Reference.Iterable -> reference.custom()
        is Reference.Custom -> Name.of(value).pascalCase()
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
     * `QueryTodoGraphQL.request(id)` writes the request of an rpc with every field of its result, and
     * `QueryTodoGraphQL.request(id) { ... }` with the fields that the block selects, as a `GraphQLRequest` with the query
     * and its `GraphQLVariables`. It has no fields, so it is an `object` in Kotlin and a record without components in Java.
     *
     * A selection that passes arguments adds variables to the document, so its request collects them in a
     * `GraphQLArguments`, after the variables of the rpc, and sends them by name instead of as `GraphQLVariables`.
     */
    fun builder(request: Request): Struct = struct(request.builder) {
        val selection = operations.composite(request.result)
        val passes = passesArguments(listOf(request.result))
        val close = operations.close(request.rpc)

        fun request(selection: Definition?) = function(REQUEST) {
            visibility(Visibility.PUBLIC)
            request.function.parameters.forEach { arg(it.name, it.type) }
            selection?.let { arg(SELECTION, it.block()) }
            returnType(type(GRAPHQL_REQUEST))
            if (passes) {
                assign(ARGUMENTS.value(), newInstance(GRAPHQL_ARGUMENTS))
                request.variables.forEach { variable ->
                    functionCall(DECLARE.value(), receiver = VariableReference(ARGUMENTS)) {
                        arg(Name.of("name"), Literal(variable.field.name.value(), Type.String))
                        arg(Name.of("type"), Literal(variable.type, Type.String))
                        arg(VALUE, VariableReference(variable.parameter.name))
                    }
                }
            }
            val query = when {
                selection == null -> Literal(operations.document(request.rpc, request.alias)!!, Type.String)
                // The block declares the variables it passes, so it runs before the document is written.
                passes -> {
                    assign(SELECTED.value(), selection.selected(VariableReference(ARGUMENTS)))
                    concat(
                        Literal(operations.operation(request.rpc)!!, Type.String),
                        FunctionCall(receiver = VariableReference(ARGUMENTS), name = TO_GRAPHQL),
                        Literal("${operations.field(request.rpc, request.alias)} ", Type.String),
                        VariableReference(SELECTED),
                        Literal(close, Type.String),
                    )
                }
                else -> concat(Literal("${operations.open(request.rpc, request.alias)!!} ", Type.String), selection.selected(null), Literal(close, Type.String))
            }
            returns(
                construct(type(GRAPHQL_REQUEST)) {
                    arg("query", query)
                    when {
                        passes -> arg("variables", FunctionCall(receiver = VariableReference(ARGUMENTS), name = VALUES))
                        request.variables.isNotEmpty() -> arg(
                            "variables",
                            ConstructorStatement(Type.Custom(GRAPHQL_VARIABLES), request.variables.associate { it.field.name to VariableReference(it.parameter.name) }),
                        )
                    }
                },
            )
        }

        if (request.isTyped) request(null)
        selection?.let { request(it) }
        // A record nested in a Java record is only public and static when it says so.
        struct(GRAPHQL_REQUEST) {
            visibility(Visibility.PUBLIC)
            implements(Type.Custom("Wirespec.GraphQLRequest"))
            field("query", Type.String, isOverride = true)
            when {
                passes -> field("variables", language.variables)
                request.variables.isNotEmpty() -> field("variables", type(GRAPHQL_VARIABLES))
            }
        }
        when {
            passes -> raw(language.arguments(request.variables.map { it.field.name.value() }.variablePrefix()))
            request.variables.isNotEmpty() -> struct(GRAPHQL_VARIABLES) {
                visibility(Visibility.PUBLIC)
                request.variables.forEach { field(it.field.name, it.field.type) }
            }
        }
    }

    /** What `GraphQLArguments` starts the name of a variable with, so it is no variable of the rpc: those have these names. */
    private fun List<String>.variablePrefix(): String = generateSequence("arg") { "_$it" }.first { prefix -> none { it.startsWith(prefix) } }

    /**
     * Adds the [text] that selects a field, with the [arguments] it is passed as parameters, to the selected fields, and
     * after it the fields that the block selects of [nested] when there is one. Returns `this`. [shared] is the
     * `GraphQLArguments` of a selection that passes arguments, which the selection of [nested] shares.
     */
    private fun StructBuilder.method(model: Definition, name: String, text: List<Expression>, arguments: List<GraphQLOperations.Argument>, nested: Definition?, shared: Expression?) = function(Name(listOf(Name.of(name).camelCase().sanitized()))) {
        visibility(Visibility.PUBLIC)
        arguments.forEach { arg(it.parameter(), it.reference.convert()) }
        nested?.let { arg(SELECTION, it.block()) }
        returnType(type(model.selection()))
        (text + listOfNotNull(nested?.selected(shared))).let { parts ->
            functionCall(APPEND.value(), receiver = parts.dropLast(1).fold(FieldCall(field = FIELDS) as Expression) { receiver, part -> receiver.append(part) }) { arg(VALUE, parts.last()) }
        }
        returns(RawExpression("this"))
    }

    private fun Expression.append(value: Expression): Expression = FunctionCall(receiver = this, name = APPEND, arguments = mapOf(VALUE to value))

    /** The block that selects the fields of a type: on `this` in Kotlin, a function of the selection in Java and Scala. */
    private fun Definition.block(): Type = Type.Custom(selection()).let { selection ->
        when (language) {
            Language.Kotlin -> Type.Function(parameterTypes = emptyList(), returnType = Type.Unit, receiver = selection)
            Language.Java, Language.Scala -> Type.Function(parameterTypes = listOf(selection), returnType = selection)
        }
    }

    /** Runs the `selection` block on a new selection of this type, which shares [arguments] when it has them, and gives its fields between braces. */
    private fun Definition.selected(arguments: Expression?): Expression = ConstructorStatement(
        Type.Custom(selection()),
        mapOf(FIELDS to newInstance(STRING_BUILDER)) + arguments?.let { mapOf(ARGUMENTS to it) }.orEmpty(),
    )
        .let { empty ->
            when (language) {
                Language.Kotlin -> FunctionCall(receiver = empty, name = APPLY, arguments = mapOf(Name.of("block") to VariableReference(SELECTION)))
                Language.Java, Language.Scala -> FunctionCall(receiver = VariableReference(SELECTION), name = APPLY, arguments = mapOf(Name.of("t") to empty))
            }
        }
        .let { FunctionCall(receiver = it, name = TO_GRAPHQL) }

    // An IR constructor call without arguments has no parentheses in Kotlin.
    private fun newInstance(type: String): Expression = when (language) {
        Language.Kotlin -> FunctionCall(name = Name(listOf(type)))
        Language.Java, Language.Scala -> ConstructorStatement(Type.Custom(type))
    }

    private fun concat(vararg parts: Expression): Expression = parts.reduce { left, right -> BinaryOp(left, BinaryOp.Operator.PLUS, right) }

    private fun reachable(results: List<Reference>): List<Definition> = generateSequence(results.mapNotNull { operations.composite(it) }.distinct()) { found ->
        (found + found.flatMap { it.nested() }).distinct().takeIf { it.size > found.size }
    }.last()

    private fun Definition.nested(): List<Definition> = when (this) {
        is Union -> entries.mapNotNull { operations.composite(it) }
        is TypeDefinition -> shape.fields.mapNotNull { operations.composite(it.reference) }
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
        const val GRAPHQL_ARGUMENTS = "GraphQLArguments"

        private val FIELDS = Name(listOf("graphQLFields"))
        private val ARGUMENTS = Name(listOf("graphQLArguments"))
        private val SELECTED = Name(listOf("selected"))
        private val DECLARE = Name(listOf("declare"))
        private val PASS = Name(listOf("pass"))
        private val VALUES = Name(listOf("values"))
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
