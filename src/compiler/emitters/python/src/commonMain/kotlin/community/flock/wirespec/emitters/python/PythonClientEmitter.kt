package community.flock.wirespec.emitters.python

import arrow.core.NonEmptyList
import community.flock.wirespec.compiler.core.emit.BaseEmitter
import community.flock.wirespec.compiler.core.emit.ClientEmitter
import community.flock.wirespec.compiler.core.emit.Emitted
import community.flock.wirespec.compiler.core.emit.ImportEmitter
import community.flock.wirespec.compiler.core.emit.PackageNameEmitter
import community.flock.wirespec.compiler.core.emit.ParamEmitter
import community.flock.wirespec.compiler.core.emit.SpaceEmitter
import community.flock.wirespec.compiler.core.parse.AST
import community.flock.wirespec.compiler.core.parse.Endpoint
import community.flock.wirespec.compiler.core.parse.Module

interface PythonClientEmitter: BaseEmitter, ClientEmitter,PackageNameEmitter, ParamEmitter, SpaceEmitter, ImportEmitter, PythonTypeDefinitionEmitter {

    override fun emitClient(ast: AST): Emitted = Emitted("Client.${extension.value}", """
        |from . import endpoint
        |
        |from typing import List, Optional
        |
        |${ast.modules.flatMap { it.statements }.toList().flatMap { it.importReferences() }.distinctBy { it.value }.joinToString("\n") { "from .model.${it.value} import ${it.value}" }}
        |
        |class Client():
        |
        |  def __init__(self, handler, serialization):
        |    self.handler = handler
        |    self.serialization = serialization
        |
        |${ast.emitClientEndpointRequest().joinToString("\n") {(endpoint, request) -> emitFunction(endpoint, request) }.spacer(1)}
        |
    """.trimMargin())

    fun Endpoint.Request.emitClientInterface(endpoint: Endpoint) =
        this.paramList(endpoint).joinToString(", ") { "${emit(it.identifier)}: ${it.reference.emit()}" }

    fun emitFunction(endpoint: Endpoint, request: Endpoint.Request) = """
        |def ${emit(endpoint.identifier)}(self, ${request.emitClientInterface(endpoint)}):
        |   req = endpoint.${emit(endpoint.identifier)}.Request${request.paramList(endpoint).takeIf { it.size > 0 }?.joinToString(", ", "(", ")") { emit(it.identifier) }.orEmpty()}
        |   return self.handler(endpoint.${emit(endpoint.identifier)}, req)
    """.trimMargin()

}
