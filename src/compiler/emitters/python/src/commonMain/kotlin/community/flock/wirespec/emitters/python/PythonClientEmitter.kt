package community.flock.wirespec.emitters.python

import arrow.core.NonEmptyList
import community.flock.wirespec.compiler.core.emit.BaseEmitter
import community.flock.wirespec.compiler.core.emit.ClientEmitter
import community.flock.wirespec.compiler.core.emit.Emitted
import community.flock.wirespec.compiler.core.emit.Emitter.Companion.firstToLower
import community.flock.wirespec.compiler.core.emit.ImportEmitter
import community.flock.wirespec.compiler.core.emit.PackageNameEmitter
import community.flock.wirespec.compiler.core.emit.ParamEmitter
import community.flock.wirespec.compiler.core.emit.SpaceEmitter
import community.flock.wirespec.compiler.core.parse.AST
import community.flock.wirespec.compiler.core.parse.Endpoint
import community.flock.wirespec.compiler.core.parse.Module

interface PythonClientEmitter: BaseEmitter, ClientEmitter,PackageNameEmitter, ParamEmitter, SpaceEmitter, ImportEmitter, PythonTypeDefinitionEmitter {

    override fun emitClient(ast: AST): Emitted = Emitted("${packageName.toDir()}/client.${extension.value}", """
        |from . import endpoint
        |
        |from typing import List, Optional
        |
        |${ast.modules.flatMap { it.statements }.toList().flatMap { it.importReferences() }.distinctBy { it.value }.joinToString("\n") { "from .model.${it.value} import ${it.value}" }}
        |
        |class Client():
        |
        |  def __init__(self, serialization, handler):
        |    self.serialization = serialization
        |    self.handler = handler
        |
        |${ast.emitClientEndpointRequest().joinToString("\n") {(endpoint, request) -> emitFunction(endpoint, request) }.spacer(1)}
        |
    """.trimMargin())

    fun Endpoint.Request.emitClientInterface(endpoint: Endpoint) =
        this.paramList(endpoint).joinToString(", ") { "${emit(it.identifier)}: ${it.reference.emit()}" }

    fun emitFunction(endpoint: Endpoint, request: Endpoint.Request) = """
        |def ${emit(endpoint.identifier).firstToLower()}(self, ${request.emitClientInterface(endpoint)}):
        |   req = endpoint.${emit(endpoint.identifier)}.Request${request.paramList(endpoint).takeIf { it.size > 0 }?.joinToString(", ", "(", ")") { emit(it.identifier) }.orEmpty()}
        |   raw_req = endpoint.${emit(endpoint.identifier)}.Convert.to_raw_request(self.serialization, req)
        |   raw_res = self.handler(raw_req)
        |   return endpoint.${emit(endpoint.identifier)}.Convert.from_raw_response(self.serialization, raw_res)
    """.trimMargin()

}
