package community.flock.wirespec.emitters.typescript

import community.flock.wirespec.compiler.core.emit.RefinedTypeDefinitionEmitter
import community.flock.wirespec.compiler.core.emit.Spacer
import community.flock.wirespec.compiler.core.parse.ast.Reference
import community.flock.wirespec.compiler.core.parse.ast.Refined

interface TypeScriptRefinedTypeDefinitionEmitter: RefinedTypeDefinitionEmitter, TypeScriptTypeDefinitionEmitter {
    override fun emit(refined: Refined) =
        """
          |export type ${refined.identifier.sanitizeSymbol()} = ${refined.reference.emit()};
          |export const validate${refined.identifier.value} = (value: ${refined.reference.emit()}): value is ${refined.identifier.sanitizeSymbol()} => {
          |${refined.emitValidator()}
          |}
          |
        """.trimMargin()

    override fun Refined.emitValidator(): String {
        val defaultReturn = "${Spacer}return true;"
        return when (val type = reference.type) {
            is Reference.Primitive.Type.Integer -> type.constraint?.emit() ?: defaultReturn
            is Reference.Primitive.Type.Number -> type.constraint?.emit() ?: defaultReturn
            is Reference.Primitive.Type.String -> type.constraint?.emit() ?: defaultReturn
            Reference.Primitive.Type.Boolean -> defaultReturn
            Reference.Primitive.Type.Bytes -> defaultReturn
        }
    }
}
