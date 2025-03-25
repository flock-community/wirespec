package community.flock.wirespec.compiler.core.emit

import arrow.core.NonEmptyList
import community.flock.wirespec.compiler.core.concatGenerics
import community.flock.wirespec.compiler.core.emit.common.DEFAULT_GENERATED_PACKAGE_STRING
import community.flock.wirespec.compiler.core.emit.common.DEFAULT_SHARED_PACKAGE_STRING
import community.flock.wirespec.compiler.core.emit.common.Emitted
import community.flock.wirespec.compiler.core.emit.common.Emitter
import community.flock.wirespec.compiler.core.emit.common.FileExtension
import community.flock.wirespec.compiler.core.emit.common.Keywords
import community.flock.wirespec.compiler.core.emit.common.PackageName
import community.flock.wirespec.compiler.core.emit.common.Spacer
import community.flock.wirespec.compiler.core.emit.shared.JavaShared
import community.flock.wirespec.compiler.core.orNull
import community.flock.wirespec.compiler.core.parse.AST
import community.flock.wirespec.compiler.core.parse.Channel
import community.flock.wirespec.compiler.core.parse.Definition
import community.flock.wirespec.compiler.core.parse.DefinitionIdentifier
import community.flock.wirespec.compiler.core.parse.Endpoint
import community.flock.wirespec.compiler.core.parse.Enum
import community.flock.wirespec.compiler.core.parse.Field
import community.flock.wirespec.compiler.core.parse.FieldIdentifier
import community.flock.wirespec.compiler.core.parse.Identifier
import community.flock.wirespec.compiler.core.parse.Reference
import community.flock.wirespec.compiler.core.parse.Refined
import community.flock.wirespec.compiler.core.parse.Type
import community.flock.wirespec.compiler.core.parse.Union
import community.flock.wirespec.compiler.utils.Logger

