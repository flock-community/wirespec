package community.flock.wirespec.ir.emit

import arrow.core.NonEmptyList
import community.flock.wirespec.compiler.core.emit.Emitted
import community.flock.wirespec.compiler.core.emit.Emitter
import community.flock.wirespec.compiler.core.emit.Shared
import community.flock.wirespec.compiler.core.parse.ast.AST
import community.flock.wirespec.compiler.core.parse.ast.Channel
import community.flock.wirespec.compiler.core.parse.ast.Definition
import community.flock.wirespec.compiler.core.parse.ast.Endpoint
import community.flock.wirespec.compiler.core.parse.ast.Enum
import community.flock.wirespec.compiler.core.parse.ast.Module
import community.flock.wirespec.compiler.core.parse.ast.Refined
import community.flock.wirespec.compiler.core.parse.ast.Type
import community.flock.wirespec.compiler.core.parse.ast.Union
import community.flock.wirespec.compiler.utils.Logger
import community.flock.wirespec.ir.converter.convertClient
import community.flock.wirespec.ir.converter.convertEndpointClient
import community.flock.wirespec.ir.core.File
import community.flock.wirespec.ir.generator.Generator

interface IrEmitter : Emitter {

    val shared: Shared?

    val generator: Generator

    override fun emit(ast: AST, logger: Logger): NonEmptyList<Emitted> {
        val moduleEmitted = ast.modules
            .flatMap { m ->
                logger.info("Emitting Nodes from ${m.fileUri.value} ")
                emit(m, logger)
            }
            .map { it.toEmitted() }

        val allEndpoints = ast.modules.toList().flatMap { it.statements.filterIsInstance<Endpoint>() }
        return allEndpoints.takeIf { it.isNotEmpty() }
            ?.let { moduleEmitted + emitClient(it, logger).toEmitted() }
            ?: moduleEmitted
    }

    fun emit(module: Module, logger: Logger): NonEmptyList<File> {
        val definitionFiles = module.statements.map { emit(it, module, logger) }
        val clientFiles = module.statements.toList().filterIsInstance<Endpoint>().map { endpoint ->
            logger.info("Emitting Client for endpoint ${endpoint.identifier.value}")
            emitEndpointClient(endpoint)
        }
        return definitionFiles + clientFiles
    }

    fun emit(definition: Definition, module: Module, logger: Logger): File {
        logger.info("Emitting ${definition::class.simpleName} ${definition.identifier.value}")
        return when (definition) {
            is Type -> emit(definition, module)
            is Endpoint -> emit(definition)
            is Enum -> emit(definition, module)
            is Refined -> emit(definition)
            is Union -> emit(definition)
            is Channel -> emit(definition)
        }
    }

    private fun File.toEmitted(): Emitted = Emitted(name.value() + "." + extension.value, generator.generate(this))

    fun emitEndpointClient(endpoint: Endpoint): File = endpoint.convertEndpointClient()

    fun emitClient(endpoints: List<Endpoint>, logger: Logger): File {
        logger.info("Emitting main Client for ${endpoints.size} endpoints")
        return endpoints.convertClient()
    }

    fun emit(type: Type, module: Module): File
    fun emit(enum: Enum, module: Module): File
    fun emit(refined: Refined): File
    fun emit(endpoint: Endpoint): File
    fun emit(union: Union): File
    fun emit(channel: Channel): File

    fun transformTestFile(file: File): File = file
}
