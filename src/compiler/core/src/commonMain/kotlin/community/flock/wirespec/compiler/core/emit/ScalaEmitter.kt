package community.flock.wirespec.compiler.core.emit

import community.flock.wirespec.compiler.core.addBackticks
import community.flock.wirespec.compiler.core.emit.common.DEFAULT_GENERATED_PACKAGE_STRING
import community.flock.wirespec.compiler.core.emit.common.DefinitionModelEmitter
import community.flock.wirespec.compiler.core.emit.common.Emitted
import community.flock.wirespec.compiler.core.emit.common.Emitter
import community.flock.wirespec.compiler.core.emit.common.Keywords
import community.flock.wirespec.compiler.core.emit.common.Spacer
import community.flock.wirespec.compiler.core.parse.AST
import community.flock.wirespec.compiler.core.parse.Channel
import community.flock.wirespec.compiler.core.parse.Definition
import community.flock.wirespec.compiler.core.parse.Endpoint
import community.flock.wirespec.compiler.core.parse.Enum
import community.flock.wirespec.compiler.core.parse.Field
import community.flock.wirespec.compiler.core.parse.Identifier
import community.flock.wirespec.compiler.core.parse.Reference
import community.flock.wirespec.compiler.core.parse.Refined
import community.flock.wirespec.compiler.core.parse.Type
import community.flock.wirespec.compiler.core.parse.Union
import community.flock.wirespec.compiler.utils.Logger
import community.flock.wirespec.compiler.utils.noLogger

