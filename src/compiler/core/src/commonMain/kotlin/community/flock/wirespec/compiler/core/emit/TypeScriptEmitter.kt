package community.flock.wirespec.compiler.core.emit

import community.flock.wirespec.compiler.core.emit.common.AbstractEmitter
import community.flock.wirespec.compiler.core.parse.AST
import community.flock.wirespec.compiler.core.parse.nodes.Endpoint
import community.flock.wirespec.compiler.core.parse.nodes.Enum
import community.flock.wirespec.compiler.core.parse.nodes.Refined
import community.flock.wirespec.compiler.core.parse.nodes.Type
import community.flock.wirespec.compiler.utils.Logger
import community.flock.wirespec.compiler.utils.noLogger

class TypeScriptEmitter(logger: Logger = noLogger) : AbstractEmitter(logger) {

    override val shared = ""

    private val endpointBase = """
        |export module Wirespec {
        |${SPACER}export type Method = "GET" | "PUT" | "POST" | "DELETE" | "OPTIONS" | "HEAD" | "PATCH" | "TRACE"
        |${SPACER}export type Content<T> = { type:string, body:T }
        |${SPACER}export type Request<T> = { path:string, method: Method, query?: Record<string, any[]>, headers?: Record<string, any[]>, content?:Content<T> }
        |${SPACER}export type Response<T> = { status:number, headers?: Record<string, any[]>, content?:Content<T> }
        |}
    """.trimMargin()

    override fun emit(ast: AST): List<Pair<String, String>> =
        super.emit(ast).map { (name, result) ->
            name.sanitizeSymbol() to """
                    |${if (ast.hasEndpoints()) endpointBase else ""}
                    |${result}
            """.trimMargin().trimStart()
        }

    override fun Type.emit() = withLogging(logger) {
        """export type ${name.sanitizeSymbol()} = {
            |${shape.emit()}
            |}
            |
            |""".trimMargin()
    }

    override fun Enum.emit() = withLogging(logger) { "type ${name.sanitizeSymbol()} = ${entries.joinToString(" | ") { """"$it"""" }}\n" }

    override fun Type.Shape.emit() = withLogging(logger) {
        value.joinToString(",\n") { it.emit() }
    }

    override fun Type.Shape.Field.emit() = withLogging(logger) {
        "${SPACER}${identifier.emit()}${if (isNullable) "?" else ""}: ${reference.emit()}"
    }

    override fun Type.Shape.Field.Identifier.emit() = withLogging(logger) { value }


    private fun Type.Shape.Field.Reference.emitSymbol() = withLogging(logger) {
        when (this) {
            is Type.Shape.Field.Reference.Unit -> "void"
            is Type.Shape.Field.Reference.Any -> "any"
            is Type.Shape.Field.Reference.Custom -> value.sanitizeSymbol()
            is Type.Shape.Field.Reference.Primitive -> when (type) {
                Type.Shape.Field.Reference.Primitive.Type.String -> "string"
                Type.Shape.Field.Reference.Primitive.Type.Integer -> "number"
                Type.Shape.Field.Reference.Primitive.Type.Number -> "number"
                Type.Shape.Field.Reference.Primitive.Type.Boolean -> "boolean"
            }
        }
    }

    override fun Type.Shape.Field.Reference.emit() = withLogging(logger) {
        emitSymbol()
            .let { if (isIterable) "$it[]" else it }
            .let { if (isMap) "Record<string, $it>" else it }
    }

    override fun Refined.emit() = withLogging(logger) {
        """type ${name.sanitizeSymbol()} = {
            |${SPACER}value: string
            |}
            |const validate$name = (type: $name) => (${validator.emit()}).test(type.value);
            |
            |""".trimMargin()
    }

    override fun Refined.Validator.emit() = withLogging(logger) {
        "new RegExp('${value.drop(1).dropLast(1)}')"
    }

