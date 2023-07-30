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
          |${requests.toSet().joinToString("\n") { "${SPACER}type ${it.emitName()} = { path: string, method: \"${method}\", headers: {${headers.map { it.emit() }.joinToString(",")}}, query: {${query.map { it.emit() }.joinToString(",")}}, content: ${it.content?.let { "{ type: \"${it.type}\", body: ${it.reference.emit()} }" } ?: "undefined"} } " }}
          |${SPACER}export type Request = ${requests.toSet().joinToString(" | ") { it.emitName() } }
          |${responses.toSet().joinToString("\n") { "${SPACER}type ${it.emitName() } = { status: ${if(it.status.isInt()) it.status else "number"}, content: ${it.content?.let { "{ type: \"${it.type}\", body: ${it.reference.emit()} }" } ?: "undefined"} }" }}
          |${SPACER}export type Response = ${responses.toSet().joinToString(" | ") { it.emitName() }}
          |${SPACER}export type Call = {
          |${SPACER}${SPACER}${name.firstToLower()}:(request: Request) => Promise<Response>
          |${SPACER}}
          |${SPACER}${requests.joinToString(",\n") { "export const ${it.emitName().firstToLower()} = (${joinParameters(it.content).joinToString(",") { it.emit() }}) => ({path: `${path.emitPath()}`, method: \"${method.name}\", query: {${query.emitMap()}}, headers: {${headers.emitMap()}}, content: ${it.content?.let { "{type: \"${it.type}\", body}" } ?: "undefined"}} as const)" }}
          |}
          |
        """.trimMargin()
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
