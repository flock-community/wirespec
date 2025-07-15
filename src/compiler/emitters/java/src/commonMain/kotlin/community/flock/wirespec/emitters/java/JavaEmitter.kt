package community.flock.wirespec.emitters.java

import arrow.core.NonEmptyList
import community.flock.wirespec.compiler.core.concatGenerics
import community.flock.wirespec.compiler.core.emit.DEFAULT_GENERATED_PACKAGE_STRING
import community.flock.wirespec.compiler.core.emit.DEFAULT_SHARED_PACKAGE_STRING
import community.flock.wirespec.compiler.core.emit.EmitShared
import community.flock.wirespec.compiler.core.emit.Emitted
import community.flock.wirespec.compiler.core.emit.Emitter
import community.flock.wirespec.compiler.core.emit.FileExtension
import community.flock.wirespec.compiler.core.emit.Keywords
import community.flock.wirespec.compiler.core.emit.PackageName
import community.flock.wirespec.compiler.core.emit.Spacer
import community.flock.wirespec.compiler.core.emit.plus
import community.flock.wirespec.compiler.core.orNull
import community.flock.wirespec.compiler.core.parse.Channel
import community.flock.wirespec.compiler.core.parse.Definition
import community.flock.wirespec.compiler.core.parse.DefinitionIdentifier
import community.flock.wirespec.compiler.core.parse.Endpoint
import community.flock.wirespec.compiler.core.parse.Enum
import community.flock.wirespec.compiler.core.parse.Field
import community.flock.wirespec.compiler.core.parse.FieldIdentifier
import community.flock.wirespec.compiler.core.parse.Identifier
import community.flock.wirespec.compiler.core.parse.Module
import community.flock.wirespec.compiler.core.parse.Reference
import community.flock.wirespec.compiler.core.parse.Refined
import community.flock.wirespec.compiler.core.parse.Type
import community.flock.wirespec.compiler.core.parse.Union
import community.flock.wirespec.compiler.utils.Logger

