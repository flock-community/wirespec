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
import community.flock.wirespec.compiler.core.ir.IR
import community.flock.wirespec.compiler.core.ir.Import
import community.flock.wirespec.compiler.core.ir.Interface
import community.flock.wirespec.compiler.core.ir.Literal
import community.flock.wirespec.compiler.core.ir.LiteralList
import community.flock.wirespec.compiler.core.ir.LiteralMap
import community.flock.wirespec.compiler.core.ir.Name
import community.flock.wirespec.compiler.core.ir.Namespace
import community.flock.wirespec.compiler.core.ir.NullableOf
import community.flock.wirespec.compiler.core.ir.Package
import community.flock.wirespec.compiler.core.ir.Parameter
import community.flock.wirespec.compiler.core.ir.RawExpression
import community.flock.wirespec.compiler.core.ir.ReturnStatement
import community.flock.wirespec.compiler.core.ir.Statement
import community.flock.wirespec.compiler.core.ir.Struct
import community.flock.wirespec.compiler.core.ir.Type
import community.flock.wirespec.compiler.core.ir.TypeDescriptor
import community.flock.wirespec.compiler.core.ir.TypeParameter
import community.flock.wirespec.compiler.core.ir.VariableReference
import community.flock.wirespec.compiler.core.ir.Visibility
import community.flock.wirespec.compiler.core.ir.converter.convert
import community.flock.wirespec.compiler.core.ir.emit.placeInPackage
import community.flock.wirespec.compiler.core.ir.extension.IrExtension
import community.flock.wirespec.compiler.core.ir.import
import community.flock.wirespec.compiler.core.ir.transformChildren
import community.flock.wirespec.compiler.core.ir.transformer
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
 * Each rpc gets one file in the `graphql` package, `<Rpc>GraphQL`, that bundles everything the rpc needs: its query
 * builder, a `GraphQLClient` that implements the rpc's `Service`, the envelopes of its response, and the selection and
 * partial result of every type its result reaches. The client posts the GraphQL document of the rpc to `/graphql`, with
 * the parameters as its variables, through the `Wirespec.Transportation` and `Wirespec.Serialization` it is given, and
 * reads the result from the response. [GraphQLOperations] writes the document at build time, so the generated code needs
 * no GraphQL library. A `GraphQLClient` in the root package, next to the `Client` of the endpoints, implements every
 * `Service` at once by delegating to those clients.
 *
 * To send the rpcs through a client of your own, each rpc also has an `Api`, a `Wirespec.GraphQLClient` that gives
 * the `Wirespec.GraphQLClientEdge` of the rpc for a serialization, as `Wirespec.Client` does for an endpoint. The edge
 * writes a `Wirespec.GraphQLRequest` as the raw request to send, and reads the raw response as a
 * `Wirespec.GraphQLResponse` that holds the data of the result next to the errors, and leaves both to its caller. The
 * generated clients send through it. An rpc with fields to select has a `SelectedApi` too, which reads the partial
 * result.
 *
 * Those four `Wirespec` types are added to the shared file by this extension, so register it on an emitter that emits
 * the shared file: the `Wirespec` of the runtime library does not have them.
 *
 * The response carries errors next to its data. An rpc with an error type returns the first error, read as that type,
 * as its `Error`; an rpc without one fails with an [IllegalStateException] listing the [GRAPHQL_ERROR]s. A union in the
 * result is selected with `__typename`, which the serialization reads to pick the member.
 *
 * Register on the Java, Kotlin or Scala emitter; for any other [language] the IR is returned unchanged.
 */
