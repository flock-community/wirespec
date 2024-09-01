package community.flock.wirespec.compiler.core.emit

import community.flock.wirespec.compiler.core.emit.common.ClassModelEmitter
import community.flock.wirespec.compiler.core.emit.common.DEFAULT_PACKAGE_STRING
import community.flock.wirespec.compiler.core.emit.common.Emitted
import community.flock.wirespec.compiler.core.emit.common.Emitter
import community.flock.wirespec.compiler.core.emit.common.Spacer
import community.flock.wirespec.compiler.core.emit.transformer.ClassModelTransformer.transform
import community.flock.wirespec.compiler.core.emit.transformer.ClassReference
import community.flock.wirespec.compiler.core.emit.transformer.EndpointClass
import community.flock.wirespec.compiler.core.emit.transformer.EnumClass
import community.flock.wirespec.compiler.core.emit.transformer.FieldClass
import community.flock.wirespec.compiler.core.emit.transformer.Parameter
import community.flock.wirespec.compiler.core.emit.transformer.RefinedClass
import community.flock.wirespec.compiler.core.emit.transformer.TypeClass
import community.flock.wirespec.compiler.core.emit.transformer.UnionClass
import community.flock.wirespec.compiler.core.parse.AST
import community.flock.wirespec.compiler.core.parse.Channel
import community.flock.wirespec.compiler.core.parse.Definition
import community.flock.wirespec.compiler.core.parse.Endpoint
import community.flock.wirespec.compiler.core.parse.Enum
import community.flock.wirespec.compiler.core.parse.Refined
import community.flock.wirespec.compiler.core.parse.Type
import community.flock.wirespec.compiler.core.parse.Union
import community.flock.wirespec.compiler.utils.Logger
import community.flock.wirespec.compiler.utils.noLogger

