package community.flock.wirespec.compiler.core.emit

import community.flock.wirespec.compiler.core.emit.common.DEFAULT_PACKAGE_NAME
import community.flock.wirespec.compiler.core.emit.common.Emitter
import community.flock.wirespec.compiler.core.emit.common.Emitter.Companion.firstToLower
import community.flock.wirespec.compiler.core.parse.AST
import community.flock.wirespec.compiler.core.parse.Endpoint
import community.flock.wirespec.compiler.core.parse.Enum
import community.flock.wirespec.compiler.core.parse.Refined
import community.flock.wirespec.compiler.core.parse.Type
import community.flock.wirespec.compiler.core.parse.Type.Shape.Field.Reference.Custom
import community.flock.wirespec.compiler.core.parse.Type.Shape.Field.Reference.Primitive
import community.flock.wirespec.compiler.utils.Logger
import community.flock.wirespec.compiler.utils.noLogger

class JavaEmitter(
    private val packageName: String = DEFAULT_PACKAGE_NAME,
    logger: Logger = noLogger
) : Emitter(logger, true) {

    private val shared = """
        |import java.lang.reflect.Type;
        |import java.lang.reflect.ParameterizedType;
        |
        |public interface WirespecShared {
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

    override fun emit(ast: AST): List<Pair<String, String>> = super.emit(ast)
        .map { (name, result) -> name to "$pkg\n\n$result" }
        .plus("WirespecShared" to "$pkg\n\n$shared")

    override fun Type.emit() = withLogging(logger) {
        """public record $name(
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

    override fun Type.Shape.Field.Identifier.emit() = withLogging(logger) { value }

    private fun Type.Shape.Field.Reference.emitPrimaryType() = withLogging(logger) {
        when (this) {
            is Custom -> value
            is Primitive -> when (type) {
                Primitive.Type.String -> "String"
                Primitive.Type.Integer -> "Integer"
                Primitive.Type.Boolean -> "Boolean"
            }
        }
    }

    override fun Type.Shape.Field.Reference.emit() = withLogging(logger) {
        emitPrimaryType()
            .let { if (isIterable) "java.util.List<$it>" else it }
    }

    override fun Enum.emit() = withLogging(logger) { "enum $name {\n${SPACER}${entries.joinToString(", ")};\n}\n" }

    override fun Refined.emit() = withLogging(logger) {
        """public record $name(String value) {
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
            |${SPACER}interface Request<T> extends WirespecShared.Request<T> {}
            |${requests.joinToString("\n"){ it.emit(this) }}
            |${SPACER}interface Response<T> extends WirespecShared.Response<T> {}
            |${responses.map{it.status.groupStatus()}.toSet().joinToString("\n") { "${SPACER}interface Response${it}<T> extends Response<T>{};" }}
            |${responses.filter { it.status.isInt() }.map{it.status}.joinToString("\n") { "${SPACER}interface Response${it}<T> extends Response${it.groupStatus()}<T>{};" }}
            |${responses.joinToString("\n"){ it.emit() }}
            |${SPACER}public Response ${name.firstToLower()}(Request request);
            |}
            |""".trimMargin()
    }

    private fun AST.hasEndpoints() = any { it is Endpoint }

    private fun Endpoint.Request.emit(endpoint: Endpoint) = """
        |${SPACER}class Request${content.emitContentType()} implements Request<${content?.reference?.emit() ?: "Void"}> {
        |${SPACER}${SPACER}private final String path;
        |${SPACER}${SPACER}private final WirespecShared.Method method;
        |${SPACER}${SPACER}private final java.util.Map<String, java.util.List<Object>> query;
        |${SPACER}${SPACER}private final java.util.Map<String, java.util.List<Object>> headers;
        |${SPACER}${SPACER}private final WirespecShared.Content<${content?.reference?.emit() ?: "Void"}> content;
        |${SPACER}${SPACER}public Request${content.emitContentType()}(${endpoint.emitRequestSignature(content)}) {
        |${SPACER}${SPACER}${SPACER}this.path = ${endpoint.path.emitPath()};
        |${SPACER}${SPACER}${SPACER}this.method = WirespecShared.Method.${endpoint.method.name};
        |${SPACER}${SPACER}${SPACER}this.query = ${endpoint.query.emitMap()};
        |${SPACER}${SPACER}${SPACER}this.headers = ${endpoint.headers.emitMap()};
        |${SPACER}${SPACER}${SPACER}this.content = ${content?.let { "new WirespecShared.Content(\"${it.type}\", body)" } ?: "null"};
        |${SPACER}${SPACER}}
        |${SPACER}${SPACER}@Override public String getPath() {return path;}
        |${SPACER}${SPACER}@Override public WirespecShared.Method getMethod() {return method;}
        |${SPACER}${SPACER}@Override public java.util.Map<String, java.util.List<Object>> getQuery() {return query;}
        |${SPACER}${SPACER}@Override public java.util.Map<String, java.util.List<Object>> getHeaders() {return headers;}
        |${SPACER}${SPACER}@Override public WirespecShared.Content<${content?.reference?.emit() ?: "Void"}> getContent() {return content;}
        |${SPACER}}
    """.trimMargin()

    private fun Endpoint.Response.emit() = """
        |${SPACER}class Response${status.firstToUpper()}${content.emitContentType()} implements Response${status.takeIf { it.isInt() }?.groupStatus().orEmptyString()}<${content?.reference?.emit() ?: "Void"}> {
        |${SPACER}${SPACER}private final int status;
        |${SPACER}${SPACER}private final java.util.Map<String, java.util.List<Object>> headers;
        |${SPACER}${SPACER}private final WirespecShared.Content<${content?.reference?.emit() ?: "Void"}> content;
        |${SPACER}${SPACER}public Response${status.firstToUpper()}${content.emitContentType()}(${status.takeIf { !it.isInt() }?.let { "int status, " }.orEmptyString()}java.util.Map<String, java.util.List<Object>> headers${content?.let { ", ${it.reference.emit()} body" } ?: ""}) {
        |${SPACER}${SPACER}${SPACER}this.status = ${status.takeIf { it.isInt() } ?: "status"};
        |${SPACER}${SPACER}${SPACER}this.headers = headers;
        |${SPACER}${SPACER}${SPACER}this.content = ${content?.let { "new WirespecShared.Content(\"${it.type}\", body)" } ?: "null"};
        |${SPACER}${SPACER}}
        |${SPACER}${SPACER}@Override public int getStatus() {return status;}
        |${SPACER}${SPACER}@Override public java.util.Map<String, java.util.List<Object>> getHeaders() {return headers;}
        |${SPACER}${SPACER}@Override public WirespecShared.Content<${content?.reference?.emit() ?: "Void"}> getContent() {return content;}
        |${SPACER}}
        """.trimMargin()

    private fun List<Endpoint.Response>.emitResponseMapper() = """
        |${SPACER}static <B> Response RESPONSE_MAPPER(WirespecShared.ContentMapper<B> contentMapper, int status, java.util.Map<String, java.util.List<Object>> headers, WirespecShared.Content<B> content) {
        |${joinToString (""){ it.emitResponseMapperCondition() }}
        |${SPACER}${SPACER}throw new IllegalStateException("Unknown response type");
        |${SPACER}}
    """.trimMargin()

    private fun Endpoint.Response.emitResponseMapperCondition() =
        when (content) {
            null -> """
                    |${SPACER}${SPACER}${SPACER}if(${status.takeIf { it.isInt() }?.let { "status == $status && " }.orEmptyString()}content == null) { return new Response${status.firstToUpper()}Void(${status.takeIf { !it.isInt() }?.let { "status, " }.orEmptyString()}headers); }
                    |
                """.trimMargin()

            else -> """
                    |${SPACER}${SPACER}${SPACER}if(${status.takeIf { it.isInt() }?.let { "status == $status && " }.orEmptyString()}content.type().equals("${content.type}")) {
                    |${SPACER}${SPACER}${SPACER}${SPACER}WirespecShared.Content<${content.reference.emit()}> c = contentMapper.read(content, WirespecShared.getType(${content.reference.emitPrimaryType()}.class, ${content.reference.isIterable}));
                    |${SPACER}${SPACER}${SPACER}${SPACER}return new Response${status.firstToUpper()}${content.emitContentType()}(${status.takeIf { !it.isInt() }?.let { "status, " }.orEmptyString()}headers, c.body());
                    |${SPACER}${SPACER}${SPACER}}
                    |
                """.trimMargin()
        }
    private fun Endpoint.Content?.emitContentType() = this
        ?.type
        ?.split("/")
        ?.joinToString("") { it.firstToUpper() }
        ?: "Void"

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

    private fun List<Type.Shape.Field>.emitMap() = joinToString(", ", "java.util.Map.of(", ")") { "\"${it.identifier.emit()}\", java.util.List.of(${it.identifier.emit()})" }

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
        if(isInt()) substring(0,1) + "XX"
        else firstToUpper()

}
