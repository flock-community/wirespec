package community.flock.wirespec.compiler.core.emit

import community.flock.wirespec.compiler.core.emit.common.ClassModelEmitter
import community.flock.wirespec.compiler.core.emit.common.ClassModelEmitter.Companion.SPACER
import community.flock.wirespec.compiler.core.parse.nodes.ClassModel
import community.flock.wirespec.compiler.core.parse.nodes.Constructor
import community.flock.wirespec.compiler.core.parse.nodes.EndpointClass
import community.flock.wirespec.compiler.core.parse.nodes.Field
import community.flock.wirespec.compiler.core.parse.nodes.Parameter
import community.flock.wirespec.compiler.core.parse.nodes.Reference
import community.flock.wirespec.compiler.core.parse.nodes.Statement

class JavaClassEmitter : ClassModelEmitter {

    override fun EndpointClass.emit(): String = """
        |public interface $name extends Wirespec.Endpoint {
        |${SPACER}static String PATH = "$path";
        |${SPACER}static String METHOD = "$method";
        |
        |${SPACER}sealed interface Request<T> extends Wirespec.Request<T> {
        |${SPACER}}
        |
        |${requestClasses.joinToString("\n") { it.emit() }.spacer()}
        |
        |${requestMapper.emit().spacer()}
        |${responseMapper.emit().spacer()}
        |}
    """.trimMargin()

    override fun EndpointClass.RequestClass.emit() = """
         |final class $name implements ${supers.joinToString(", ") { it.emit() }} {
         |${fields.joinToString("\n") { "${it.emit()};" }.spacer()}
         |
         |${constructors.joinToString("\n\n") { it.emit() }.spacer()}
         |
         |${fields.joinToString("\n\n") { it.emitGetter() }.spacer()}
         |}
    """.trimMargin()

    override fun EndpointClass.RequestMapper.emit(): String = """
        |static <B, Req extends Request<?>> Function<Wirespec.Request<B>, Req> $name(Wirespec.ContentMapper<B> contentMapper) {
        |${SPACER}return request -> {
        |${this.conditions.joinToString ("\n") { it.emit() }.spacer(2)}
        |${SPACER}${SPACER}throw new IllegalStateException("Unknown response type");
        |${SPACER}}
        |}
    """.trimMargin()

    override fun EndpointClass.RequestMapper.Condition.emit(): String =
        if(content != null)
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
    override fun EndpointClass.ResponseClass.emit(): String {
        TODO("Not yet implemented")
    }

    override fun EndpointClass.ResponseMapper.emit(): String = """
        |static <B, Res extends Response<?>> Function<Wirespec.Response<B>, Res> $name(Wirespec.ContentMapper<B> contentMapper) {
        |${SPACER}return response -> {
        |${this.conditions.joinToString ("\n") { it.emit() }.spacer(2)}
        |${SPACER}${SPACER}throw new IllegalStateException("Unknown response type");
        |${SPACER}};
        |}
    """.trimMargin()

    override fun EndpointClass.ResponseMapper.Condition.emit(): String =
        if(content != null)
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

    override fun EndpointClass.ResponseInterface.emit(): String {
        TODO("Not yet implemented")
    }

    override fun Parameter.emit(): String = """
        |${reference.emit()} $identifier
    """.trimMargin()

    override fun Reference.Generics.emit(): String = """
        |${references.takeIf { it.isNotEmpty() }?.joinToString(", ", "<", ">") { it.emit() }.orEmpty()}
    """.trimMargin()

    override fun Reference.Custom.emit(): String = """
        |${name}${generics.emit() }
    """.trimMargin()

    override fun Reference.Language.emit(): String = """
        |${primitive.emit()}${generics.emit() }
    """.trimMargin()

    override fun Reference.Language.Primitive.emit(): String = when(this){
        Reference.Language.Primitive.Any -> "Object"
        Reference.Language.Primitive.Unit -> "void"
        Reference.Language.Primitive.String -> "String"
        Reference.Language.Primitive.Integer -> "Long"
        Reference.Language.Primitive.Number -> "Double"
        Reference.Language.Primitive.Boolean -> "Boolean"
        Reference.Language.Primitive.Map -> "java.util.Map"
        Reference.Language.Primitive.List -> "java.util.List"
    }

    override fun Statement.AssignField.emit(): String = """
        |this.${value} = ${statement.emit()}
    """.trimMargin()

    override fun Statement.Variable.emit(): String = """
        |$value
    """.trimMargin()

    override fun Statement.Literal.emit(): String = """
        |"$value"
    """.trimMargin()

    override fun Statement.Initialize.emit(): String = """
        |${reference.emitInitialize()}(${parameters.joinToString(", ") { it }})
    """.trimMargin()

    override fun Statement.Concat.emit(): String = """
        |${values.joinToString(" + ") { it.emit() }}
    """.trimMargin()

    override fun EndpointClass.Content.emit(): String {
        TODO("Not yet implemented")
    }

    private fun Reference.emitInitialize(): String =
        when(this){
            is Reference.Custom -> "new $name"
            is Reference.Language -> when(this.primitive){
                Reference.Language.Primitive.Map -> "${primitive.emit()}.of"
                Reference.Language.Primitive.List -> "${primitive.emit()}.of"
                else -> "new ${primitive.emit()}"
            }
        }


    override fun Field.emit(): String = """
        |private final ${reference.emit()} $identifier
    """.trimMargin()

    private fun Field.emitGetter(): String = """
        |@Override
        |public ${reference.emit()} get${identifier.replaceFirstChar { it.uppercase() }}() {
        |  return ${identifier};
        |}
    """.trimMargin()

    override fun Constructor.emit(): String = """
        |public ${name}(
        |${fields.joinToString(",\n") { it.emit() }.spacer()}
        |) {
        |${body.joinToString ("\n"){ "${it.emit()};"  }.spacer()}
        |}
    """.trimMargin()
}