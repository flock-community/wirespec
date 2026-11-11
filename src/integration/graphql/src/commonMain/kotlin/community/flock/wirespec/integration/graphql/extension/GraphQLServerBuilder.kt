package community.flock.wirespec.integration.graphql.extension

import community.flock.wirespec.compiler.core.emit.PackageName
import community.flock.wirespec.compiler.core.ir.Field
import community.flock.wirespec.compiler.core.ir.Function
import community.flock.wirespec.compiler.core.ir.IR
import community.flock.wirespec.compiler.core.ir.Import
import community.flock.wirespec.compiler.core.ir.Name
import community.flock.wirespec.compiler.core.ir.Package
import community.flock.wirespec.compiler.core.ir.RawElement
import community.flock.wirespec.compiler.core.ir.Struct
import community.flock.wirespec.compiler.core.ir.generator.JavaGenerator
import community.flock.wirespec.compiler.core.ir.generator.KotlinGenerator
import community.flock.wirespec.compiler.core.ir.generator.ScalaGenerator
import community.flock.wirespec.compiler.core.parse.ast.Annotation
import community.flock.wirespec.compiler.core.parse.ast.Channel
import community.flock.wirespec.compiler.core.parse.ast.Definition
import community.flock.wirespec.compiler.core.parse.ast.Endpoint
import community.flock.wirespec.compiler.core.parse.ast.Enum
import community.flock.wirespec.compiler.core.parse.ast.HasAnnotations
import community.flock.wirespec.compiler.core.parse.ast.Part
import community.flock.wirespec.compiler.core.parse.ast.Reference
import community.flock.wirespec.compiler.core.parse.ast.Refined
import community.flock.wirespec.compiler.core.parse.ast.Rpc
import community.flock.wirespec.compiler.core.parse.ast.Union
import community.flock.wirespec.compiler.core.parse.ast.fields
import community.flock.wirespec.converter.graphql.GraphQLOperations
import community.flock.wirespec.compiler.core.ir.File as LanguageFile
import community.flock.wirespec.compiler.core.parse.ast.Type as TypeDefinition

/**
 * Writes the `GraphQLServer` that answers a GraphQL request with the `Service` of every rpc:
 *
 * ```kotlin
 * val server = GraphQLServer(serialization, service)
 * val response: Wirespec.RawResponse = server.handle(request)
 * ```
 *
 * A client writes its own document, so the server reads it when the request comes in: it parses the query, calls the
 * `Service` of each field that is an rpc with the arguments of the field, and answers with the fields the query selects
 * of each result. A server that is given a function of the `Selection` asks it for the `Service` of each call, so an
 * implementation sees which fields the query selects of its result. The parser and the execution are part of the generated file, so it needs no GraphQL library. They are
 * written as source text, one for each language: the IR has no loops or mutable state to write a parser with.
 *
 * The arguments of a field are read as the `GraphQLVariables` of the rpc, and the values of the response are written, by
 * the `Wirespec.Serialization` the server is given, which therefore has to read and write a map of any values.
 */
