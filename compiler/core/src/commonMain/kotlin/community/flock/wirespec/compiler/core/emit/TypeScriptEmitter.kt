package community.flock.wirespec.compiler.core.emit

import community.flock.wirespec.compiler.core.emit.common.Emitter
import community.flock.wirespec.compiler.core.parse.AST
import community.flock.wirespec.compiler.core.parse.Endpoint
import community.flock.wirespec.compiler.core.parse.Enum
import community.flock.wirespec.compiler.core.parse.Refined
import community.flock.wirespec.compiler.core.parse.Type
import community.flock.wirespec.compiler.core.parse.Type.Shape.Field.Reference.Custom
import community.flock.wirespec.compiler.core.parse.Type.Shape.Field.Reference.Primitive
import community.flock.wirespec.compiler.utils.Logger
import community.flock.wirespec.compiler.utils.noLogger

class TypeScriptEmitter(logger: Logger = noLogger) : Emitter(logger) {

    private val endpointBase = """
        |export namespace WirespecShared {
        |${SPACER}export type Method = "GET" | "PUT" | "POST" | "DELETE" | "OPTIONS" | "HEAD" | "PATCH" | "TRACE"
        |${SPACER}export type Content<T> = { type: string, body: T }
        |${SPACER}export type Request<T> = { path: string, method: Method, query: Record<string, any[]>, headers: Record<string, any[]>, content?: Content<T> }
        |${SPACER}export type Response<T> = { status: number, headers: Record<string, any[]>, content?: Content<T> }
        |}
    """.trimMargin()

    override fun emit(ast: AST): List<Pair<String, String>> {
        return super.emit(ast).map { (name, result) ->
            name to """
                    |${if (ast.hasEndpoints()) endpointBase else ""}
                    |${result}
            """.trimMargin().trimStart()
        }
    }
    override fun Type.emit() = withLogging(logger) {
        """interface $name {
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
            is Custom -> value
            is Primitive -> when (type) {
                Primitive.Type.String -> "string"
                Primitive.Type.Integer -> "number"
                Primitive.Type.Boolean -> "boolean"
            }
        }.let { if (isIterable) "$it[]" else it }
    }

    override fun Enum.emit() = withLogging(logger) { "type $name = ${entries.joinToString(" | ") { """"$it"""" }}\n" }

    override fun Refined.emit() = withLogging(logger) {
        """interface $name {
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
          |${requests.toSet().joinToString("\n") { "${SPACER}type ${it.emitName()} = { path: string, method: \"${method}\", headers: {${headers.map { it.emit() }.joinToString(",")}}, query: {${headers.map { it.emit() }.joinToString(",")}}, content: ${it.content?.let { "{ type: \"${it.type}\", body: ${it.reference.emit()} }" } ?: "undefined"} } " }}
          |${SPACER}export type Request = ${requests.toSet().joinToString(" | ") { it.emitName() } }
          |${responses.toSet().joinToString("\n") { "${SPACER}type ${it.emitName() } = { status: ${it.status}, content: ${it.content?.let { "{ type: \"${it.type}\", body: ${it.reference.emit()} }" } ?: "undefined"} }" }}
          |${SPACER}export type Response = ${responses.toSet().joinToString(" | ") { it.emitName() }}
          |${SPACER}export type Call = {
          |${SPACER}${SPACER}${name.firstToLower()}:(request: Request) => Promise<Response>
          |${SPACER}}
          |}
        """.trimMargin()
    }


    private fun Endpoint.Request.emitName() = "Request" + (content?.emitContentType() ?: "unknown")
    private fun Endpoint.Response.emitName() = "Response" + status + (content?.emitContentType() ?: "unknown")
}