open class JavaEmitter(
    private val packageName: PackageName = PackageName(DEFAULT_GENERATED_PACKAGE_STRING),
) : Emitter(true) {

    val import = """
        |
        |import $DEFAULT_SHARED_PACKAGE_STRING.java.Wirespec;
        |
    """.trimMargin()

    override val extension = FileExtension.Java

    override val shared = JavaShared

    override fun Definition.emitName(): String = when (this) {
        is Endpoint -> "${emit(identifier)}Endpoint"
        is Channel -> "${emit(identifier)}Channel"
        is Enum -> emit(identifier)
        is Refined -> emit(identifier)
        is Type -> emit(identifier)
        is Union -> emit(identifier)
    }

    override val singleLineComment = "//"

    override fun emit(ast: AST, logger: Logger): NonEmptyList<Emitted> =
        super.emit(ast, logger).map { (typeName, result) ->
            Emitted(
                typeName = typeName.sanitizeSymbol(),
                result = """
                    |package $packageName;
                    |${if (ast.needImports()) import else ""}
                    |$result
                """.trimMargin().trimStart()
            )
        }

    override fun emit(type: Type, ast: AST) = """
        |public record ${type.emitName()} (
        |${type.shape.emit()}
        |)${type.extends.run { if (isEmpty()) "" else " extends ${joinToString(", ") { it.emit() }}" }}${type.emitUnion(ast)} {
        |};
        |
    """.trimMargin()

    fun Type.emitUnion(ast: AST) = ast
        .filterIsInstance<Union>()
        .filter { union -> union.entries.filterIsInstance<Reference.Custom>().any { it.value == identifier.value } }
        .map { it.identifier.value }
        .takeIf { it.isNotEmpty() }
        ?.joinToString(", ", "implements ")
        .orEmpty()

    override fun Type.Shape.emit() = value.joinToString("\n") { "${Spacer}${it.emit()}," }.dropLast(1)

    override fun Field.emit() = "${reference.emit()} ${emit(identifier)}"

    override fun Reference.emit(): String = emitType()
        .let { if (isNullable) "java.util.Optional<$it>" else it }

    private fun Reference.emitType(): String = when (this) {
        is Reference.Dict -> "java.util.Map<String, ${reference.emit()}>"
        is Reference.Iterable -> "java.util.List<${reference.emit()}>"
        is Reference.Unit -> "void"
        is Reference.Any -> "Object"
        is Reference.Custom -> value
        is Reference.Primitive -> emit()
    }

    fun Reference.emitRoot(void: String = "void"): String = when (this) {
        is Reference.Dict -> reference.emitRoot()
        is Reference.Iterable -> reference.emitRoot()
        is Reference.Unit -> void
        is Reference.Any -> emitType()
        is Reference.Custom -> emitType()
        is Reference.Primitive -> emitType()
    }

    private fun Reference.Custom.emit() = value
        .takeIf { it in internalClasses }
        ?.let { "$packageName." }
        .orEmpty()
        .let { "$it$value" }

    private fun Reference.Primitive.emit() = when (type) {
        is Reference.Primitive.Type.String -> "String"
        is Reference.Primitive.Type.Integer -> when (type.precision) {
            Reference.Primitive.Type.Precision.P32 -> "Integer"
            Reference.Primitive.Type.Precision.P64 -> "Long"
        }

        is Reference.Primitive.Type.Number -> when (type.precision) {
            Reference.Primitive.Type.Precision.P32 -> "Float"
            Reference.Primitive.Type.Precision.P64 -> "Double"
        }

        is Reference.Primitive.Type.Boolean -> "Boolean"
        is Reference.Primitive.Type.Bytes -> "byte[]"
    }

    override fun emit(identifier: Identifier) = when (identifier) {
        is DefinitionIdentifier -> identifier.value.sanitizeSymbol()
        is FieldIdentifier -> identifier.value.sanitizeSymbol().sanitizeKeywords()
    }

    override fun emit(refined: Refined) = """
        |public record ${emit(refined.identifier)} (String value) implements Wirespec.Refined {
        |${Spacer}@Override
        |${Spacer}public String toString() { return value; }
        |${Spacer}public static boolean validate(${refined.emitName()} record) {
        |${Spacer}${refined.validator.emit()}
        |${Spacer}}
        |${Spacer}@Override
        |${Spacer}public String getValue() { return value; }
        |}
        |
    """.trimMargin()

    override fun Refined.Validator.emit() =
        """${Spacer}return java.util.regex.Pattern.compile("${expression.replace("\\", "\\\\")}").matcher(record.value).find();"""

    override fun emit(enum: Enum, ast: AST) = """
        |public enum ${emit(enum.identifier)} implements Wirespec.Enum {
        |${enum.entries.joinToString(",\n") { "${it.sanitizeEnum().sanitizeKeywords()}(\"$it\")" }.spacer()};
        |${Spacer}public final String label;
        |${Spacer}${emit(enum.identifier)}(String label) {
        |${Spacer(2)}this.label = label;
        |${Spacer}}
        |${Spacer}@Override
        |${Spacer}public String toString() {
        |${Spacer(2)}return label;
        |${Spacer}}
        |${Spacer}@Override
        |${Spacer}public String getLabel() {
        |${Spacer(2)}return label;
        |${Spacer}}
        |}
        |
    """.trimMargin()

    override fun emit(union: Union) = """
        |public sealed interface ${union.emitName()} permits ${union.entries.joinToString { it.value }} {}
        |
    """.trimMargin()

    override fun emit(channel: Channel) = """
        |public interface ${emit(channel.identifier)}Channel {
        |   void invoke(${channel.reference.emitRoot()} message);
        |}
        |
    """.trimMargin()

    override fun emit(endpoint: Endpoint) = """
        |public interface ${emit(endpoint.identifier)}Endpoint extends Wirespec.Endpoint {
        |${endpoint.pathParams.emitObject("Path", "Wirespec.Path") { it.emit() }}
        |
        |${endpoint.queries.emitObject("Queries", "Wirespec.Queries") { it.emit() }}
        |
        |${endpoint.headers.emitObject("RequestHeaders", "Wirespec.Request.Headers") { it.emit() }}
        |
        |${endpoint.requests.first().emit(endpoint)}
        |
        |${Spacer}sealed interface Response<T> extends Wirespec.Response<T> {}
        |${endpoint.emitStatusInterfaces()}
        |${endpoint.emitResponseInterfaces()}
        |
        |${endpoint.responses.distinctBy { it.status }.joinToString("\n") { it.emit() }}
        |
        |${Spacer}interface Handler extends Wirespec.Handler {
        |
        |${endpoint.requests.first().emitRequestFunctions(endpoint)}
        |
        |${Spacer(2)}static Wirespec.RawResponse toResponse(Wirespec.Serializer<String> serialization, Response<?> response) {
        |${endpoint.responses.distinctBy { it.status }.joinToString("\n") { it.emitSerialized() }}
        |${Spacer(3)}else { throw new IllegalStateException("Cannot match response with status: " + response.getStatus());}
        |${Spacer(2)}}
        |
        |${Spacer(2)}static Response<?> fromResponse(Wirespec.Deserializer<String> serialization, Wirespec.RawResponse response) {
        |${Spacer(3)}return switch (response.statusCode()) {
        |${endpoint.responses.distinctBy { it.status }.filter { it.status.isStatusCode() }.joinToString("\n") { it.emitDeserialized() }}
        |${Spacer(4)}default -> throw new IllegalStateException("Cannot match response with status: " + response.statusCode());
        |${Spacer(3)}};
        |${Spacer(2)}}
        |
        |${Spacer(2)}${emitHandleFunction(endpoint)}
        |${Spacer(2)}class Handlers implements Wirespec.Server<Request, Response<?>>, Wirespec.Client<Request, Response<?>> {
        |${Spacer(3)}@Override public String getPathTemplate() { return "/${endpoint.path.joinToString("/") { it.emit() }}"; }
        |${Spacer(3)}@Override public String getMethod() { return "${endpoint.method}"; }
        |${Spacer(3)}@Override public Wirespec.ServerEdge<Request, Response<?>> getServer(Wirespec.Serialization<String> serialization) {
        |${Spacer(4)}return new Wirespec.ServerEdge<>() {
        |${Spacer(5)}@Override public Request from(Wirespec.RawRequest request) { return fromRequest(serialization, request); }
        |${Spacer(5)}@Override public Wirespec.RawResponse to(Response<?> response) { return toResponse(serialization, response); }
        |${Spacer(4)}};
        |${Spacer(3)}}
        |${Spacer(3)}@Override public Wirespec.ClientEdge<Request, Response<?>> getClient(Wirespec.Serialization<String> serialization) {
        |${Spacer(4)}return new Wirespec.ClientEdge<>() {
        |${Spacer(5)}@Override public Wirespec.RawRequest to(Request request) { return toRequest(serialization, request); }
        |${Spacer(5)}@Override public Response<?> from(Wirespec.RawResponse response) { return fromResponse(serialization, response); }
        |${Spacer(4)}};
        |${Spacer(3)}}
        |${Spacer(2)}}
        |${Spacer}}
        |}
        |
    """.trimMargin()

    open fun emitHandleFunction(endpoint: Endpoint) =
        "java.util.concurrent.CompletableFuture<Response<?>> ${emit(endpoint.identifier).firstToLower()}(Request request);"

    private fun Endpoint.emitStatusInterfaces() = responses
        .map { it.status.first() }
        .distinct()
        .joinToString("\n") { "${Spacer}sealed interface Response${it}XX<T> extends Response<T> {}" }

    private fun Endpoint.emitResponseInterfaces() = responses
        .distinctBy { it.status }
        .map { it.content.emit() }
        .distinct()
        .joinToString("\n") { "${Spacer}sealed interface Response${it.concatGenerics()} extends Response<$it> {}" }

    private fun <E> List<E>.emitObject(name: String, extends: String, block: (E) -> String) =
        if (isEmpty()) "${Spacer}class $name implements $extends {}"
        else """
            |${Spacer}public record $name(
            |${joinToString(",\n") { "${Spacer(2)}${block(it)}" }}
            |${Spacer}) implements $extends {}
        """.trimMargin()

    fun Endpoint.Request.emit(endpoint: Endpoint) = """
        |${Spacer}class Request implements Wirespec.Request<${content.emit()}> {
        |${Spacer(2)}private final Path path;
        |${Spacer(2)}private final Wirespec.Method method;
        |${Spacer(2)}private final Queries queries;
        |${Spacer(2)}private final RequestHeaders headers;
        |${Spacer(2)}private final ${content.emit()} body;
        |${Spacer(2)}${emitConstructor(endpoint)}
        |${Spacer(2)}@Override public Path getPath() { return path; }
        |${Spacer(2)}@Override public Wirespec.Method getMethod() { return method; }
        |${Spacer(2)}@Override public Queries getQueries() { return queries; }
        |${Spacer(2)}@Override public RequestHeaders getHeaders() { return headers; }
        |${Spacer(2)}@Override public ${content.emit()} getBody() { return body; }
        |${Spacer}}
    """.trimMargin()

    private fun Endpoint.Request.emitRequestFunctions(endpoint: Endpoint) = """
        |${Spacer(2)}static Wirespec.RawRequest toRequest(Wirespec.Serializer<String> serialization, Request request) {
        |${Spacer(3)}return new Wirespec.RawRequest(
        |${Spacer(4)}request.method.name(),
        |${Spacer(4)}java.util.List.of(${endpoint.path.joinToString { when (it) {is Endpoint.Segment.Literal -> """"${it.value}""""; is Endpoint.Segment.Param -> it.emitIdentifier() } }}),
        |${Spacer(4)}${if (endpoint.queries.isNotEmpty()) "java.util.Map.ofEntries(${endpoint.queries.joinToString { it.emitSerializedParams("queries") }})" else "java.util.Collections.emptyMap()"},
        |${Spacer(4)}${if (endpoint.headers.isNotEmpty()) "java.util.Map.ofEntries(${endpoint.headers.joinToString { it.emitSerializedParams("headers") }})" else "java.util.Collections.emptyMap()"},
        |${Spacer(4)}serialization.serialize(request.getBody(), Wirespec.getType(${content.emit()}.class, ${content?.reference?.isIterable ?: false}))
        |${Spacer(3)});
        |${Spacer(2)}}
        |
        |${Spacer(2)}static Request fromRequest(Wirespec.Deserializer<String> serialization, Wirespec.RawRequest request) {
        |${Spacer(3)}return new Request(${emitDeserializedParams(endpoint)});
        |${Spacer(2)}}
    """.trimMargin()

    fun Endpoint.Response.emit() = """
        |${Spacer}record Response${status.firstToUpper()}(${listOfNotNull(headers.joinToString { it.emit() }.orNull(), content?.let { "${it.emit()} body" }).joinToString()}) implements Response${status.first()}XX<${content.emit()}>, Response${content.emit().concatGenerics()} {
        |${Spacer(2)}@Override public int getStatus() { return ${status.fixStatus()}; }
        |${Spacer(2)}${headers.joinToString { emit(it.identifier) }.let { "@Override public Headers getHeaders() { return new Headers($it); }" }}
        |${Spacer(2)}@Override public ${content.emit()} getBody() { return ${if (content == null) "null" else "body"}; }
        |${Spacer(1)}${headers.emitObject("Headers", "Wirespec.Response.Headers") { it.emit() }}
        |${Spacer}}
    """.trimMargin()

    private fun Endpoint.Request.emitConstructor(endpoint: Endpoint) =
        "public Request(${
            listOfNotNull(
                endpoint.pathParams.joinToString { it.emit() }.orNull(),
                endpoint.queries.joinToString { it.emit() }.orNull(),
                endpoint.headers.joinToString { it.emit() }.orNull(),
                content?.let { "${it.emit()} body" }
            ).joinToString()
        }) {\n${Spacer(3)}${
            listOfNotNull(
                endpoint.pathParams.joinToString { emit(it.identifier) }.let { "this.path = new Path($it);" },
                "this.method = Wirespec.Method.${endpoint.method.name};",
                endpoint.queries.joinToString { emit(it.identifier) }.let { "this.queries = new Queries($it);" },
                endpoint.headers.joinToString { emit(it.identifier) }
                    .let { "this.headers = new RequestHeaders($it);" },
                "this.body = ${content?.let { "body" } ?: "null"};"
            ).joinToString("\n${Spacer(3)}")
        }\n${Spacer(2)}}"

    private fun Endpoint.Request.emitDeserializedParams(endpoint: Endpoint) = listOfNotNull(
        endpoint.indexedPathParams.joinToString { it.emitDeserialized() }.orNull(),
        endpoint.queries.joinToString { it.emitDeserializedParams("queries") }.orNull(),
        endpoint.headers.joinToString { it.emitDeserializedParams("headers") }.orNull(),
        content?.let { """${Spacer(4)}serialization.deserialize(request.body(), Wirespec.getType(${it.emit()}.class, ${it.reference.isIterable}))""" }
    ).joinToString(",\n").let { if (it.isBlank()) "" else "\n$it\n${Spacer(3)}" }

    private fun Endpoint.Response.emitDeserializedParams() = listOfNotNull(
        headers.joinToString { """${Spacer(4)}java.util.Optional.ofNullable(response.headers().get("${it.identifier.value}")).map(it -> serialization.<${it.reference.emitType()}>deserializeParam(it, Wirespec.getType(${it.reference.emitRoot()}.class, ${it.reference.isIterable})))${if (!it.reference.isNullable) ".get()" else ""}""" }
            .orNull(),
        content?.let { """${Spacer(4)}serialization.deserialize(response.body(), Wirespec.getType(${it.emitRoot()}.class, ${it.reference.isIterable}))""" }
    ).joinToString(",\n").let { if (it.isBlank()) "" else "\n$it\n${Spacer(3)}" }

    private fun Endpoint.Response.emitSerialized() =
        """${Spacer(3)}if (response instanceof Response${status.firstToUpper()} r) { return new Wirespec.RawResponse(r.getStatus(), ${if (headers.isNotEmpty()) "java.util.Map.ofEntries(${headers.joinToString { it.emitSerializedHeader() }})" else "java.util.Collections.emptyMap()"}, ${
            if (content != null) "serialization.serialize(r.body, Wirespec.getType(${content.reference.emitRoot("Void")}.class, ${content.reference.isIterable}))"
            else "null"}); }"""

    private fun Endpoint.Response.emitDeserialized() =
        """${Spacer(4)}case $status -> new Response${status.firstToUpper()}(${this.emitDeserializedParams()});"""

    private fun Field.emitSerializedParams(fields: String) =
        """java.util.Map.entry("${identifier.value}", serialization.serializeParam(request.$fields.${emit(identifier)}, Wirespec.getType(${reference.emitRoot()}.class, ${reference.isIterable})))"""

    private fun IndexedValue<Endpoint.Segment.Param>.emitDeserialized() =
        """${Spacer(4)}serialization.<${value.reference.emit()}>deserialize(request.path().get(${index}), Wirespec.getType(${value.reference.emitRoot()}.class, ${value.reference.isIterable}))"""

    private fun Field.emitDeserializedParams(fields: String) =
        """${Spacer(4)}java.util.Optional.ofNullable(request.$fields().get("${identifier.value}")).map(it -> serialization.<${reference.emitType()}>deserializeParam(it, Wirespec.getType(${reference.emitRoot()}.class, ${reference.isIterable})))${if (!reference.isNullable) ".get()" else ""}"""

    private fun Field.emitSerializedHeader() =
        """java.util.Map.entry("${identifier.value}", serialization.serializeParam(r.getHeaders().${emit(identifier)}(), Wirespec.getType(${reference.emitRoot()}.class, ${reference.isIterable})))"""

    private fun Endpoint.Segment.Param.emitIdentifier() =
        "serialization.serialize(request.path.${emit(identifier).firstToLower()}, Wirespec.getType(${reference.emitRoot()}.class, ${reference.isIterable}))"

    private fun Endpoint.Content?.emitRoot() = this?.reference?.emitRoot() ?: "Void"
    private fun Endpoint.Content?.emit() = this?.reference?.emit() ?: "Void"

    private fun Endpoint.Segment.Param.emit() = "${reference.emit()} ${emit(identifier)}"

    private val Reference.isIterable get() = this is Reference.Iterable

    private fun String.fixStatus(): String = when (this) {
        "default" -> "200"
        else -> this
    }

    private fun String.sanitizeSymbol() = this
        .split(".", " ", "-")
        .mapIndexed { index, s -> if (index > 0) s.firstToUpper() else s }
        .joinToString("")
        .asSequence()
        .filter { it.isLetterOrDigit() || it in listOf('_') }
        .joinToString("")
        .sanitizeFirstIsDigit()

    private fun String.sanitizeFirstIsDigit() = if (firstOrNull()?.isDigit() == true) "_${this}" else this

    fun String.sanitizeEnum() = split("-", ", ", ".", " ", "//").joinToString("_").sanitizeFirstIsDigit()

    fun String.sanitizeKeywords() = if (this in reservedKeywords) "_$this" else this

    companion object : Keywords {
        override val reservedKeywords = setOf(
            "abstract", "continue", "for", "new", "switch",
            "assert", "default", "goto", "package", "synchronized",
            "boolean", "do", "if", "private", "this",
            "break", "double", "implements", "protected", "throw",
            "byte", "else", "import", "public", "throws",
            "case", "enum", "instanceof", "return", "transient",
            "catch", "extends", "int", "short", "try",
            "char", "final", "interface", "static", "void",
            "class", "finally", "long", "strictfp", "volatile",
            "const", "float", "native", "super", "while",
            "true", "false"
        )
    }
}
