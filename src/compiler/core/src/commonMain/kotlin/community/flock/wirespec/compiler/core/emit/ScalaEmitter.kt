package community.flock.wirespec.compiler.core.emit

import community.flock.wirespec.compiler.core.emit.common.DEFAULT_PACKAGE_NAME
import community.flock.wirespec.compiler.core.emit.common.Emitter
import community.flock.wirespec.compiler.core.parse.AST
import community.flock.wirespec.compiler.core.parse.Endpoint
import community.flock.wirespec.compiler.core.parse.Enum
import community.flock.wirespec.compiler.core.parse.Refined
import community.flock.wirespec.compiler.core.parse.Type
import community.flock.wirespec.compiler.core.parse.Type.Shape.Field.Reference
import community.flock.wirespec.compiler.utils.Logger
import community.flock.wirespec.compiler.utils.noLogger

class ScalaEmitter(
    private val packageName: String = DEFAULT_PACKAGE_NAME,
    logger: Logger = noLogger
) : Emitter(logger) {

    override val shared = """
        |package community.flock.wirespec
        |
        |import java.lang.reflect.Type
        |import java.lang.reflect.ParameterizedType
        |
        |object Wirespec {
        |${Enum("Method", setOf("GET", "PUT", "POST", "DELETE", "OPTIONS", "HEAD", "PATCH", "TRACE")).emit().prependIndent(SPACER)}
        |${SPACER}case class Content[T] (`type`:String, body:T )
        |${SPACER}trait Request[T] { val path:String; val method: Method; val query: Map[String, List[Any]]; val headers: Map[String, List[Any]]; val content:Content[T] }
        |${SPACER}trait Response[T] { val status: Int; val headers: Map[String, List[Any]]; val content:Content[T] }
        |${SPACER}trait ContentMapper[B] { def read[T](content: Content[B], valueType: Type): Content[T]; def write[T](content: Content[T]): Content[B] }
        |${SPACER}def getType(`type`: Class[_], isIterable: Boolean): Type = {
        |${SPACER}${SPACER}if (isIterable) {
        |${SPACER}${SPACER}${SPACER}new ParameterizedType {
        |${SPACER}${SPACER}${SPACER}${SPACER}override def getRawType():Class[List[_]] = classOf[List[_]]
        |${SPACER}${SPACER}${SPACER}${SPACER}override def getActualTypeArguments():Array[Type] = Array(`type`)
        |${SPACER}${SPACER}${SPACER}${SPACER}override def getOwnerType() = null
        |${SPACER}${SPACER}${SPACER}}
        |${SPACER}${SPACER}} else {
        |${SPACER}${SPACER}${SPACER}`type`
        |${SPACER}${SPACER}}
        |${SPACER}}
        |}
    """.trimMargin()

    val import = """
        |
        |import scala.language.higherKinds
        |import community.flock.wirespec.Wirespec
        |
    """.trimMargin()

    override fun emit(ast: AST): List<Pair<String, String>> =
        super.emit(ast).map { (name, result) ->
            name to """
                    |${if (packageName.isBlank()) "" else "package $packageName"}
                    |${if (ast.hasEndpoints()) import else ""}
                    |${result}
            """.trimMargin().trimStart()
        }

    override fun Type.emit() = withLogging(logger) {
        """case class $name(
            |${shape.emit()}
            |)
            |
            |""".trimMargin()
    }

    override fun Type.Shape.emit() = withLogging(logger) {
        value.joinToString(",\n") { "${SPACER}val ${it.emit()}" }
    }

    override fun Type.Shape.Field.emit() = withLogging(logger) {
        "${identifier.emit()}: ${if (isNullable) "Option[${reference.emit()}]" else reference.emit()}"
    }

    override fun Type.Shape.Field.Identifier.emit() = withLogging(logger) {
        if (preservedKeywords.contains(value)) "`$value`" else value
    }


    private fun Reference.emitPrimaryType() = withLogging(logger) {
        when (this) {
            is Reference.Any -> "Any"
            is Reference.Custom -> value
            is Reference.Primitive -> when (type) {
                Reference.Primitive.Type.String -> "String"
                Reference.Primitive.Type.Integer -> "Int"
                Reference.Primitive.Type.Boolean -> "Boolean"
            }
        }
    }

    override fun Reference.emit() = withLogging(logger) {
        emitPrimaryType()
            .let { if (isIterable) "List[$it]" else it }
            .let { if (isMap) "Map[String, $it]" else it }
    }

    override fun Enum.emit() = withLogging(logger) {
        fun String.sanitize() = replace("-", "_").let { if(it.first().isDigit()) "_$it" else it }
        """
        |sealed abstract class $name(val label: String)
        |object $name {
        |${entries.joinToString("\n") { """${SPACER}final case object ${it.sanitize().uppercase()} extends $name(label = "$it")""" }}
        |}
        |""".trimMargin()
    }

    override fun Refined.emit() = withLogging(logger) {
        """case class $name(val value: String) {
            |${SPACER}implicit class ${name}Ops(val that: $name) {
            |${validator.emit()}
            |${SPACER}}
            |}
            |
            |""".trimMargin()
    }

    override fun Refined.Validator.emit() = withLogging(logger) {
        """${SPACER}${SPACER}val regex = new scala.util.matching.Regex($value)
            |${SPACER}${SPACER}regex.findFirstIn(that.value)""".trimMargin()
    }

    override fun Endpoint.emit() = withLogging(logger) {
        """trait $name[F[_]] {
            |${SPACER}def ${name.firstToLower()}(request: $name.Request[_]): F[$name.Response[_]]
            |}
            |object $name {
            |${SPACER}sealed trait Request[T] extends Wirespec.Request[T]
            ||${requests.joinToString("\n") { "${SPACER}class Request${it.content.emitContentType()}(override val path:String, override val method: Wirespec.Method, override val query: Map[String, List[Any]], override val headers: Map[String, List[Any]], override val content:Wirespec.Content[${it.content?.reference?.emit() ?: "Unit"}]) extends Request[${it.content?.reference?.emit() ?: "Unit"}] { def this${emitRequestSignature(it.content)}{ this(path = \"${path.emitPath()}\", method = Wirespec.Method.${method.name}, query = Map(${query.emitMap()}), headers = Map(${headers.emitMap()}), content = ${it.content?.let { "Wirespec.Content(\"${it.type}\", body)" } ?: "null"})}}" }}
            |${SPACER}sealed trait Response[T] extends Wirespec.Response[T]
            |${responses.map { it.status.groupStatus() }.toSet().joinToString("\n") { "${SPACER}sealed trait Response${it}[T] extends Response[T]" }}
            |${responses.filter { it.status.isInt() }.map { it.status }.toSet().joinToString("\n") { "${SPACER}sealed trait Response${it}[T] extends Response${it.groupStatus()}[T]" }}
            |${responses.filter { it.status.isInt() }.distinctBy { it.status to it.content?.type }.joinToString("\n") { "${SPACER}class Response${it.status}${it.content?.emitContentType() ?: "Unit"} (override val headers: Map[String, List[Any]]${it.content?.let { ", body: ${it.reference.emit()}" } ?: ""} ) extends Response${it.status}[${it.content?.reference?.emit() ?: "Unit"}] { override val status = ${it.status}; override val content = ${it.content?.let { "Wirespec.Content(\"${it.type}\", body)" } ?: "null"}}" }}
            |${responses.filter { !it.status.isInt() }.distinctBy { it.status to it.content?.type }.joinToString("\n") { "${SPACER}class Response${it.status.firstToUpper()}${it.content?.emitContentType() ?: "Unit"} (override val status: Int, override val headers: Map[String, List[Any]]${it.content?.let { ", body: ${it.reference.emit()}" } ?: ""} ) extends Response${it.status.firstToUpper()}[${it.content?.reference?.emit() ?: "Unit"}] { override val content = ${it.content?.let { "Wirespec.Content(\"${it.type}\", body)" } ?: "null"}}" }}
            |${responses.emitResponseMapper()}
            |}
            |
        """.trimMargin()
    }

    private fun Endpoint.Content?.emitContentType() = this
        ?.type
        ?.split("/", "-")
        ?.joinToString("") { it.firstToUpper() }
        ?: "Unit"

    private fun List<Endpoint.Segment>.emitPath() = "/" + joinToString("/") { it.emit() }

    private fun Endpoint.Segment.emit(): String = withLogging(logger) {
        when (this) {
            is Endpoint.Segment.Literal -> value
            is Endpoint.Segment.Param -> "\${${identifier.value}}"
        }
    }

    private fun List<Type.Shape.Field>.emitMap() =
        joinToString(", ") { "\"${it.identifier.emit()}\" -> List(${it.identifier.emit()})" }

    private fun Endpoint.emitRequestSignature(content: Endpoint.Content? = null): String {
        val pathField = path
            .filterIsInstance<Endpoint.Segment.Param>()
            .map { Type.Shape.Field(it.identifier, it.reference, false) }
        val parameters = pathField + query + headers + cookies
        return """
            |(${
            parameters
                .plus(content?.reference?.toField("body", false))
                .filterNotNull()
                .joinToString(", ") { it.emit() }
        })
        """.trimMargin()
    }

    private fun List<Endpoint.Response>.emitResponseMapper() = """
        |${SPACER}def RESPONSE_MAPPER[B](contentMapper: Wirespec.ContentMapper[B], status: Int, headers:Map[String, List[Any]], content: Wirespec.Content[B]) =
        |${SPACER}${SPACER}(status, content.`type`) match {
        |${filter { it.status.isInt() }.distinctBy { it.status to it.content?.type }.joinToString("\n") { it.emitResponseMapperCondition() }}
        |${filter { !it.status.isInt() }.distinctBy { it.status to it.content?.type }.joinToString("\n") { it.emitResponseMapperCondition() }}
        |${SPACER}${SPACER}${SPACER}case _ => throw new Exception(s"Cannot map response with status ${"$"}status")
        |${SPACER}${SPACER}}
    """.trimMargin()

    private fun Endpoint.Response.emitResponseMapperCondition() =
        when (content) {
            null -> """
                    |${SPACER}${SPACER}${SPACER}case (${status.takeIf { it.isInt() }?.let { status }?:"_"}, null) => new Response${status.firstToUpper()}Unit(${status.takeIf { !it.isInt() }?.let { "status, " }.orEmptyString()}headers)
                """.trimMargin()

            else -> """
                    |${SPACER}${SPACER}${SPACER}case (${status.takeIf { it.isInt() }?.let { status }?:"_"}, "${content.type}") => {
                    |${SPACER}${SPACER}${SPACER}${SPACER}new Response${status.firstToUpper()}${content.emitContentType()}(${status.takeIf { !it.isInt() }?.let { "status, " }.orEmptyString()}headers, contentMapper.read[${content.reference.emit()}](content, Wirespec.getType(classOf[${content.reference.emitPrimaryType()}], ${content.reference.isIterable})).body)
                    |${SPACER}${SPACER}${SPACER}}
                """.trimMargin()
        }

    private fun String?.orEmptyString() = this ?: ""
    private fun String.groupStatus() =
        if (isInt()) substring(0, 1) + "XX"
        else firstToUpper()

    companion object {
        private val preservedKeywords = listOf(
            "abstract",
            "case",
            "catch",
            "class",
            "def",
            "do",
            "else",
            "extends",
            "false",
            "final",
            "finally",
            "for",
            "forSome",
            "if",
            "implicit",
            "import",
            "lazy",
            "match",
            "new",
            "null",
            "object",
            "override",
            "package",
            "private",
            "protected",
            "return",
            "sealed",
            "super",
            "this",
            "throw",
            "trait",
            "true",
            "try",
            "type",
            "val",
            "var",
            "while",
            "with",
            "yield",
        )
    }

}
