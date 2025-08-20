package community.flock.wirespec.emitters.wirespec

import community.flock.wirespec.compiler.core.emit.RefinedTypeDefinitionEmitter
import community.flock.wirespec.compiler.core.parse.Reference
import community.flock.wirespec.compiler.core.parse.Refined

interface WirespecRefinedTypeDefinitionEmitter: RefinedTypeDefinitionEmitter, WirespecTypeDefinitionEmitter {

    override fun emit(refined: Refined) =
        "type ${emit(refined.identifier)} = ${refined.reference.emit()}${refined.emitValidator()}\n"

    override fun Refined.emitValidator():String {
        return when (val type = reference.type) {
            is Reference.Primitive.Type.Integer -> type.constraint?.emit() ?: ""
            is Reference.Primitive.Type.Number -> type.constraint?.emit() ?: ""
            is Reference.Primitive.Type.String -> type.constraint?.emit() ?: ""
            Reference.Primitive.Type.Boolean -> ""
            Reference.Primitive.Type.Bytes -> ""
        }
    }
}
