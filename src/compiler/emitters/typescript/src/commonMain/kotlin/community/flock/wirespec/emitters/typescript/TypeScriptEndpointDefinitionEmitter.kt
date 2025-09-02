package community.flock.wirespec.emitters.typescript

import community.flock.wirespec.compiler.core.emit.LanguageEmitter.Companion.firstToLower
import community.flock.wirespec.compiler.core.emit.LanguageEmitter.Companion.firstToUpper
import community.flock.wirespec.compiler.core.emit.EndpointDefinitionEmitter
import community.flock.wirespec.compiler.core.emit.Param
import community.flock.wirespec.compiler.core.emit.Spacer
import community.flock.wirespec.compiler.core.emit.distinctByStatus
import community.flock.wirespec.compiler.core.emit.fixStatus
import community.flock.wirespec.compiler.core.emit.importReferences
import community.flock.wirespec.compiler.core.emit.indexedPathParams
import community.flock.wirespec.compiler.core.emit.paramList
import community.flock.wirespec.compiler.core.emit.pathParams
import community.flock.wirespec.compiler.core.parse.Endpoint
import community.flock.wirespec.compiler.core.parse.Field

interface TypeScriptEndpointDefinitionEmitter: EndpointDefinitionEmitter, TypeScriptTypeDefinitionEmitter {
    override fun emit(endpoint: Endpoint) =
        """
          |${endpoint.importReferences().distinctBy { it.value }.map { "import {type ${it.value}} from '../model'" }.joinToString("\n") { it.trimStart() }}
          |export namespace ${endpoint.identifier.sanitizeSymbol()} {
          |${endpoint.pathParams.emitType("Path") { it.emit() }}
          |${endpoint.queries.emitType("Queries") { it.emit() }}
          |${endpoint.headers.emitType("Headers") { it.emit() }}
          |${endpoint.requests.first().emitType(endpoint)}
          |${endpoint.responses.distinctByStatus().joinToString("\n") { it.emitType() }}
          |${Spacer}export type Response = ${endpoint.responses.distinctByStatus().joinToString(" | ") { it.emitName() }}
          |${endpoint.requests.first().emitFunction(endpoint)}
          |${endpoint.responses.distinctByStatus().joinToString("\n") { it.emitFunction() }}
          |${Spacer}export type Handler = {
          |${Spacer(2)}${emitHandleFunction(endpoint)}
          |${Spacer}}
          |${endpoint.emitClient().prependIndent(Spacer(1))}
          |${endpoint.emitServer().prependIndent(Spacer(1))}
          |${Spacer}export const api = {
          |${Spacer(2)}name: "${endpoint.identifier.sanitizeSymbol().firstToLower()}",
          |${Spacer(2)}method: "${endpoint.method.name}",
          |${Spacer(2)}path: "${endpoint.path.joinToString("/") { it.emit() }}",
          |${Spacer(2)}server,
          |${Spacer(2)}client
          |${Spacer}} as const
          |}
          |
        """.trimMargin()

    private fun Endpoint.Segment.emit() =
        when (this) {
            is Endpoint.Segment.Literal -> value
            is Endpoint.Segment.Param -> ":${identifier.value}"
        }

    private fun Endpoint.emitClient() = """
        |export const client: Wirespec.Client<Request, Response> = (serialization: Wirespec.Serialization) => ({
        |${emitClientTo().prependIndent(Spacer(1))},
        |${emitClientFrom().prependIndent(Spacer(1))}
        |})
    """.trimMargin()

    private fun Endpoint.emitClientTo() = """
        |to: (it) => ({
        |${Spacer(1)}method: "${method.name.uppercase()}",
        |${Spacer(1)}path: ${emitPathArray()},
        |${Spacer(1)}queries: {${queries.joinToString { it.emitSerialize("queries") }}},
        |${Spacer(1)}headers: {${headers.joinToString { it.emitSerialize("headers") }}},
        |${Spacer(1)}body: serialization.serialize(it.body)
        |})
    """.trimMargin()

    private fun Endpoint.emitClientFrom() = """
        |from: (it) => {
        |${Spacer(1)}switch (it.status) {
        |${responses.distinctByStatus().joinToString("\n") { it.emitClientFromResponse() }.prependIndent(Spacer(2))}
        |${Spacer(2)}default:
        |${Spacer(3)}throw new Error(`Cannot internalize response with status: ${'$'}{it.status}`);
        |${Spacer(1)}}
        |}
    """.trimMargin()

    private fun Endpoint.Response.emitClientFromResponse() = """
        |case ${status.fixStatus()}:
        |${Spacer(1)}return {
        |${Spacer(2)}status: ${status.fixStatus()},
        |${Spacer(2)}headers: {${headers.joinToString { it.emitDeserialize("headers") }}},
        |${Spacer(2)}body: serialization.deserialize<${emitReference()}>(it.body)
        |${Spacer(1)}};
    """.trimMargin()

    private fun Endpoint.emitServer() = """
        |export const server:Wirespec.Server<Request, Response> = (serialization: Wirespec.Serialization) => ({
        |${emitServerFrom().prependIndent(Spacer(1))},
        |${emitServerTo().prependIndent(Spacer(1))}
        |})
    """.trimMargin()

