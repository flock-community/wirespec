package community.flock.wirespec.compiler.core.emit

import community.flock.wirespec.compiler.core.emit.common.DEFAULT_PACKAGE_NAME
import community.flock.wirespec.compiler.core.emit.common.AbstractEmitter
import community.flock.wirespec.compiler.core.parse.AST
import community.flock.wirespec.compiler.core.parse.nodes.Endpoint
import community.flock.wirespec.compiler.core.parse.nodes.Enum
import community.flock.wirespec.compiler.core.parse.nodes.Refined
import community.flock.wirespec.compiler.core.parse.nodes.Type
import community.flock.wirespec.compiler.core.parse.nodes.Type.Shape.Field.Reference
import community.flock.wirespec.compiler.utils.Logger
import community.flock.wirespec.compiler.utils.noLogger

class JavaEmitter(
    packageName: String = DEFAULT_PACKAGE_NAME,
    logger: Logger = noLogger
) : AbstractEmitter(logger, true) {

    override val shared = """
        |package community.flock.wirespec;
        |
        |import java.lang.reflect.Type;
        |import java.lang.reflect.ParameterizedType;
        |
        |public interface Wirespec {
        |${SPACER}enum Method { GET, PUT, POST, DELETE, OPTIONS, HEAD, PATCH, TRACE };
        |${SPACER}record Content<T> (String type, T body) {};
        |${SPACER}interface Request<T> { String getPath(); Method getMethod(); java.util.Map<String, java.util.List<Object>> getQuery(); java.util.Map<String, java.util.List<Object>> getHeaders(); Content<T> getContent(); }
        |${SPACER}interface Response<T> { int getStatus(); java.util.Map<String, java.util.List<Object>> getHeaders(); Content<T> getContent(); }
        |${SPACER}interface ContentMapper<B> { <T> Content<T> read(Content<B> content, Type valueType); <T> Content<B> write(Content<T> content); }
        |${SPACER}static Type getType(final Class<?> type, final boolean isIterable) {
        |${SPACER}${SPACER}if(isIterable) {
        |${SPACER}${SPACER}${SPACER}return new ParameterizedType() {
        |${SPACER}${SPACER}${SPACER}${SPACER}public Type getRawType() {return java.util.List.class;}
        |${SPACER}${SPACER}${SPACER}${SPACER}public Type[] getActualTypeArguments() {Class<?>[] types = {type};return types;}
        |${SPACER}${SPACER}${SPACER}${SPACER}public Type getOwnerType() {return null;}
        |${SPACER}${SPACER}${SPACER}};
        |${SPACER}${SPACER}}
        |${SPACER}${SPACER}else {
        |${SPACER}${SPACER}${SPACER}return type;
        |${SPACER}${SPACER}}
        |${SPACER}}
        |}
    """.trimMargin()

    private val pkg = if (packageName.isBlank()) "" else "package $packageName;"
    private fun import(ast: AST) =
        if (!ast.hasEndpoints()) "" else "import community.flock.wirespec.Wirespec;\nimport java.util.concurrent.CompletableFuture;\nimport java.util.function.Function;\n\n"

    override fun emit(ast: AST): List<Pair<String, String>> = super.emit(ast)
        .map { (name, result) -> name.sanitizeSymbol() to "$pkg\n\n${import(ast)}$result" }

    override fun Type.emit() = withLogging(logger) {
        """public record ${name.sanitizeSymbol()}(
            |${shape.emit()}
            |) {};
            |""".trimMargin()
    }

    override fun Type.Shape.emit() = withLogging(logger) {
        value.joinToString(",\n") { it.emit() }
    }

    override fun Type.Shape.Field.emit() = withLogging(logger) {
        "$SPACER${if (isNullable) "java.util.Optional<${reference.emit()}>" else reference.emit()} ${identifier.emit()}"
    }

    override fun Type.Shape.Field.Identifier.emit() = withLogging(logger) {
        value
            .split("-")
            .mapIndexed { index, s -> if (index > 0) s.firstToUpper() else s }
            .joinToString("")
            .sanitizeKeywords()
            .sanitizeSymbol()
    }

    private fun Reference.emitSymbol() = withLogging(logger) {
        when (this) {
            is Reference.Unit -> "Void"
            is Reference.Any -> "Object"
            is Reference.Custom -> value
            is Reference.Primitive -> when (type) {
                Reference.Primitive.Type.String -> "String"
                Reference.Primitive.Type.Integer -> "Integer"
                Reference.Primitive.Type.Number -> "Double"
                Reference.Primitive.Type.Boolean -> "Boolean"
            }
        }.sanitizeSymbol()
    }

    override fun Reference.emit() = withLogging(logger) {
        emitSymbol()
            .let { if (isIterable) "java.util.List<$it>" else it }
            .let { if (isMap) "java.util.Map<String, $it>" else it }
    }

    override fun Enum.emit() = withLogging(logger) {
        fun String.sanitizeEnum() = this
            .replace("/", "_")
            .replace(" ", "_")
            .replace("-", "_")
            .replace("–", "_")
            .let { if (it.first().isDigit()) "_$it" else it }

        val body = """
          |${SPACER}public final String label;
          |${SPACER}${name.sanitizeSymbol()}(String label) {
          |${SPACER}${SPACER}this.label = label;
          |${SPACER}}
          """.trimMargin()
        val toString = """
          |${SPACER}@Override
          |${SPACER}public String toString() {
          |${SPACER}${SPACER}return label;
          |${SPACER}}
          """.trimMargin()
        "public enum ${name.sanitizeSymbol()} {\n${SPACER}${entries.joinToString(",\n${SPACER}") { enum -> "${enum.sanitizeEnum()}(\"${enum}\")" }};\n${body}\n${toString}\n}\n"
    }

    override fun Refined.emit() = withLogging(logger) {
        """public record ${name.sanitizeSymbol()}(String value) {
            |${SPACER}static void validate($name record) {
            |${SPACER}${validator.emit()}
            |${SPACER}}
            |}
            |""".trimMargin()
    }

    override fun Refined.Validator.emit() = withLogging(logger) {
        "${SPACER}java.util.regex.Pattern.compile($value).matcher(record.value).find();"
    }

    override fun Endpoint.emit() = withLogging(logger) {
        """public interface $name {
            |${SPACER}static String PATH = "${path.emitSegment()}";
            |${responses.emitResponseMapper()}
            |${SPACER}sealed interface Request<T> extends Wirespec.Request<T> {}
            |${requests.joinToString("\n") { it.emit(this) }}
            |${SPACER}sealed interface Response<T> extends Wirespec.Response<T> {}
            |${responses.map { it.status.groupStatus() }.toSet().joinToString("\n") { "${SPACER}sealed interface Response${it}<T> extends Response<T>{};" }}
            |${responses.filter { it.status.isInt() }.map { it.status }.toSet().joinToString("\n") { "${SPACER}sealed interface Response${it}<T> extends Response${it.groupStatus()}<T>{};" }}
            |${responses.distinctBy { it.status to it.content?.type }.joinToString("\n") { it.emit() }}
            |${SPACER}public CompletableFuture<Response<?>> ${name.firstToLower()}(Request<?> request);
            |}
            |""".trimMargin()
    }

    private fun Endpoint.Request.emit(endpoint: Endpoint) = """
        |${SPACER}final class Request${content?.emitContentType() ?: "Void"} implements Request<${content?.reference?.emit() ?: "Void"}> {
        |${SPACER}${SPACER}private final String path;
        |${SPACER}${SPACER}private final Wirespec.Method method;
        |${SPACER}${SPACER}private final java.util.Map<String, java.util.List<Object>> query;
        |${SPACER}${SPACER}private final java.util.Map<String, java.util.List<Object>> headers;
        |${SPACER}${SPACER}private final Wirespec.Content<${content?.reference?.emit() ?: "Void"}> content;
        |${SPACER}${SPACER}public Request${content?.emitContentType() ?: "Void"}(${endpoint.emitRequestSignature(content)}) {
        |${SPACER}${SPACER}${SPACER}this.path = ${endpoint.path.emitPath()};
        |${SPACER}${SPACER}${SPACER}this.method = Wirespec.Method.${endpoint.method.name};
        |${SPACER}${SPACER}${SPACER}this.query = ${endpoint.query.emitMap()};
        |${SPACER}${SPACER}${SPACER}this.headers = ${endpoint.headers.emitMap()};
        |${SPACER}${SPACER}${SPACER}this.content = ${content?.let { "new Wirespec.Content(\"${it.type}\", body)" } ?: "null"};
        |${SPACER}${SPACER}}
        |${SPACER}${SPACER}@Override public String getPath() {return path;}
        |${SPACER}${SPACER}@Override public Wirespec.Method getMethod() {return method;}
        |${SPACER}${SPACER}@Override public java.util.Map<String, java.util.List<Object>> getQuery() {return query;}
        |${SPACER}${SPACER}@Override public java.util.Map<String, java.util.List<Object>> getHeaders() {return headers;}
        |${SPACER}${SPACER}@Override public Wirespec.Content<${content?.reference?.emit() ?: "Void"}> getContent() {return content;}
        |${SPACER}}
    """.trimMargin()

    private fun Endpoint.Response.emit() = """
        |${SPACER}final class Response${status.firstToUpper()}${content?.emitContentType()?:"Void"} implements Response${
        status.firstToUpper().orEmptyString()
    }<${content?.reference?.emit() ?: "Void"}> {
        |${SPACER}${SPACER}private final int status;
        |${SPACER}${SPACER}private final java.util.Map<String, java.util.List<Object>> headers;
        |${SPACER}${SPACER}private final Wirespec.Content<${content?.reference?.emit() ?: "Void"}> content;
        |${SPACER}${SPACER}public Response${status.firstToUpper()}${content?.emitContentType() ?: "Void"}(${
        status.takeIf { !it.isInt() }?.let { "int status, " }.orEmptyString()
    }java.util.Map<String, java.util.List<Object>> headers${content?.let { ", ${it.reference.emit()} body" } ?: ""}) {
        |${SPACER}${SPACER}${SPACER}this.status = ${status.takeIf { it.isInt() } ?: "status"};
        |${SPACER}${SPACER}${SPACER}this.headers = headers;
        |${SPACER}${SPACER}${SPACER}this.content = ${content?.let { "new Wirespec.Content(\"${it.type}\", body)" } ?: "null"};
        |${SPACER}${SPACER}}
        |${SPACER}${SPACER}@Override public int getStatus() {return status;}
        |${SPACER}${SPACER}@Override public java.util.Map<String, java.util.List<Object>> getHeaders() {return headers;}
        |${SPACER}${SPACER}@Override public Wirespec.Content<${content?.reference?.emit() ?: "Void"}> getContent() {return content;}
        |${SPACER}}
        """.trimMargin()

    private fun List<Endpoint.Response>.emitResponseMapper() = """
        |${SPACER}static <B, Res extends Response<?>> Function<Wirespec.Response<B>, Res> RESPONSE_MAPPER(Wirespec.ContentMapper<B> contentMapper) {
        |return response -> {
        |${distinctBy { it.status to it.content?.type }.joinToString("") { it.emitResponseMapperCondition() }}
        |${SPACER}${SPACER}throw new IllegalStateException("Unknown response type");
        |${SPACER}};
        |}
    """.trimMargin()

    private fun Endpoint.Response.emitResponseMapperCondition() =
        when (content) {
            null -> """
                    |${SPACER}${SPACER}${SPACER}if(${status.takeIf { it.isInt() }?.let { "response.getStatus() == $status && " }.orEmptyString()}response.getContent() == null) { return (Res) new Response${status.firstToUpper()}Void(${status.takeIf { !it.isInt() }?.let { "response.getStatus(), " }.orEmptyString()}response.getHeaders()); }
                    |
                """.trimMargin()

            else -> """
                    |${SPACER}${SPACER}${SPACER}if(${status.takeIf { it.isInt() }?.let { "response.getStatus() == $status && " }.orEmptyString()}response.getContent().type().equals("${content.type}")) {
                    |${SPACER}${SPACER}${SPACER}${SPACER}Wirespec.Content<${content.reference.emit()}> content = contentMapper.read(response.getContent(), Wirespec.getType(${content.reference.emitSymbol()}.class, ${content.reference.isIterable}));
                    |${SPACER}${SPACER}${SPACER}${SPACER}return (Res) new Response${status.firstToUpper()}${content.emitContentType()}(${status.takeIf { !it.isInt() }?.let { "response.getStatus(), " }.orEmptyString()}response.getHeaders(), content.body());
                    |${SPACER}${SPACER}${SPACER}}
                    |
                """.trimMargin()
        }

    private fun Endpoint.Segment.emit(): String = withLogging(logger) {
        when (this) {
            is Endpoint.Segment.Literal -> "\"$value\""
            is Endpoint.Segment.Param -> identifier.value
        }
    }

    private fun Endpoint.emitRequestSignature(content: Endpoint.Content? = null): String {
        val pathField = path
            .filterIsInstance<Endpoint.Segment.Param>()
            .map { Type.Shape.Field(it.identifier, it.reference, false) }
        val parameters = pathField + query + headers + cookies
        return parameters
            .plus(content?.reference?.toField("body", false))
            .filterNotNull()
            .joinToString(", ") { it.emit() }
    }

    private fun List<Type.Shape.Field>.emitMap() = joinToString(", ", "java.util.Map.ofEntries(", ")") { "java.util.Map.entry(\"${it.identifier.value}\", java.util.List.of(${it.identifier.emit()}))" }

    private fun List<Endpoint.Segment>.emitSegment() = "/" + joinToString("/") {
        when (it) {
            is Endpoint.Segment.Param -> "{${it.identifier.value}}"
            is Endpoint.Segment.Literal -> it.value
        }
    }

    private fun List<Endpoint.Segment>.emitPath() = "\"/\" + " + joinToString(" + \"/\" + ") { it.emit() }

    private fun String?.orEmptyString() = this ?: ""

    private fun String.isInt() = toIntOrNull() != null

    private fun String.groupStatus() =
        if (isInt()) substring(0, 1) + "XX"
        else firstToUpper()

    fun String.sanitizeKeywords() = if (reservedKeywords.contains(this)) "_$this" else this

    fun String.sanitizeSymbol() = replace(".", "").replace(" ", "_")
    companion object {
        private val reservedKeywords = listOf(
            "abstract", "continue", "for", "new", "switch",
            "assert", "default", "goto", "package", "synchronized",
            "boolean", "do", "if", "private", "this",
            "break", "double", "implements", "protected", "throw",
            "byte", "else", "import", "public", "throws",
            "case", "enum", "instanceof", "return", "transient",
            "catch", "extends", "int", "short", "try",
            "char", "final", "interface", "static", "void",
            "class", "finally", "long", "strictfp", "volatile",
            "const", "float", "native", "super", "while"
        )
    }
}
