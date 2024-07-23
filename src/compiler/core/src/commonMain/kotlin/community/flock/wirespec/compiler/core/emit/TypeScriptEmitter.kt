package community.flock.wirespec.compiler.core.emit

import community.flock.wirespec.compiler.core.emit.common.DefinitionModelEmitter
import community.flock.wirespec.compiler.core.emit.common.Emitted
import community.flock.wirespec.compiler.core.emit.common.Emitter
import community.flock.wirespec.compiler.core.parse.AST
import community.flock.wirespec.compiler.core.parse.Channel
import community.flock.wirespec.compiler.core.parse.Definition
import community.flock.wirespec.compiler.core.parse.Endpoint
import community.flock.wirespec.compiler.core.parse.Enum
import community.flock.wirespec.compiler.core.parse.Field
import community.flock.wirespec.compiler.core.parse.Identifier
import community.flock.wirespec.compiler.core.parse.Refined
import community.flock.wirespec.compiler.core.parse.Type
import community.flock.wirespec.compiler.core.parse.Union
import community.flock.wirespec.compiler.utils.Logger
import community.flock.wirespec.compiler.utils.noLogger

open class TypeScriptEmitter(logger: Logger = noLogger) : DefinitionModelEmitter, Emitter(logger) {

    private val endpointBase = """
        |export module Wirespec {
        |${SPACER}export type Method = "GET" | "PUT" | "POST" | "DELETE" | "OPTIONS" | "HEAD" | "PATCH" | "TRACE"
        |${SPACER}export type Content<T> = { type:string, body:T }
        |${SPACER}export type Request<T> = { path:string, method: Method, query?: Record<string, any[]>, headers?: Record<string, any[]>, content?:Content<T> }
        |${SPACER}export type Response<T> = { status:number, headers?: Record<string, any[]>, content?:Content<T> }
        |}
    """.trimMargin()

    override fun Definition.emitName(): String = when (this) {
        is Endpoint -> identifier.emit()
        is Enum -> identifier.emit()
        is Refined -> identifier.emit()
        is Type -> identifier.emit()
        is Union -> identifier.emit()
        is Channel -> identifier.emit()
    }

    override fun notYetImplemented() =
        """// TODO("Not yet implemented")
            |
        """.trimMargin()

    override fun emit(ast: AST): List<Emitted> =
        super.emit(ast).map {
            Emitted(
                it.typeName.sanitizeSymbol(), """
                    |${if (ast.hasEndpoints()) endpointBase else ""}
                    |${it.result}
            """.trimMargin().trimStart()
            )
        }

    override fun Type.emit(ast: AST) =
        """export type ${identifier.sanitizeSymbol()} = {
            |${shape.emit()}
            |}
            |
            |""".trimMargin()

    override fun Enum.emit() =
        "export type ${identifier.sanitizeSymbol()} = ${entries.joinToString(" | ") { """"$it"""" }}\n"

    override fun Type.Shape.emit() = value.joinToString(",\n") { it.emit() }

    override fun Field.emit() =
        "${SPACER}\"${identifier.emit()}\"${if (isNullable) "?" else ""}: ${reference.emit()}"

    private fun Field.Reference.emitSymbol() = when (this) {
        is Field.Reference.Unit -> "void"
        is Field.Reference.Any -> "any"
        is Field.Reference.Custom -> value.sanitizeSymbol()
        is Field.Reference.Primitive -> when (type) {
            Field.Reference.Primitive.Type.String -> "string"
            Field.Reference.Primitive.Type.Integer -> "number"
            Field.Reference.Primitive.Type.Number -> "number"
            Field.Reference.Primitive.Type.Boolean -> "boolean"
        }
    }

    override fun Field.Reference.emit() = emitSymbol()
        .let { if (isIterable) "$it[]" else it }
        .let { if (isDictionary) "Record<string, $it>" else it }

    override fun Refined.emit() =
        """export type ${identifier.sanitizeSymbol()} = string;
            |const regExp${identifier.emit()} = ${validator.emit()};
            |export const validate${identifier.emit()} = (value: string): value is ${identifier.sanitizeSymbol()} => 
            |${SPACER}regExp${identifier.emit()}.test(value);
            |""".trimMargin()

    override fun Refined.Validator.emit() = "/${value.drop(1).dropLast(1)}/g"

