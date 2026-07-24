package community.flock.wirespec.emitters.wirespec

import community.flock.wirespec.compiler.core.addBackticks
import community.flock.wirespec.compiler.core.emit.IdentifierEmitter
import community.flock.wirespec.compiler.core.emit.Keywords
import community.flock.wirespec.compiler.core.parse.ast.DefinitionIdentifier
import community.flock.wirespec.compiler.core.parse.ast.FieldIdentifier
import community.flock.wirespec.compiler.core.parse.ast.Identifier

interface WirespecIdentifierEmitter: IdentifierEmitter {

    override fun emit(identifier: Identifier) = when (identifier) {
        is DefinitionIdentifier -> identifier.run { if (value in reservedKeywords) value.addBackticks() else value }
        is FieldIdentifier -> identifier.run {
            // Bare field identifiers must start with a lowercase letter (LanguageSpec
            // tokenizes a leading `_`, digit or uppercase letter differently), so any
            // other start — `_embedded`, reserved keywords — is backtick-quoted to keep
            // its wire name intact rather than renamed.
            if (value in reservedKeywords || !value.first().isLowerCase()) value.addBackticks() else value
        }
    }

    companion object : Keywords {
        override val reservedKeywords = setOf(
            "type", "enum", "endpoint"
        )
    }
}
