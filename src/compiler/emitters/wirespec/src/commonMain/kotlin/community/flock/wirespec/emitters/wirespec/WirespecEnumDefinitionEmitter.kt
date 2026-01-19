package community.flock.wirespec.emitters.wirespec

import community.flock.wirespec.compiler.core.emit.EnumDefinitionEmitter
import community.flock.wirespec.compiler.core.emit.Spacer
import community.flock.wirespec.compiler.core.parse.ast.Enum
import community.flock.wirespec.compiler.core.parse.ast.Module

interface WirespecEnumDefinitionEmitter: EnumDefinitionEmitter, WirespecIdentifierEmitter {

    override fun emit(enum: Enum, module: Module) =
        "enum ${emit(enum.identifier)} {\n${Spacer}${enum.entries.joinToString(", ") { it.capitalize() }}\n}\n"

    private fun String.capitalize() = replaceFirstChar { it.uppercase() }
}