    override fun Endpoint.emit() =
        """
          |export module ${identifier.sanitizeSymbol()} {
          |${SPACER}export const PATH = "/${
            path.joinToString("/") {
                when (it) {
                    is Endpoint.Segment.Literal -> it.value; is Endpoint.Segment.Param -> ":${it.identifier.value}"
                }
            }
        }"
          |${SPACER}export const METHOD = "${method.name}"
          |${
            requests.toSet().joinToString("\n") {
                "${SPACER}type ${it.emitName()} = { path: ${path.emitType()}, method: \"${method}\", headers: {${
                    headers.joinToString(",") { it.emit() }
                }}, query: {${
                    query.map { it.emit() }.joinToString(",")
                }}${it.content?.let { ", content: { type: \"${it.type}\", body: ${it.reference.emit()} }" } ?: ""} } "
            }
        }
          |${SPACER}export type Request = ${
            requests.toSet().joinToString(" | ") { it.emitName() }.ifEmpty { "undefined" }
        }
          |${
            responses.toSet()
                .joinToString("\n") { "${SPACER}type ${it.emitName()} = { status: ${if (it.status.isInt()) it.status else "string"}${it.content?.let { ", content: { type: \"${it.type}\", body: ${it.reference.emit()} }" } ?: ""} }" }
        }
          |${SPACER}export type Response = ${
            responses.toSet().joinToString(" | ") { it.emitName() }.ifEmpty { "undefined" }
        }
          |${SPACER}export type Handler = (request:Request) => Promise<Response>
          |${SPACER}export type Call = {
          |${SPACER}${SPACER}${identifier.sanitizeSymbol().firstToLower()}: Handler
          |${SPACER}}
          |${SPACER}export const call = (handler:Handler) => ({METHOD, PATH, handler})
          |${
            requests.distinct().joinToString("\n") {
                "${SPACER}export const ${it.emitName().firstToLower()} = (${
                    joinParameters(
                        it.content,
                        null
                    ).takeIf { it.isNotEmpty() }?.joinToString(",", "props:{", "}") { it.emit() }.orEmpty()
                }) => ({path: `${path.emitPath()}`, method: \"${method.name}\", query: {${query.emitMap()}}, headers: {${headers.emitMap()}}${it.content?.let { ", content: {type: \"${it.type}\", body: props.body}" } ?: ""}} as const)"
            }
        }
          |${
            responses.distinct().joinToString("\n") {
                "${SPACER}export const ${it.emitName().firstToLower()} = (${
                    joinParameters(
                        it.content,
                        it
                    ).takeIf { it.isNotEmpty() }?.joinToString(",", "props:{", "}") { it.emit() }.orEmpty()
                }) => ({status: ${if (it.status.isInt()) it.status else "`${it.status}`"}, headers: {${it.headers.emitMap()}}${it.content?.let { ", content: {type: \"${it.type}\", body: props.body}" } ?: ""}} as const)"
            }
        }
          |}
          |
        """.trimMargin()

    override fun Union.emit() =
        "export type ${identifier.sanitizeSymbol()} = ${entries.joinToString(" | ") { it.emit() }}\n"

    override fun Channel.emit() = notYetImplemented()

    private fun List<Endpoint.Segment>.emitType() = "`${joinToString("") { "/" + it.emitType() }}`"

    private fun Endpoint.Segment.emitType() = when (this) {
        is Endpoint.Segment.Literal -> value
        is Endpoint.Segment.Param -> "${"$"}{${reference.emit()}}"
    }

    private fun Endpoint.Request.emitName() = "Request" + (content?.emitContentType() ?: "Undefined")
    private fun Endpoint.Response.emitName() =
        "Response" + status.firstToUpper() + (content?.emitContentType() ?: "Undefined")

    private fun Endpoint.Content.emitContentType() = type
        .substringBefore(";")
        .split("/", "-")
        .joinToString("") { it.firstToUpper() }
        .replace("+", "")

    private fun List<Field>.emitMap() = joinToString(", ") {
        "\"${it.identifier.emit()}\": props.${
            it.identifier.sanitizeSymbol().firstToLower()
        }"
    }

    private fun List<Endpoint.Segment>.emitPath() = "/" + joinToString("/") { it.emit() }
    private fun Endpoint.Segment.emit(): String = when (this) {
        is Endpoint.Segment.Literal -> value
        is Endpoint.Segment.Param -> "\${props.${identifier.sanitizeSymbol().firstToLower()}}"
    }

    private fun Endpoint.joinParameters(
        content: Endpoint.Content? = null,
        response: Endpoint.Response?
    ): List<Field> {
        val pathField = path
            .filterIsInstance<Endpoint.Segment.Param>()
            .map { Field(it.identifier, it.reference, false) }
        val parameters = response?.headers ?: (pathField + query + headers + cookies)
        return parameters
            .plus(content?.reference?.toField("body", false))
            .filterNotNull()
            .map {
                it.copy(
                    identifier = Identifier(
                        it.identifier.sanitizeSymbol().firstToLower()
                    )
                )
            }
    }

    private fun Identifier.sanitizeSymbol() = value.sanitizeSymbol()

    private fun String.sanitizeSymbol() = asSequence()
        .filter { it.isLetterOrDigit() || it in listOf('_') }
        .joinToString("")

    private fun Field.Reference.toField(identifier: String, isNullable: Boolean) = Field(
        Identifier(identifier),
        this,
        isNullable
    )


}
