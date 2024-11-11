package community.flock.wirespec.compiler.core.emit

import community.flock.wirespec.compiler.core.emit.common.DefinitionModelEmitter
import community.flock.wirespec.compiler.core.emit.common.Emitted
import community.flock.wirespec.compiler.core.emit.common.Emitter
import community.flock.wirespec.compiler.core.emit.common.Spacer
import community.flock.wirespec.compiler.core.emit.shared.TypeScriptShared
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

open class TypeScriptEmitter(logger: Logger = noLogger) : DefinitionModelEmitter, Emitter(logger) {

    override fun Definition.emitName(): String = when (this) {
        is Endpoint -> emit(identifier)
        is Enum -> emit(identifier)
        is Refined -> emit(identifier)
        is Type -> emit(identifier)
        is Union -> emit(identifier)
        is Channel -> emit(identifier)
    }

    override fun notYetImplemented() =
        """// TODO("Not yet implemented")
            |
        """.trimMargin()

    override fun emit(ast: AST): List<Emitted> =
        super.emit(ast).map {
            Emitted(
                it.typeName.sanitizeSymbol(), """
                    |${if (ast.hasEndpoints()) TypeScriptShared.source else ""}
                    |${it.result}
            """.trimMargin().trimStart()
            )
        }

    override fun emit(type: Type, ast: AST) =
        """export type ${type.identifier.sanitizeSymbol()} = {
            |${type.shape.emit()}
            |}
            |
            |""".trimMargin()

    override fun emit(enum: Enum) =
        "export type ${enum.identifier.sanitizeSymbol()} = ${enum.entries.joinToString(" | ") { """"$it"""" }}\n"

    override fun Type.Shape.emit() = value.joinToString(",\n") { it.emit() }

    internal fun Endpoint.Segment.Param.emit() =
        "${Spacer}\"${emit(identifier)}\": ${reference.emit()}"

    override fun Field.emit() =
        "${Spacer}\"${emit(identifier)}\"${if (isNullable) "?" else ""}: ${reference.emit()}"

    override fun Reference.emit() = when (this) {
        is Reference.Unit -> "void"
        is Reference.Any -> "any"
        is Reference.Custom -> value.sanitizeSymbol()
        is Reference.Primitive -> when (type) {
            Reference.Primitive.Type.String -> "string"
            Reference.Primitive.Type.Integer -> "number"
            Reference.Primitive.Type.Number -> "number"
            Reference.Primitive.Type.Boolean -> "boolean"
        }
    }
        .let { if (isIterable) "$it[]" else it }
        .let { if (isDictionary) "Record<string, $it>" else it }

    override fun emit(refined: Refined) =
        """export type ${refined.identifier.sanitizeSymbol()} = string;
            |const regExp${emit(refined.identifier)} = ${refined.validator.emit()};
            |export const validate${emit(refined.identifier)} = (value: string): value is ${refined.identifier.sanitizeSymbol()} => 
            |${Spacer}regExp${emit(refined.identifier)}.test(value);
            |""".trimMargin()

    override fun Refined.Validator.emit() = value

    override fun emit(endpoint: Endpoint) =
        """
          |export namespace ${endpoint.identifier.sanitizeSymbol()} {
          |${endpoint.pathParams.emitType("Path") { it.emit() }}
          |${endpoint.queries.emitType("Queries") { it.emit() }}
          |${endpoint.headers.emitType("Headers") { it.emit() }}
          |${endpoint.requests.first().emitType(endpoint)}
          |${endpoint.responses.toSet().joinToString("\n") { it.emitType() }}
          |${Spacer}export type Response = ${endpoint.responses.toSet().joinToString(" | ") { it.emitName() }}
          |${endpoint.requests.first().emitFunction(endpoint)}
          |${endpoint.responses.joinToString("\n") { it.emitFunction(endpoint) }}
          |${Spacer}export type Handler = {
          |${Spacer(2)}${emitHandleFunction(endpoint)}
          |${Spacer}}
          |${endpoint.emitClient().prependIndent(Spacer(1))}
          |${endpoint.emitServer().prependIndent(Spacer(1))}
          |}
          |
        """.trimMargin()

