package community.flock.wirespec.compiler.core.emit

import community.flock.wirespec.compiler.core.emit.common.Emitter
import community.flock.wirespec.compiler.core.parse.AST
import community.flock.wirespec.compiler.core.parse.Endpoint
import community.flock.wirespec.compiler.core.parse.Enum
import community.flock.wirespec.compiler.core.parse.Refined
import community.flock.wirespec.compiler.core.parse.Type
import community.flock.wirespec.compiler.core.parse.Type.Shape.Field.Reference
import community.flock.wirespec.compiler.utils.Logger
import community.flock.wirespec.compiler.utils.noLogger

class TypeScriptEmitter(logger: Logger = noLogger) : Emitter(logger) {

    override val shared = ""

    private val endpointBase = """
        |export namespace WirespecShared {
        |${SPACER}type Method = "GET" | "PUT" | "POST" | "DELETE" | "OPTIONS" | "HEAD" | "PATCH" | "TRACE"
        |${SPACER}type Content<T> = { type:string, body:T }
        |${SPACER}export type Request<T> = { path:string, method: Method, query?: Record<string, any[]>, headers?: Record<string, any[]>, content?:Content<T> }
        |${SPACER}export type Response<T> = { status:number, headers?: Record<string, any[]>, content?:Content<T> }
        |}
    """.trimMargin()

    override fun emit(ast: AST): List<Pair<String, String>> =
        super.emit(ast).map { (name, result) ->
            name to """
                    |${if (ast.hasEndpoints()) endpointBase else ""}
                    |${result}
            """.trimMargin().trimStart()
        }

    override fun Type.emit() = withLogging(logger) {
        """export type $name = {
            |${shape.emit()}
            |}
            |
            |""".trimMargin()
    }

    override fun Type.Shape.emit() = withLogging(logger) {
        value.joinToString(",\n") { it.emit() }
    }

    override fun Type.Shape.Field.emit() = withLogging(logger) {
        "${SPACER}${identifier.emit()}${if (isNullable) "?" else ""}: ${reference.emit()}"
    }

    override fun Type.Shape.Field.Identifier.emit() = withLogging(logger) { value }

    override fun Type.Shape.Field.Reference.emit() = withLogging(logger) {
        when (this) {
            is Reference.Any -> "any"
            is Reference.Custom -> value
            is Reference.Primitive -> when (type) {
                Reference.Primitive.Type.String -> "string"
                Reference.Primitive.Type.Integer -> "number"
                Reference.Primitive.Type.Boolean -> "boolean"
            }
        }
            .let { if (isIterable) "$it[]" else it }
            .let { if (isMap) "Record<string, $it>" else it }
    }

    override fun Enum.emit() = withLogging(logger) { "type $name = ${entries.joinToString(" | ") { """"$it"""" }}\n" }

    override fun Refined.emit() = withLogging(logger) {
        """type $name = {
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
          |export namespace ${name} {
          |${requests.toSet().joinToString("\n") { "${SPACER}type ${it.emitName()} = { path: ${path.emitType()}, method: \"${method}\", headers: {${headers.map { it.emit() }.joinToString(",")}}, query: {${query.map { it.emit() }.joinToString(",")}}${it.content?.let { ", content: { type: \"${it.type}\", body: ${it.reference.emit()} }" } ?: ""} } " }}
          |${SPACER}export type Request = ${requests.toSet().joinToString(" | ") { it.emitName() }}
          |${responses.toSet().joinToString("\n") { "${SPACER}type ${it.emitName()} = { status: ${if (it.status.isInt()) it.status else "number"}${it.content?.let { ", content: { type: \"${it.type}\", body: ${it.reference.emit()} }" } ?: ""} }" }}
          |${SPACER}export type Response = ${responses.toSet().joinToString(" | ") { it.emitName() }}
          |${SPACER}export type Call = {
          |${SPACER}${SPACER}${name.firstToLower()}:(request: Request) => Promise<Response>
          |${SPACER}}
          |${SPACER}${requests.joinToString("\n") { "export const ${it.emitName().firstToLower()} = (${joinParameters(it.content).joinToString(",") { it.emit() }}) => ({path: `${path.emitPath()}`, method: \"${method.name}\", query: {${query.emitMap()}}, headers: {${headers.emitMap()}}${it.content?.let { ", content: {type: \"${it.type}\", body}" } ?: ""}} as const)" }}
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

    private fun List<Type.Shape.Field>.emitMap() = joinToString(", ") { "\"${it.identifier.emit()}\": ${it.identifier.emit()}" }

    private fun List<Endpoint.Segment>.emitPath() = "/" + joinToString("/") { it.emit() }
    private fun Endpoint.Segment.emit(): String = withLogging(logger) {
        when (this) {
            is Endpoint.Segment.Literal -> value
            is Endpoint.Segment.Param -> "\${${identifier.value}}"
        }
    }

}
