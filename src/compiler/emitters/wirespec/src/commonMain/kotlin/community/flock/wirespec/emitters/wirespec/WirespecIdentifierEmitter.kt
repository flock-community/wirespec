package community.flock.wirespec.emitters.wirespec

import community.flock.wirespec.compiler.core.addBackticks
import community.flock.wirespec.compiler.core.emit.IdentifierEmitter
import community.flock.wirespec.compiler.core.emit.Keywords
import community.flock.wirespec.compiler.core.parse.ast.DefinitionIdentifier
import community.flock.wirespec.compiler.core.parse.ast.FieldIdentifier
import community.flock.wirespec.compiler.core.parse.ast.Identifier

internal interface WirespecIdentifierEmitter: IdentifierEmitter {

    override fun emit(identifier: Identifier) = when (identifier) {
        is DefinitionIdentifier -> identifier.run { if (value in reservedKeywords) value.addBackticks() else value }
        is FieldIdentifier -> identifier.run {
            if (value in reservedKeywords || !fieldIdentifier.matches(value)) value.addBackticks() else value
        }
    }

    companion object : Keywords {
        override val reservedKeywords = setOf(
            "type", "enum", "endpoint", "channel", "rpc"
        )

        private val fieldIdentifier = Regex("[a-z][a-zA-Z0-9_\\-]*")
    }
}
