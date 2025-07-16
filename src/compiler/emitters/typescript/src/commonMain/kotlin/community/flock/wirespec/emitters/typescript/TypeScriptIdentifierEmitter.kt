package community.flock.wirespec.emitters.typescript

import community.flock.wirespec.compiler.core.emit.IdentifierEmitter
import community.flock.wirespec.compiler.core.parse.Identifier

interface TypeScriptIdentifierEmitter: IdentifierEmitter {
    override fun emit(identifier: Identifier) = """"${identifier.value}""""
}