open class JavaEmitter(
    val packageName: String = DEFAULT_PACKAGE_STRING,
    logger: Logger = noLogger,
) : ClassModelEmitter, Emitter(logger, true) {

    private val pkg = if (packageName.isNotBlank()) {
        """
        |package $packageName;
        |
        |
        """.trimMargin()
    } else {
        ""
    }

    private fun importWireSpec(ast: AST) = if (ast.needImports()) {
        """
        |import community.flock.wirespec.Wirespec;
        |
        |
        """.trimMargin()
    } else {
        ""
    }

    private fun importJava(ast: AST) = if (ast.hasEndpoints()) {
        """
        |import java.util.concurrent.CompletableFuture;
        |import java.util.function.Function;
        |
        |
        """.trimMargin()
    } else {
        ""
    }

    override fun Definition.emitName(): String = when (this) {
        is Endpoint -> "${identifier.emit()}Endpoint"
        is Channel -> "${identifier.emit()}Channel"
        is Enum -> identifier.emit()
        is Refined -> identifier.emit()
        is Type -> identifier.emit()
        is Union -> identifier.emit()
    }

    override fun notYetImplemented() =
        """// TODO("Not yet implemented")
            |
        """.trimMargin()

    override fun emit(ast: AST): List<Emitted> = super.emit(ast)
        .map { Emitted(it.typeName.sanitizeSymbol(), "$pkg${importWireSpec(ast)}${importJava(ast)}${it.result}\n") }

    override fun emit(type: Type, ast: AST) = type.transform(ast).emit()

    override fun TypeClass.emit() = """
        |public record ${name.sanitizeSymbol()} (
        |${fields.joinToString(",\n") { it.emit() }.spacer()}
        |) ${if (supers.isNotEmpty()) "implements ${supers.joinToString(", ") { it.emit() }} " else ""}{
        |};
    """.trimMargin()

    override fun emit(refined: Refined) = refined.transform().emit()

    override fun RefinedClass.emit() = """
        |public record ${name.sanitizeSymbol()} (String value) implements Wirespec.Refined {
        |${Spacer}@Override
        |${Spacer}public String toString() { return value; }
        |${Spacer}public static boolean validate($name record) {
        |${Spacer}${validator.emit()}
        |${Spacer}}
        |${Spacer}@Override
        |${Spacer}public String getValue() { return value; }
        |}
    """.trimMargin()


    override fun RefinedClass.Validator.emit() = run {
        """
        |${Spacer}return java.util.regex.Pattern.compile(${value.replace("\\", "\\\\")}).matcher(record.value).find();
        """.trimMargin()
    }

    override fun emit(enum: Enum) = enum.transform().emit()

    override fun EnumClass.emit(): String = """
        |public enum ${name.sanitizeSymbol()} implements Wirespec.Enum {
        |${entries.joinToString(",\n") { "${it.sanitizeEnum().sanitizeKeywords()}(\"${it}\")" }.spacer()};
        |${Spacer}public final String label;
        |${Spacer}${name.sanitizeSymbol()}(String label) {
        |${Spacer(2)}this.label = label;
        |${Spacer}}
        |${Spacer}@Override
        |${Spacer}public String toString() {
        |${Spacer(2)}return label;
        |${Spacer}}
        |}
    """.trimMargin()

    override fun emit(union: Union) = union.transform().emit()

    override fun emit(channel: Channel): String =
        """
            |interface ${channel.identifier.emit()}Channel {
            |   void invoke(${channel.reference.transform(channel.isNullable, false).emitWrap()} message)
            |}
        """.trimMargin()

    override fun UnionClass.emit(): String = """
        |public sealed interface $name permits ${entries.joinToString(", ")} {}
    """.trimMargin()

    override fun emit(endpoint: Endpoint) = endpoint.transform().emit()

    override fun EndpointClass.emit() = """
        |public interface ${name.sanitizeSymbol()} extends Wirespec.Endpoint {
        |${Spacer}static String PATH = "$path";
        |${Spacer}static String METHOD = "$method";
        |
        |${Spacer}sealed interface Request<T> extends Wirespec.Request<T> {
        |${Spacer}}
        |
        |${requestClasses.joinToString("\n\n") { it.emit() }.spacer()}
        |
        |${Spacer}sealed interface Response<T> extends Wirespec.Response<T> {
        |${Spacer}};
        |
        |${responseInterfaces.joinToString("\n\n") { it.emit() }.spacer()}
        |
        |${responseClasses.joinToString("\n\n") { it.emit() }.spacer()}
        |
        |${requestMapper.emit().spacer()}
        |${responseMapper.emit().spacer()}
        |
        |${Spacer}public CompletableFuture<Response<?>> ${functionName}(Request<?> request);
        |
        |}
    """.trimMargin()


    override fun EndpointClass.RequestClass.emit() = """
         |final class ${name.sanitizeSymbol()} implements ${supers.joinToString(", ") { it.emitWrap() }} {
         |${fields.joinToString("\n") { "${it.emit()};" }.spacer()}
         |
         |${requestAllArgsConstructor.emit().spacer()}
         |
         |${requestParameterConstructor.emit().spacer()}
         |
         |${fields.joinToString("\n\n") { it.emitGetter() }.spacer()}
         |}
    """.trimMargin()

    override fun EndpointClass.RequestClass.RequestAllArgsConstructor.emit(): String = """
        |public ${name.sanitizeSymbol()}(
        |${parameters.joinToString(",\n") { it.emit() }.spacer()}
        |) {
        |${Spacer}this.path = path;
        |${Spacer}this.method = method;
        |${Spacer}this.query = query;
        |${Spacer}this.headers = headers;
        |${Spacer}this.content = content;
        |}
    """.trimMargin()

    override fun EndpointClass.RequestClass.RequestParameterConstructor.emit(): String = """
        |public ${name.sanitizeSymbol()}(
        |${parameters.joinToString(",\n") { it.emit() }.spacer()}
        |) {
        |${Spacer}this.path = ${path.emit()};
        |${Spacer}this.method = Wirespec.Method.${method};
        |${Spacer}this.query = java.util.Map.ofEntries(${query.joinToString(", ") { "java.util.Map.entry(\"$it\", java.util.List.of(${it.sanitizeIdentifier()}))" }});
        |${Spacer}this.headers = java.util.Map.ofEntries(${headers.joinToString(", ") { "java.util.Map.entry(\"$it\", java.util.List.of(${it.sanitizeIdentifier()}))" }});
        |${Spacer}this.content = ${content?.emit() ?: "null"};
        |}
    """.trimMargin()

    override fun EndpointClass.RequestMapper.emit(): String = """
        |static <B, Req extends Request<?>> Function<Wirespec.Request<B>, Req> $name(Wirespec.ContentMapper<B> contentMapper) {
        |${Spacer}return request -> {
        |${conditions.joinToString("\n") { it.emit() }.spacer(2)}
        |${Spacer(2)}throw new IllegalStateException("Unknown response type");
        |${Spacer}};
        |}
    """.trimMargin()

    override fun EndpointClass.RequestMapper.RequestCondition.emit(): String =
        if (content == null)
            """
                |if (request.getContent() == null) {
                |${Spacer}return (Req) new ${responseReference.emitWrap()}(request.getPath(), request.getMethod(), request.getQuery(), request.getHeaders(), null);
                |}
            """.trimMargin()
        else
            """
                |if (request.getContent().type().equals("${content.type}")) {
                |${Spacer}Wirespec.Content<${content.reference.emitWrap()}> content = contentMapper.read(request.getContent(), Wirespec.getType(${content.reference.emit()}.class, ${isIterable}));
                |${Spacer}return (Req) new ${responseReference.emitWrap()}(request.getPath(), request.getMethod(), request.getQuery(), request.getHeaders(), content);
                |}
            """.trimMargin()


    override fun EndpointClass.ResponseClass.emit(): String = """
        |final class $name implements ${`super`.emitWrap()} {
        |${fields.joinToString("\n") { "${it.emit()};" }.spacer()}
        |
        |${responseAllArgsConstructor.emit().spacer()}
        |
        |${responseParameterConstructor.emit().spacer()}
        |
        |${fields.joinToString("\n\n") { it.emitGetter() }.spacer()}
        |}
    """.trimMargin()

    override fun EndpointClass.ResponseClass.ResponseAllArgsConstructor.emit(): String = """
        |public $name(${parameters.joinToString(", ") { it.emit() }}) {
        |${Spacer}this.status = status;
        |${Spacer}this.headers = headers;
        |${Spacer}this.content = content;
        |}
    """.trimMargin()

    override fun EndpointClass.ResponseClass.ResponseParameterConstructor.emit(): String = """
        |public ${name.sanitizeSymbol()}(
        |${parameters.joinToString(",\n") { it.emit() }.spacer()}
        |) {
        |${Spacer}this.status = ${if (statusCode.isInt()) statusCode else "status"};
        |${Spacer}this.headers = java.util.Map.ofEntries(${headers.joinToString(", ") { "java.util.Map.entry(\"$it\", java.util.List.of(${it.sanitizeIdentifier()}))" }});
        |${Spacer}this.content = ${content?.emit() ?: "null"};
        |}
    """.trimMargin()

    override fun EndpointClass.ResponseMapper.emit(): String = """
        |static <B, Res extends Response<?>> Function<Wirespec.Response<B>, Res> $name(Wirespec.ContentMapper<B> contentMapper) {
        |${Spacer}return response -> {
        |${this.conditions.joinToString("\n") { it.emit() }.spacer(2)}
        |${Spacer(2)}throw new IllegalStateException("Unknown response type");
        |${Spacer}};
        |}
    """.trimMargin()

    override fun EndpointClass.ResponseMapper.ResponseCondition.emit(): String =
        if (content == null)
            """
                |if (${if (statusCode.isInt()) "response.getStatus() == $statusCode && " else ""}response.getContent() == null) {
                |${Spacer}return (Res) new ${responseReference.emitWrap()}(response.getStatus(), response.getHeaders(), null);
                |}
            """.trimMargin()
        else
            """
                |if (${if (statusCode.isInt()) "response.getStatus() == $statusCode && " else ""}response.getContent().type().equals("${content.type}")) {
                |${Spacer}Wirespec.Content<${content.reference.emitWrap()}> content = contentMapper.read(response.getContent(), Wirespec.getType(${content.reference.emit()}.class, ${isIterable}));
                |${Spacer}return (Res) new ${responseReference.emitWrap()}(response.getStatus(), response.getHeaders(), content);
                |}
            """.trimMargin()

    override fun EndpointClass.ResponseInterface.emit(): String = """
        |sealed interface ${name.emitWrap()} extends ${`super`.emitWrap()} {
        |};
    """.trimMargin()

    override fun Parameter.emit(): String = "${reference.emitWrap()} ${identifier.sanitizeIdentifier()}"

    override fun ClassReference.Generics.emit(): String = references
        .takeIf { it.isNotEmpty() }
        ?.map { it.emitWrap() }
        ?.joinToString(", ", "<", ">") { it }
        .orEmpty()

    override fun ClassReference.emit(): String = when (this) {
        is ClassReference.Custom -> emit()
        is ClassReference.Language -> emit()
        is ClassReference.Wirespec -> emit()
    }

    private fun ClassReference.emitWrap(): String = emit()
        .let { if (isIterable) "java.util.List<$it>" else it }
        .let { if (isOptional) "java.util.Optional<$it>" else it }
        .let { if (isDictionary) "java.util.Map<String, $it>" else it }

    override fun ClassReference.Custom.emit(): String = """
        |${if (name in internalClasses && !isInternal) "${packageName}." else ""}${name.sanitizeSymbol()}${generics.emit()}
    """.trimMargin()


    override fun ClassReference.Language.emit(): String = """
        |${primitive.emit()}${generics.emit()}
    """.trimMargin()

    override fun ClassReference.Wirespec.emit(): String = "Wirespec.${name}${generics.emit()}"

    override fun ClassReference.Language.Primitive.emit(): String = when (this) {
        ClassReference.Language.Primitive.Any -> "Object"
        ClassReference.Language.Primitive.Unit -> "Void"
        ClassReference.Language.Primitive.String -> "String"
        ClassReference.Language.Primitive.Integer -> "int"
        ClassReference.Language.Primitive.Long -> "Long"
        ClassReference.Language.Primitive.Number -> "Double"
        ClassReference.Language.Primitive.Boolean -> "Boolean"
        ClassReference.Language.Primitive.Map -> "java.util.Map"
        ClassReference.Language.Primitive.List -> "java.util.List"
        ClassReference.Language.Primitive.Double -> "Double"
    }

    override fun EndpointClass.Path.emit(): String = value
        .flatMap { listOf(EndpointClass.Path.Literal("/"), it) }
        .joinToString(" + ") {
            when (it) {
                is EndpointClass.Path.Literal -> "\"${it.value}\""
                is EndpointClass.Path.Parameter -> it.value
            }
        }

    override fun EndpointClass.Content.emit(): String = """new Wirespec.Content("$type", body)"""

    override fun FieldClass.emit(): String =
        """${if (isPrivate) "private " else ""}${if (isFinal) "final " else ""}${reference.emitWrap()} ${identifier.sanitizeIdentifier()}"""

    private fun FieldClass.emitGetter(): String = """
        |@Override
        |public ${reference.emitWrap()} get${identifier.sanitizeIdentifier().replaceFirstChar { it.uppercase() }}() {
        |  return ${identifier.sanitizeIdentifier()};
        |}
    """.trimMargin()

    private fun String.sanitizeIdentifier() = split("-")
        .mapIndexed { index, s -> if (index > 0) s.firstToUpper() else s }
        .joinToString("")
        .sanitizeKeywords()
        .sanitizeSymbol()
        .firstToLower()

    private fun String.sanitizeEnum() = this
        .replace("/", "_")
        .replace(" ", "_")
        .replace("-", "_")
        .replace("–", "_")
        .let { if (it.first().isDigit()) "_$it" else it }

    private fun String.sanitizeKeywords() = if (this in reservedKeywords) "_$this" else this

    private fun String.sanitizeSymbol() = replace(".", "").replace(" ", "_")

    companion object {
        val reservedKeywords = listOf(
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
