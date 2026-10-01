package community.flock.wirespec.emitters.wirespec

import community.flock.wirespec.compiler.core.addBackticks
import community.flock.wirespec.compiler.core.emit.EnumDefinitionEmitter
import community.flock.wirespec.compiler.core.emit.Spacer
import community.flock.wirespec.compiler.core.parse.ast.Endpoint
import community.flock.wirespec.compiler.core.parse.ast.Enum
import community.flock.wirespec.compiler.core.parse.ast.Module

internal interface WirespecEnumDefinitionEmitter: EnumDefinitionEmitter, WirespecIdentifierEmitter {

    override fun emit(enum: Enum, module: Module) =
        "enum ${emit(enum.identifier)} {\n${Spacer}${enum.entries.joinToString(", ") { it.emitEntry() }}\n}\n"

    private fun String.emitEntry() = if (entry.matches(this) && this !in methods) this else addBackticks()
}

private val entry = Regex("[A-Z][a-zA-Z0-9_]*|-?[0-9]+")

private val methods = Endpoint.Method.entries.map { it.name }.toSet()