    private fun Endpoint.emitServerFrom() = """
        |from: (it) => {
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
        |${Spacer(2)}body: serialization.deserialize(it.body)
        |${Spacer(1)}}
        |}
    """.trimMargin()

    private fun Endpoint.emitServerTo() = """
        |to: (it) => {
        |${Spacer(1)}switch (it.status) {
        |${responses.distinctByStatus().joinToString("\n") { it.emitServerToResponse() }.prependIndent(Spacer(2))}
        |${Spacer(1)}}
        |}
    """.trimMargin()

    private fun Endpoint.Response.emitServerToResponse() = """
        |case ${status.fixStatus()}:
        |${Spacer(1)}return {
        |${Spacer(2)}status: ${status.fixStatus()},
        |${Spacer(2)}headers: {${headers.joinToString { it.emitSerialize("headers") }}},
        |${Spacer(2)}body: serialization.serialize(it.body),
        |${Spacer(1)}};
    """.trimMargin()

    private fun <E> List<E>.emitType(name: String, block: (E) -> String) =
        if (isEmpty()) "${Spacer}type $name = {}"
        else
            """
                |${Spacer}type $name = {
                |${joinToString(",\n") { "${Spacer(1)}${block(it)}" }},
                |${Spacer}}
            """.trimMargin()

    private fun Endpoint.Response.emitName() = "Response" + status.firstToUpper()

    private fun Endpoint.Request.emitFunction(endpoint: Endpoint) = """
      |${Spacer}export type RequestParams = ${paramList(endpoint).joinToObject { it.emit() }}
      |${Spacer}export const request = (${
        paramList(endpoint).takeIf { it.isNotEmpty() }?.run { "params: RequestParams" }.orEmpty()
    }): Request => ({
      |${Spacer(2)}path: ${endpoint.pathParams.joinToObject { "${emit(it.identifier)}: params[${emit(it.identifier)}]" }},
      |${Spacer(2)}method: "${endpoint.method}",
      |${Spacer(2)}queries: ${endpoint.queries.joinToObject { "${emit(it.identifier)}: params[${emit(it.identifier)}]" }},
      |${Spacer(2)}headers: ${endpoint.headers.joinToObject { "${emit(it.identifier)}: params[${emit(it.identifier)}]" }},
      |${Spacer(2)}body: ${content?.let { "params.body" } ?: "undefined"},
      |${Spacer}})
    """.trimIndent()

    private fun Endpoint.Response.emitFunction() = """
      |${Spacer}export type Response${status.firstToUpper()}Params = ${paramList().joinToObject { it.emit() }}
      |${Spacer}export const response${status.firstToUpper()} = (${
        paramList().takeIf { it.isNotEmpty() }?.run { "params: Response${status.firstToUpper()}Params" }.orEmpty()
    }): Response${status.firstToUpper()} => ({
      |${Spacer(2)}status: ${status.fixStatus()},
      |${Spacer(2)}headers: ${headers.joinToObject { "${emit(it.identifier)}: params[${emit(it.identifier)}]" }},
      |${Spacer(2)}body: ${content?.let { "params.body" } ?: "undefined"},
      |${Spacer}})
    """.trimIndent()

    private fun Endpoint.Response.emitReference() = content?.reference?.emit() ?: "undefined"

    private fun Endpoint.emitPathArray() = path.joinToString(", ", "[", "]") {
        when (it) {
            is Endpoint.Segment.Literal -> """"${it.value}""""
            is Endpoint.Segment.Param -> "serialization.serialize(it.path[${emit(it.identifier)}])"
        }
    }

    private fun Endpoint.Response.emitType() = """
      |${Spacer}export type ${emitName()} = {
      |${Spacer(2)}status: ${status.fixStatus()}
      |${Spacer(2)}headers: {${headers.joinToString { "${emit(it.identifier)}: ${it.reference.emit()}" }}}
      |${Spacer(2)}body: ${emitReference()}
      |${Spacer}}
    """.trimIndent()

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

    private fun Endpoint.Segment.Param.emit() =
        "${Spacer}${emit(identifier)}: ${reference.emit()}"

    private fun Param.emit() =
        "${emit(identifier)}${if (reference.isNullable) "?" else ""}: ${reference.copy(isNullable = false).emit()}"

    private fun IndexedValue<Endpoint.Segment.Param>.emitDeserialize() =
        """${emit(value.identifier)}: serialization.deserialize(it.path[${index}])"""

    private fun Field.emitDeserialize(fields: String) =
        """${emit(identifier)}: serialization.deserialize(it.$fields[${emit(identifier)}])"""

    private fun Field.emitSerialize(fields: String) =
        """${emit(identifier)}: serialization.serialize(it.$fields[${emit(identifier)}])"""

    private fun <T> Iterable<T>.joinToObject(transform: ((T) -> CharSequence)) =
        joinToString(", ", "{", "}", transform = transform)

    private fun emitHandleFunction(endpoint: Endpoint) =
        "${endpoint.identifier.sanitizeSymbol().firstToLower()}: (request:Request) => Promise<Response>"

}
