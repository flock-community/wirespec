package community.flock.wirespec.emitters.wirespec

import community.flock.wirespec.compiler.core.emit.IdentifierEmitter
import community.flock.wirespec.compiler.core.emit.RefinedTypeDefinitionEmitter
import community.flock.wirespec.compiler.core.emit.TypeDefinitionEmitter
import community.flock.wirespec.compiler.core.parse.Refined

interface WirespecRefinedTypeDefinitionEmitter: RefinedTypeDefinitionEmitter, IdentifierEmitter, TypeDefinitionEmitter {

    override fun emit(refined: Refined) =
        "type ${emit(refined.identifier)} = ${refined.reference.emit()}${refined.emitValidator()}\n"

}