public class GraphQLClientExtension(
    private val packageName: PackageName,
    language: FileExtension,
) : IrExtension {

    private val dialect: Dialect? = when (language) {
        FileExtension.Kotlin -> Dialect.Kotlin
        FileExtension.Java -> Dialect.Java
        FileExtension.Scala -> Dialect.Scala
        FileExtension.TypeScript,
        FileExtension.Python,
        FileExtension.Rust,
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
            val operations = GraphQLOperations(ast)
            ast.modules.flatMap { it.statements }.filterIsInstance<Rpc>()
                .filter { rpc -> rpc.annotations.any { it.name in OPERATIONS } }
                .mapNotNull { rpc ->
                    operations.open(rpc, RESULT)?.let { open -> Operation(rpc, operations.document(rpc, RESULT)!!, open, operations.close(rpc), operations.path(rpc), ir.serviceOf(rpc)) }
                }
                .takeIf { it.isNotEmpty() }
                ?.let { ir.withGraphQLTypes(dialect) + Client(dialect, GraphQLQueryBuilder(operations, dialect.language), it).files() }
        }
        ?: ir

    /**
     * Adds the types the rpcs share to the `Wirespec` object of the shared file, next to the `Client` and `ClientEdge` of
     * the endpoints. Only the shared file of this run gets them: the generated code does not compile against a `Wirespec`
     * from elsewhere, such as the runtime library, so emit the shared file along with it.
     */
    private fun IR.withGraphQLTypes(dialect: Dialect): IR = map { element ->
        (element as? LanguageFile)
            ?.takeIf { file -> file.elements.any { it.isWirespec() } }
            ?.let { file -> file.copy(elements = file.elements.map { if (it is Namespace && it.isWirespec()) it.copy(elements = it.elements + dialect.graphQLTypes()) else it }) }
            ?: element
    }

    private fun Element.isWirespec(): Boolean = this is Namespace && name == Name.of(WIRESPEC)

    private class Operation(val rpc: Rpc, document: String, open: String, close: String, path: List<String>, val service: Service) {
        val name: String = service.rpc.name.pascalCase()

        /** The fields the response nests the result in: those that lead to the type the rpc is a field of, and its alias. */
        val fields: List<String> = path + RESULT

        /** The envelopes of the data of a response, named [data]: one for each of the [fields]. */
        fun dataEnvelopes(data: String): List<String> = fields.dropLast(1).runningFold(data) { envelope, field -> envelope + Name.of(field).pascalCase() }

        /** Each variable keeps the name of the argument it is passed as, and takes the parameter of the `Service` function. */
        val variables: List<Pair<Field, Parameter>> = rpc.shape.fields
            .zip(service.function.parameters)
            .map { (field, parameter) -> Field(Name(listOf(field.identifier.value)), parameter.type) to parameter }

        val request: GraphQLQueryBuilder.Request = GraphQLQueryBuilder.Request(name, service.function, document, open, close, rpc.result, variables)
        val builderType: String = request.builder
        val client: String = "$builderType.$GRAPHQL_CLIENT"
        val requestType: String = "$builderType.${GraphQLQueryBuilder.GRAPHQL_REQUEST}"
    }

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

        fun files(): List<LanguageFile> = operations.map { it.file() } + graphQLClientFile()

        /** The builder of the rpc, with its client, edges, envelopes, errors, selections and partial results nested in it. */
        private fun Operation.file(): LanguageFile = builder.results(listOf(rpc.result)).let { results ->
            builder.builder(request)
                .let { it.copy(elements = it.elements + client() + edges() + envelopes() + selectedEnvelopes() + errors() + selections() + results.map { result -> result.struct }) }
                .let(::listOf)
                .inFile(
                    Name.of(builderType),
                    service.imports + import("${packageName.value}.rpc", name) + results.flatMap { it.models }.map { import("${packageName.value}.model", it) },
                )
        }

        private fun Operation.client(): Struct = struct(
            name = GRAPHQL_CLIENT,
            interfaces = listOf(Type.Custom("$name.Service")),
            elements = listOfNotNull(
                call(),
                fromResponse(FROM_RESPONSE, TYPED_RESPONSE, returnType(), dialect.fromResponse(this)),
                selectedCall(),
                block?.let { fromResponse(FROM_SELECTED_RESPONSE, TYPED_SELECTED_RESPONSE, selectedReturnType(), dialect.fromSelectedResponse(this)) },
            ),
        )

        /** Reads the result of the rpc from the response of the edge, and fails on what the result cannot hold. */
        private fun Operation.fromResponse(name: Name, response: String, result: Type, read: List<Statement>): Function = Function(
            name = name,
            parameters = listOf(Parameter(RESPONSE_NAME, Type.Custom("$builderType.$response"))),
            returnType = dialect.result(result),
            body = read,
            visibility = Visibility.PRIVATE,
        )

        /** The edge, the response and the api of the rpc, and those of its partial result when it has fields to select. */
        private fun Operation.edges(): List<Struct> = listOf(
            edge(CLIENT_EDGE, TYPED_RESPONSE, RESPONSE, DATA),
            response(TYPED_RESPONSE, rpc.result.dataType()),
            api(API, CLIENT_EDGE, TYPED_RESPONSE),
        ) + block?.let {
            listOf(
                edge(SELECTED_CLIENT_EDGE, TYPED_SELECTED_RESPONSE, SELECTED_RESPONSE, SELECTED_DATA),
                response(TYPED_SELECTED_RESPONSE, builder.resultType(rpc.result.copy(isNullable = false))),
                api(SELECTED_API, SELECTED_CLIENT_EDGE, TYPED_SELECTED_RESPONSE),
            )
        }.orEmpty()

        private fun Operation.edgeType(response: String): Type.Custom = Type.Custom(WIRESPEC_CLIENT_EDGE, listOf(Type.Custom(requestType), Type.Custom("$builderType.$response")))

        /** Writes a request as the raw request to send, and reads the raw response, through its [envelope], as the [response]. */
        private fun Operation.edge(name: String, response: String, envelope: String, data: String): Struct = struct(
            name = name,
            fields = listOf(CLIENT_FIELDS.first()),
            interfaces = listOf(edgeType(response)),
            elements = listOf(
                Function(
                    name = TO,
                    parameters = listOf(Parameter(REQUEST, Type.Custom(requestType))),
                    returnType = RAW_REQUEST,
                    body = listOf(ReturnStatement(rawRequest())),
                    isOverride = true,
                ),
                Function(
                    name = FROM,
                    parameters = listOf(Parameter(RESPONSE_NAME, RAW_RESPONSE)),
                    returnType = Type.Custom("$builderType.$response"),
                    body = dialect.fromRawResponse(this, envelope, data, "$builderType.$response"),
                    isOverride = true,
                ),
            ),
        )

        /** What an edge reads: the [data] of the result, without the fields it is nested in, next to the errors. */
        private fun Operation.response(name: String, data: Type): Struct = Struct(
            name = Name.of(name),
            fields = listOf(
                Field(Name.of("data"), Type.Nullable(data), isOverride = true),
                Field(Name.of("errors"), Type.Nullable(Type.Array(errorType())), isOverride = true),
            ),
            interfaces = listOf(Type.Custom(WIRESPEC_RESPONSE, listOf(data, errorType()))),
            visibility = Visibility.PUBLIC,
        )

        private fun Operation.errorType(): Type = rpc.error?.convert() ?: Type.Custom(GRAPHQL_ERROR)

        /** What a client of your own takes to send the rpc: it gives the edge named [edge] for a serialization. */
        private fun Operation.api(name: String, edge: String, response: String): Struct = struct(
            name = name,
            fields = emptyList(),
            interfaces = listOf(Type.Custom(WIRESPEC_CLIENT, edgeType(response).generics)),
            elements = listOf(
                Function(
                    name = dialect.client,
                    parameters = listOf(Parameter(SERIALIZATION, CLIENT_FIELDS.first().type)),
                    returnType = edgeType(response),
                    body = listOf(ReturnStatement(ConstructorStatement(Type.Custom("$builderType.$edge"), mapOf(SERIALIZATION to VariableReference(SERIALIZATION))))),
                    isOverride = true,
                ),
            ),
        )

        /** In Kotlin, the `@DslMarker` that limits a block to its own selection, and every selection marked with it. */
        private fun Operation.selections(): List<Element> = listOfNotNull(builder.dslMarkerDefinition) +
            builder.selections(listOf(rpc.result)).flatMap { listOfNotNull(builder.dslMarker, it) }

        /** The selections and partial results nested in the file of this rpc, which other files name through its builder. */
        private fun Operation.nestedTypes(): Set<String> = listOf(rpc.result).let { results ->
            (builder.selections(results).map { it.name } + builder.results(results).map { it.struct.name }).map { it.pascalCase() }.toSet()
        }

        private fun Operation.call(): Function = service.function.copy(
            returnType = returnType(),
            isOverride = true,
            body = send(CLIENT_EDGE, FROM_RESPONSE, service.function.parameters.map { it.name }),
        )

        /** Asks the builder for the request of [arguments], sends it through the edge named [edge] and reads the result with [read]. */
        private fun Operation.send(edge: String, read: Name, arguments: List<Name>): List<Statement> = listOf(
            Assignment(EDGE, ConstructorStatement(Type.Custom("$builderType.$edge"), mapOf(SERIALIZATION to FieldCall(field = SERIALIZATION)))),
            Assignment(
                RAW_REQUEST_NAME,
                FunctionCall(
                    receiver = VariableReference(EDGE),
                    name = TO,
                    arguments = mapOf(
                        REQUEST to FunctionCall(receiver = dialect.builder(builderType), name = GraphQLQueryBuilder.REQUEST, arguments = arguments.associateWith { VariableReference(it) }),
                    ),
                ),
            ),
            ReturnStatement(dialect.transport(read)),
        )

        /** The block that selects the fields of the result, for a result with fields. */
        private val Operation.block: Type? get() = builder.block(rpc.result)

        /** `queryTodo(id) { ... }` reads the fields the block selects into the partial result. */
        private fun Operation.selectedCall(): Function? = block?.let { block ->
            service.function.copy(
                parameters = service.function.parameters + Parameter(SELECTION, block),
                returnType = selectedReturnType(),
                body = send(SELECTED_CLIENT_EDGE, FROM_SELECTED_RESPONSE, service.function.parameters.map { it.name } + SELECTION),
            )
        }

        private fun Operation.selectedReturnType(): Type = builder.resultType(rpc.result)

        private fun Operation.selectedEnvelopes(): List<Struct> = block?.let {
            listOf(
                nested(
                    SELECTED_RESPONSE,
                    listOf(
                        Field(Name.of("data"), Type.Nullable(Type.Custom(SELECTED_DATA))),
                        Field(Name.of("errors"), Type.Nullable(Type.Array(errorType()))),
                    ),
                ),
            ) + data(SELECTED_DATA, builder.resultType(rpc.result.copy(isNullable = true)))
        }.orEmpty()

        private fun Operation.rawRequest(): ConstructorStatement = ConstructorStatement(
            type = RAW_REQUEST,
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
                        arguments = mapOf(Name.of("t") to VariableReference(REQUEST), Name.of("type") to dialect.type(requestType)),
                    ),
                ),
            ),
        )

        private fun Operation.returnType(): Type = rpc.error?.let { Type.Custom("$name.Response") } ?: rpc.result.convert()

        private fun Operation.envelopes(): List<Struct> = listOf(
            nested(
                RESPONSE,
                listOf(
                    Field(Name.of("data"), Type.Nullable(Type.Custom(DATA))),
                    Field(Name.of("errors"), Type.Nullable(Type.Array(errorType()))),
                ),
            ),
        ) + data(DATA, Type.Nullable(rpc.result.dataType()))

        /** The data of a response, named [name]: an envelope for each field the [result] is nested in. */
        private fun Operation.data(name: String, result: Type): List<Struct> = dataEnvelopes(name).let { envelopes ->
            envelopes.zip(fields).mapIndexed { index, (envelope, field) ->
                nested(envelope, listOf(Field(Name(listOf(field)), envelopes.getOrNull(index + 1)?.let { Type.Nullable(Type.Custom(it)) } ?: result)))
            }
        }

        /** GraphQL has no void, so a field without a result returns a Boolean. */
        private fun Reference.dataType(): Type = when (this) {
            is Reference.Unit -> Type.Boolean
            is Reference.Any, is Reference.Custom, is Reference.Dict, is Reference.Iterable, is Reference.Primitive -> copy(isNullable = false).convert()
        }

        /** The errors of a response, for an rpc without an error type of its own. */
        private fun Operation.errors(): List<Element> = when (rpc.error) {
            null -> struct(
                name = GRAPHQL_ERROR,
                fields = listOf(
                    Field(Name.of("message"), Type.String),
                    Field(Name.of("locations"), Type.Nullable(Type.Array(Type.Custom("$GRAPHQL_ERROR.Location")))),
                    Field(Name.of("path"), Type.Nullable(Type.Array(Type.Any))),
                    Field(Name.of("extensions"), Type.Nullable(Type.Dict(Type.String, Type.Any))),
                ),
                elements = emptyList(),
            ).withNested(listOf(nested("Location", listOf(Field(Name.of("line"), Type.Integer()), Field(Name.of("column"), Type.Integer())))))
            else -> emptyList()
        }

        private fun graphQLClientFile(): LanguageFile = struct(
            name = GRAPHQL_CLIENT,
            interfaces = operations.map { Type.Custom("${it.name}.Service") },
            elements = operations.flatMap { operation ->
                listOfNotNull(
                    operation.service.function.copy(returnType = operation.returnType(), isOverride = true),
                    operation.selectedCall()?.qualifiedIn(operation),
                ).map { it.delegating(operation) }
            },
        ).let(::listOf).inRootFile(
            Name.of(GRAPHQL_CLIENT),
            operations.flatMap { it.service.imports + import("${packageName.value}.rpc", it.name) + import("${packageName.value}.$GRAPHQL", it.builderType) },
        )

        /** Names the selections and partial results of [operation] through the builder they are nested in. */
        private fun Function.qualifiedIn(operation: Operation): Function = operation.nestedTypes().let { nested ->
            transformer {
                type { type, transformer ->
                    (type as? Type.Custom)
                        ?.takeIf { it.name.pascalCase() in nested }
                        ?.let { Type.Custom("${operation.builderType}.${it.name.pascalCase()}") }
                        ?: type.transformChildren(transformer)
                }
            }.transformElement(this) as Function
        }

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

        // A record nested in a Java record is only public and static when it says so.
        private fun struct(name: String, fields: List<Field> = CLIENT_FIELDS, interfaces: List<Type.Custom> = emptyList(), elements: List<Element>): Struct = Struct(name = Name.of(name), fields = fields, interfaces = interfaces, elements = elements, visibility = Visibility.PUBLIC)

        private fun nested(name: String, fields: List<Field>): Struct = Struct(name = Name.of(name), fields = fields, visibility = Visibility.PUBLIC)

        /** Nests [nested] in this struct. A case class nested in a Scala case class belongs to an instance, so Scala puts them in its companion object. */
        private fun Struct.withNested(nested: List<Struct>): List<Element> = when (dialect) {
            Dialect.Scala -> listOf(this, Namespace(name, nested))
            Dialect.Kotlin, Dialect.Java -> listOf(copy(elements = elements + nested))
        }

        private fun List<Element>.inFile(name: Name, imports: List<Import>): LanguageFile = LanguageFile(name, (dialect.imports + imports).distinct() + this)
            .placeInPackage(packageName, GRAPHQL)

        /** In the root package, where the emitter puts the `Client` of the endpoints. */
        private fun List<Element>.inRootFile(name: Name, imports: List<Import>): LanguageFile = LanguageFile(
            Name.of(packageName.toDir() + name.pascalCase()),
            listOf(Package(packageName.value)) + (dialect.imports + imports).distinct() + this,
        )
    }

    private enum class Dialect {
        Kotlin {
            override val language = GraphQLQueryBuilder.Language.Kotlin

            override val imports = listOf(import("$DEFAULT_SHARED_PACKAGE_STRING.kotlin", "Wirespec"), import("kotlin.reflect", "typeOf"))

            override val visibility = Visibility.PUBLIC

            override fun type(name: String): Expression = TypeDescriptor(Type.Custom(name))

            override fun builder(name: String): Expression = RawExpression(name)

            override fun transport(read: Name): Expression = RawExpression("${read.value()}(edge.from(transportation.transport(rawRequest)))")

            override fun fromRawResponse(operation: Operation, envelope: String, data: String, response: String): List<Statement> = listOf(
                Assignment(
                    ENVELOPE,
                    RawExpression(
                        "serialization.deserializeBody<$envelope>(response.body ?: error(\"GraphQL response without a body, status \" + response.statusCode), typeOf<$envelope>())",
                    ),
                ),
                ReturnStatement(RawExpression("$response(data = envelope.data${operation.fields.joinToString("") { "?.$it" }}, errors = envelope.errors)")),
            )

            override fun fromResponse(operation: Operation): List<Statement> = listOfNotNull(
                failOnErrors().takeIf { operation.rpc.error == null },
                ReturnStatement(
                    when (operation.rpc.error) {
                        null -> RawExpression(operation.result())
                        else -> RawExpression("response.errors?.firstOrNull()?.let { ${operation.name}.Error(value = it) } ?: ${operation.name}.Result(value = ${operation.result()})")
                    },
                ),
            )

            override fun fromSelectedResponse(operation: Operation): List<Statement> = listOf(failOnErrors(), ReturnStatement(RawExpression(operation.result())))

            private fun failOnErrors(): Statement = RawExpression("response.errors?.takeIf { it.isNotEmpty() }?.let { error(\"GraphQL request failed: \" + it) }")

            private fun Operation.result(): String = when {
                rpc.result is Reference.Unit -> "Unit"
                rpc.result.isNullable -> "response.data"
                else -> "response.data ?: error(\"${rpc.noData()}\")"
            }
        },

        Java {
            override val language = GraphQLQueryBuilder.Language.Java

            override val imports = listOf(import("$DEFAULT_SHARED_PACKAGE_STRING.java", "Wirespec"))

            override val client = Name.of("getClient")

            override fun type(name: String): Expression = RawExpression("Wirespec.getType($name.class, null)")

            override fun builder(name: String): Expression = ConstructorStatement(Type.Custom(name))

            override fun transport(read: Name): Expression = RawExpression("transportation().transport(rawRequest).thenApply(edge::from).thenApply(this::${read.value()})")

            /** A function that a future is mapped with has to return a value, so a `Unit` result is a `Void` that is null. */
            override fun result(type: Type): Type = if (type == Type.Unit) Type.Custom("Void") else type

            override fun fromRawResponse(operation: Operation, envelope: String, data: String, response: String): List<Statement> = listOf(
                Assignment(
                    ENVELOPE,
                    RawExpression(
                        "serialization().<$envelope>deserializeBody(response.body().orElseThrow(() -> new IllegalStateException(\"GraphQL response without a body, status \" + response.statusCode())), Wirespec.getType($envelope.class, null))",
                    ),
                ),
                ReturnStatement(
                    RawExpression("new $response(envelope.data()${operation.dataEnvelopes(data).zip(operation.fields).joinToString("") { (envelope, field) -> ".flatMap($envelope::$field)" }}, envelope.errors())"),
                ),
            )

            override fun fromResponse(operation: Operation): List<Statement> = listOfNotNull(
                failOnErrors().takeIf { operation.rpc.error == null },
                ReturnStatement(
                    when (operation.rpc.error) {
                        null -> RawExpression(operation.result())
                        else -> RawExpression(
                            "response.errors().flatMap(errors -> errors.stream().findFirst()).<${operation.name}.Response>map(${operation.name}.Error::new)" +
                                ".orElseGet(() -> new ${operation.name}.Result(${operation.result()}))",
                        )
                    },
                ),
            )

            override fun fromSelectedResponse(operation: Operation): List<Statement> = listOf(failOnErrors(), ReturnStatement(RawExpression(operation.result())))

            private fun failOnErrors(): Statement = RawExpression("response.errors().filter(errors -> !errors.isEmpty()).ifPresent(errors -> { throw new IllegalStateException(\"GraphQL request failed: \" + errors); })")

            private fun Operation.result(): String = when {
                rpc.result is Reference.Unit -> "null"
                rpc.result.isNullable -> "response.data()"
                else -> "response.data().orElseThrow(() -> new IllegalStateException(\"${rpc.noData()}\"))"
            }
        },

        /** Scala calls are not async. */
        Scala {
            override val language = GraphQLQueryBuilder.Language.Scala

            override val imports = listOf(import("$DEFAULT_SHARED_PACKAGE_STRING.scala", "Wirespec"))

            override fun type(name: String): Expression = TypeDescriptor(Type.Custom(name))

            override fun builder(name: String): Expression = RawExpression(name)

            override fun transport(read: Name): Expression = RawExpression("${read.value()}(edge.from(transportation.transport(rawRequest)))")

            override fun fromRawResponse(operation: Operation, envelope: String, data: String, response: String): List<Statement> = listOf(
                Assignment(
                    ENVELOPE,
                    RawExpression(
                        "serialization.deserializeBody[$envelope](response.body.getOrElse(throw new IllegalStateException(\"GraphQL response without a body, status \" + response.statusCode)), scala.reflect.classTag[$envelope])",
                    ),
                ),
                ReturnStatement(RawExpression("$response(envelope.data${operation.fields.joinToString("") { ".flatMap(_.$it)" }}, envelope.errors)")),
            )

            override fun fromResponse(operation: Operation): List<Statement> = listOfNotNull(
                failOnErrors().takeIf { operation.rpc.error == null },
                ReturnStatement(
                    when (operation.rpc.error) {
                        null -> RawExpression(operation.result())
                        else -> RawExpression("response.errors.flatMap(_.headOption).map(${operation.name}.Error(_)).getOrElse(${operation.name}.Result(${operation.result()}))")
                    },
                ),
            )

            override fun fromSelectedResponse(operation: Operation): List<Statement> = listOf(failOnErrors(), ReturnStatement(RawExpression(operation.result())))

            private fun failOnErrors(): Statement = RawExpression("response.errors.filter(_.nonEmpty).foreach(errors => throw new IllegalStateException(\"GraphQL request failed: \" + errors))")

            private fun Operation.result(): String = when {
                rpc.result is Reference.Unit -> "()"
                rpc.result.isNullable -> "response.data"
                else -> "response.data.getOrElse(throw new IllegalStateException(\"${rpc.noData()}\"))"
            }
        },
        ;

        abstract val language: GraphQLQueryBuilder.Language

        /** Kotlin compiles the shared file in explicit API mode, so there its declarations spell their visibility out. */
        open val visibility: Visibility? = null

        /**
         * What the client side of every rpc has in common. A GraphQL request is no `Wirespec.Request` and its response
         * no `Wirespec.Response`, so they have messages and edges of their own.
         */
        fun graphQLTypes(): List<Interface> = listOf(
            Interface(
                name = Name.of(GraphQLQueryBuilder.GRAPHQL_REQUEST),
                elements = emptyList(),
                fields = listOf(Field(Name.of("query"), Type.String, visibility = visibility)),
                visibility = visibility,
            ),
            Interface(
                name = Name.of(GRAPHQL_RESPONSE),
                elements = emptyList(),
                typeParameters = listOf(TypeParameter(DATA_TYPE), TypeParameter(ERROR_TYPE)),
                fields = listOf(
                    Field(Name.of("data"), Type.Nullable(DATA_TYPE), visibility = visibility),
                    Field(Name.of("errors"), Type.Nullable(Type.Array(ERROR_TYPE)), visibility = visibility),
                ),
                visibility = visibility,
            ),
            Interface(
                name = Name.of(GRAPHQL_CLIENT_EDGE),
                typeParameters = MESSAGES,
                elements = listOf(
                    Function(name = TO, parameters = listOf(Parameter(REQUEST, REQ)), returnType = Type.Custom("RawRequest"), body = emptyList(), visibility = visibility),
                    Function(name = FROM, parameters = listOf(Parameter(RESPONSE_NAME, Type.Custom("RawResponse"))), returnType = RES, body = emptyList(), visibility = visibility),
                ),
                visibility = visibility,
            ),
            Interface(
                name = Name.of(GRAPHQL_CLIENT),
                typeParameters = MESSAGES,
                elements = listOf(
                    Function(
                        name = client,
                        parameters = listOf(Parameter(SERIALIZATION, Type.Custom("Serialization"))),
                        returnType = Type.Custom(GRAPHQL_CLIENT_EDGE, listOf(REQ, RES)),
                        body = emptyList(),
                        visibility = visibility,
                    ),
                ),
                visibility = visibility,
            ),
        )

        abstract val imports: List<Import>

        /** The function of `Wirespec.GraphQLClient` that gives the edge, named as the emitter names it. */
        open val client: Name = Name.of("client")

        /** The runtime type of an envelope, as `Wirespec.Serialization` takes it. */
        abstract fun type(name: String): Expression

        /** The builder of an rpc: an `object` in Kotlin and Scala, a record without components in Java. */
        abstract fun builder(name: String): Expression

        /** Sends `rawRequest` through `edge`, and reads the result from its response with the function named [read]. */
        abstract fun transport(read: Name): Expression

        /** The type a function that reads a result returns for a result of [type]. */
        open fun result(type: Type): Type = type

        /** Reads the raw `response` as the [envelope] whose data is named [data], and takes the [response] of the edge from it. */
        abstract fun fromRawResponse(operation: Operation, envelope: String, data: String, response: String): List<Statement>

        /** Reads the result from the `response` of the edge; an rpc without an error type fails on GraphQL errors. */
        abstract fun fromResponse(operation: Operation): List<Statement>

        /** Reads the partial result; GraphQL errors fail it, whether or not the rpc has an error type. */
        abstract fun fromSelectedResponse(operation: Operation): List<Statement>

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
        private const val RESPONSE = "GraphQLResponse"
        private const val DATA = "GraphQLData"
        private const val SELECTED_RESPONSE = "GraphQLSelectedResponse"
        private const val SELECTED_DATA = "GraphQLSelectedData"

        private val SERIALIZATION = Name.of("serialization")
        private val TRANSPORTATION = Name.of("transportation")
        private const val WIRESPEC = "Wirespec"
        private const val GRAPHQL_RESPONSE = "GraphQLResponse"
        private const val GRAPHQL_CLIENT_EDGE = "GraphQLClientEdge"
        private const val WIRESPEC_CLIENT = "$WIRESPEC.$GRAPHQL_CLIENT"
        private const val WIRESPEC_CLIENT_EDGE = "$WIRESPEC.$GRAPHQL_CLIENT_EDGE"
        private const val WIRESPEC_RESPONSE = "$WIRESPEC.$GRAPHQL_RESPONSE"
        private val DATA_TYPE = Type.Custom("T")
        private val ERROR_TYPE = Type.Custom("E")
        private val REQ = Type.Custom("Req")
        private val RES = Type.Custom("Res")
        private val MESSAGES = listOf(
            TypeParameter(REQ, listOf(Type.Custom(GraphQLQueryBuilder.GRAPHQL_REQUEST))),
            TypeParameter(RES, listOf(Type.Custom(GRAPHQL_RESPONSE, listOf(Type.Wildcard, Type.Wildcard)))),
        )
        private const val TYPED_RESPONSE = "Response"
        private const val TYPED_SELECTED_RESPONSE = "SelectedResponse"
        private val FROM_RESPONSE = Name.of("fromResponse")
        private val FROM_SELECTED_RESPONSE = Name.of("fromSelectedResponse")
        private val ENVELOPE = Name.of("envelope")
        private const val CLIENT_EDGE = "ClientEdge"
        private const val SELECTED_CLIENT_EDGE = "SelectedClientEdge"
        private const val API = "Api"
        private const val SELECTED_API = "SelectedApi"
        private val RAW_REQUEST = Type.Custom("Wirespec.RawRequest")
        private val RAW_RESPONSE = Type.Custom("Wirespec.RawResponse")
        private val TO = Name.of("to")
        private val FROM = Name.of("from")
        private val EDGE = Name.of("edge")
        private val REQUEST = Name.of("request")
        private val RAW_REQUEST_NAME = Name.of("rawRequest")
        private val RESPONSE_NAME = Name.of("response")
        private val SELECTION = Name.of("selection")
        private val CLIENT_FIELDS = listOf(
            Field(SERIALIZATION, Type.Custom("Wirespec.Serialization")),
            Field(TRANSPORTATION, Type.Custom("Wirespec.Transportation")),
        )
    }
}
