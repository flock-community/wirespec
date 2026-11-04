package community.flock.wirespec.integration.graphql.extension

import community.flock.wirespec.compiler.core.emit.DEFAULT_SHARED_PACKAGE_STRING
import community.flock.wirespec.compiler.core.emit.FileExtension
import community.flock.wirespec.compiler.core.emit.PackageName
import community.flock.wirespec.compiler.core.ir.Assignment
import community.flock.wirespec.compiler.core.ir.ConstructorStatement
import community.flock.wirespec.compiler.core.ir.Element
import community.flock.wirespec.compiler.core.ir.Expression
import community.flock.wirespec.compiler.core.ir.Field
import community.flock.wirespec.compiler.core.ir.FieldCall
import community.flock.wirespec.compiler.core.ir.Function
import community.flock.wirespec.compiler.core.ir.FunctionCall
import community.flock.wirespec.compiler.core.ir.HasName
import community.flock.wirespec.compiler.core.ir.IR
import community.flock.wirespec.compiler.core.ir.Import
import community.flock.wirespec.compiler.core.ir.Interface
import community.flock.wirespec.compiler.core.ir.Literal
import community.flock.wirespec.compiler.core.ir.LiteralList
import community.flock.wirespec.compiler.core.ir.LiteralMap
import community.flock.wirespec.compiler.core.ir.Name
import community.flock.wirespec.compiler.core.ir.Namespace
import community.flock.wirespec.compiler.core.ir.NullableOf
import community.flock.wirespec.compiler.core.ir.Parameter
import community.flock.wirespec.compiler.core.ir.RawElement
import community.flock.wirespec.compiler.core.ir.RawExpression
import community.flock.wirespec.compiler.core.ir.ReturnStatement
import community.flock.wirespec.compiler.core.ir.Statement
import community.flock.wirespec.compiler.core.ir.Struct
import community.flock.wirespec.compiler.core.ir.Type
import community.flock.wirespec.compiler.core.ir.TypeDescriptor
import community.flock.wirespec.compiler.core.ir.VariableReference
import community.flock.wirespec.compiler.core.ir.Visibility
import community.flock.wirespec.compiler.core.ir.converter.convert
import community.flock.wirespec.compiler.core.ir.emit.placeInPackage
import community.flock.wirespec.compiler.core.ir.extension.IrExtension
import community.flock.wirespec.compiler.core.ir.import
import community.flock.wirespec.compiler.core.parse.ast.AST
import community.flock.wirespec.compiler.core.parse.ast.Reference
import community.flock.wirespec.compiler.core.parse.ast.Rpc
import community.flock.wirespec.compiler.core.parse.ast.fields
import community.flock.wirespec.converter.graphql.GraphQLOperations
import community.flock.wirespec.compiler.core.ir.File as LanguageFile

/**
 * Generates a typed GraphQL client for every `rpc` marked `@GraphQLQuery` or `@GraphQLMutation`, as the GraphQL
 * converter marks the fields of the root operation types. Any other rpc, a subscription or one without a GraphQL
 * annotation, gets no client.
 *
 * ```kotlin
 * val client = GraphQLClient(serialization, transportation)
 * val todo: Todo? = client.queryTodo(id = ID("1"))
 * ```
 *
 * Each rpc gets a `<Rpc>Client` in the `graphql` package that implements the rpc's `Service`. It posts the GraphQL
 * document of the rpc to `/graphql`, with the parameters as its variables, through the `Wirespec.Transportation` and
 * `Wirespec.Serialization` it is given, and reads the result from the response. [GraphQLOperations] writes the document
 * at build time, so the generated code needs no GraphQL library. `GraphQLClient` implements every `Service` at once by
 * delegating to those clients.
 *
 * The response carries errors next to its data. An rpc with an error type returns the first error, read as that type,
 * as its `Error`; an rpc without one fails with an [IllegalStateException] listing the [GRAPHQL_ERROR]s. A union in the
 * result is selected with `__typename`, which the serialization reads to pick the member.
 *
 * Register on the Java or Kotlin emitter; for any other [language] the IR is returned unchanged.
 */