    private fun emitHandleFunction(endpoint: Endpoint) =
        "${endpoint.identifier.sanitizeSymbol().firstToLower()}: (request:Request) => Promise<Response>"

    override fun emit(union: Union) =
        "export type ${union.identifier.sanitizeSymbol()} = ${union.entries.joinToString(" | ") { it.emit() }}\n"

    override fun emit(identifier: Identifier) = identifier.value

    override fun emit(channel: Channel) = notYetImplemented()

    private fun <E> List<E>.emitType(name: String, block: (E) -> String) =
        if (isEmpty()) "${Spacer}type $name = {}"
        else
            """
                |${Spacer}type $name = {
                |${joinToString(",\n") { "${Spacer(1)}${block(it)}" }},
                |${Spacer}}
            """.trimMargin()

    private fun Endpoint.Request.emitReference() = content?.reference?.emit() ?: "undefined"

    private fun Endpoint.Request.emitType(endpoint: Endpoint) = """
      |${Spacer}export type Request = { 
      |${Spacer(2)}path: Path
      |${Spacer(2)}method: "${endpoint.method}"
      |${Spacer(2)}queries: Queries
      |${Spacer(2)}headers: Headers
      |${Spacer(2)}body: ${emitReference()}
      |${Spacer}}
    """.trimIndent()

    private fun Endpoint.Request.emitFunction(endpoint: Endpoint) = """
      |${Spacer}export const request = (${paramList(endpoint).takeIf { it.isNotEmpty() }?.let { "props: ${it.joinToObject { it.emit() }}" }.orEmpty()}): Request => ({
      |${Spacer(2)}path: ${endpoint.pathParams.joinToObject { "${emit(it.identifier)}: props.${emit(it.identifier)}" }},
      |${Spacer(2)}method: "${endpoint.method}",
      |${Spacer(2)}queries: ${endpoint.queries.joinToObject { "${emit(it.identifier)}: props.${emit(it.identifier)}" }},
      |${Spacer(2)}headers: ${endpoint.headers.joinToObject { "${emit(it.identifier)}: props.${emit(it.identifier)}" }},
      |${Spacer(2)}body: ${content?.let { "props.body" } ?: "undefined"},
      |${Spacer}})
    """.trimIndent()

    private fun Endpoint.Response.emitFunction(endpoint: Endpoint) = """
      |${Spacer}export const response${status.firstToUpper()} = (${paramList().takeIf { it.isNotEmpty() }?.let { "props: ${it.joinToObject { it.emit() }}" }.orEmpty()}): Response${status.firstToUpper()} => ({
      |${Spacer(2)}status: ${status},
      |${Spacer(2)}headers: ${endpoint.headers.joinToObject { "${emit(it.identifier)}: props.${emit(it.identifier)}" }},
      |${Spacer(2)}body: ${content?.let { "props.body" } ?: "undefined"},
      |${Spacer}})
    """.trimIndent()

    private fun <T> Iterable<T>.joinToObject(transform: ((T) -> CharSequence)) =
        joinToString(", ", "{", "}", transform = transform)

    private fun Param.emit() = "${emit(identifier)}${if (isNullable) "?" else ""}: ${reference.emit()}"

    private fun Endpoint.Response.emitName() = "Response" + status.firstToUpper()

    private fun Endpoint.Response.emitReference() = content?.reference?.emit() ?: "undefined"

    private fun Endpoint.Response.emitType() = """
      |${Spacer}export type ${emitName()} = { 
      |${Spacer(2)}status: $status
      |${Spacer(2)}headers: {${headers.joinToString { it.emit() }}}
      |${Spacer(2)}body: ${emitReference()}
      |${Spacer}}
    """.trimIndent()

    private fun Identifier.sanitizeSymbol() = value.sanitizeSymbol()