open class ScalaEmitter(
    private val packageName: String = DEFAULT_GENERATED_PACKAGE_STRING,
    logger: Logger = noLogger
) : DefinitionModelEmitter, Emitter(logger) {

    override fun Definition.emitName(): String = when (this) {
        is Endpoint -> "${identifier.emit()}Endpoint"
        is Channel -> "${identifier.emit()}Channel"
        is Enum -> identifier.emit()
        is Refined -> identifier.emit()
        is Type -> identifier.emit()
        is Union -> "${identifier.emit()}Union"
    }

    override fun notYetImplemented() =
        """// TODO("Not yet implemented")
            |
        """.trimMargin()

    override fun emit(ast: AST): List<Emitted> = super.emit(ast)
        .map { Emitted(it.typeName, if (packageName.isBlank()) "" else "package $packageName\n\n${it.result}") }

    override fun emit(type: Type, ast: AST) = """
        |case class ${type.emitName()}(
        |${type.shape.emit()}
        |)
        |
    """.trimMargin()

    override fun Type.Shape.emit() = value.joinToString("\n") { it.emit() }.dropLast(1)

    override fun Field.emit() =
        "${Spacer}val ${identifier.emit()}: ${if (isNullable) "Option[${reference.emit()}]" else reference.emit()},"

    override fun Identifier.emit() = if (value in reservedKeywords) value.addBackticks() else value

    override fun emit(channel: Channel) = """
        |trait ${channel.identifier.emit()}Channel {
        |  def invoke(message: ${channel.reference.emit()})
        |}
        |
    """.trimMargin()

    override fun Reference.emit() = when (this) {
        is Reference.Unit -> "Unit"
        is Reference.Any -> "Any"
        is Reference.Custom -> value
        is Reference.Primitive -> when (type) {
            Reference.Primitive.Type.String -> "String"
            Reference.Primitive.Type.Integer -> "Long"
            Reference.Primitive.Type.Number -> "Double"
            Reference.Primitive.Type.Boolean -> "Boolean"
        }
    }
        .let { if (isIterable) "List[$it]" else it }
        .let { if (isDictionary) "Map[String, $it]" else it }

    override fun emit(enum: Enum) = enum.run {
        fun String.sanitize() = replace("-", "_").let { if (it.first().isDigit()) "_$it" else it }
        """
        |sealed abstract class ${emitName()}(val label: String)
        |object ${identifier.emit()} {
        |${
            entries.joinToString("\n") {
                """${Spacer}final case object ${
                    it.sanitize().uppercase()
                } extends ${identifier.emit()}(label = "$it")"""
            }
        }
        |}
        |""".trimMargin()
    }

    override fun emit(refined: Refined) =
        """case class ${refined.emitName()}(val value: String) {
            |${Spacer}implicit class ${refined.emitName()}Ops(val that: ${refined.emitName()}) {
            |${refined.validator.emit()}
            |${Spacer}}
            |}
            |
            |""".trimMargin()


    override fun Refined.Validator.emit() =
        """${Spacer(2)}val regex = new scala.util.matching.Regex(${"\"\"\""}$expression${"\"\"\""})
            |${Spacer(2)}regex.findFirstIn(that.value)""".trimMargin()

    override fun emit(endpoint: Endpoint) = """
        |trait ${endpoint.identifier.emit()}Endpoint extends Wirespec.Endpoint {
        |${endpoint.pathParams.emitObject("Path", "Wirespec.Path") { it.emit() }}
        |
        |${endpoint.queries.emitObject("Queries", "Wirespec.Queries") { it.emit() }}
        |
        |${endpoint.headers.emitObject("Headers", "Wirespec.Request.Headers") { it.emit() }}
        |
        |${endpoint.requests.joinToString("\n") { it.emit(endpoint) }}
        |
        |${Spacer}sealed trait Response[T] extends Wirespec.Response[T]
        |${endpoint.emitResponseInterfaces()}
        |
        |${endpoint.responses.joinToString("\n") { it.emit() }}
        |
        |${Spacer}def toResponse(serialization: Wirespec.Serializer[String], response: Response[_]): Wirespec.RawResponse =
        |${Spacer(2)}response match {
        |${endpoint.responses.joinToString("\n") { it.emitSerialized() }}
        |${Spacer(2)}}
        |
        |${Spacer}def fromResponse(serialization: Wirespec.Deserializer[String], response: Wirespec.RawResponse): Response[_] =
        |${Spacer(2)}response.statusCode match {
        |${endpoint.responses.joinToString("\n") { it.emitDeserialized() }}
        |${Spacer(3)}case _ => throw new IllegalStateException(s"Cannot match response with status: ${'$'}{response.statusCode}")
        |${Spacer(2)}}
        |
        |${Spacer}trait Handler extends Wirespec.Handler {
        |${Spacer(2)}def ${endpoint.identifier.emit().firstToLower()}(request: Request): Response[_]
        |${Spacer(2)}object Handler extends Wirespec.Server[Request, Response[_]] with Wirespec.Client[Request, Response[_]] {
        |${Spacer(3)}override val pathTemplate = "/${endpoint.path.joinToString("/") { it.emit() }}"
        |${Spacer(3)}override val method = "${endpoint.method}"
        |${Spacer(3)}override def server(serialization: Wirespec.Serialization[String]) = new Wirespec.ServerEdge[Request, Response[_]] {
        |${Spacer(4)}override def from(request: Wirespec.RawRequest) = fromRequest(serialization, request)
        |${Spacer(4)}override def to(response: Response[_]) = toResponse(serialization, response)
        |${Spacer(3)}}
        |${Spacer(3)}override def client(serialization: Wirespec.Serialization[String]) = new Wirespec.ClientEdge[Request, Response[_]] {
        |${Spacer(4)}override def to(request: Request) = toRequest(serialization, request)
        |${Spacer(4)}override def from(response: Wirespec.RawResponse) = fromResponse(serialization, response)
        |${Spacer(3)}}
        |${Spacer(2)}}
        |${Spacer}}
        |}
    """.trimMargin()

    private fun Endpoint.emitResponseInterfaces() = responses
        .distinctBy { it.status[0] }
        .joinToString("\n") { "${Spacer}sealed trait Response${it.status[0]}XX[T] extends Response[T]" }

    private fun <E> List<E>.emitObject(name: String, extends: String, block: (E) -> String) =
        if (isEmpty()) "${Spacer}case object $name extends $extends"
        else """
            |${Spacer}case class $name(
            |${joinToString(",\n") { "${Spacer(2)}val ${block(it)}" }},
            |${Spacer}) extends $extends
        """.trimMargin()

    fun Endpoint.Request.emit(endpoint: Endpoint) = """
        |${Spacer}case class Request(
        |${endpoint.pathParams.joinToString { it.emit() }.spacer(2)},
        |${endpoint.queries.joinToString { it.emit() }.spacer(2)},
        |${endpoint.headers.joinToString { it.emit() }.spacer(2)},
        |${Spacer(2)}override val body: ${content.emit()}
        |) extends Wirespec.Request[${content.emit()}] {
        |${Spacer(2)}override val path = Path${endpoint.pathParams.joinToString { it.identifier.emit() }.brace()}
        |${Spacer(2)}override val method = Wirespec.Method.${endpoint.method.name}
        |${Spacer(2)}override val queries = Queries${endpoint.queries.joinToString { it.identifier.emit() }.brace()}
        |${Spacer(2)}override val headers = Headers${
        endpoint.headers.joinToString { it.identifier.emit() }.brace()
    }${if (content == null) "\n${Spacer(2)}override val body = Unit" else ""}
        |${Spacer}}
        |
        |${Spacer}def toRequest(serialization: Wirespec.Serializer[String], request: Request): Wirespec.RawRequest =
        |${Spacer(2)}Wirespec.RawRequest(
        |${Spacer(3)}path = List(${
        endpoint.path.joinToString {
            when (it) {
                is Endpoint.Segment.Literal -> """"${it.value}""""; is Endpoint.Segment.Param -> it.emitIdentifier()
            }
        }
    }),
        |${Spacer(3)}method = request.method.name,
        |${Spacer(3)}queries = ${
        if (endpoint.queries.isNotEmpty()) "List(${
            endpoint.queries.joinToString {
                it.emitSerialized(
                    "queries"
                )
            }
        }).filterNotNull().toMap" else "Map.empty"
    },
        |${Spacer(3)}headers = ${
        if (endpoint.headers.isNotEmpty()) "List(${
            endpoint.headers.joinToString {
                it.emitSerialized(
                    "headers"
                )
            }
        }).filterNotNull().toMap" else "Map.empty"
    },
        |${Spacer(3)}body = serialization.serialize(request.body, typeOf[${content.emit()}]),
        |${Spacer(2)})
        |
        |${Spacer}def fromRequest(serialization: Wirespec.Deserializer[String], request: Wirespec.RawRequest): Request =
        |${Spacer(2)}Request${emitDeserializedParams(endpoint)}
    """.trimMargin()

    fun Endpoint.Response.emit() = """
        |${Spacer}case class Response${status}(
        |${Spacer(2)}override val body: ${content.emit()}
        |) extends Response${status[0]}XX[${content.emit()}] {
        |${Spacer(2)}override val status = ${status.fixStatus()}
        |${Spacer(2)}override val headers = Headers
        |${Spacer(2)}case object Headers extends Wirespec.Response.Headers
        |${Spacer}}
    """.trimMargin()

    private fun Endpoint.Request.emitConstructor(endpoint: Endpoint) = listOfNotNull(
        endpoint.pathParams.joinToString { Spacer(2) + it.emit() }.orNull(),
        endpoint.queries.joinToString { Spacer(2) + it.emit() }.orNull(),
        endpoint.headers.joinToString { Spacer(2) + it.emit() }.orNull(),
        content?.let { "${Spacer(2)}override val body: ${it.emit()}," }
    ).joinToString(",\n")
        .let { if (it.isBlank()) "object Request extends Wirespec.Request[${content.emit()}] {" else "case class Request(\n$it\n${Spacer}) extends Wirespec.Request[${content.emit()}] {" }

    private fun Endpoint.Request.emitDeserializedParams(endpoint: Endpoint) = listOfNotNull(
        endpoint.indexedPathParams.joinToString { it.emitDeserialized() }.orNull(),
        endpoint.queries.joinToString { it.emitDeserialized("queries") }.orNull(),
        endpoint.headers.joinToString { it.emitDeserialized("headers") }.orNull(),
        content?.let { """${Spacer(3)}body = serialization.deserialize(requireNotNull(request.body) { "body is null" }, typeOf[${it.emit()}]),""" }
    ).joinToString(",\n").let { if (it.isBlank()) "" else "(\n$it\n${Spacer(2)})" }

    private fun Endpoint.Response.emitSerialized() = """
        |${Spacer(3)}case r: Response${status} => Wirespec.RawResponse(
        |${Spacer(4)}statusCode = r.status,
        |${Spacer(4)}headers = Map.empty,
        |${Spacer(4)}body = ${if (content != null) "serialization.serialize(r.body, typeOf[${content.emit()}])" else "null"},
        |${Spacer(3)})
    """.trimMargin()

    private fun Endpoint.Response.emitDeserialized() = """
        |${Spacer(3)}case ${status} => Response${status}(
        |${Spacer(4)}body = serialization.deserialize(requireNotNull(response.body) { "body is null" }, typeOf[${content.emit()}]),
        |${Spacer(3)})
    """.trimMargin()

    private fun Field.emitSerialized(fields: String) =
        """request.$fields.${identifier.emit()}?.let{"${identifier.emit()}" -> serialization.serialize(it, typeOf[${reference.emit()}]).let(List(_))}"""

    private fun IndexedValue<Endpoint.Segment.Param>.emitDeserialized() =
        """${Spacer(3)}${value.identifier.emit()} = serialization.deserialize(request.path(${index}), typeOf[${value.reference.emit()}])"""

    private fun Field.emitDeserialized(fields: String) =
        if (isNullable)
            """${Spacer(3)}${identifier.emit()} = request.$fields.get("${identifier.emit()}").flatMap(_.headOption).map(serialization.deserialize(_, typeOf[${reference.emit()}]))"""
        else
            """${Spacer(3)}${identifier.emit()} = serialization.deserialize(requireNotNull(request.$fields.get("${identifier.emit()}").flatMap(_.headOption)) { "${identifier.emit()} is null" }, typeOf[${reference.emit()}])"""

    private fun Endpoint.Segment.Param.emitIdentifier() = "request.path.${identifier.value}.toString()"

    private fun Endpoint.Content?.emit() = this?.reference?.emit() ?: "Unit"

    private fun Endpoint.Segment.Param.emit() = "${identifier.emit()}: ${reference.emit()}"

    private fun String.brace() = wrap("(", ")")
    private fun String.wrap(prefix: String, postfix: String) = if (isEmpty()) "" else "$prefix$this$postfix"

    private fun Reference.emitWrap(isNullable: Boolean): String = value
        .let { if (isIterable) "List[$it]" else it }
        .let { if (isNullable) "$it?" else it }
        .let { if (isDictionary) "Map[String, $it]" else it }

    private fun String.fixStatus(): String = when (this) {
        "default" -> "200"
        else -> this
    }

    private fun String.sanitizeSymbol() = this
        .split(".", " ")
        .joinToString("") { it.firstToUpper() }
        .asSequence()
        .filter { it.isLetterOrDigit() || it in listOf('_') }
        .joinToString("")
        .sanitizeFirstIsDigit()

    private fun String.sanitizeFirstIsDigit() = if (firstOrNull()?.isDigit() == true) "_${this}" else this

    private fun String.sanitizeEnum() = split("-", ", ", ".", " ", "//").joinToString("_").sanitizeFirstIsDigit()

    private fun String.sanitizeKeywords() = if (this in reservedKeywords) "`$this`" else this

    companion object : Keywords {
        override val reservedKeywords = setOf(
            "abstract", "case", "catch", "class", "def",
            "do", "else", "extends", "false", "final",
            "finally", "for", "forSome", "if", "implicit",
            "import", "lazy", "match", "new", "null",
            "object", "override", "package", "private", "protected",
            "return", "sealed", "super", "this", "throw",
            "trait", "true", "try", "type", "val",
            "var", "while", "with", "yield",
        )
    }
}