open class JavaEmitter(
    private val packageName: PackageName = PackageName(DEFAULT_GENERATED_PACKAGE_STRING),
    private val emitShared: EmitShared = EmitShared()
) : Emitter() {

    val import = """
        |
        |import $DEFAULT_SHARED_PACKAGE_STRING.java.Wirespec;
        |
    """.trimMargin()

    override val extension = FileExtension.Java

    override val shared = JavaShared

    override val singleLineComment = "//"

    override fun emit(module: Module, logger: Logger): NonEmptyList<Emitted> =
        super.emit(module, logger).let {
            if (emitShared.value) it + Emitted(PackageName("${DEFAULT_SHARED_PACKAGE_STRING}.java").toDir() + "Wirespec", shared.source)
            else it
        }

    override fun emit(definition: Definition, module: Module, logger: Logger): Emitted =
        super.emit(definition, module, logger).let {
            val subPackageName = packageName + definition
            Emitted(
                file = subPackageName.toDir() + it.file.sanitizeSymbol(),
                result = """
                    |package $subPackageName;
                    |${if (module.needImports()) import else ""}
                    |${it.result}
                """.trimMargin().trimStart()
            )
        }

    override fun emit(type: Type, module: Module) = """
        |public record ${emit(type.identifier)} (
        |${type.shape.emit()}
        |)${type.extends.run { if (isEmpty()) "" else " extends ${joinToString(", ") { it.emit() }}" }}${type.emitUnion(module)} {
        |};
        |
    """.trimMargin()

    fun Type.emitUnion(module: Module) = module.statements
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

    fun Reference?.emitRoot(void: String = "void"): String = when (this) {
        is Reference.Dict -> reference.emitRoot()
        is Reference.Iterable -> reference.emitRoot()
        is Reference.Unit -> void
        is Reference.Any -> emitType()
        is Reference.Custom -> emitType()
        is Reference.Primitive -> emitType()
        null -> void
    }

    private fun Reference.Primitive.emit() = when (val t = type) {
        is Reference.Primitive.Type.String -> "String"
        is Reference.Primitive.Type.Integer -> when (t.precision) {
            Reference.Primitive.Type.Precision.P32 -> "Integer"
            Reference.Primitive.Type.Precision.P64 -> "Long"
        }

        is Reference.Primitive.Type.Number -> when (t.precision) {
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
        |${Spacer}public static boolean validate(${emit(refined.identifier)} record) {
        |${Spacer}${Spacer}return ${refined.emitValidator()}
        |${Spacer}}
        |${Spacer}@Override
        |${Spacer}public String getValue() { return value; }
        |}
        |
    """.trimMargin()

    override fun Refined.emitValidator():String {
        val defaultReturn = "true;"
        return when (val type = reference.type) {
            is Reference.Primitive.Type.Integer -> type.constraint?.emit() ?: defaultReturn
            is Reference.Primitive.Type.Number -> type.constraint?.emit() ?: defaultReturn
            is Reference.Primitive.Type.String -> type.constraint?.emit() ?: defaultReturn
            Reference.Primitive.Type.Boolean -> defaultReturn
            Reference.Primitive.Type.Bytes -> defaultReturn
        }
    }

    override fun Reference.Primitive.Type.Constraint.emit() = when(this){
        is Reference.Primitive.Type.Constraint.RegExp -> """java.util.regex.Pattern.compile("${expression.replace("\\", "\\\\")}").matcher(record.value).find();"""
        is Reference.Primitive.Type.Constraint.Bound -> """$min < record.value && record.value < $max;"""
    }

    override fun emit(enum: Enum, module: Module) = """
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
        |public sealed interface ${emit(union.identifier)} permits ${union.entries.joinToString { it.value }} {}
        |
    """.trimMargin()

    override fun emit(channel: Channel) = """
        |${channel.emitImports()}
        |
        |public interface ${emit(channel.identifier)} {
        |   void invoke(${channel.emitFullyQualified(channel.reference)}${channel.reference.emitRoot()} message);
        |}
        |
    """.trimMargin()

    private fun Definition.emitFullyQualified(reference: Reference) =
        if (identifier.value == reference.value) {
            "${packageName.value}.model."
        } else {
            ""
        }

    private fun Definition.emitImports() = importReferences()
        .filter { identifier.value != it.value }
        .map { "import ${packageName.value}.model.${it.value};" }.joinToString("\n") { it.trimStart() }


    override fun emit(endpoint: Endpoint) = """
        |${endpoint.emitImports()}
        |
        |public interface ${emit(endpoint.identifier)} extends Wirespec.Endpoint {
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
        |${endpoint.responses.distinctByStatus().joinToString("\n") { it.emit() }}
        |
        |${Spacer}interface Handler extends Wirespec.Handler {
        |
        |${endpoint.requests.first().emitRequestFunctions(endpoint)}
        |
        |${Spacer(2)}static Wirespec.RawResponse toResponse(Wirespec.Serializer<String> serialization, Response<?> response) {
        |${endpoint.responses.distinctByStatus().joinToString("\n") { it.emitSerialized() }}
        |${Spacer(3)}else { throw new IllegalStateException("Cannot match response with status: " + response.getStatus());}
        |${Spacer(2)}}
        |
        |${Spacer(2)}static Response<?> fromResponse(Wirespec.Deserializer<String> serialization, Wirespec.RawResponse response) {
        |${Spacer(3)}switch (response.statusCode()) {
        |${endpoint.responses.distinctByStatus().filter { it.status.isStatusCode() }.joinToString("\n") { it.emitDeserialized() }}
        |${Spacer(4)}default: throw new IllegalStateException("Cannot match response with status: " + response.statusCode());
        |${Spacer(3)}}
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
        .distinctByStatus()
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
        |${Spacer(4)}${if (endpoint.queries.isNotEmpty()) "java.util.Map.ofEntries(${endpoint.queries.joinToString { it.emitSerializedParams("queries") }})" else EMPTY_MAP},
        |${Spacer(4)}${if (endpoint.headers.isNotEmpty()) "java.util.Map.ofEntries(${endpoint.headers.joinToString { it.emitSerializedParams("headers") }})" else EMPTY_MAP},
        |${Spacer(4)}serialization.serialize(request.getBody(), ${content?.reference?.emitGetType()})
        |${Spacer(3)});
        |${Spacer(2)}}
        |
        |${Spacer(2)}static Request fromRequest(Wirespec.Deserializer<String> serialization, Wirespec.RawRequest request) {
        |${Spacer(3)}return new Request(${emitDeserializedParams(endpoint)});
        |${Spacer(2)}}
    """.trimMargin()

    private fun Reference?.emitGetType() = "Wirespec.getType(${emitRoot("Void")}.class, ${emitGetTypeRaw()})"
    private fun Reference?.emitGetTypeRaw() = when {
        this?.isNullable?:false -> "java.util.Optional.class"
        this is Reference.Iterable -> "java.util.List.class"
        else -> null
    }

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
        endpoint.queries.joinToString(",\n"){ it.emitDeserializedParams("queries") }.orNull(),
        endpoint.headers.joinToString(",\n"){ it.emitDeserializedParams("headers") }.orNull(),
        content?.let { """${Spacer(4)}serialization.deserialize(request.body(), ${it.reference.emitGetType()})""" }
    ).joinToString(",\n").let { if (it.isBlank()) "" else "\n$it\n${Spacer(3)}" }

    private fun Endpoint.Response.emitDeserializedParams() = listOfNotNull(
        headers.joinToString(",\n") { """${Spacer(4)}serialization.deserializeParam(response.headers().getOrDefault("${it.identifier.value}", java.util.Collections.emptyList()), ${it.reference.emitGetType()})""" }.orNull(),
        content?.let { """${Spacer(4)}serialization.deserialize(response.body(), ${it.reference.emitGetType()})""" }
    ).joinToString(",\n").let { if (it.isBlank()) "" else "\n$it\n${Spacer(3)}" }

    private fun Endpoint.Response.emitSerialized() =
        """${Spacer(3)}if (response instanceof Response${status.firstToUpper()} r) { return new Wirespec.RawResponse(r.getStatus(), ${if (headers.isNotEmpty()) "java.util.Map.ofEntries(${headers.joinToString { it.emitSerializedHeader() }})" else EMPTY_MAP}, ${
            if (content != null) "serialization.serialize(r.body, ${content!!.reference.emitGetType()})"
            else "null"}); }"""

    private fun Endpoint.Response.emitDeserialized() =
        """${Spacer(4)}case $status: return new Response${status.firstToUpper()}(${this.emitDeserializedParams()});"""

    private fun Field.emitSerializedParams(fields: String) =
        """java.util.Map.entry("${identifier.value}", serialization.serializeParam(request.$fields.${emit(identifier)}, ${reference.emitGetType()}))"""

    private fun IndexedValue<Endpoint.Segment.Param>.emitDeserialized() =
        """${Spacer(4)}serialization.deserialize(request.path().get(${index}), ${value.reference.emitGetType()})"""

    private fun Field.emitDeserializedParams(fields: String) =
        """${Spacer(4)}serialization.deserializeParam(request.$fields().getOrDefault("${identifier.value}", java.util.Collections.emptyList()), ${reference.emitGetType()})"""

    private fun Field.emitSerializedHeader() =
        """java.util.Map.entry("${identifier.value}", serialization.serializeParam(r.getHeaders().${emit(identifier)}(), ${reference.emitGetType()}))"""

    private fun Endpoint.Segment.Param.emitIdentifier() =
        "serialization.serialize(request.path.${emit(identifier).firstToLower()}, ${reference.emitGetType()})"

    private fun Endpoint.Content?.emit() = this?.reference?.emit() ?: "Void"

    private fun Endpoint.Segment.Param.emit() = "${reference.emit()} ${emit(identifier)}"

    private val Reference.isIterable get() = this is Reference.Iterable

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

        private const val EMPTY_MAP = "java.util.Collections.emptyMap()"
    }
}
