package community.flock.wirespec.compiler.core.emit

import community.flock.wirespec.compiler.core.emit.common.DEFAULT_PACKAGE_NAME
import community.flock.wirespec.compiler.core.emit.common.Emitter
import community.flock.wirespec.compiler.core.emit.common.Emitter.Companion.firstToLower
import community.flock.wirespec.compiler.core.parse.AST
import community.flock.wirespec.compiler.core.parse.Endpoint
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
        |
        |enum Method { GET, PUT, POST, DELETE, OPTIONS, HEAD, PATCH, TRACE };
        |record Content<T> (String type, T body) {};
        |interface Request<T> { String getPath(); Method getMethod(); java.util.Map<String, String> getQuery(); java.util.Map<String, java.util.List<String>> getHeaders(); Content<T> getContent(); }
        |interface Response<T> { int getStatus(); java.util.Map<String, java.util.List<String>> getHeaders(); Content<T> getContent(); }
        |interface ContentMapper<B> { <T> Content<T> read(Content<B> content, Type valueType); <T> Content<B> write(Content<T> content); }
        |
    """.trimMargin()

    override fun emit(ast: AST): List<Pair<String, String>> = super.emit(ast)
        .map { (name, result) -> name to if (packageName.isBlank()) "" else "package $packageName;\n\n$result" }
        .plus("WirespecShared" to if (packageName.isBlank()) "" else "package $packageName;\n\n$shared")

    override fun Type.emit() = withLogging(logger) {
        """public record $name(
            |${shape.emit()}
            |) {};
            |
            |""".trimMargin()
    }

    override fun Type.Shape.emit() = withLogging(logger) {
        value.joinToString(",\n") { it.emit() }
    }

    override fun Type.Shape.Field.emit() = withLogging(logger) {
        "$SPACER${if (isNullable) "java.util.Optional<${reference.emit()}>" else reference.emit()} ${identifier.emit()}"
    }

    override fun Type.Shape.Field.Identifier.emit() = withLogging(logger) { value }

    override fun Type.Shape.Field.Reference.emit() = withLogging(logger) {
        when (this) {
            is Custom -> value
            is Primitive -> when (type) {
                Primitive.Type.String -> "String"
                Primitive.Type.Integer -> "Integer"
                Primitive.Type.Boolean -> "Boolean"
            }
        }.let { if (isIterable) "java.util.List<$it>" else it }
    }

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
        """interface $name {
            |static String PATH = "${path.emitSegment()}";
            |${SPACER}interface ${name}Request<T> extends Request<T> {}
            |${requests.joinToString("\n"){ it.emit(this) }}
            |${SPACER}interface ${name}Response<T> extends Response<T> {}
            |${responses.map{it.status.groupStatus()}.toSet().joinToString("\n") { "${SPACER}interface ${name}Response${it}<T> extends ${name}Response<T>{};" }}
            |${responses.filter { it.status.isInt() }.map{it.status}.joinToString("\n") { "${SPACER}interface ${name}Response${it}<T> extends ${name}Response${it.groupStatus()}<T>{};" }}
            |${responses.joinToString("\n"){ it.emit(this) }}
            |${SPACER}public ${name}Response ${name.firstToLower()}(${name}Request request);
            |}
            |""".trimMargin()
    }

    private fun AST.hasEndpoints() = any { it is Endpoint }

    private fun Endpoint.Request.emit(endpoint: Endpoint) = """
        |${SPACER}class ${endpoint.name}Request${content.emitContentType()} implements ${endpoint.name}Request<${content?.reference?.emit() ?: "Void"}> {
        |${SPACER}${SPACER}private final String path;
        |${SPACER}${SPACER}private final Method method;
        |${SPACER}${SPACER}private final java.util.Map<String, String> query;
        |${SPACER}${SPACER}private final java.util.Map<String, java.util.List<String>> headers;
        |${SPACER}${SPACER}private final Content<${content?.reference?.emit() ?: "Void"}> content;
        |${SPACER}${SPACER}public ${endpoint.name}Request${content.emitContentType()}(${endpoint.emitRequestSignature(content)}) {
        |${SPACER}${SPACER}${SPACER}this.path = ${endpoint.path.emitPath()};
        |${SPACER}${SPACER}${SPACER}this.method = Method.${endpoint.method.name};
        |${SPACER}${SPACER}${SPACER}this.query = ${endpoint.query.emitMap()};
        |${SPACER}${SPACER}${SPACER}this.headers = ${endpoint.headers.emitMap()};
        |${SPACER}${SPACER}${SPACER}this.content = ${content?.let { "new Content(\"${it.type}\", body)" } ?: "null"};
        |${SPACER}${SPACER}}
        |${SPACER}${SPACER}@Override public String getPath() {return path;}
        |${SPACER}${SPACER}@Override public Method getMethod() {return method;}
        |${SPACER}${SPACER}@Override public java.util.Map<String, String> getQuery() {return query;}
        |${SPACER}${SPACER}@Override public java.util.Map<String, java.util.List<String>> getHeaders() {return headers;}
        |${SPACER}${SPACER}@Override public Content<${content?.reference?.emit() ?: "Void"}> getContent() {return content;}
        |${SPACER}}
    """.trimMargin()

    private fun Endpoint.Response.emit(endpoint: Endpoint) = """
        |${SPACER}class ${endpoint.name}Response${status.firstToUpper()}${content.emitContentType()} implements ${endpoint.name}Response${status.takeIf { it.isInt() }?.groupStatus().orEmptyString()}<${content?.reference?.emit() ?: "Void"}> {
        |${SPACER}${SPACER}private final int status;
        |${SPACER}${SPACER}private final java.util.Map<String, java.util.List<String>> headers;
        |${SPACER}${SPACER}private final Content<${content?.reference?.emit() ?: "Void"}> content;
        |${SPACER}${SPACER}public ${endpoint.name}Response${status.firstToUpper()}${content.emitContentType()}(java.util.Map<String, java.util.List<String>> headers${status.takeIf { !it.isInt() }?.let { ", int status" }.orEmptyString()}${content?.let { ", ${it.reference.emit()} body" } ?: ""}) {
        |${SPACER}${SPACER}${SPACER}this.status = ${status.takeIf { it.isInt() } ?: "status"};
        |${SPACER}${SPACER}${SPACER}this.headers = headers;
        |${SPACER}${SPACER}${SPACER}this.content = ${content?.let { "new Content(\"${it.type}\", body)" } ?: "null"};
        |${SPACER}${SPACER}}
        |${SPACER}${SPACER}@Override public int getStatus() {return status;}
        |${SPACER}${SPACER}@Override public java.util.Map<String, java.util.List<String>> getHeaders() {return headers;}
        |${SPACER}${SPACER}@Override public Content<${content?.reference?.emit() ?: "Void"}> getContent() {return content;}
        |${SPACER}}
        """.trimMargin()

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

    private fun List<Type.Shape.Field>.emitMap() = joinToString(", ", "java.util.Map.of(", ")") { "\"${it.identifier.emit()}\", ${it.identifier.emit()}.toString()" }

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
