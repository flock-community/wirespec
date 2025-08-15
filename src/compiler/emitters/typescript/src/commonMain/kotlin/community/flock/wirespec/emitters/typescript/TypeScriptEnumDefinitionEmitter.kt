package community.flock.wirespec.emitters.typescript

import community.flock.wirespec.compiler.core.emit.EnumDefinitionEmitter
import community.flock.wirespec.compiler.core.parse.Enum
import community.flock.wirespec.compiler.core.parse.Module

interface TypeScriptEnumDefinitionEmitter: EnumDefinitionEmitter, TypeScriptTypeDefinitionEmitter {

    override fun emit(enum: Enum, module: Module) =
        "export type ${enum.identifier.sanitizeSymbol()} = ${enum.entries.joinToString(" | ") { """"$it"""" }}\n"

}
