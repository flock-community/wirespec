package community.flock.wirespec.emitters.typescript

import community.flock.wirespec.compiler.core.emit.BaseEmitter
import community.flock.wirespec.compiler.core.emit.ClientEmitter
import community.flock.wirespec.compiler.core.emit.Emitted
import community.flock.wirespec.compiler.core.emit.ImportEmitter
import community.flock.wirespec.compiler.core.emit.ParamEmitter
import community.flock.wirespec.compiler.core.emit.SpaceEmitter
import community.flock.wirespec.compiler.core.emit.Spacer
import community.flock.wirespec.compiler.core.parse.AST
import community.flock.wirespec.compiler.core.parse.Endpoint

interface TypeScriptClientEmitter: BaseEmitter, ClientEmitter, ParamEmitter, SpaceEmitter, ImportEmitter, TypeScriptTypeDefinitionEmitter {

    override fun emitClient(ast: AST) = Emitted("client.${extension.value}", """
        |import {Wirespec} from "./Wirespec"
        |
        |${ast.emitClientEndpointRequest().joinToString("\n") { (endpoint) -> "import {${endpoint.identifier.value}} from \"./endpoint/${endpoint.identifier.value}\"" }}
        |
        |${ast.modules.flatMap { it.statements }.toList().flatMap { it.importReferences() }.distinctBy { it.value }.joinToString("\n") { "import {${it.value}} from \"./model/${it.value}\"" }}
        |
        |type RawHandler = (req: Wirespec.RawRequest) => Promise<Wirespec.RawResponse>
        |
        |export const Client = (serialization: Wirespec.Serialization, handler: RawHandler) => ({
        |${ast.emitClientEndpointRequest().joinToString("\n") { (endpoint, request) -> emitFunction(endpoint, request) }.spacer(1)}
        |})
        |
    """.trimMargin())

    private fun Endpoint.Request.emitClientInterface(endpoint: Endpoint) =
        this.paramList(endpoint).joinToString(", ") { "${it.identifier.value}: ${it.reference.emit()}" }

    private fun emitFunction(endpoint: Endpoint, request: Endpoint.Request) = """
        |${endpoint.identifier.value}: async (props: {${request.emitClientInterface(endpoint)}}) => {
        |${Spacer}const req = ${endpoint.identifier.value}.request(${request.paramList(endpoint).takeIf { it.isNotEmpty() }?.let { "props" }.orEmpty()})
        |${Spacer}const rawRequest = ${endpoint.identifier.value}.client(serialization).to(req)
        |${Spacer}const rawResponse = await handler(rawRequest)
        |${Spacer}return ${endpoint.identifier.value}.client(serialization).from(rawResponse)
        |},
    """.trimMargin()

}