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
 * builder, its requests, a `GraphQLClient` that implements the rpc's `Service`, the envelopes of its response, and the
 * selection and partial result of every type its result reaches. A request implements the shared
 * `Wirespec.GraphQLRequest`: it posts the GraphQL document of the rpc to `/graphql`, with the parameters as its
 * variables, and reads the result from the response, so any `Wirespec.GraphQLClient` sends it through its own
 * `Wirespec.Transportation` and `Wirespec.Serialization`. [GraphQLOperations] writes the document at build time, so the
 * generated code needs no GraphQL library. A `GraphQLClient` in the root package, next to the `Client` of the endpoints,
 * implements `Wirespec.GraphQLClient`, and every `Service` at once by delegating to those clients.
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
            val definitions = ast.modules.flatMap { it.statements }
            val operations = GraphQLOperations(definitions)
            definitions.filterIsInstance<Rpc>()
                .filter { rpc -> rpc.annotations.any { it.name in OPERATIONS } }
                .mapNotNull { rpc ->
                    operations.open(rpc, RESULT)?.let { open -> Operation(rpc, operations.document(rpc, RESULT)!!, open, ir.serviceOf(rpc)) }
                }
                .takeIf { it.isNotEmpty() }
                ?.let { ir + Client(dialect, GraphQLQueryBuilder(operations, dialect.language), it).files() }
        }
        ?: ir

    private class Operation(val rpc: Rpc, val document: String, val open: String, val service: Service) {
        val name: String = service.rpc.name.pascalCase()

        /** Each variable keeps the name of the argument it is passed as, and takes the parameter of the `Service` function. */
        val variables: List<Pair<Field, Parameter>> = rpc.shape.fields
            .zip(service.function.parameters)
            .map { (field, parameter) -> Field(Name(listOf(field.identifier.value)), parameter.type) to parameter }

        val request: GraphQLQueryBuilder.Request = GraphQLQueryBuilder.Request(name, service.function, document, open, rpc.result, variables)
        val builderType: String = request.builder
        val client: String = "$builderType.$GRAPHQL_CLIENT"
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

        /** The builder of the rpc, with its client, envelopes, errors, selections and partial results nested in it. */
        private fun Operation.file(): LanguageFile = builder.results(listOf(rpc.result)).let { results ->
            builder.builder(request)
                .let { it.copy(elements = it.elements + requests() + client() + envelopes() + selectedEnvelopes() + errors() + selections() + results.map { result -> result.struct }) }
                .let(::listOf)
                .inFile(
                    Name.of(builderType),
                    service.imports + import("${packageName.value}.rpc", name) + results.flatMap { it.models }.map { import("${packageName.value}.model", it) },
                )
        }

        private fun Operation.client(): Struct = struct(
            name = GRAPHQL_CLIENT,
            interfaces = listOf(Type.Custom("$name.Service")),
            elements = listOfNotNull(call(), selectedCall()),
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
            body = sending(service.function.parameters.map { it.name }),
        )

        /** The block that selects the fields of the result, for a result with fields. */
        private val Operation.block: Type? get() = builder.block(rpc.result)

        /** `queryTodo(id) { ... }` reads the fields the block selects into the partial result. */
        private fun Operation.selectedCall(): Function? = block?.let { block ->
            service.function.copy(
                parameters = service.function.parameters + Parameter(SELECTION, block),
                returnType = builder.resultType(rpc.result),
                body = sending(service.function.parameters.map { it.name } + SELECTION),
            )
        }

        /** Writes the request with the builder of the rpc and sends it. */
        private fun Operation.sending(arguments: List<Name>): List<Statement> = listOf(
            Assignment(
                REQUEST,
                FunctionCall(
                    receiver = dialect.builder(builderType),
                    name = GraphQLQueryBuilder.REQUEST,
                    arguments = arguments.associateWith { VariableReference(it) },
                ),
            ),
            ReturnStatement(dialect.send(REQUEST)),
        )

        /**
         * The requests of the rpc, which implement `Wirespec.GraphQLRequest`: each writes its own raw request and reads
         * its own response, so any `Wirespec.GraphQLClient` sends it.
         */
        private fun Operation.requests(): List<Struct> = listOfNotNull(
            request(GraphQLQueryBuilder.GRAPHQL_REQUEST, returnType(), dialect.fromRawResponse(this)),
            block?.let { request(GraphQLQueryBuilder.GRAPHQL_SELECTED_REQUEST, builder.resultType(rpc.result), dialect.fromSelectedRawResponse(this)) },
        )

        private fun Operation.request(name: String, result: Type, read: List<Statement>): Struct = struct(
            name = name,
            fields = listOf(Field(GraphQLQueryBuilder.QUERY, Type.String, isOverride = true)) +
                listOfNotNull(Field(GraphQLQueryBuilder.VARIABLES, Type.Custom(GraphQLQueryBuilder.GRAPHQL_VARIABLES)).takeIf { variables.isNotEmpty() }),
            interfaces = listOf(Type.Custom("Wirespec.GraphQLRequest", listOf(result))),
            elements = listOf(
                Function(
                    name = TO_RAW_REQUEST,
                    parameters = listOf(Parameter(SERIALIZATION, Type.Custom("Wirespec.Serializer"))),
                    returnType = Type.Custom("Wirespec.RawRequest"),
                    body = listOf(ReturnStatement(rawRequest("$builderType.$name"))),
                    isOverride = true,
                ),
                Function(
                    name = FROM_RAW_RESPONSE,
                    parameters = listOf(Parameter(SERIALIZATION, Type.Custom("Wirespec.Deserializer")), Parameter(RESPONSE_PARAMETER, Type.Custom("Wirespec.RawResponse"))),
                    returnType = dialect.returnType(result),
                    body = read,
                    isOverride = true,
                ),
            ),
        )

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

        private fun rawRequest(requestType: String): ConstructorStatement = ConstructorStatement(
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
                        receiver = VariableReference(SERIALIZATION),
                        name = Name.of("serializeBody"),
                        arguments = mapOf(Name.of("t") to RawExpression("this"), Name.of("type") to dialect.type(requestType)),
                    ),
                ),
            ),
        )

        private fun Operation.returnType(): Type = rpc.error?.let { Type.Custom("$name.Response") } ?: rpc.result.convert()

        private fun Operation.envelopes(): List<Struct> = listOfNotNull(
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
            interfaces = listOf(Type.Custom("Wirespec.GraphQLClient")) + operations.map { Type.Custom("${it.name}.Service") },
            elements = listOf(send()) + operations.flatMap { operation ->
                listOfNotNull(
                    operation.service.function.copy(returnType = operation.returnType(), isOverride = true),
                    operation.selectedCall()?.qualifiedIn(operation),
                ).map { it.delegating(operation) }
            },
        ).let(::listOf).inRootFile(
            Name.of(GRAPHQL_CLIENT),
            operations.flatMap { it.service.imports + import("${packageName.value}.rpc", it.name) + import("${packageName.value}.$GRAPHQL", it.builderType) },
        )

        /** Sends any GraphQL request, which reads its own response. */
        private fun send(): Function = Type.Custom("T").let { result ->
            Function(
                name = Name.of("send"),
                typeParameters = listOf(TypeParameter(result, listOf(Type.Nullable(Type.Any)))),
                parameters = listOf(Parameter(REQUEST, Type.Custom("Wirespec.GraphQLRequest", listOf(result)))),
                returnType = result,
                body = listOf(ReturnStatement(dialect.send(REQUEST))),
                isAsync = true,
                isOverride = true,
            )
        }

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

            override fun type(name: String): Expression = TypeDescriptor(Type.Custom(name))

            override fun builder(name: String): Expression = RawExpression(name)

            override fun send(request: Name): Expression = request.value().let { RawExpression("$it.fromRawResponse(serialization, transportation.transport($it.toRawRequest(serialization)))") }

            override fun returnType(result: Type): Type = result

            override fun fromRawResponse(operation: Operation): List<Statement> = listOfNotNull(
                read(RESPONSE),
                failOnErrors().takeIf { operation.rpc.error == null },
                ReturnStatement(
                    when (operation.rpc.error) {
                        null -> RawExpression(result(operation.rpc))
                        else -> RawExpression("$ENVELOPE.errors?.firstOrNull()?.let { ${operation.name}.Error(value = it) } ?: ${operation.name}.Result(value = ${result(operation.rpc)})")
                    },
                ),
            )

            override fun fromSelectedRawResponse(operation: Operation): List<Statement> = listOf(read(SELECTED_RESPONSE), failOnErrors(), ReturnStatement(RawExpression(result(operation.rpc))))

            private fun read(response: String): Statement = Assignment(
                Name.of(ENVELOPE),
                RawExpression(
                    "serialization.deserializeBody<$response>(response.body ?: error(\"GraphQL response without a body, status \" + response.statusCode), typeOf<$response>())",
                ),
            )

            private fun failOnErrors(): Statement = RawExpression("$ENVELOPE.errors?.takeIf { it.isNotEmpty() }?.let { error(\"GraphQL request failed: \" + it) }")

            private fun result(rpc: Rpc): String = when {
                rpc.result is Reference.Unit -> "Unit"
                rpc.result.isNullable -> "$ENVELOPE.data?.$RESULT"
                else -> "$ENVELOPE.data?.$RESULT ?: error(\"${rpc.noData()}\")"
            }
        },

        Java {
            override val language = GraphQLQueryBuilder.Language.Java

            override val imports = listOf(import("$DEFAULT_SHARED_PACKAGE_STRING.java", "Wirespec"))

            override fun type(name: String): Expression = RawExpression("Wirespec.getType($name.class, null)")

            override fun builder(name: String): Expression = ConstructorStatement(Type.Custom(name))

            override fun send(request: Name): Expression = request.value().let {
                RawExpression("transportation().transport($it.toRawRequest(serialization())).thenApply(rawResponse -> $it.fromRawResponse(serialization(), rawResponse))")
            }

            /** A generic `Void` reads no result, so it returns null. */
            override fun returnType(result: Type): Type = if (result == Type.Unit) Type.Custom("Void") else result

            override fun fromRawResponse(operation: Operation): List<Statement> = listOfNotNull(
                read(RESPONSE),
                failOnErrors().takeIf { operation.rpc.error == null },
                when (operation.rpc.error) {
                    null -> ReturnStatement(RawExpression(result(operation.rpc) ?: "null"))
                    else -> ReturnStatement(
                        RawExpression(
                            "$ENVELOPE.errors().flatMap(errors -> errors.stream().findFirst()).<${operation.name}.Response>map(${operation.name}.Error::new)" +
                                ".orElseGet(() -> new ${operation.name}.Result(${result(operation.rpc) ?: "null"}))",
                        ),
                    )
                },
            )

            override fun fromSelectedRawResponse(operation: Operation): List<Statement> = listOfNotNull(read(SELECTED_RESPONSE), failOnErrors(), result(operation.rpc, SELECTED_DATA)?.let { ReturnStatement(RawExpression(it)) })

            private fun read(response: String): Statement = Assignment(
                Name.of(ENVELOPE),
                RawExpression(
                    "serialization.<$response>deserializeBody(response.body().orElseThrow(() -> new IllegalStateException(\"GraphQL response without a body, status \" + response.statusCode())), Wirespec.getType($response.class, null))",
                ),
            )

            private fun failOnErrors(): Statement = RawExpression("$ENVELOPE.errors().filter(errors -> !errors.isEmpty()).ifPresent(errors -> { throw new IllegalStateException(\"GraphQL request failed: \" + errors); })")

            /** Null for a Unit result, which reads nothing. */
            private fun result(rpc: Rpc, data: String = DATA): String? = when {
                rpc.result is Reference.Unit -> null
                rpc.result.isNullable -> "$ENVELOPE.data().flatMap($data::$RESULT)"
                else -> "$ENVELOPE.data().flatMap($data::$RESULT).orElseThrow(() -> new IllegalStateException(\"${rpc.noData()}\"))"
            }
        },

        /** Scala calls are not async. */
        Scala {
            override val language = GraphQLQueryBuilder.Language.Scala

            override val imports = listOf(import("$DEFAULT_SHARED_PACKAGE_STRING.scala", "Wirespec"))

            override fun type(name: String): Expression = TypeDescriptor(Type.Custom(name))

            override fun builder(name: String): Expression = RawExpression(name)

            override fun send(request: Name): Expression = request.value().let { RawExpression("$it.fromRawResponse(serialization, transportation.transport($it.toRawRequest(serialization)))") }

            override fun returnType(result: Type): Type = result

            override fun fromRawResponse(operation: Operation): List<Statement> = listOfNotNull(
                read(RESPONSE),
                failOnErrors().takeIf { operation.rpc.error == null },
                ReturnStatement(
                    when (operation.rpc.error) {
                        null -> RawExpression(result(operation.rpc))
                        else -> RawExpression("$ENVELOPE.errors.flatMap(_.headOption).map(${operation.name}.Error(_)).getOrElse(${operation.name}.Result(${result(operation.rpc)}))")
                    },
                ),
            )

            override fun fromSelectedRawResponse(operation: Operation): List<Statement> = listOf(read(SELECTED_RESPONSE), failOnErrors(), ReturnStatement(RawExpression(result(operation.rpc))))

            private fun read(response: String): Statement = Assignment(
                Name.of(ENVELOPE),
                RawExpression(
                    "serialization.deserializeBody[$response](response.body.getOrElse(throw new IllegalStateException(\"GraphQL response without a body, status \" + response.statusCode)), scala.reflect.classTag[$response])",
                ),
            )

            private fun failOnErrors(): Statement = RawExpression("$ENVELOPE.errors.filter(_.nonEmpty).foreach(errors => throw new IllegalStateException(\"GraphQL request failed: \" + errors))")

            private fun result(rpc: Rpc): String = when {
                rpc.result is Reference.Unit -> "()"
                rpc.result.isNullable -> "$ENVELOPE.data.flatMap(_.$RESULT)"
                else -> "$ENVELOPE.data.flatMap(_.$RESULT).getOrElse(throw new IllegalStateException(\"${rpc.noData()}\"))"
            }
        },
        ;

        abstract val language: GraphQLQueryBuilder.Language

        abstract val imports: List<Import>

        /** The runtime type of an envelope, as `Wirespec.Serialization` takes it. */
        abstract fun type(name: String): Expression

        /** The builder of an rpc: an `object` in Kotlin and Scala, a record without components in Java. */
        abstract fun builder(name: String): Expression

        /** Sends the GraphQL request in the variable [request] through `transportation`, and reads its response. */
        abstract fun send(request: Name): Expression

        /** The return type of a `fromRawResponse` that reads [result]. */
        abstract fun returnType(result: Type): Type

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
        private const val RESPONSE = "GraphQLResponse"
        private const val DATA = "GraphQLData"
        private const val SELECTED_RESPONSE = "GraphQLSelectedResponse"
        private const val SELECTED_DATA = "GraphQLSelectedData"

        private val SERIALIZATION = Name.of("serialization")
        private val TRANSPORTATION = Name.of("transportation")
        private const val ENVELOPE = "envelope"

        private val TO_RAW_REQUEST = Name(listOf("toRawRequest"))
        private val FROM_RAW_RESPONSE = Name(listOf("fromRawResponse"))
        private val REQUEST = Name.of("request")
        private val RESPONSE_PARAMETER = Name.of("response")
        private val SELECTION = Name.of("selection")
        private val CLIENT_FIELDS = listOf(
            Field(SERIALIZATION, Type.Custom("Wirespec.Serialization")),
            Field(TRANSPORTATION, Type.Custom("Wirespec.Transportation")),
        )
    }
}
