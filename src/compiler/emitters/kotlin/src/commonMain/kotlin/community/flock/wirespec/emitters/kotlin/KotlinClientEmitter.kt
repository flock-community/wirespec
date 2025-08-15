package community.flock.wirespec.emitters.kotlin

import arrow.core.NonEmptyList
import community.flock.wirespec.compiler.core.emit.BaseEmitter
import community.flock.wirespec.compiler.core.emit.ClientEmitter
import community.flock.wirespec.compiler.core.emit.Emitted
import community.flock.wirespec.compiler.core.emit.Emitter.Companion.firstToLower
import community.flock.wirespec.compiler.core.emit.IdentifierEmitter
import community.flock.wirespec.compiler.core.emit.ImportEmitter
import community.flock.wirespec.compiler.core.emit.PackageNameEmitter
import community.flock.wirespec.compiler.core.emit.ParamEmitter
import community.flock.wirespec.compiler.core.emit.SpaceEmitter
import community.flock.wirespec.compiler.core.parse.AST
import community.flock.wirespec.compiler.core.parse.Endpoint
import community.flock.wirespec.compiler.core.parse.Module
import kotlin.compareTo

interface KotlinClientEmitter: BaseEmitter, ClientEmitter,PackageNameEmitter, ParamEmitter, SpaceEmitter, ImportEmitter, KotlinTypeDefinitionEmitter {

    override fun emitClient(ast: AST): Emitted = Emitted("${packageName.toDir()}/Client.${extension.value}", """
        |package ${packageName.value}
        |
        |import community.flock.wirespec.kotlin.Wirespec
        |
        |${ast.emitClientEndpointRequest().joinToString("\n") { (endpoint) -> "import ${packageName.value}.endpoint.${emit(endpoint.identifier)}" }}
        |
        |${ast.modules.flatMap { it.statements }.toList().flatMap { it.importReferences() }.distinctBy { it.value }.joinToString("\n") { "import ${packageName.value}.model.${it.value}" }}
        |
        |class Client(val handler: (Wirespec.Request<*>) -> Wirespec.Response<*> ){
        |${ast.emitClientEndpointRequest().joinToString("\n") { (endpoint, request) -> emitFunction(endpoint, request) }.spacer(1)}
        |}
        |
    """.trimMargin())

    fun Endpoint.Request.emitClientInterface(endpoint: Endpoint) =
        this.paramList(endpoint).joinToString(", ") { "${emit(it.identifier)}: ${it.reference.emit()}" }

    fun emitFunction(endpoint: Endpoint, request: Endpoint.Request) = """
        |suspend fun ${emit(endpoint.identifier)}(${request.emitClientInterface(endpoint)}) = 
        |   ${emit(endpoint.identifier)}.Request${request.paramList(endpoint).takeIf { it.isNotEmpty() }?.joinToString(", ", "(", ")") { emit(it.identifier) }.orEmpty()}
        |     .let{req -> handler(req) as ${emit(endpoint.identifier)}.Response<*> }
    """.trimMargin()
}