    private fun String.sanitizeSymbol() = asSequence()
        .filter { it.isLetterOrDigit() || it in listOf('_') }
        .joinToString("")

    private fun Endpoint.emitClient() = """
        |export const client: Wirespec.Client<Request, Response> = (serialization: Wirespec.Serialization) => ({
        |${emitClientTo().prependIndent(Spacer(1))},
        |${emitClientFrom().prependIndent(Spacer(1))}
        |})
    """.trimMargin()

    private fun Endpoint.emitPathArray() = path.joinToString(", ", "[", "]") {
        when (it) {
            is Endpoint.Segment.Literal -> """"${it.value}""""
            is Endpoint.Segment.Param -> "serialization.serialize(request.path.${emit(it.identifier)})"
        }
    }

    private fun Endpoint.emitClientTo() = """
        |to: (request) => ({
        |${Spacer(1)}method: "${method.name.uppercase()}",
        |${Spacer(1)}path: ${emitPathArray()},
        |${Spacer(1)}queries: {${queries.joinToString { it.emitSerialize("queries") }}},
        |${Spacer(1)}headers: {${headers.joinToString { it.emitSerialize("headers") }}},
        |${Spacer(1)}body: serialization.serialize(request.body)
        |})
    """.trimMargin()

    private fun Endpoint.emitClientFrom() = """
        |from: (response) => {
        |${Spacer(1)}switch (response.status) {
        |${responses.joinToString("\n") { it.emitClientFromResponse() }.prependIndent(Spacer(2))}
        |${Spacer(2)}default:
        |${Spacer(3)}throw new Error(`Cannot internalize response with status: ${'$'}{response.status}`);
        |${Spacer(1)}}
        |}
    """.trimMargin()

    private fun Endpoint.Response.emitClientFromResponse() = """
        |case ${status}:
        |${Spacer(1)}return {
        |${Spacer(2)}status: ${status},
        |${Spacer(2)}headers: {},
        |${Spacer(2)}body: serialization.deserialize<${emitReference()}>(response.body)
        |${Spacer(1)}};
    """.trimMargin()

    private fun Endpoint.emitServer() = """
        |export const server:Wirespec.Server<Request, Response> = (serialization: Wirespec.Serialization) => ({
        |${emitServerFrom().prependIndent(Spacer(1))},
        |${emitServerTo().prependIndent(Spacer(1))}
        |})
    """.trimMargin()

    private fun Endpoint.emitServerFrom() = """
        |from: (request) => {
        |${Spacer(1)}return {
        |${Spacer(2)}method: "${method.name.uppercase()}",
        |${Spacer(2)}path: { 
        |${indexedPathParams.joinToString(",") { it.emitDeserialize() }.prependIndent(Spacer(3))}
        |${Spacer(2)}},
        |${Spacer(2)}queries: {
        |${queries.joinToString(",") { it.emitDeserialize("queries").prependIndent(Spacer(3)) }}
        |${Spacer(2)}},
        |${Spacer(2)}headers: {
        |${headers.joinToString(",") { it.emitDeserialize("headers").prependIndent(Spacer(3)) }}
        |${Spacer(2)}},
        |${Spacer(2)}body: serialization.deserialize(request.body)
        |${Spacer(1)}}
        |}
    """.trimMargin()

    private fun emitServerTo() = """
        |to: (response) => ({
        |${Spacer(1)}status: response.status,
        |${Spacer(1)}headers: {},
        |${Spacer(1)}body: serialization.serialize(response.body),
        |})
    """.trimMargin()

    private fun IndexedValue<Endpoint.Segment.Param>.emitDeserialize() =
        """${emit(value.identifier)}: serialization.deserialize(request.path[${index}])"""

    private fun Field.emitDeserialize(fields: String) =
        """${emit(identifier)}: serialization.deserialize(request.$fields.${emit(identifier)})"""

    private fun Field.emitSerialize(fields: String) =
        """${emit(identifier)}: serialization.serialize(request.$fields.${emit(identifier)})"""
}
