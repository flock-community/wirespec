package community.flock.wirespec.emitters.wirespec

import community.flock.wirespec.compiler.core.emit.IdentifierEmitter
import community.flock.wirespec.compiler.core.emit.TypeDefinitionEmitter
import community.flock.wirespec.compiler.core.emit.UnionDefinitionEmitter
import community.flock.wirespec.compiler.core.parse.Union

interface WirespecUnionDefinitionEmitter: UnionDefinitionEmitter, IdentifierEmitter, TypeDefinitionEmitter {

    override fun emit(union: Union) =
        "type ${emit(union.identifier)} = ${union.entries.joinToString(" | ") { it.emit() }}\n"

}