internal class GraphQLServerBuilder(
    private val packageName: PackageName,
    private val operations: GraphQLOperations,
    private val definitions: List<Definition>,
    private val ir: IR,
    private val language: GraphQLQueryBuilder.Language,
) {

    /** An rpc the server answers: its `Service` function, the variables of its document and the document without a selection. */
    class Served(val rpc: Rpc, val name: String, val function: Function, val variables: List<Name>, open: String, close: String, val path: List<String>, alias: String) {
        val keyword: String = open.substringBefore(' ')

        val document: String = open + close

        /** The name of the field, which the document aliases and passes the variables to. */
        val field: String = open
            .let { if (rpc.shape.fields.isEmpty()) it else it.substringBeforeLast("(") }
            .substringAfterLast("$alias: ")

        val hasDefaults: Boolean = " = " in open.substringBefore("{")

        /** The type the rpc is a field of, when that is not a root operation type. */
        val owner: String? = path.takeIf { it.isNotEmpty() }?.let { rpc.annotations.firstOrNull { it.name in OPERATIONS }?.single(DEFAULT) }
    }

    fun file(served: List<Served>, imports: List<Import>): LanguageFile = Schema(served).let { schema ->
        LanguageFile(
            Name.of(packageName.toDir() + SERVER),
            listOf(Package(packageName.value)) + imports + RawElement(
                when (language) {
                    GraphQLQueryBuilder.Language.Kotlin -> Kotlin(schema).source()
                    GraphQLQueryBuilder.Language.Java -> Java(schema).source()
                    GraphQLQueryBuilder.Language.Scala -> Scala(schema).source()
                },
            ),
        )
    }

    /** A field of a type: its name in GraphQL, and in the model the emitter wrote. */
    private class Member(val name: String, val accessor: String, val reference: Reference)

    /** A type whose fields a query selects: a root operation type, or a type or union that a result reaches. */
    private class Selectable(val function: String, val graphQLName: String, val conditions: List<String>, val definition: Definition?, val members: List<Member>, val rpcs: List<Served>)

    private inner class Schema(val served: List<Served>) {
        val roots: List<Selectable> = served.filter { it.path.isEmpty() }
            .groupBy { it.keyword }
            .map { (keyword, rpcs) -> rootType(keyword).let { Selectable(keyword, it, listOf(it), null, emptyList(), rpcs) } }

        val types: List<Selectable> = reachable(served.map { it.rpc.result }).map { definition ->
            Selectable(
                function = definition.function(),
                graphQLName = operations.graphQLName(definition),
                conditions = (listOf(definition) + definition.unions()).map { operations.graphQLName(it) }.distinct(),
                definition = definition,
                members = (definition as? TypeDefinition)?.members().orEmpty(),
                rpcs = served.filter { it.owner != null && it.owner in listOf(definition.identifier.value, operations.graphQLName(definition)) },
            )
        }

        private fun rootType(keyword: String): String = definitions
            .firstNotNullOfOrNull { definition -> (definition as? HasAnnotations)?.annotations?.firstOrNull { it.name == SCHEMA }?.single(keyword) }
            ?: keyword.replaceFirstChar(Char::uppercase)

        private fun Definition.unions(): List<Union> = generateSequence(listOf(this)) { found ->
            (found + definitions.filterIsInstance<Union>().filter { union -> union.entries.any { operations.composite(it) in found } }).distinct().takeIf { it.size > found.size }
        }.last().filterIsInstance<Union>().filterNot { it == this }

        private fun TypeDefinition.members(): List<Member> = shape.fields.let { fields ->
            (struct()?.fields?.filterIsInstance<Field>()?.map { it.name.value() }?.takeIf { it.size == fields.size } ?: fields.map { it.identifier.value })
                .zip(fields)
                .map { (accessor, field) -> Member(field.identifier.value, accessor.sanitized(), field.reference) }
        }

        /** The struct the emitter wrote for a type, with its fields named as that language names them. */
        private fun TypeDefinition.struct(): Struct? = ir.filterIsInstance<LanguageFile>()
            .filter { file -> Package("${packageName.value}.$MODEL") in file.elements }
            .firstNotNullOfOrNull { file -> file.elements.filterIsInstance<Struct>().find { it.name.pascalCase() == identifier.value.pascal() } }

        private fun reachable(results: List<Reference>): List<Definition> = generateSequence(results.mapNotNull { operations.composite(it) }.distinct()) { found ->
            (found + found.flatMap { it.nested() }).distinct().takeIf { it.size > found.size }
        }.last()

        private fun Definition.nested(): List<Definition> = when (this) {
            is Union -> entries.mapNotNull { operations.composite(it) }
            is TypeDefinition -> shape.fields.mapNotNull { operations.composite(it.reference) }
            is Endpoint, is Channel, is Rpc, is Enum, is Refined, is Part -> emptyList()
        }
    }

    private fun Definition.function(): String = "select${identifier.value.pascal()}"

    private fun Definition.model(): String = "${packageName.value}.$MODEL.${identifier.value.pascal()}"

    private fun Served.namespace(): String = "${packageName.value}.rpc.$name"

    private fun Served.variablesType(): String = "${packageName.value}.graphql.${GraphQLQueryBuilder.builder(name)}.${GraphQLQueryBuilder.GRAPHQL_VARIABLES}"

    private fun Served.call(): String = function.name.camelCase().sanitized()

    private fun Served.defaults(): String = "${Name.of(name).camelCase()}Defaults"

    /** The members of a union a query selects the fields of, with whether those are all its members. */
    private fun Union.members(): Pair<List<Definition>, Boolean> = entries.mapNotNull { operations.composite(it) }.let { it to (it.size == entries.size) }

    private fun String.pascal(): String = Name.of(this).pascalCase()

    private fun String.sanitized(): String = when (language) {
        GraphQLQueryBuilder.Language.Kotlin -> if (this in KotlinGenerator.reservedKeywords) "`$this`" else this
        GraphQLQueryBuilder.Language.Java -> if (this in JavaGenerator.reservedKeywords) "_$this" else this
        GraphQLQueryBuilder.Language.Scala -> if (this in ScalaGenerator.reservedKeywords) "`$this`" else this
    }

    private fun String.literal(): String = replace("\\", "\\\\").replace("\"", "\\\"")
        .let { if (language == GraphQLQueryBuilder.Language.Kotlin) it.replace("$", "\\$") else it }
        .let { "\"$it\"" }

    private fun List<String>.literals(): String = joinToString(", ") { it.literal() }

    private fun List<String>.indented(indent: Int): String = joinToString("\n").prependIndent(" ".repeat(indent))

    private fun unknown(type: String): String = when (language) {
        GraphQLQueryBuilder.Language.Kotlin -> "field.field"
        GraphQLQueryBuilder.Language.Java -> "field.field()"
        GraphQLQueryBuilder.Language.Scala -> "field.field.orNull"
    }.let { "Cannot query field \" + $it + \" on type $type" }

    private inner class Kotlin(private val schema: Schema) {

        fun source(): String = """
            |import kotlin.reflect.KType
            |
            |public class GraphQLServer(
            |  private val serialization: Wirespec.Serialization,
            |  private val service: (Selection) -> Service,
            |) {
            |  public constructor(serialization: Wirespec.Serialization, service: Service) : this(serialization, { service })
            |
            |  public interface Service : ${schema.served.joinToString(", ") { "${it.namespace()}.Service" }}
            |
            |  public data class Request(
            |    val query: String,
            |    val operationName: String?,
            |    val variables: Map<String, Any?>?,
            |  )
            |
            |  public data class Selection(
            |    val name: String,
            |    val alias: String?,
            |    val arguments: Map<String, Any?>,
            |    val fields: List<Selection>,
            |  ) {
            |    public operator fun get(name: String): Selection? = fields.find { it.name == name }
            |
            |    public operator fun contains(name: String): Boolean = fields.any { it.name == name }
            |  }
            |
            |${schema.served.filter { it.hasDefaults }.map { "private val ${it.defaults()}: Map<String, Any?> = Parser(${it.document.literal()}).document().operations.first().defaults" }.indented(2)}
            |
            |  public suspend fun handle(request: Wirespec.RawRequest): Wirespec.RawResponse {
            |    val errors = mutableListOf<Any>()
            |    val data = try {
            |      val body = serialization.deserializeBody<Request>(requireNotNull(request.body) { "GraphQL request without a body" }, typeOf<Request>())
            |      val document = Parser(body.query).document()
            |      val operation = document.operation(body.operationName)
            |      val execution = Execution(document, operation.defaults + body.variables.orEmpty(), errors)
            |      when (operation.type) {
            |${schema.roots.map { "${it.function.literal()} -> execution.${it.function}(operation.selections, emptyList())" }.indented(8)}
            |        else -> throw IllegalArgumentException("Cannot execute a " + operation.type)
            |      }
            |    } catch (e: IllegalArgumentException) {
            |      errors.add(mapOf("message" to e.message))
            |      null
            |    }
            |    val response = buildMap<String, Any?> {
            |      if (data != null) put("data", data)
            |      if (errors.isNotEmpty()) put("errors", errors)
            |    }
            |    return Wirespec.RawResponse(
            |      statusCode = 200,
            |      headers = mapOf("Content-Type" to listOf("application/json")),
            |      body = serialization.serializeBody(response, typeOf<Map<String, Any?>>()),
            |    )
            |  }
            |
            |  private inner class Execution(
            |    private val document: Document,
            |    private val variables: Map<String, Any?>,
            |    private val errors: MutableList<Any>,
            |  ) {
            |${(schema.roots + schema.types).map { it.render() }.indented(4)}
            |
        """.trimMargin() + KOTLIN_RUNTIME

        private fun Selectable.render(): String = when (val union = definition as? Union) {
            null -> listOf(
                "${if (definition == null) "" else "private "}suspend fun $function(${definition?.let { "value: ${it.model()}, " }.orEmpty()}selections: List<Node>, path: List<Any>): Map<String, Any?> =",
                "  fields(selections, setOf(${conditions.literals()})).associate { field ->",
                "    field.key to when (field.field) {",
                "      \"__typename\" -> ${graphQLName.literal()}",
                (members.map { "${it.name.literal()} -> ${it.reference.project("value.${it.accessor}")}" } + rpcs.map { it.case() }).indented(6),
                "      else -> throw IllegalArgumentException(\"${unknown(graphQLName)}\")",
                "    }",
                "  }",
            )
            else -> union.members().let { (members, exhaustive) ->
                listOf(
                    "private suspend fun $function(value: ${union.model()}, selections: List<Node>, path: List<Any>): Map<String, Any?> = when (value) {",
                    members.map { "is ${it.model()} -> ${it.function()}(value, selections, path)" }.indented(2),
                    if (exhaustive && members.isNotEmpty()) "" else "  else -> throw IllegalArgumentException(\"Cannot select the fields of a \" + value::class.simpleName)",
                    "}",
                )
            }
        }.filter { it.isNotEmpty() }.joinToString("\n", postfix = "\n")

        private fun Served.case(): String = listOfNotNull(
            "${field.literal()} -> rpc(field, path) {",
            "  val arguments = arguments<${variablesType()}>(field, ${if (hasDefaults) defaults() else "emptyMap()"}, typeOf<${variablesType()}>())".takeIf { variables.isNotEmpty() },
            "service(selected(field)).${call()}(${variables.joinToString(", ") { "arguments.${it.value().sanitized()}" }})".let { call ->
                when {
                    rpc.error != null -> listOf(
                        "when (val response = $call) {",
                        "  is ${namespace()}.Result -> ${rpc.result.projectResult("response.value")}",
                        "  is ${namespace()}.Error -> null.also { errors.add(response.value) }",
                        "}",
                    ).indented(2)
                    else -> "  ${rpc.result.projectResult(call)}"
                }
            },
            "}",
        ).joinToString("\n")

        private fun Reference.projectResult(expression: String): String = when (this) {
            is Reference.Unit -> "$expression.let { true }"
            is Reference.Any, is Reference.Custom, is Reference.Dict, is Reference.Iterable, is Reference.Primitive -> project(expression)
        }

        private fun Reference.project(expression: String, depth: Int = 1): String = when (val composite = operations.composite(this)) {
            null -> expression
            else -> when (this) {
                is Reference.Iterable -> "$expression${if (isNullable) "?" else ""}.map { v$depth -> ${reference.project("v$depth", depth + 1)} }"
                is Reference.Any, is Reference.Custom, is Reference.Dict, is Reference.Primitive, is Reference.Unit -> when {
                    isNullable -> "$expression?.let { v$depth -> ${composite.function()}(v$depth, field.selections, path + field.key) }"
                    else -> "${composite.function()}($expression, field.selections, path + field.key)"
                }
            }
        }
    }

    private inner class Java(private val schema: Schema) {

        fun source(): String = """
            |import java.util.ArrayList;
            |import java.util.Collections;
            |import java.util.LinkedHashMap;
            |import java.util.List;
            |import java.util.Map;
            |import java.util.Optional;
            |import java.util.Set;
            |import java.util.concurrent.CompletableFuture;
            |import java.util.concurrent.CompletionException;
            |import java.util.function.Function;
            |import java.util.function.Supplier;
            |
            |public class GraphQLServer {
            |  public interface Service extends ${schema.served.joinToString(", ") { "${it.namespace()}.Service" }} {}
            |
            |  public record Request(String query, Optional<String> operationName, Optional<Map<String, Object>> variables) {}
            |
            |  public record Selection(String name, Optional<String> alias, Map<String, Object> arguments, List<Selection> fields) {
            |    public Optional<Selection> get(String name) {
            |      return fields.stream().filter(field -> field.name().equals(name)).findFirst();
            |    }
            |
            |    public boolean contains(String name) {
            |      return get(name).isPresent();
            |    }
            |  }
            |
            |${schema.served.filter { it.hasDefaults }.map { "private static final Map<String, Object> ${it.defaults()} = new Parser(${it.document.literal()}).document().operations().get(0).defaults();" }.indented(2)}
            |
            |  private final Wirespec.Serialization serialization;
            |  private final Function<Selection, Service> service;
            |
            |  public GraphQLServer(Wirespec.Serialization serialization, Function<Selection, Service> service) {
            |    this.serialization = serialization;
            |    this.service = service;
            |  }
            |
            |  public GraphQLServer(Wirespec.Serialization serialization, Service service) {
            |    this(serialization, selection -> service);
            |  }
            |
            |  public CompletableFuture<Wirespec.RawResponse> handle(Wirespec.RawRequest request) {
            |    List<Object> errors = Collections.synchronizedList(new ArrayList<>());
            |    CompletableFuture<Object> data;
            |    try {
            |      Request body = serialization.deserializeBody(request.body().orElseThrow(() -> new IllegalArgumentException("GraphQL request without a body")), Wirespec.getType(Request.class, null));
            |      if (body.query() == null) throw new IllegalArgumentException("GraphQL request without a query");
            |      Document document = new Parser(body.query()).document();
            |      Operation operation = document.operation(body.operationName() == null ? null : body.operationName().orElse(null));
            |      Map<String, Object> variables = new LinkedHashMap<>(operation.defaults());
            |      if (body.variables() != null) body.variables().ifPresent(variables::putAll);
            |      Execution execution = new Execution(document, variables, errors);
            |      data = switch (operation.type()) {
            |${schema.roots.map { "case ${it.function.literal()} -> execution.${it.function}(operation.selections(), List.of());" }.indented(8)}
            |        default -> throw new IllegalArgumentException("Cannot execute a " + operation.type());
            |      };
            |    } catch (IllegalArgumentException e) {
            |      data = CompletableFuture.failedFuture(e);
            |    }
            |    return data.handle((result, failure) -> {
            |      Throwable cause = failure instanceof CompletionException && failure.getCause() != null ? failure.getCause() : failure;
            |      if (cause != null && !(cause instanceof IllegalArgumentException)) throw new CompletionException(cause);
            |      Map<String, Object> response = new LinkedHashMap<>();
            |      if (cause == null) response.put("data", result);
            |      else errors.add(Map.of("message", String.valueOf(cause.getMessage())));
            |      if (!errors.isEmpty()) response.put("errors", new ArrayList<>(errors));
            |      return new Wirespec.RawResponse(200, Map.of("Content-Type", List.of("application/json")), Optional.of(serialization.serializeBody(response, Wirespec.getType(Map.class, null))));
            |    });
            |  }
            |
            |  private final class Execution {
            |    private final Document document;
            |    private final Map<String, Object> variables;
            |    private final List<Object> errors;
            |
            |    Execution(Document document, Map<String, Object> variables, List<Object> errors) {
            |      this.document = document;
            |      this.variables = variables;
            |      this.errors = errors;
            |    }
            |
            |${(schema.roots + schema.types).map { it.render() }.indented(4)}
            |
        """.trimMargin() + JAVA_RUNTIME

        private fun Selectable.render(): String = when (val union = definition as? Union) {
            null -> listOf(
                "CompletableFuture<Object> $function(${definition?.let { "${it.model()} value, " }.orEmpty()}List<Node> selections, List<Object> path) {",
                "  return object(fields(selections, Set.of(${conditions.literals()})), field -> switch (field.field()) {",
                "    case \"__typename\" -> done(${graphQLName.literal()});",
                (members.map { "case ${it.name.literal()} -> ${it.reference.project("value.${it.accessor}()")};" } + rpcs.map { it.case() }).indented(4),
                "    default -> throw new IllegalArgumentException(\"${unknown(graphQLName)}\");",
                "  });",
                "}",
            )
            else -> listOf(
                "CompletableFuture<Object> $function(${union.model()} value, List<Node> selections, List<Object> path) {",
                union.members().first.map { "if (value instanceof ${it.model()} member) return ${it.function()}(member, selections, path);" }.indented(2),
                "  throw new IllegalArgumentException(\"Cannot select the fields of a \" + value.getClass().getSimpleName());",
                "}",
            )
        }.filter { it.isNotEmpty() }.joinToString("\n", postfix = "\n")

        private fun Served.case(): String = listOfNotNull(
            "case ${field.literal()} -> rpc(field, path, () -> {",
            "  var arguments = arguments(field, ${if (hasDefaults) defaults() else "Map.of()"}, ${variablesType()}.class);".takeIf { variables.isNotEmpty() },
            "service.apply(selected(field)).${call()}(${variables.joinToString(", ") { "arguments.${it.value().sanitized()}()" }})".let { call ->
                when {
                    rpc.error != null -> listOf(
                        "return $call.thenCompose(response -> {",
                        "  if (response instanceof ${namespace()}.Error error) {",
                        "    errors.add(error.value());",
                        "    return done(null);",
                        "  }",
                        "  return ${rpc.result.projectResult("((${namespace()}.Result) response).value()")};",
                        "});",
                    ).indented(2)
                    else -> "  return $call.thenCompose(result -> ${rpc.result.projectResult("result")});"
                }
            },
            "});",
        ).joinToString("\n")

        private fun Reference.projectResult(expression: String): String = when (this) {
            is Reference.Unit -> "done(true)"
            is Reference.Any, is Reference.Custom, is Reference.Dict, is Reference.Iterable, is Reference.Primitive -> project(expression)
        }

        /** A future of the value in the response; an `Optional` that is not selected from is left to the serialization. */
        private fun Reference.project(expression: String, depth: Int = 1): String = when {
            operations.composite(this) == null -> "done($expression)"
            isNullable -> "$expression.map(v$depth -> ${required("v$depth", depth + 1)}).orElseGet(() -> done(null))"
            else -> required(expression, depth)
        }

        private fun Reference.required(expression: String, depth: Int): String = when (this) {
            is Reference.Iterable -> "list($expression.stream().map(v$depth -> ${reference.project("v$depth", depth + 1)}).toList())"
            is Reference.Any, is Reference.Custom, is Reference.Dict, is Reference.Primitive, is Reference.Unit ->
                "${operations.composite(this)?.function()}($expression, field.selections(), path(path, field))"
        }
    }

    private inner class Scala(private val schema: Schema) {

        fun source(): String = """
            |import scala.collection.immutable.ListMap
            |import scala.collection.mutable
            |
            |class GraphQLServer(serialization: Wirespec.Serialization, service: GraphQLServer.Selection => GraphQLServer.Service) {
            |  import GraphQLServer._
            |
            |  def this(serialization: Wirespec.Serialization, service: GraphQLServer.Service) = this(serialization, (_: GraphQLServer.Selection) => service)
            |
            |  def handle(request: Wirespec.RawRequest): Wirespec.RawResponse = {
            |    val errors = mutable.ListBuffer.empty[Any]
            |    val data: Option[Map[String, Any]] = try {
            |      val body = serialization.deserializeBody[Request](request.body.getOrElse(throw new IllegalArgumentException("GraphQL request without a body")), scala.reflect.classTag[Request])
            |      val document = new Parser(body.query).document()
            |      val operation = document.operation(body.operationName)
            |      val execution = new Execution(document, operation.defaults ++ body.variables.getOrElse(Map.empty), errors)
            |      Some(operation.`type` match {
            |${schema.roots.map { "case ${it.function.literal()} => execution.${it.function}(operation.selections, Nil)" }.indented(8)}
            |        case other => throw new IllegalArgumentException("Cannot execute a " + other)
            |      })
            |    } catch {
            |      case e: IllegalArgumentException =>
            |        errors += Map("message" -> e.getMessage)
            |        None
            |    }
            |    val response: Map[String, Any] = ListMap.from(List[Option[(String, Any)]](data.map("data" -> _), Option.when(errors.nonEmpty)("errors" -> errors.toList)).flatten)
            |    Wirespec.RawResponse(200, Map("Content-Type" -> List("application/json")), Some(serialization.serializeBody(response, scala.reflect.classTag[Map[String, Any]])))
            |  }
            |
            |  private class Execution(document: Document, variables: Map[String, Any], errors: mutable.ListBuffer[Any]) {
            |${(schema.roots + schema.types).map { it.render() }.indented(4)}
            |
            |$SCALA_EXECUTION
            |  }
            |}
            |
            |object GraphQLServer {
            |  trait Service extends ${schema.served.joinToString(" with ") { "${it.namespace()}.Service" }}
            |
            |  case class Request(query: String, operationName: Option[String], variables: Option[Map[String, Any]])
            |
            |  case class Selection(name: String, alias: Option[String], arguments: Map[String, Any], fields: List[Selection]) {
            |    def get(name: String): Option[Selection] = fields.find(_.name == name)
            |
            |    def contains(name: String): Boolean = get(name).isDefined
            |  }
            |
            |${schema.served.filter { it.hasDefaults }.map { "private lazy val ${it.defaults()}: Map[String, Any] = new Parser(${it.document.literal()}).document().operations.head.defaults" }.indented(2)}
            |
        """.trimMargin() + SCALA_RUNTIME

        private fun Selectable.render(): String = when (val union = definition as? Union) {
            null -> listOf(
                "def $function(${definition?.let { "value: ${it.model()}, " }.orEmpty()}selections: List[Node], path: List[Any]): Map[String, Any] =",
                "  ListMap.from(fields(selections, Some(Set(${conditions.literals()}))).map { field =>",
                "    val resolved: Any = field.field.orNull match {",
                "      case \"__typename\" => ${graphQLName.literal()}",
                (members.map { "case ${it.name.literal()} => ${it.reference.project("value.${it.accessor}")}" } + rpcs.map { it.case() }).indented(6),
                "      case _ => throw new IllegalArgumentException(\"${unknown(graphQLName)}\")",
                "    }",
                "    field.key -> resolved",
                "  })",
            )
            else -> union.members().let { (members, exhaustive) ->
                listOf(
                    "def $function(value: ${union.model()}, selections: List[Node], path: List[Any]): Map[String, Any] = value match {",
                    members.map { "case member: ${it.model()} => ${it.function()}(member, selections, path)" }.indented(2),
                    if (exhaustive && members.isNotEmpty()) "" else "  case other => throw new IllegalArgumentException(\"Cannot select the fields of a \" + other.getClass.getSimpleName)",
                    "}",
                )
            }
        }.filter { it.isNotEmpty() }.joinToString("\n", postfix = "\n")

        private fun Served.case(): String = listOfNotNull(
            "case ${field.literal()} => rpc(field, path) {",
            "  val arguments = this.arguments[${variablesType()}](field, ${if (hasDefaults) defaults() else "Map.empty"}, scala.reflect.classTag[${variablesType()}])".takeIf { variables.isNotEmpty() },
            "service(selected(field)).${call()}(${variables.joinToString(", ") { "arguments.${it.value().sanitized()}" }})".let { call ->
                when {
                    rpc.error != null -> listOf(
                        "$call match {",
                        "  case response: ${namespace()}.Result => ${rpc.result.projectResult("response.value")}",
                        "  case response: ${namespace()}.Error =>",
                        "    errors += response.value",
                        "    None",
                        "}",
                    ).indented(2)
                    rpc.result is Reference.Unit -> "  $call\n  true"
                    else -> "  ${rpc.result.projectResult(call)}"
                }
            },
            "}",
        ).joinToString("\n")

        private fun Reference.projectResult(expression: String): String = when (this) {
            is Reference.Unit -> "true"
            is Reference.Any, is Reference.Custom, is Reference.Dict, is Reference.Iterable, is Reference.Primitive -> project(expression)
        }

        private fun Reference.project(expression: String, depth: Int = 1): String = when {
            operations.composite(this) == null -> expression
            isNullable -> "$expression.map(v$depth => ${required("v$depth", depth + 1)})"
            else -> required(expression, depth)
        }

        private fun Reference.required(expression: String, depth: Int): String = when (this) {
            is Reference.Iterable -> "$expression.map(v$depth => ${reference.project("v$depth", depth + 1)})"
            is Reference.Any, is Reference.Custom, is Reference.Dict, is Reference.Primitive, is Reference.Unit ->
                "${operations.composite(this)?.function()}($expression, field.selections, path :+ field.key)"
        }
    }

    private companion object {
        const val SERVER = "GraphQLServer"
        const val MODEL = "model"
        const val SCHEMA = "GraphQLSchema"
        const val DEFAULT = "default"
        val OPERATIONS = setOf("GraphQLQuery", "GraphQLMutation")

        fun Annotation.single(parameter: String): String? = (parameters.find { it.name == parameter }?.value as? Annotation.Value.Single)?.value

        // The texts below are the generated sources as they are, so they hold no `$`: a variable starts with `$`.

        val KOTLIN_RUNTIME = """
            |    private suspend fun rpc(field: Node, path: List<Any>, call: suspend () -> Any?): Any? = try {
            |      call()
            |    } catch (e: kotlin.coroutines.cancellation.CancellationException) {
            |      throw e
            |    } catch (e: Exception) {
            |      errors.add(mapOf("message" to (e.message ?: e.toString()), "path" to path + field.key))
            |      null
            |    }
            |
            |    private fun <T : Any> arguments(field: Node, defaults: Map<String, Any?>, type: KType): T =
            |      serialization.deserializeBody(serialization.serializeBody(defaults + resolve(field.arguments), typeOf<Map<String, Any?>>()), type)
            |
            |    private fun selected(field: Node): Selection =
            |      Selection(field.field.orEmpty(), field.alias, resolve(field.arguments), fields(field.selections, null).map { selected(it) })
            |
            |    private fun fields(selections: List<Node>, types: Set<String>?, spread: Set<String> = emptySet()): List<Node> = selections
            |      .filter { resolve(it.directives["skip"]?.get("if")) != true && resolve(it.directives["include"]?.get("if")) != false }
            |      .flatMap { selection ->
            |        when {
            |          selection.field != null -> listOf(selection)
            |          selection.fragment == null -> fragment(selection, types, spread)
            |          selection.fragment in spread -> throw IllegalArgumentException("Fragment " + selection.fragment + " spreads itself")
            |          else -> fragment(document.fragments[selection.fragment] ?: throw IllegalArgumentException("Unknown fragment " + selection.fragment), types, spread + selection.fragment)
            |        }
            |      }
            |      .groupBy { it.key }
            |      .values
            |      .map { same -> same.first().let { if (same.size == 1) it else Node(it.field, null, null, it.alias, it.arguments, it.directives, same.flatMap { field -> field.selections }) } }
            |
            |    private fun fragment(fragment: Node, types: Set<String>?, spread: Set<String>): List<Node> =
            |      if (fragment.type == null || types == null || fragment.type in types) fields(fragment.selections, types, spread) else emptyList()
            |
            |    private fun resolve(values: Map<String, Any?>): Map<String, Any?> = values
            |      .filterValues { it !is Variable || it.name in variables }
            |      .mapValues { resolve(it.value) }
            |
            |    @Suppress("UNCHECKED_CAST")
            |    private fun resolve(value: Any?): Any? = when (value) {
            |      is Variable -> variables[value.name]
            |      is List<*> -> value.map { resolve(it) }
            |      is Map<*, *> -> resolve(value as Map<String, Any?>)
            |      else -> value
            |    }
            |  }
            |
            |  private class Variable(val name: String)
            |
            |  private class Node(
            |    val field: String?,
            |    val fragment: String?,
            |    val type: String?,
            |    val alias: String?,
            |    val arguments: Map<String, Any?>,
            |    val directives: Map<String, Map<String, Any?>>,
            |    val selections: List<Node>,
            |  ) {
            |    val key: String = alias ?: field.orEmpty()
            |  }
            |
            |  private class Operation(val type: String, val name: String?, val defaults: Map<String, Any?>, val selections: List<Node>)
            |
            |  private class Document(val operations: List<Operation>, val fragments: Map<String, Node>) {
            |    fun operation(name: String?): Operation = when (name) {
            |      null -> operations.singleOrNull() ?: throw IllegalArgumentException("Expected one operation, or an operationName that picks one")
            |      else -> operations.find { it.name == name } ?: throw IllegalArgumentException("Unknown operation " + name)
            |    }
            |  }
            |
            |  private class Parser(private val text: String) {
            |    private var position = 0
            |
            |    fun document(): Document {
            |      val operations = mutableListOf<Operation>()
            |      val fragments = mutableMapOf<String, Node>()
            |      while (!end()) {
            |        if (peek("{")) {
            |          operations.add(Operation("query", null, emptyMap(), selections()))
            |        } else {
            |          when (val keyword = name()) {
            |            "fragment" -> {
            |              val fragment = name()
            |              if (name() != "on") throw error("Expected on")
            |              val type = name()
            |              fragments[fragment] = Node(null, null, type, null, emptyMap(), directives(), selections())
            |            }
            |            "query", "mutation", "subscription" -> {
            |              val name = if (peek("(") || peek("@") || peek("{")) null else name()
            |              val defaults = defaults()
            |              directives()
            |              operations.add(Operation(keyword, name, defaults, selections()))
            |            }
            |            else -> throw error("Unexpected " + keyword)
            |          }
            |        }
            |      }
            |      return Document(operations, fragments)
            |    }
            |
            |    private fun defaults(): Map<String, Any?> {
            |      val defaults = mutableMapOf<String, Any?>()
            |      if (take("(")) {
            |        while (!take(")")) {
            |          expect("$")
            |          val name = name()
            |          expect(":")
            |          type()
            |          if (take("=")) defaults[name] = value()
            |          directives()
            |        }
            |      }
            |      return defaults
            |    }
            |
            |    private fun type() {
            |      if (take("[")) {
            |        type()
            |        expect("]")
            |      } else {
            |        name()
            |      }
            |      take("!")
            |    }
            |
            |    private fun selections(): List<Node> {
            |      val selections = mutableListOf<Node>()
            |      expect("{")
            |      while (!take("}")) selections.add(selection())
            |      return selections
            |    }
            |
            |    private fun selection(): Node {
            |      if (take("...")) {
            |        if (peek("{") || peek("@")) return Node(null, null, null, null, emptyMap(), directives(), selections())
            |        val name = name()
            |        if (name != "on") return Node(null, name, null, null, emptyMap(), directives(), emptyList())
            |        val type = name()
            |        return Node(null, null, type, null, emptyMap(), directives(), selections())
            |      }
            |      val name = name()
            |      val field = if (take(":")) name() else name
            |      return Node(field, null, null, name.takeIf { it != field }, arguments(), directives(), if (peek("{")) selections() else emptyList())
            |    }
            |
            |    private fun arguments(): Map<String, Any?> {
            |      val arguments = mutableMapOf<String, Any?>()
            |      if (take("(")) {
            |        while (!take(")")) {
            |          val name = name()
            |          expect(":")
            |          arguments[name] = value()
            |        }
            |      }
            |      return arguments
            |    }
            |
            |    private fun directives(): Map<String, Map<String, Any?>> {
            |      val directives = mutableMapOf<String, Map<String, Any?>>()
            |      while (take("@")) {
            |        val name = name()
            |        directives[name] = arguments()
            |      }
            |      return directives
            |    }
            |
            |    private fun value(): Any? {
            |      if (take("$")) return Variable(name())
            |      if (take("[")) {
            |        val list = mutableListOf<Any?>()
            |        while (!take("]")) list.add(value())
            |        return list
            |      }
            |      if (take("{")) {
            |        val fields = mutableMapOf<String, Any?>()
            |        while (!take("}")) {
            |          val name = name()
            |          expect(":")
            |          fields[name] = value()
            |        }
            |        return fields
            |      }
            |      if (peek("\"")) return string()
            |      if (!end() && (text[position] == '-' || text[position].isDigit())) return number()
            |      return when (val name = name()) {
            |        "true" -> true
            |        "false" -> false
            |        "null" -> null
            |        else -> name
            |      }
            |    }
            |
            |    private fun string(): String {
            |      val builder = StringBuilder()
            |      if (take("\"\"\"")) {
            |        val close = text.indexOf("\"\"\"", position)
            |        if (close < 0) throw error("Unterminated string")
            |        builder.append(text.substring(position, close))
            |        position = close + 3
            |        return builder.toString()
            |      }
            |      position++
            |      while (true) {
            |        if (position >= text.length) throw error("Unterminated string")
            |        val char = text[position++]
            |        if (char == '"') return builder.toString()
            |        if (char != '\\') {
            |          builder.append(char)
            |          continue
            |        }
            |        if (position >= text.length) throw error("Unterminated string")
            |        when (val escaped = text[position++]) {
            |          'n' -> builder.append('\n')
            |          'r' -> builder.append('\r')
            |          't' -> builder.append('\t')
            |          'b' -> builder.append('\b')
            |          'f' -> builder.append('\u000C')
            |          'u' -> {
            |            val code = text.substring(position, minOf(position + 4, text.length)).toIntOrNull(16) ?: throw error("Unexpected escape")
            |            builder.append(code.toChar())
            |            position += 4
            |          }
            |          else -> builder.append(escaped)
            |        }
            |      }
            |    }
            |
            |    private fun number(): Any {
            |      val start = position
            |      while (position < text.length && (text[position].isLetterOrDigit() || text[position] == '-' || text[position] == '+' || text[position] == '.')) position++
            |      val number = text.substring(start, position)
            |      return number.toLongOrNull() ?: number.toDoubleOrNull() ?: throw error("Unexpected " + number)
            |    }
            |
            |    private fun name(): String {
            |      skip()
            |      val start = position
            |      while (position < text.length && (text[position].isLetterOrDigit() || text[position] == '_')) position++
            |      if (start == position) throw error("Expected a name")
            |      return text.substring(start, position)
            |    }
            |
            |    private fun skip() {
            |      while (position < text.length) {
            |        val char = text[position]
            |        if (char == '#') {
            |          while (position < text.length && text[position] != '\n') position++
            |        } else if (char.isWhitespace() || char == ',' || char == '﻿') {
            |          position++
            |        } else {
            |          return
            |        }
            |      }
            |    }
            |
            |    private fun end(): Boolean {
            |      skip()
            |      return position >= text.length
            |    }
            |
            |    private fun peek(token: String): Boolean {
            |      skip()
            |      return text.startsWith(token, position)
            |    }
            |
            |    private fun take(token: String): Boolean = peek(token).also { if (it) position += token.length }
            |
            |    private fun expect(token: String) {
            |      if (!take(token)) throw error("Expected " + token)
            |    }
            |
            |    private fun error(message: String): IllegalArgumentException =
            |      IllegalArgumentException("Syntax error in the GraphQL document at " + position + ": " + message)
            |  }
            |}
            |
        """.trimMargin()

        val JAVA_RUNTIME = """
            |    private CompletableFuture<Object> rpc(Node field, List<Object> path, Supplier<CompletableFuture<Object>> call) {
            |      CompletableFuture<Object> result;
            |      try {
            |        result = call.get();
            |      } catch (RuntimeException e) {
            |        result = CompletableFuture.failedFuture(e);
            |      }
            |      return result.exceptionally(failure -> {
            |        Throwable cause = failure instanceof CompletionException && failure.getCause() != null ? failure.getCause() : failure;
            |        if (!(cause instanceof Exception)) throw new CompletionException(cause);
            |        Map<String, Object> error = new LinkedHashMap<>();
            |        error.put("message", cause.getMessage() == null ? cause.toString() : cause.getMessage());
            |        error.put("path", path(path, field));
            |        errors.add(error);
            |        return null;
            |      });
            |    }
            |
            |    private <T> T arguments(Node field, Map<String, Object> defaults, Class<T> type) {
            |      Map<String, Object> arguments = new LinkedHashMap<>(defaults);
            |      arguments.putAll(resolve(field.arguments()));
            |      return serialization.deserializeBody(serialization.serializeBody(arguments, Wirespec.getType(Map.class, null)), Wirespec.getType(type, null));
            |    }
            |
            |    private CompletableFuture<Object> object(List<Node> fields, Function<Node, CompletableFuture<Object>> resolve) {
            |      CompletableFuture<Map<String, Object>> result = CompletableFuture.completedFuture(new LinkedHashMap<>());
            |      for (Node field : fields) {
            |        result = result.thenCompose(object -> resolve.apply(field).thenApply(value -> {
            |          object.put(field.key(), value);
            |          return object;
            |        }));
            |      }
            |      return result.thenApply(object -> object);
            |    }
            |
            |    private CompletableFuture<Object> list(List<CompletableFuture<Object>> values) {
            |      CompletableFuture<List<Object>> result = CompletableFuture.completedFuture(new ArrayList<>());
            |      for (CompletableFuture<Object> value : values) {
            |        result = result.thenCompose(list -> value.thenApply(item -> {
            |          list.add(item);
            |          return list;
            |        }));
            |      }
            |      return result.thenApply(list -> list);
            |    }
            |
            |    private CompletableFuture<Object> done(Object value) {
            |      return CompletableFuture.completedFuture(value);
            |    }
            |
            |    private List<Object> path(List<Object> path, Node field) {
            |      List<Object> extended = new ArrayList<>(path);
            |      extended.add(field.key());
            |      return extended;
            |    }
            |
            |    private Selection selected(Node field) {
            |      return new Selection(field.field(), Optional.ofNullable(field.alias()), resolve(field.arguments()), fields(field.selections(), null).stream().map(this::selected).toList());
            |    }
            |
            |    private List<Node> fields(List<Node> selections, Set<String> types) {
            |      Map<String, Node> fields = new LinkedHashMap<>();
            |      collect(selections, types, Set.of(), fields);
            |      return new ArrayList<>(fields.values());
            |    }
            |
            |    private void collect(List<Node> selections, Set<String> types, Set<String> spread, Map<String, Node> fields) {
            |      for (Node selection : selections) {
            |        if (Boolean.TRUE.equals(directive(selection, "skip")) || Boolean.FALSE.equals(directive(selection, "include"))) continue;
            |        if (selection.field() != null) {
            |          fields.merge(selection.key(), selection, (first, second) -> {
            |            List<Node> merged = new ArrayList<>(first.selections());
            |            merged.addAll(second.selections());
            |            return new Node(first.field(), null, null, first.alias(), first.arguments(), first.directives(), merged);
            |          });
            |        } else if (selection.fragment() == null) {
            |          if (selection.type() == null || types == null || types.contains(selection.type())) collect(selection.selections(), types, spread, fields);
            |        } else {
            |          Node fragment = document.fragments().get(selection.fragment());
            |          if (fragment == null) throw new IllegalArgumentException("Unknown fragment " + selection.fragment());
            |          if (spread.contains(selection.fragment())) throw new IllegalArgumentException("Fragment " + selection.fragment() + " spreads itself");
            |          Set<String> spreading = new java.util.HashSet<>(spread);
            |          spreading.add(selection.fragment());
            |          if (types == null || types.contains(fragment.type())) collect(fragment.selections(), types, spreading, fields);
            |        }
            |      }
            |    }
            |
            |    private Object directive(Node selection, String name) {
            |      Map<String, Object> arguments = selection.directives().get(name);
            |      return arguments == null ? null : resolve(arguments.get("if"));
            |    }
            |
            |    private Map<String, Object> resolve(Map<?, ?> values) {
            |      Map<String, Object> resolved = new LinkedHashMap<>();
            |      values.forEach((name, value) -> {
            |        if (!(value instanceof Variable variable) || variables.containsKey(variable.name())) resolved.put((String) name, resolve(value));
            |      });
            |      return resolved;
            |    }
            |
            |    private Object resolve(Object value) {
            |      if (value instanceof Variable variable) return variables.get(variable.name());
            |      if (value instanceof List<?> list) {
            |        List<Object> resolved = new ArrayList<>();
            |        list.forEach(item -> resolved.add(resolve(item)));
            |        return resolved;
            |      }
            |      if (value instanceof Map<?, ?> map) return resolve(map);
            |      return value;
            |    }
            |  }
            |
            |  private record Variable(String name) {}
            |
            |  private record Node(String field, String fragment, String type, String alias, Map<String, Object> arguments, Map<String, Map<String, Object>> directives, List<Node> selections) {
            |    String key() {
            |      return alias != null ? alias : field;
            |    }
            |  }
            |
            |  private record Operation(String type, String name, Map<String, Object> defaults, List<Node> selections) {}
            |
            |  private record Document(List<Operation> operations, Map<String, Node> fragments) {
            |    Operation operation(String name) {
            |      if (name == null && operations.size() == 1) return operations.get(0);
            |      if (name == null) throw new IllegalArgumentException("Expected one operation, or an operationName that picks one");
            |      return operations.stream().filter(operation -> name.equals(operation.name())).findFirst()
            |        .orElseThrow(() -> new IllegalArgumentException("Unknown operation " + name));
            |    }
            |  }
            |
            |  private static final class Parser {
            |    private final String text;
            |    private int position = 0;
            |
            |    Parser(String text) {
            |      this.text = text;
            |    }
            |
            |    Document document() {
            |      List<Operation> operations = new ArrayList<>();
            |      Map<String, Node> fragments = new LinkedHashMap<>();
            |      while (!end()) {
            |        if (peek("{")) {
            |          operations.add(new Operation("query", null, Map.of(), selections()));
            |          continue;
            |        }
            |        String keyword = name();
            |        switch (keyword) {
            |          case "fragment" -> {
            |            String fragment = name();
            |            if (!name().equals("on")) throw error("Expected on");
            |            String type = name();
            |            fragments.put(fragment, new Node(null, null, type, null, Map.of(), directives(), selections()));
            |          }
            |          case "query", "mutation", "subscription" -> {
            |            String name = peek("(") || peek("@") || peek("{") ? null : name();
            |            Map<String, Object> defaults = defaults();
            |            directives();
            |            operations.add(new Operation(keyword, name, defaults, selections()));
            |          }
            |          default -> throw error("Unexpected " + keyword);
            |        }
            |      }
            |      return new Document(operations, fragments);
            |    }
            |
            |    private Map<String, Object> defaults() {
            |      Map<String, Object> defaults = new LinkedHashMap<>();
            |      if (take("(")) {
            |        while (!take(")")) {
            |          expect("$");
            |          String name = name();
            |          expect(":");
            |          type();
            |          if (take("=")) defaults.put(name, value());
            |          directives();
            |        }
            |      }
            |      return defaults;
            |    }
            |
            |    private void type() {
            |      if (take("[")) {
            |        type();
            |        expect("]");
            |      } else {
            |        name();
            |      }
            |      take("!");
            |    }
            |
            |    private List<Node> selections() {
            |      List<Node> selections = new ArrayList<>();
            |      expect("{");
            |      while (!take("}")) selections.add(selection());
            |      return selections;
            |    }
            |
            |    private Node selection() {
            |      if (take("...")) {
            |        if (peek("{") || peek("@")) return new Node(null, null, null, null, Map.of(), directives(), selections());
            |        String name = name();
            |        if (!name.equals("on")) return new Node(null, name, null, null, Map.of(), directives(), List.of());
            |        String type = name();
            |        return new Node(null, null, type, null, Map.of(), directives(), selections());
            |      }
            |      String name = name();
            |      String field = take(":") ? name() : name;
            |      return new Node(field, null, null, name.equals(field) ? null : name, arguments(), directives(), peek("{") ? selections() : List.of());
            |    }
            |
            |    private Map<String, Object> arguments() {
            |      Map<String, Object> arguments = new LinkedHashMap<>();
            |      if (take("(")) {
            |        while (!take(")")) {
            |          String name = name();
            |          expect(":");
            |          arguments.put(name, value());
            |        }
            |      }
            |      return arguments;
            |    }
            |
            |    private Map<String, Map<String, Object>> directives() {
            |      Map<String, Map<String, Object>> directives = new LinkedHashMap<>();
            |      while (take("@")) {
            |        String name = name();
            |        directives.put(name, arguments());
            |      }
            |      return directives;
            |    }
            |
            |    private Object value() {
            |      if (take("$")) return new Variable(name());
            |      if (take("[")) {
            |        List<Object> list = new ArrayList<>();
            |        while (!take("]")) list.add(value());
            |        return list;
            |      }
            |      if (take("{")) {
            |        Map<String, Object> fields = new LinkedHashMap<>();
            |        while (!take("}")) {
            |          String name = name();
            |          expect(":");
            |          fields.put(name, value());
            |        }
            |        return fields;
            |      }
            |      if (peek("\"")) return string();
            |      if (!end() && (text.charAt(position) == '-' || Character.isDigit(text.charAt(position)))) return number();
            |      String name = name();
            |      return switch (name) {
            |        case "true" -> true;
            |        case "false" -> false;
            |        case "null" -> null;
            |        default -> name;
            |      };
            |    }
            |
            |    private String string() {
            |      StringBuilder builder = new StringBuilder();
            |      if (take("\"\"\"")) {
            |        int close = text.indexOf("\"\"\"", position);
            |        if (close < 0) throw error("Unterminated string");
            |        builder.append(text, position, close);
            |        position = close + 3;
            |        return builder.toString();
            |      }
            |      position++;
            |      while (true) {
            |        if (position >= text.length()) throw error("Unterminated string");
            |        char next = text.charAt(position++);
            |        if (next == '"') return builder.toString();
            |        if (next != '\\') {
            |          builder.append(next);
            |          continue;
            |        }
            |        if (position >= text.length()) throw error("Unterminated string");
            |        char escaped = text.charAt(position++);
            |        switch (escaped) {
            |          case 'n' -> builder.append('\n');
            |          case 'r' -> builder.append('\r');
            |          case 't' -> builder.append('\t');
            |          case 'b' -> builder.append('\b');
            |          case 'f' -> builder.append('\f');
            |          case 'u' -> {
            |            try {
            |              builder.append((char) Integer.parseInt(text.substring(position, Math.min(position + 4, text.length())), 16));
            |            } catch (NumberFormatException e) {
            |              throw error("Unexpected escape");
            |            }
            |            position += 4;
            |          }
            |          default -> builder.append(escaped);
            |        }
            |      }
            |    }
            |
            |    private Object number() {
            |      int start = position;
            |      while (position < text.length() && (Character.isLetterOrDigit(text.charAt(position)) || text.charAt(position) == '-' || text.charAt(position) == '+' || text.charAt(position) == '.')) position++;
            |      String number = text.substring(start, position);
            |      try {
            |        return Long.parseLong(number);
            |      } catch (NumberFormatException e) {
            |        try {
            |          return Double.parseDouble(number);
            |        } catch (NumberFormatException e2) {
            |          throw error("Unexpected " + number);
            |        }
            |      }
            |    }
            |
            |    private String name() {
            |      skip();
            |      int start = position;
            |      while (position < text.length() && (Character.isLetterOrDigit(text.charAt(position)) || text.charAt(position) == '_')) position++;
            |      if (start == position) throw error("Expected a name");
            |      return text.substring(start, position);
            |    }
            |
            |    private void skip() {
            |      while (position < text.length()) {
            |        char next = text.charAt(position);
            |        if (next == '#') {
            |          while (position < text.length() && text.charAt(position) != '\n') position++;
            |        } else if (Character.isWhitespace(next) || next == ',' || next == 0xFEFF) {
            |          position++;
            |        } else {
            |          return;
            |        }
            |      }
            |    }
            |
            |    private boolean end() {
            |      skip();
            |      return position >= text.length();
            |    }
            |
            |    private boolean peek(String token) {
            |      skip();
            |      return text.startsWith(token, position);
            |    }
            |
            |    private boolean take(String token) {
            |      boolean found = peek(token);
            |      if (found) position += token.length();
            |      return found;
            |    }
            |
            |    private void expect(String token) {
            |      if (!take(token)) throw error("Expected " + token);
            |    }
            |
            |    private IllegalArgumentException error(String message) {
            |      return new IllegalArgumentException("Syntax error in the GraphQL document at " + position + ": " + message);
            |    }
            |  }
            |}
            |
        """.trimMargin()

        val SCALA_EXECUTION = """
            |    private def rpc(field: Node, path: List[Any])(call: => Any): Any = try call catch {
            |      case scala.util.control.NonFatal(e) =>
            |        errors += Map("message" -> Option(e.getMessage).getOrElse(e.toString), "path" -> (path :+ field.key))
            |        None
            |    }
            |
            |    private def arguments[T](field: Node, defaults: Map[String, Any], `type`: scala.reflect.ClassTag[?]): T =
            |      serialization.deserializeBody[T](serialization.serializeBody(defaults ++ resolveAll(field.arguments), scala.reflect.classTag[Map[String, Any]]), `type`)
            |
            |    private def selected(field: Node): Selection =
            |      Selection(field.field.getOrElse(""), field.alias, resolveAll(field.arguments), fields(field.selections, None).map(selected))
            |
            |    private def fields(selections: List[Node], types: Option[Set[String]], spread: Set[String] = Set.empty): List[Node] = {
            |      val collected = selections
            |        .filter(selection => !directive(selection, "skip").contains(true) && !directive(selection, "include").contains(false))
            |        .flatMap { selection =>
            |          (selection.field, selection.fragment) match {
            |            case (Some(_), _) => List(selection)
            |            case (None, None) => fragment(selection, types, spread)
            |            case (None, Some(name)) if spread.contains(name) => throw new IllegalArgumentException("Fragment " + name + " spreads itself")
            |            case (None, Some(name)) => fragment(document.fragments.getOrElse(name, throw new IllegalArgumentException("Unknown fragment " + name)), types, spread + name)
            |          }
            |        }
            |      collected.map(_.key).distinct.map { key =>
            |        val same = collected.filter(_.key == key)
            |        if (same.size == 1) same.head else same.head.copy(selections = same.flatMap(_.selections))
            |      }
            |    }
            |
            |    private def fragment(fragment: Node, types: Option[Set[String]], spread: Set[String]): List[Node] =
            |      if (fragment.`type`.forall(condition => types.forall(_.contains(condition)))) fields(fragment.selections, types, spread) else Nil
            |
            |    private def directive(selection: Node, name: String): Option[Any] =
            |      selection.directives.get(name).flatMap(_.get("if")).map(resolve)
            |
            |    private def resolveAll(values: Map[String, Any]): Map[String, Any] = values.collect {
            |      case (name, Variable(variable)) if variables.contains(variable) => name -> variables(variable)
            |      case (name, value) if !value.isInstanceOf[Variable] => name -> resolve(value)
            |    }
            |
            |    private def resolve(value: Any): Any = value match {
            |      case Variable(name) => variables.getOrElse(name, null)
            |      case list: List[?] => list.map(resolve)
            |      case map: Map[?, ?] => resolveAll(map.asInstanceOf[Map[String, Any]])
            |      case other => other
            |    }
        """.trimMargin()

        val SCALA_RUNTIME = """
            |  private case class Variable(name: String)
            |
            |  private case class Node(
            |    field: Option[String],
            |    fragment: Option[String],
            |    `type`: Option[String],
            |    alias: Option[String],
            |    arguments: Map[String, Any],
            |    directives: Map[String, Map[String, Any]],
            |    selections: List[Node],
            |  ) {
            |    def key: String = alias.orElse(field).getOrElse("")
            |  }
            |
            |  private case class Operation(`type`: String, name: Option[String], defaults: Map[String, Any], selections: List[Node])
            |
            |  private case class Document(operations: List[Operation], fragments: Map[String, Node]) {
            |    def operation(name: Option[String]): Operation = name match {
            |      case None if operations.size == 1 => operations.head
            |      case None => throw new IllegalArgumentException("Expected one operation, or an operationName that picks one")
            |      case Some(picked) => operations.find(_.name.contains(picked)).getOrElse(throw new IllegalArgumentException("Unknown operation " + picked))
            |    }
            |  }
            |
            |  private class Parser(text: String) {
            |    private var position = 0
            |
            |    def document(): Document = {
            |      val operations = List.newBuilder[Operation]
            |      val fragments = Map.newBuilder[String, Node]
            |      while (!end()) {
            |        if (peek("{")) {
            |          operations += Operation("query", None, Map.empty, selections())
            |        } else {
            |          name() match {
            |            case "fragment" =>
            |              val fragment = name()
            |              if (name() != "on") throw error("Expected on")
            |              val condition = name()
            |              fragments += fragment -> Node(None, None, Some(condition), None, Map.empty, directives(), selections())
            |            case keyword @ ("query" | "mutation" | "subscription") =>
            |              val operation = if (peek("(") || peek("@") || peek("{")) None else Some(name())
            |              val variables = defaults()
            |              directives()
            |              operations += Operation(keyword, operation, variables, selections())
            |            case keyword => throw error("Unexpected " + keyword)
            |          }
            |        }
            |      }
            |      Document(operations.result(), fragments.result())
            |    }
            |
            |    private def defaults(): Map[String, Any] = {
            |      val defaults = Map.newBuilder[String, Any]
            |      if (take("(")) {
            |        while (!take(")")) {
            |          expect("$")
            |          val variable = name()
            |          expect(":")
            |          skipType()
            |          if (take("=")) defaults += variable -> value()
            |          directives()
            |        }
            |      }
            |      defaults.result()
            |    }
            |
            |    private def skipType(): Unit = {
            |      if (take("[")) {
            |        skipType()
            |        expect("]")
            |      } else {
            |        name()
            |      }
            |      take("!")
            |    }
            |
            |    private def selections(): List[Node] = {
            |      val selections = List.newBuilder[Node]
            |      expect("{")
            |      while (!take("}")) selections += selection()
            |      selections.result()
            |    }
            |
            |    private def selection(): Node =
            |      if (take("...")) {
            |        if (peek("{") || peek("@")) Node(None, None, None, None, Map.empty, directives(), selections())
            |        else name() match {
            |          case "on" =>
            |            val condition = name()
            |            Node(None, None, Some(condition), None, Map.empty, directives(), selections())
            |          case fragment => Node(None, Some(fragment), None, None, Map.empty, directives(), Nil)
            |        }
            |      } else {
            |        val first = name()
            |        val field = if (take(":")) name() else first
            |        Node(Some(field), None, None, Some(first).filter(_ != field), arguments(), directives(), if (peek("{")) selections() else Nil)
            |      }
            |
            |    private def arguments(): Map[String, Any] = {
            |      val arguments = Map.newBuilder[String, Any]
            |      if (take("(")) {
            |        while (!take(")")) {
            |          val argument = name()
            |          expect(":")
            |          arguments += argument -> value()
            |        }
            |      }
            |      arguments.result()
            |    }
            |
            |    private def directives(): Map[String, Map[String, Any]] = {
            |      val directives = Map.newBuilder[String, Map[String, Any]]
            |      while (take("@")) {
            |        val directive = name()
            |        directives += directive -> arguments()
            |      }
            |      directives.result()
            |    }
            |
            |    private def value(): Any =
            |      if (take("$")) {
            |        Variable(name())
            |      } else if (take("[")) {
            |        val list = List.newBuilder[Any]
            |        while (!take("]")) list += value()
            |        list.result()
            |      } else if (take("{")) {
            |        val fields = Map.newBuilder[String, Any]
            |        while (!take("}")) {
            |          val field = name()
            |          expect(":")
            |          fields += field -> value()
            |        }
            |        fields.result()
            |      } else if (peek("\"")) {
            |        string()
            |      } else if (!end() && (text.charAt(position) == '-' || text.charAt(position).isDigit)) {
            |        number()
            |      } else {
            |        name() match {
            |          case "true" => true
            |          case "false" => false
            |          case "null" => null
            |          case other => other
            |        }
            |      }
            |
            |    private def string(): String = {
            |      val builder = new StringBuilder
            |      if (take("\"\"\"")) {
            |        val close = text.indexOf("\"\"\"", position)
            |        if (close < 0) throw error("Unterminated string")
            |        builder.append(text.substring(position, close))
            |        position = close + 3
            |      } else {
            |        position += 1
            |        var open = true
            |        while (open) {
            |          if (position >= text.length) throw error("Unterminated string")
            |          val next = text.charAt(position)
            |          position += 1
            |          if (next == '"') {
            |            open = false
            |          } else if (next != '\\') {
            |            builder.append(next)
            |          } else {
            |            if (position >= text.length) throw error("Unterminated string")
            |            val escaped = text.charAt(position)
            |            position += 1
            |            escaped match {
            |              case 'n' => builder.append('\n')
            |              case 'r' => builder.append('\r')
            |              case 't' => builder.append('\t')
            |              case 'b' => builder.append('\b')
            |              case 'f' => builder.append('\f')
            |              case 'u' =>
            |                val code = try Integer.parseInt(text.substring(position, math.min(position + 4, text.length)), 16) catch {
            |                  case _: NumberFormatException => throw error("Unexpected escape")
            |                }
            |                builder.append(code.toChar)
            |                position += 4
            |              case other => builder.append(other)
            |            }
            |          }
            |        }
            |      }
            |      builder.toString
            |    }
            |
            |    private def number(): Any = {
            |      val start = position
            |      while (position < text.length && (text.charAt(position).isLetterOrDigit || text.charAt(position) == '-' || text.charAt(position) == '+' || text.charAt(position) == '.')) position += 1
            |      val number = text.substring(start, position)
            |      number.toLongOption.orElse(number.toDoubleOption).getOrElse(throw error("Unexpected " + number))
            |    }
            |
            |    private def name(): String = {
            |      skip()
            |      val start = position
            |      while (position < text.length && (text.charAt(position).isLetterOrDigit || text.charAt(position) == '_')) position += 1
            |      if (start == position) throw error("Expected a name")
            |      text.substring(start, position)
            |    }
            |
            |    private def skip(): Unit = {
            |      var skipping = true
            |      while (skipping && position < text.length) {
            |        val next = text.charAt(position)
            |        if (next == '#') {
            |          while (position < text.length && text.charAt(position) != '\n') position += 1
            |        } else if (next.isWhitespace || next == ',' || next == '﻿') {
            |          position += 1
            |        } else {
            |          skipping = false
            |        }
            |      }
            |    }
            |
            |    private def end(): Boolean = {
            |      skip()
            |      position >= text.length
            |    }
            |
            |    private def peek(token: String): Boolean = {
            |      skip()
            |      text.startsWith(token, position)
            |    }
            |
            |    private def take(token: String): Boolean = {
            |      val found = peek(token)
            |      if (found) position += token.length
            |      found
            |    }
            |
            |    private def expect(token: String): Unit =
            |      if (!take(token)) throw error("Expected " + token)
            |
            |    private def error(message: String): IllegalArgumentException =
            |      new IllegalArgumentException("Syntax error in the GraphQL document at " + position + ": " + message)
            |  }
            |}
            |
        """.trimMargin()
    }
}
