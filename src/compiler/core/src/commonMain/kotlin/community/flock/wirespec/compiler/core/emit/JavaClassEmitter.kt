package community.flock.wirespec.compiler.core.emit

import community.flock.wirespec.compiler.core.emit.common.ClassModelEmitter
import community.flock.wirespec.compiler.core.emit.common.ClassModelEmitter.Companion.SPACER
import community.flock.wirespec.compiler.core.parse.nodes.EndpointClass
import community.flock.wirespec.compiler.core.parse.nodes.EnumClass
import community.flock.wirespec.compiler.core.parse.nodes.Field
import community.flock.wirespec.compiler.core.parse.nodes.Parameter
import community.flock.wirespec.compiler.core.parse.nodes.Reference
import community.flock.wirespec.compiler.core.parse.nodes.RefinedClass
import community.flock.wirespec.compiler.core.parse.nodes.TypeClass

class JavaClassEmitter : ClassModelEmitter {

    override fun TypeClass.emit(): String = """
        |public record $name(
        |${fields.joinToString(",\n") { it.emit() }.spacer()}
        |){
        |};
    """.trimMargin()

    override fun RefinedClass.emit() = """
        |public record ${name.sanitizeSymbol()} (String value) implements Wirespec.Refined {
        |${SPACER}static boolean validate($name record) {
        |${SPACER}${validator.emit()}
        |${SPACER}}
        |${SPACER}@Override
        |${SPACER}public String getValue() { return value; }
        |}
    """.trimMargin()


    override fun RefinedClass.Validator.emit() = """
        |${SPACER}return java.util.regex.Pattern.compile($value).matcher(record.value).find();
    """.trimMargin()

    override fun EnumClass.emit(): String = """
        |public enum ${name.sanitizeSymbol()} implements Wirespec.Enum {
        |${entries.joinToString(",\n") { "${it.sanitizeEnum().sanitizeKeywords()}(\"${it}\")" }.spacer()};
        |${SPACER}public final String label;
        |${SPACER}${name.sanitizeSymbol()}(String label) {
        |${SPACER}${SPACER}this.label = label;
        |${SPACER}}
        |${SPACER}@Override
        |${SPACER}public String toString() {
        |${SPACER}${SPACER}return label;
        |${SPACER}}
        |}
    """.trimMargin()

    override fun EndpointClass.emit(): String = """
        |public interface $name extends Wirespec.Endpoint {
        |${SPACER}static String PATH = "$path";
        |${SPACER}static String METHOD = "$method";
        |
        |${SPACER}sealed interface Request<T> extends Wirespec.Request<T> {
        |${SPACER}}
        |
        |${requestClasses.joinToString("\n\n") { it.emit() }.spacer()}
        |
        |${SPACER}sealed interface Response<T> extends Wirespec.Response<T> {
        |${SPACER}};
        |
        |${responseInterfaces.joinToString("\n\n") { it.emit() }.spacer()}
        |
        |${responseClasses.joinToString("\n\n") { it.emit() }.spacer()}
        |
        |${requestMapper.emit().spacer()}
        |${responseMapper.emit().spacer()}
        |}
    """.trimMargin()

    override fun EndpointClass.RequestClass.emit() = """
         |final class $name implements ${supers.joinToString(", ") { it.emit() }} {
         |${fields.joinToString("\n") { "${it.emit()};" }.spacer()}
         |
         |${primaryConstructor.emit().spacer()}
         |
         |${secondaryConstructor.emit().spacer()}
         |
         |${fields.joinToString("\n\n") { it.emitGetter() }.spacer()}
         |}
    """.trimMargin()

    override fun EndpointClass.RequestClass.PrimaryConstructor.emit(): String = """
        |public $name(
        |${parameters.joinToString(",\n") { it.emit() }.spacer()}
        |) {
        |  this.path = path;
        |  this.method = method;
        |  this.query = query;
        |  this.headers = headers;
        |  this.content = content;
        |}
    """.trimMargin()

    override fun EndpointClass.RequestClass.SecondaryConstructor.emit(): String = """
        |public $name(
        |${parameters.joinToString(",\n") { it.emit() }.spacer()}
        |) {
        |  this.path = ${path.emit()};
        |  this.method = Wirespec.Method.${method};
        |  this.query = java.util.Map.of(${query});
        |  this.headers = java.util.Map.of(${headers});
        |  this.content = ${content?.emit() ?: "null"};
        |}
    """.trimMargin()

    override fun EndpointClass.RequestMapper.emit(): String = """
        |static <B, Req extends Request<?>> Function<Wirespec.Request<B>, Req> $name(Wirespec.ContentMapper<B> contentMapper) {
        |${SPACER}return request -> {
        |${this.conditions.joinToString("\n") { it.emit() }.spacer(2)}
        |${SPACER}${SPACER}throw new IllegalStateException("Unknown response type");
        |${SPACER}}
        |}
    """.trimMargin()

