package community.flock.wirespec.compiler.core.emit.model

import community.flock.wirespec.compiler.core.emit.common.Emitted
import community.flock.wirespec.compiler.core.emit.common.Emitter
import community.flock.wirespec.compiler.core.emit.common.EndpointDefinitionEmitter
import community.flock.wirespec.compiler.core.emit.common.EnumDefinitionEmitter
import community.flock.wirespec.compiler.core.emit.common.RefinedTypeDefinitionEmitter
import community.flock.wirespec.compiler.core.emit.common.TypeDefinitionEmitter
import community.flock.wirespec.compiler.core.emit.common.UnionDefinitionEmitter
import community.flock.wirespec.compiler.core.parse.AST
import community.flock.wirespec.compiler.core.parse.Definition
import community.flock.wirespec.compiler.core.parse.Endpoint
import community.flock.wirespec.compiler.core.parse.Enum
import community.flock.wirespec.compiler.core.parse.Refined
import community.flock.wirespec.compiler.core.parse.Type
import community.flock.wirespec.compiler.core.parse.Union
import community.flock.wirespec.compiler.utils.Logger

interface Emitters :
    TypeDefinitionEmitter,
    EnumDefinitionEmitter,
    RefinedTypeDefinitionEmitter,
    EndpointDefinitionEmitter,
    UnionDefinitionEmitter

abstract class DefinitionModelEmitter(logger: Logger, split: Boolean = false) : Emitter(logger, split), Emitters {

    override fun emit(ast: AST): List<Emitted> = ast
        .filterIsInstance<Definition>()
        .map {
            logger.log("Emitting Node $this")
            when (it) {
                is Type -> Emitted(it.name, it.emit())
                is Endpoint -> Emitted(it.name, it.emit())
                is Enum -> Emitted(it.name, it.emit())
                is Refined -> Emitted(it.name, it.emit())
                is Union -> Emitted(it.name, it.emit())
            }
        }
        .run {
            if (split) this
            else listOf(Emitted("NoName", joinToString("\n") { it.result }))
        }

}