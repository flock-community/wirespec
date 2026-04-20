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
import community.flock.wirespec.ir.core.File
import community.flock.wirespec.ir.extensions.ClientIrExtension
import community.flock.wirespec.ir.extensions.IrExtension
import community.flock.wirespec.ir.generator.Generator

interface IrEmitter : Emitter {

    val shared: Shared?

    val generator: Generator

    val extensions: List<IrExtension>

    override fun emit(ast: AST, logger: Logger): NonEmptyList<Emitted> {
        val moduleEmitted = ast.modules.flatMap { m ->
            logger.info("Emitting Nodes from ${m.fileUri.value} ")
            emit(m, logger)
        }.map { file -> Emitted(file.name.value() + "." + extension.value, generator.generate(file)) }

        val allEndpoints = ast.modules.toList().flatMap { it.statements.filterIsInstance<Endpoint>() }
        return if (allEndpoints.isNotEmpty()) {
            val mainClient = extension<ClientIrExtension>().emitClient(allEndpoints, logger)
            moduleEmitted + Emitted(mainClient.name.value() + "." + extension.value, generator.generate(mainClient))
        } else {
            moduleEmitted
        }
    }

    fun emit(module: Module, logger: Logger): NonEmptyList<File> {
        val definitionFiles = module.statements.map { emit(it, module, logger) }
        val endpoints = module.statements.toList().filterIsInstance<Endpoint>()
        val client = extension<ClientIrExtension>()
        val clientFiles = endpoints.map { endpoint ->
            logger.info("Emitting Client for endpoint ${endpoint.identifier.value}")
            client.emitEndpointClient(endpoint)
        }
        return definitionFiles + clientFiles
    }

    fun emit(definition: Definition, module: Module, logger: Logger): File = run {
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

    fun emit(type: Type, module: Module): File
    fun emit(enum: Enum, module: Module): File
    fun emit(refined: Refined): File
    fun emit(endpoint: Endpoint): File
    fun emit(union: Union): File
    fun emit(channel: Channel): File

    fun transformTestFile(file: File): File = file
}

inline fun <reified T : IrExtension> IrEmitter.extension(): T = extensions.filterIsInstance<T>().singleOrNull()
    ?: error("No ${T::class.simpleName} registered on this emitter")