    override fun EndpointClass.RequestMapper.RequestCondition.emit(): String =
        if (content != null)
            """
                |if (request.getContent().type().equals("${content.type}")) {
                |${SPACER}Wirespec.Content<Pet> content = contentMapper.read(request.getContent(), Wirespec.getType(${content.reference.emit()}.class, ${isIterable}));
                |${SPACER}return (Req) new ${responseReference.emit()}(request.getPath(), request.getMethod(), request.getQuery(), request.getHeaders(), content);
                |}
            """.trimMargin()
        else
            """
                |if (request.getContent().type() == null) {
                |${SPACER}return (Req) new ${responseReference.emit()}(request.getPath(), request.getMethod(), request.getQuery(), request.getHeaders());
                |}
            """.trimMargin()

    override fun EndpointClass.ResponseClass.emit(): String = """
        |final class ${name} implements ${returnReference.emit()} {
        |${fields.joinToString("\n") { "${it.emit()};" }.spacer()}
        |
        |${allArgsConstructor.emit().spacer()}
        |
        |${fields.joinToString("\n\n") { it.emitGetter() }.spacer()}
        |}
    """.trimMargin()

    override fun EndpointClass.ResponseClass.AllArgsConstructor.emit(): String = """
        |public $name(${parameters.joinToString(", ") { it.emit() }}) {
        |${SPACER}this.status = ${statusCode};
        |${SPACER}this.headers = headers;
        |${SPACER}this.content = ${content?.emit()};
        |}
    """.trimMargin()

    override fun EndpointClass.ResponseMapper.emit(): String = """
        |static <B, Res extends Response<?>> Function<Wirespec.Response<B>, Res> $name(Wirespec.ContentMapper<B> contentMapper) {
        |${SPACER}return response -> {
        |${this.conditions.joinToString("\n") { it.emit() }.spacer(2)}
        |${SPACER}${SPACER}throw new IllegalStateException("Unknown response type");
        |${SPACER}};
        |}
    """.trimMargin()

    override fun EndpointClass.ResponseMapper.ResponseCondition.emit(): String =
        if (content != null)
            """
                |if (response.getStatus() == $statusCode && response.getContent().type().equals("${content.type}")) {
                |${SPACER}Wirespec.Content<Pet> content = contentMapper.read(response.getContent(), Wirespec.getType(${content.reference.emit()}.class, ${isIterable}));
                |${SPACER}return (Res) new ${responseReference.emit()}(response.getHeaders(), content.body());
                |}
            """.trimMargin()
        else
            """
                |if (response.getStatus() == $statusCode && response.getContent() == null) {
                |${SPACER}return (Res) new ${responseReference.emit()}(response.getHeaders());
                |}
            """.trimMargin()

    override fun EndpointClass.ResponseInterface.emit(): String = """
        |sealed interface ${name.emit()} extends ${`super`.emit()} {
        |};
    """.trimMargin()

    override fun Parameter.emit(): String = """
        |${reference.emit()} $identifier
    """.trimMargin()

    override fun Reference.Generics.emit(): String = """
        |${references.takeIf { it.isNotEmpty() }?.joinToString(", ", "<", ">") { it.emit() }.orEmpty()}
    """.trimMargin()

    fun Reference.emit(): String =
        when (this) {
            is Reference.Custom -> emit()
            is Reference.Language -> emit()
        }
            .let { if (isOptional) "java.util.Optional<$it>" else it }
            .let { if (isIterable) "java.util.List<$it>" else it }


    override fun Reference.Custom.emit(): String = this
        .let {
            """
                |${name}${generics.emit()}
            """.trimMargin()

        }

    override fun Reference.Language.emit(): String = """
        |${primitive.emit()}${generics.emit()}
    """.trimMargin()

    override fun Reference.Language.Primitive.emit(): String = when (this) {
        Reference.Language.Primitive.Any -> "Object"
        Reference.Language.Primitive.Unit -> "Void"
        Reference.Language.Primitive.String -> "String"
        Reference.Language.Primitive.Integer -> "int"
        Reference.Language.Primitive.Long -> "Long"
        Reference.Language.Primitive.Number -> "Double"
        Reference.Language.Primitive.Boolean -> "boolean"
        Reference.Language.Primitive.Map -> "java.util.Map"
        Reference.Language.Primitive.List -> "java.util.List"
    }

    override fun EndpointClass.Path.emit(): String = value
        .flatMap { listOf(EndpointClass.Path.Literal("/"), it) }
        .joinToString(" + ") {
            when (it) {
                is EndpointClass.Path.Literal -> "\"${it.value}\""
                is EndpointClass.Path.Parameter -> it.value
            }
        }


    override fun EndpointClass.Content.emit(): String = """
        |new Wirespec.Content("$type", body)
    """.trimMargin()

    override fun Field.emit(): String = """
        |${if (isPrivate) "private " else ""}${if (isPrivate) "final " else ""}${reference.emit()} $identifier
    """.trimMargin()

    private fun Field.emitGetter(): String = """
        |@Override
        |public ${reference.emit()} get${identifier.replaceFirstChar { it.uppercase() }}() {
        |  return ${identifier};
        |}
    """.trimMargin()

    private fun String.sanitizeEnum() = this
        .replace("/", "_")
        .replace(" ", "_")
        .replace("-", "_")
        .replace("–", "_")
        .let { if (it.first().isDigit()) "_$it" else it }

    private fun String.sanitizeKeywords() = if (reservedKeywords.contains(this)) "_$this" else this

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