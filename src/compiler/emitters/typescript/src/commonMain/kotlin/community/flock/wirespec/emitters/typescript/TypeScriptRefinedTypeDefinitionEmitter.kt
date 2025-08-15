package community.flock.wirespec.emitters.typescript

import community.flock.wirespec.compiler.core.emit.RefinedTypeDefinitionEmitter
import community.flock.wirespec.compiler.core.emit.Spacer
import community.flock.wirespec.compiler.core.parse.Refined

interface TypeScriptRefinedTypeDefinitionEmitter: RefinedTypeDefinitionEmitter, TypeScriptTypeDefinitionEmitter {
    override fun emit(refined: Refined) =
        """
          |export type ${refined.identifier.sanitizeSymbol()} = string;
          |export const validate${refined.identifier.value} = (value: string): value is ${refined.identifier.sanitizeSymbol()} => 
          |${Spacer}${refined.emitValidator()};
          |
        """.trimMargin()
}
