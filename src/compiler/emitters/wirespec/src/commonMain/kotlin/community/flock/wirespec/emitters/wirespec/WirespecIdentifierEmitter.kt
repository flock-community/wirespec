package community.flock.wirespec.emitters.wirespec

import community.flock.wirespec.compiler.core.addBackticks
import community.flock.wirespec.compiler.core.emit.IdentifierEmitter
import community.flock.wirespec.compiler.core.parse.DefinitionIdentifier
import community.flock.wirespec.compiler.core.parse.FieldIdentifier
import community.flock.wirespec.compiler.core.parse.Identifier
import community.flock.wirespec.emitters.wirespec.WirespecEmitter.Companion.reservedKeywords

interface WirespecIdentifierEmitter: IdentifierEmitter {

    override fun emit(identifier: Identifier) = when (identifier) {
        is DefinitionIdentifier -> identifier.run { if (value in reservedKeywords) value.addBackticks() else value }
        is FieldIdentifier -> identifier.run {
            if (value in reservedKeywords || value.first().isUpperCase()) value.addBackticks() else value
        }
    }

}