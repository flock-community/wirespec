package community.flock.wirespec.compiler.core.emit

import community.flock.wirespec.compiler.core.emit.common.DefinitionModelEmitter
import community.flock.wirespec.compiler.core.emit.common.Emitted
import community.flock.wirespec.compiler.core.emit.common.Emitter
import community.flock.wirespec.compiler.core.parse.AST
import community.flock.wirespec.compiler.core.parse.Definition
import community.flock.wirespec.compiler.core.parse.Endpoint
import community.flock.wirespec.compiler.core.parse.Enum
import community.flock.wirespec.compiler.core.parse.Refined
import community.flock.wirespec.compiler.core.parse.Type
import community.flock.wirespec.compiler.core.parse.Union
import community.flock.wirespec.compiler.utils.Logger
import community.flock.wirespec.compiler.utils.noLogger

class TypeScriptEmitter(logger: Logger = noLogger) : DefinitionModelEmitter, Emitter(logger) {

    private val endpointBase = """
        |export module Wirespec {
        |${SPACER}export type Method = "GET" | "PUT" | "POST" | "DELETE" | "OPTIONS" | "HEAD" | "PATCH" | "TRACE"
        |${SPACER}export type Content<T> = { type:string, body:T }
        |${SPACER}export type Request<T> = { path:string, method: Method, query?: Record<string, any[]>, headers?: Record<string, any[]>, content?:Content<T> }
        |${SPACER}export type Response<T> = { status:number, headers?: Record<string, any[]>, content?:Content<T> }
        |}
    """.trimMargin()

    override fun Definition.emitName(): String = when (this) {
        is Endpoint -> name
        is Enum -> name
        is Refined -> name
        is Type -> name
        is Union -> name
    }

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
        """export type ${name.sanitizeSymbol()} = {
            |${shape.emit()}
            |}
            |
            |""".trimMargin()

    override fun Enum.emit() = "export type ${name.sanitizeSymbol()} = ${entries.joinToString(" | ") { """"$it"""" }}\n"

    override fun Type.Shape.emit() = value.joinToString(",\n") { it.emit() }

    override fun Type.Shape.Field.emit() =
        "${SPACER}\"${identifier.emit()}\"${if (isNullable) "?" else ""}: ${reference.emit()}"

    override fun Type.Shape.Field.Identifier.emit() = value


    private fun Type.Shape.Field.Reference.emitSymbol() = when (this) {
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

    override fun Type.Shape.Field.Reference.emit() = emitSymbol()
        .let { if (isIterable) "$it[]" else it }
        .let { if (isMap) "Record<string, $it>" else it }

    override fun Refined.emit() =
        """export type ${name.sanitizeSymbol()} = string;
            |const regExp$name = ${validator.emit()};
            |export const validate$name = (value: string): value is ${name.sanitizeSymbol()} => 
            |${SPACER}regExp$name.test(value);
            |""".trimMargin()

    override fun Refined.Validator.emit() = "/${value.drop(1).dropLast(1)}/g"

    override fun Endpoint.emit() =
        """
          |export module ${name.sanitizeSymbol()} {
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
          |${SPACER}${SPACER}${name.sanitizeSymbol().firstToLower()}: Handler
          |${SPACER}}
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

    override fun Union.emit() = "export type ${name.sanitizeSymbol()} = ${entries.joinToString(" | ") { it.emit() }}\n"

    private fun List<Endpoint.Segment>.emitType() = "`${joinToString("") { "/" + it.emitType() }}`"

    private fun Endpoint.Segment.emitType() = when (this) {
        is Endpoint.Segment.Literal -> value
        is Endpoint.Segment.Param -> "${"$"}{${reference.emit()}}"
    }

    private fun Endpoint.Request.emitName() = "Request" + (content?.emitContentType() ?: "Undefined")
    private fun Endpoint.Response.emitName() =
        "Response" + status.firstToUpper() + (content?.emitContentType() ?: "Undefined")

    private fun List<Type.Shape.Field>.emitMap() = joinToString(", ") {
        "\"${it.identifier.emit()}\": props.${
            it.identifier.emit().sanitizeSymbol().firstToLower()
        }"
    }

    private fun List<Endpoint.Segment>.emitPath() = "/" + joinToString("/") { it.emit() }
    private fun Endpoint.Segment.emit(): String = when (this) {
        is Endpoint.Segment.Literal -> value
        is Endpoint.Segment.Param -> "\${props.${identifier.value.sanitizeSymbol().firstToLower()}}"
    }

    private fun Endpoint.joinParameters(
        content: Endpoint.Content? = null,
        response: Endpoint.Response?
    ): List<Type.Shape.Field> {
        val pathField = path
            .filterIsInstance<Endpoint.Segment.Param>()
            .map { Type.Shape.Field(it.identifier, it.reference, false) }
        val parameters = response?.headers ?: (pathField + query + headers + cookies)
        return parameters
            .plus(content?.reference?.toField("body", false))
            .filterNotNull()
            .map {
                it.copy(
                    identifier = Type.Shape.Field.Identifier(
                        it.identifier.value.sanitizeSymbol().firstToLower()
                    )
                )
            }
    }

    private fun String.sanitizeSymbol() = this
        .asSequence()
        .filter { it.isLetterOrDigit() || listOf('_').contains(it) }
        .joinToString("")

    private fun Type.Shape.Field.Reference.toField(identifier: String, isNullable: Boolean) = Type.Shape.Field(
        Type.Shape.Field.Identifier(identifier),
        this,
        isNullable
    )


}