public class GraphQLClientExtension(
    private val packageName: PackageName,
    private val language: FileExtension,
) : IrExtension {

    private val dialect: Dialect? = when (language) {
        FileExtension.Kotlin -> Dialect.Kotlin
        FileExtension.Java -> Dialect.Java
        FileExtension.TypeScript,
        FileExtension.Python,
        FileExtension.Rust,
        FileExtension.Scala,
        FileExtension.Wirespec,
        FileExtension.JSON,
        FileExtension.YAML,
        FileExtension.AvroJson,
        FileExtension.AvroIdl,
        FileExtension.GraphQL,
        -> null
    }

    override fun extend(ir: IR, ast: AST): IR = dialect
        ?.let { dialect ->
            val definitions = ast.modules.flatMap { it.statements }
            val operations = GraphQLOperations(definitions)
            definitions.filterIsInstance<Rpc>()
                .filter { rpc -> rpc.annotations.any { it.name in OPERATIONS } }
                .mapNotNull { rpc ->
                    operations.open(rpc, RESULT)?.let { open -> Operation(rpc, operations.document(rpc, RESULT)!!, open, ir.serviceOf(rpc)) }
                }
                .takeIf { it.isNotEmpty() }
                ?.let { GraphQLQueryBuilder(operations, language).let { builder -> ir + Client(dialect, builder, it).files() + QueryBuilder(dialect, builder, it).files() } }
        }
        ?: ir

    private class Operation(val rpc: Rpc, val document: String, val open: String, val service: Service) {
        val name: String = service.rpc.name.pascalCase()
        val client: String = "${name}Client"

        /** Each variable keeps the name of the argument it is passed as, and takes the parameter of the `Service` function. */
        val variables: List<Pair<Field, Parameter>> = rpc.shape.fields
            .zip(service.function.parameters)
            .map { (field, parameter) -> Field(Name(listOf(field.identifier.value)), parameter.type) to parameter }
    }

    /** A selection per type and the `GraphQLRequests` that take them. */
    private inner class QueryBuilder(private val dialect: Dialect, private val builder: GraphQLQueryBuilder, private val operations: List<Operation>) {

        private val results = operations.map { it.rpc.result }

        fun files(): List<LanguageFile> = builder.selections(results).map { it.inFile(dialect, emptyList(), listOfNotNull(builder.dslMarker)) } +
            builder.results(results).map { result -> result.struct.inFile(dialect, result.models.map { import("${packageName.value}.model", it) }) } +
            listOfNotNull(builder.dslMarkerDefinition?.let { LanguageFile(Name.of(GraphQLQueryBuilder.DSL_MARKER), listOf(it)).placeInPackage(packageName, GRAPHQL) }) +
            builder.requests(operations.map { GraphQLQueryBuilder.Request(it.service.function, it.open, it.rpc.result, it.client, it.variables.map { (field, parameter) -> field.name to parameter }) })
                .inFile(dialect, operations.flatMap { it.service.imports })
    }

    private fun HasName.inFile(dialect: Dialect, imports: List<Import>, annotations: List<RawElement> = emptyList()): LanguageFile = LanguageFile(name, (dialect.imports + imports).distinct() + annotations + this)
        .placeInPackage(packageName, GRAPHQL)

    /** The `Service` interface the emitter wrote for an rpc, with its parameters named as that language names them. */
    private class Service(val rpc: Namespace, val function: Function, val imports: List<Import>)

    private fun IR.serviceOf(rpc: Rpc): Service = filterIsInstance<LanguageFile>()
        .firstNotNullOfOrNull { file ->
            file.elements.filterIsInstance<Namespace>()
                .find { it.name.pascalCase() == Name.of(rpc.identifier.value).pascalCase() && it.extends?.name?.value() == RPC }
                ?.let { namespace ->
                    namespace.elements.filterIsInstance<Interface>()
                        .find { it.name.pascalCase() == SERVICE }
                        ?.elements?.filterIsInstance<Function>()?.singleOrNull()
                        ?.let { Service(namespace, it, file.elements.filterIsInstance<Import>()) }
                }
        }
        ?: error("The emitter wrote no Service for rpc ${rpc.identifier.value}")

    private inner class Client(private val dialect: Dialect, private val builder: GraphQLQueryBuilder, private val operations: List<Operation>) {

        fun files(): List<LanguageFile> = operations.map { it.file() } +
            listOfNotNull(errorFile().takeIf { operations.any { it.rpc.error == null } }) +
            graphQLClientFile()

        private fun Operation.file(): LanguageFile = struct(
            name = client,
            interfaces = listOf(Type.Custom("$name.Service")),
            elements = listOf(call(), fromRawResponse()) + listOfNotNull(selectedCall(), selectedFromRawResponse()) + envelopes() + selectedEnvelopes(),
        ).inFile(service.imports + import("${packageName.value}.rpc", name))

        private fun Operation.call(): Function = service.function.copy(
            returnType = returnType(),
            isOverride = true,
            body = listOf(
                Assignment(Name.of("request"), ConstructorStatement(Type.Custom(REQUEST), requestArguments())),
                Assignment(Name.of("rawRequest"), rawRequest()),
                ReturnStatement(dialect.transport(FROM_RAW_RESPONSE, returnType() == Type.Unit)),
            ),
        )

        /** The block that selects the fields of the result, for a result with fields. */
        private val Operation.block: Type? get() = builder.block(rpc.result)

        /** `queryTodo(id) { ... }` reads the fields the block selects into the partial result. */
        private fun Operation.selectedCall(): Function? = block?.let { block ->
            service.function.copy(
                parameters = service.function.parameters + Parameter(SELECTION, block),
                returnType = builder.resultType(rpc.result),
                body = listOf(
                    Assignment(
                        Name.of("request"),
                        FunctionCall(
                            receiver = RawExpression(GraphQLQueryBuilder.REQUESTS),
                            name = Name(listOf(service.function.name.camelCase())),
                            arguments = (service.function.parameters.map { it.name } + SELECTION).associateWith { VariableReference(it) },
                        ),
                    ),
                    Assignment(Name.of("rawRequest"), rawRequest()),
                    ReturnStatement(dialect.transport(FROM_SELECTED_RAW_RESPONSE, false)),
                ),
            )
        }

        private fun Operation.selectedFromRawResponse(): Function? = block?.let {
            Function(
                name = FROM_SELECTED_RAW_RESPONSE,
                parameters = listOf(Parameter(Name.of("rawResponse"), Type.Custom("Wirespec.RawResponse"))),
                returnType = builder.resultType(rpc.result),
                body = dialect.fromSelectedRawResponse(this),
                visibility = Visibility.PRIVATE,
            )
        }

        private fun Operation.selectedEnvelopes(): List<Struct> = block?.let {
            listOf(
                nested(
                    SELECTED_RESPONSE,
                    listOf(
                        Field(Name.of("data"), Type.Nullable(Type.Custom(SELECTED_DATA))),
                        Field(Name.of("errors"), Type.Nullable(Type.Array(rpc.error?.convert() ?: Type.Custom(GRAPHQL_ERROR)))),
                    ),
                ),
                nested(SELECTED_DATA, listOf(Field(Name.of(RESULT), builder.resultType(rpc.result.copy(isNullable = true))))),
            )
        }.orEmpty()

        private fun Operation.requestArguments(): Map<Name, Expression> = mapOf(Name.of("query") to Literal(document, Type.String)) +
            variables.takeIf { it.isNotEmpty() }
                ?.let { variables -> mapOf(Name.of("variables") to ConstructorStatement(Type.Custom(VARIABLES), variables.associate { (field, parameter) -> field.name to VariableReference(parameter.name) })) }
                .orEmpty()

        private fun rawRequest(): ConstructorStatement = ConstructorStatement(
            type = Type.Custom("Wirespec.RawRequest"),
            namedArguments = mapOf(
                Name.of("method") to Literal("POST", Type.String),
                Name.of("path") to LiteralList(listOf(Literal("graphql", Type.String)), Type.String),
                Name.of("queries") to LiteralMap(emptyMap(), Type.String, Type.Array(Type.String)),
                Name.of("headers") to LiteralMap(
                    mapOf("Content-Type" to LiteralList(listOf(Literal("application/json", Type.String)), Type.String)),
                    Type.String,
                    Type.Array(Type.String),
                ),
                Name.of("body") to NullableOf(
                    FunctionCall(
                        receiver = FieldCall(field = SERIALIZATION),
                        name = Name.of("serializeBody"),
                        arguments = mapOf(Name.of("t") to VariableReference(Name.of("request")), Name.of("type") to dialect.type(REQUEST)),
                    ),
                ),
            ),
        )

        private fun Operation.fromRawResponse(): Function = Function(
            name = FROM_RAW_RESPONSE,
            parameters = listOf(Parameter(Name.of("rawResponse"), Type.Custom("Wirespec.RawResponse"))),
            returnType = returnType(),
            body = dialect.fromRawResponse(this),
            visibility = Visibility.PRIVATE,
        )

        private fun Operation.returnType(): Type = rpc.error?.let { Type.Custom("$name.Response") } ?: rpc.result.convert()

        private fun Operation.envelopes(): List<Struct> = listOfNotNull(
            nested(REQUEST, listOf(Field(Name.of("query"), Type.String)) + listOfNotNull(Field(Name.of("variables"), Type.Custom(VARIABLES)).takeIf { variables.isNotEmpty() })),
            variables.takeIf { it.isNotEmpty() }?.let { variables -> nested(VARIABLES, variables.map { it.first }) },
            nested(
                RESPONSE,
                listOf(
                    Field(Name.of("data"), Type.Nullable(Type.Custom(DATA))),
                    Field(Name.of("errors"), Type.Nullable(Type.Array(rpc.error?.convert() ?: Type.Custom(GRAPHQL_ERROR)))),
                ),
            ),
            nested(DATA, listOf(Field(Name.of(RESULT), Type.Nullable(rpc.result.dataType())))),
        )

        /** GraphQL has no void, so a field without a result returns a Boolean. */
        private fun Reference.dataType(): Type = when (this) {
            is Reference.Unit -> Type.Boolean
            is Reference.Any, is Reference.Custom, is Reference.Dict, is Reference.Iterable, is Reference.Primitive -> copy(isNullable = false).convert()
        }

        private fun errorFile(): LanguageFile = struct(
            name = GRAPHQL_ERROR,
            fields = listOf(
                Field(Name.of("message"), Type.String),
                Field(Name.of("locations"), Type.Nullable(Type.Array(Type.Custom("$GRAPHQL_ERROR.Location")))),
                Field(Name.of("path"), Type.Nullable(Type.Array(Type.Any))),
                Field(Name.of("extensions"), Type.Nullable(Type.Dict(Type.String, Type.Any))),
            ),
            elements = listOf(nested("Location", listOf(Field(Name.of("line"), Type.Integer()), Field(Name.of("column"), Type.Integer())))),
        ).inFile(emptyList())

        private fun graphQLClientFile(): LanguageFile = struct(
            name = GRAPHQL_CLIENT,
            interfaces = operations.map { Type.Custom("${it.name}.Service") },
            elements = operations.flatMap { operation ->
                listOfNotNull(
                    operation.service.function.copy(returnType = operation.returnType(), isOverride = true).delegating(operation),
                    operation.selectedCall()?.delegating(operation),
                )
            },
        ).inFile(operations.flatMap { it.service.imports + import("${packageName.value}.rpc", it.name) })

        /** Calls the same function on the client of [operation]. */
        private fun Function.delegating(operation: Operation): Function = copy(
            body = listOf(
                ReturnStatement(
                    FunctionCall(
                        receiver = ConstructorStatement(
                            Type.Custom(operation.client),
                            mapOf(SERIALIZATION to FieldCall(field = SERIALIZATION), TRANSPORTATION to FieldCall(field = TRANSPORTATION)),
                        ),
                        name = Name(listOf(name.camelCase())),
                        arguments = parameters.associate { it.name to VariableReference(it.name) },
                    ),
                ),
            ),
        )

        private fun struct(name: String, fields: List<Field> = CLIENT_FIELDS, interfaces: List<Type.Custom> = emptyList(), elements: List<Element>): Struct = Struct(name = Name.of(name), fields = fields, interfaces = interfaces, elements = elements)

        // A record nested in a Java record is only public and static when it says so.
        private fun nested(name: String, fields: List<Field>): Struct = Struct(name = Name.of(name), fields = fields, visibility = Visibility.PUBLIC)

        private fun Struct.inFile(imports: List<Import>): LanguageFile = inFile(dialect, imports)
    }

    private enum class Dialect {
        Kotlin {
            override val imports = listOf(import("$DEFAULT_SHARED_PACKAGE_STRING.kotlin", "Wirespec"), import("kotlin.reflect", "typeOf"))

            override fun type(name: String): Expression = TypeDescriptor(Type.Custom(name))

            override fun transport(read: Name, isUnit: Boolean): Expression = RawExpression("${read.value()}(transportation.transport(rawRequest))")

            override fun fromRawResponse(operation: Operation): List<Statement> = listOfNotNull(
                read(RESPONSE),
                failOnErrors().takeIf { operation.rpc.error == null },
                ReturnStatement(
                    when (operation.rpc.error) {
                        null -> RawExpression(result(operation.rpc))
                        else -> RawExpression("response.errors?.firstOrNull()?.let { ${operation.name}.Error(value = it) } ?: ${operation.name}.Result(value = ${result(operation.rpc)})")
                    },
                ),
            )

            override fun fromSelectedRawResponse(operation: Operation): List<Statement> = listOf(read(SELECTED_RESPONSE), failOnErrors(), ReturnStatement(RawExpression(result(operation.rpc))))

            private fun read(response: String): Statement = Assignment(
                Name.of("response"),
                RawExpression(
                    "serialization.deserializeBody<$response>(rawResponse.body ?: error(\"GraphQL response without a body, status \" + rawResponse.statusCode), typeOf<$response>())",
                ),
            )

            private fun failOnErrors(): Statement = RawExpression("response.errors?.takeIf { it.isNotEmpty() }?.let { error(\"GraphQL request failed: \" + it) }")

            private fun result(rpc: Rpc): String = when {
                rpc.result is Reference.Unit -> "Unit"
                rpc.result.isNullable -> "response.data?.$RESULT"
                else -> "response.data?.$RESULT ?: error(\"${rpc.noData()}\")"
            }
        },

        Java {
            override val imports = listOf(import("$DEFAULT_SHARED_PACKAGE_STRING.java", "Wirespec"))

            override fun type(name: String): Expression = RawExpression("Wirespec.getType($name.class, null)")

            override fun transport(read: Name, isUnit: Boolean): Expression = RawExpression(
                when {
                    isUnit -> "transportation().transport(rawRequest).thenApply(rawResponse -> { ${read.value()}(rawResponse); return null; })"
                    else -> "transportation().transport(rawRequest).thenApply(this::${read.value()})"
                },
            )

            override fun fromRawResponse(operation: Operation): List<Statement> = listOfNotNull(
                read(RESPONSE),
                failOnErrors().takeIf { operation.rpc.error == null },
                when (operation.rpc.error) {
                    null -> result(operation.rpc)?.let { ReturnStatement(RawExpression(it)) }
                    else -> ReturnStatement(
                        RawExpression(
                            "response.errors().flatMap(errors -> errors.stream().findFirst()).<${operation.name}.Response>map(${operation.name}.Error::new)" +
                                ".orElseGet(() -> new ${operation.name}.Result(${result(operation.rpc) ?: "null"}))",
                        ),
                    )
                },
            )

            override fun fromSelectedRawResponse(operation: Operation): List<Statement> = listOfNotNull(read(SELECTED_RESPONSE), failOnErrors(), result(operation.rpc, SELECTED_DATA)?.let { ReturnStatement(RawExpression(it)) })

            private fun read(response: String): Statement = Assignment(
                Name.of("response"),
                RawExpression(
                    "serialization().<$response>deserializeBody(rawResponse.body().orElseThrow(() -> new IllegalStateException(\"GraphQL response without a body, status \" + rawResponse.statusCode())), Wirespec.getType($response.class, null))",
                ),
            )

            private fun failOnErrors(): Statement = RawExpression("response.errors().filter(errors -> !errors.isEmpty()).ifPresent(errors -> { throw new IllegalStateException(\"GraphQL request failed: \" + errors); })")

            /** Null for a Unit result, whose `fromRawResponse` is void. */
            private fun result(rpc: Rpc, data: String = DATA): String? = when {
                rpc.result is Reference.Unit -> null
                rpc.result.isNullable -> "response.data().flatMap($data::$RESULT)"
                else -> "response.data().flatMap($data::$RESULT).orElseThrow(() -> new IllegalStateException(\"${rpc.noData()}\"))"
            }
        },
        ;

        abstract val imports: List<Import>

        /** The runtime type of an envelope, as `Wirespec.Serialization` takes it. */
        abstract fun type(name: String): Expression

        /** Sends `rawRequest` and reads the response with the function named [read]. */
        abstract fun transport(read: Name, isUnit: Boolean): Expression

        abstract fun fromRawResponse(operation: Operation): List<Statement>

        /** Reads the partial result; GraphQL errors fail it, whether or not the rpc has an error type. */
        abstract fun fromSelectedRawResponse(operation: Operation): List<Statement>

        protected fun Rpc.noData(): String = "GraphQL response without data for ${identifier.value}"
    }

    public companion object {
        /** The type of the errors in a response, for an rpc without an error type of its own. */
        public const val GRAPHQL_ERROR: String = "GraphQLError"

        private const val GRAPHQL = "graphql"
        private val OPERATIONS = setOf("GraphQLQuery", "GraphQLMutation")
        private const val GRAPHQL_CLIENT = "GraphQLClient"
        private const val RPC = "Wirespec.Rpc"
        private const val SERVICE = "Service"
        private const val RESULT = "result"
        private const val REQUEST = "GraphQLRequest"
        private const val VARIABLES = "GraphQLVariables"
        private const val RESPONSE = "GraphQLResponse"
        private const val DATA = "GraphQLData"
        private const val SELECTED_RESPONSE = "GraphQLSelectedResponse"
        private const val SELECTED_DATA = "GraphQLSelectedData"

        private val SERIALIZATION = Name.of("serialization")
        private val TRANSPORTATION = Name.of("transportation")
        private val FROM_RAW_RESPONSE = Name.of("fromRawResponse")
        private val FROM_SELECTED_RAW_RESPONSE = Name.of("fromSelectedRawResponse")
        private val SELECTION = Name.of("selection")
        private val CLIENT_FIELDS = listOf(
            Field(SERIALIZATION, Type.Custom("Wirespec.Serialization")),
            Field(TRANSPORTATION, Type.Custom("Wirespec.Transportation")),
        )
    }
}
