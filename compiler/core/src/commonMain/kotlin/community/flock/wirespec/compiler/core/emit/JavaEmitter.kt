package community.flock.wirespec.compiler.core.emit

import community.flock.wirespec.compiler.core.emit.common.DEFAULT_PACKAGE_NAME
import community.flock.wirespec.compiler.core.emit.common.Emitter
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

    private val imports = """
        |import java.util.List;
        |import java.util.Map;
    """.trimMargin()

    private val shared = """
        |${imports}
        |
        |import java.lang.reflect.Type;
        |
        |enum Method { GET, PUT, POST, DELETE, OPTIONS, HEAD, PATCH, TRACE };
        |record Content<T> (String type, T body) {};
        |interface Request<T> { String getPath(); Method getMethod(); Map<String, String> getQuery(); Map<String, List<String>> getHeaders(); Content<T> getContent(); }
        |interface Response<T> { int getStatus(); Map<String, List<String>> getHeaders(); Content<T> getContent(); }
        |interface ContentMapper<B> { <T> Content<T> read(Content<B> content, Type valueType); <T> Content<B> write(Content<T> content); }
        |
    """.trimMargin()

    override fun emit(ast: AST): List<Pair<String, String>> = super.emit(ast)
        .map { (name, result) -> name to if (packageName.isBlank()) "" else "package $packageName;\n\n$imports\n\n$result" }
        .plus("WirespecShared" to if (packageName.isBlank()) "" else "package $packageName;\n\n$shared")

    override fun Type.emit() = withLogging(logger) {
        """public record $name(
            |${shape.emit()}
            |) {};
            |
            |""".trimMargin()
    }

    override fun Type.Shape.emit() = withLogging(logger) {
        value.joinToString(",\n") { it.emit() }.dropLast(1)
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
            |${SPACER}abstract interface ${name}Request<T> extends Request<T> {}
            |${requests.joinToString("\n"){ it.emit(this) }}
            |}
            |""".trimMargin()
    }

    private fun AST.hasEndpoints() = any { it is Endpoint }

    private fun Endpoint.Request.emit(endpoint: Endpoint) = """
        |${SPACER}class ${endpoint.name}Request${content.emitContentType()} implements ${endpoint.name}Request<${content?.reference?.emit() ?: "Void"}> {
        |${SPACER}${SPACER}private final String path;
        |${SPACER}${SPACER}private final Method method;
        |${SPACER}${SPACER}private final Map<String, String> query;
        |${SPACER}${SPACER}private final Map<String, List<String>> headers;
        |${SPACER}${SPACER}private final Content<${content?.reference?.emit() ?: "Void"}> content;
        |${SPACER}${SPACER}public ${endpoint.name}Request${content.emitContentType()}(${endpoint.emitRequestSignature(content)}) {
        |${SPACER}${SPACER}${SPACER}path = ${endpoint.path.emitPath()};
        |${SPACER}${SPACER}${SPACER}method = Method.${endpoint.method.name};
        |${SPACER}${SPACER}${SPACER}query = ${endpoint.query.emitMap()};
        |${SPACER}${SPACER}${SPACER}headers = ${endpoint.headers.emitMap()};
        |${SPACER}${SPACER}${SPACER}content = ${content?.let { "new Content(\"${it.type}\", body)" } ?: "null"};
        |${SPACER}${SPACER}}
        |${SPACER}${SPACER}@Override public String getPath() {return path;}
        |${SPACER}${SPACER}@Override public Method getMethod() {return method;}
        |${SPACER}${SPACER}@Override public Map<String, String> getQuery() {return query;}
        |${SPACER}${SPACER}@Override public Map<String, List<String>> getHeaders() {return headers;}
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

    private fun List<Type.Shape.Field>.emitMap() = joinToString(", ", "Map.of(", ")") { "\"${it.identifier.emit()}\", ${it.identifier.emit()}.toString()" }

    private fun List<Endpoint.Segment>.emitPath() = "\"/\" + " + joinToString(" + \"/\" + ") { it.emit() }


}