    override fun Endpoint.emit() = withLogging(logger) {
        """
          |export module ${name.sanitizeSymbol()} {
          |${SPACER}export const PATH = "/${path.joinToString ("/"){ when (it) {is Endpoint.Segment.Literal -> it.value; is Endpoint.Segment.Param -> ":${it.identifier.value}" } }}"
          |${SPACER}export const METHOD = "${method.name}"
          |${requests.toSet().joinToString("\n") { "${SPACER}type ${it.emitName()} = { path: ${path.emitType()}, method: \"${method}\", headers: {${headers.map { it.emit() }.joinToString(",")}}, query: {${query.map { it.emit() }.joinToString(",")}}${it.content?.let { ", content: { type: \"${it.type}\", body: ${it.reference.emit()} }" } ?: ""} } " }}
          |${SPACER}export type Request = ${requests.toSet().joinToString(" | ") { it.emitName() }}
          |${responses.toSet().joinToString("\n") { "${SPACER}type ${it.emitName()} = { status: ${if (it.status.isInt()) it.status else "string"}${it.content?.let { ", content: { type: \"${it.type}\", body: ${it.reference.emit()} }" } ?: ""} }" }}
          |${SPACER}export type Response = ${responses.toSet().joinToString(" | ") { it.emitName() }}
          |${SPACER}export type Handler = (request:Request) => Promise<Response>
          |${SPACER}export type Call = {
          |${SPACER}${SPACER}${name.firstToLower()}: Handler
          |${SPACER}}
          |${requests.joinToString("\n") { "${SPACER}export const ${it.emitName().firstToLower()} = (${joinParameters(it.content, null).takeIf { it.isNotEmpty() }?.joinToString(",", "obj:{", "}") { it.emit() }.orEmpty()}) => ({path: `${path.emitPath()}`, method: \"${method.name}\", query: {${query.emitMap()}}, headers: {${headers.emitMap()}}${it.content?.let { ", content: {type: \"${it.type}\", body: obj.body}" } ?: ""}} as const)" }}
          |${responses.joinToString("\n") { "${SPACER}export const ${it.emitName().firstToLower()} = (${joinParameters(it.content, it).takeIf { it.isNotEmpty() }?.joinToString(",", "obj:{", "}") { it.emit() }.orEmpty()}) => ({status: ${if(it.status.isInt()) it.status else "`${it.status}`"}, headers: {${it.headers.emitMap()}}${it.content?.let { ", content: {type: \"${it.type}\", body: obj.body}" } ?: ""}} as const)" }}
          |}
          |
        """.trimMargin()
    }

    private fun List<Endpoint.Segment>.emitType() = "`${joinToString("") { "/" + it.emitType() }}`"
    private fun Endpoint.Segment.emitType() = withLogging(logger) {
        when (this) {
            is Endpoint.Segment.Literal -> value
            is Endpoint.Segment.Param -> "${"$"}{${reference.emit()}}"
        }
    }

    private fun Endpoint.Request.emitName() = "Request" + (content?.emitContentType() ?: "Undefined")
    private fun Endpoint.Response.emitName() = "Response" + status.firstToUpper() + (content?.emitContentType() ?: "Undefined")

    private fun List<Type.Shape.Field>.emitMap() = joinToString(", ") { "\"${it.identifier.emit()}\": obj.${it.identifier.emit().sanitizeSymbol().firstToLower()}" }

    private fun List<Endpoint.Segment>.emitPath() = "/" + joinToString("/") { it.emit() }
    private fun Endpoint.Segment.emit(): String = withLogging(logger) {
        when (this) {
            is Endpoint.Segment.Literal -> value
            is Endpoint.Segment.Param -> "\${obj.${identifier.value}}"
        }
    }

    private fun Endpoint.joinParameters(content: Endpoint.Content? = null, response: Endpoint.Response?): List<Type.Shape.Field> {
        val pathField = path
            .filterIsInstance<Endpoint.Segment.Param>()
            .map { Type.Shape.Field(it.identifier, it.reference, false) }
        val parameters = response?.headers ?: (pathField + query + headers + cookies)
        return parameters
            .plus(content?.reference?.toField("body", false))
            .filterNotNull()
            .map { it.copy(identifier = Type.Shape.Field.Identifier(it.identifier.value.sanitizeSymbol().firstToLower())) }
    }

    private fun String.sanitizeSymbol() = this
        .replace("-", "")
        .replace(".", "")
        .replace(" ", "_")

}
