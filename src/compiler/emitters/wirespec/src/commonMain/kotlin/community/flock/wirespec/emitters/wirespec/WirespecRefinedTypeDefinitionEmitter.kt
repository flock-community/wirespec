package community.flock.wirespec.emitters.wirespec

import community.flock.wirespec.compiler.core.emit.RefinedTypeDefinitionEmitter
import community.flock.wirespec.compiler.core.parse.ast.Refined

internal interface WirespecRefinedTypeDefinitionEmitter: RefinedTypeDefinitionEmitter, WirespecTypeDefinitionEmitter {

    override fun emit(refined: Refined) =
        "type ${emit(refined.identifier)} = ${refined.reference.copy(isNullable = false).emit()}\n"

    // A Wirespec reference carries its own constraint, so a refined type adds nothing to it.
    override fun Refined.emitValidator(): String = ""
}
