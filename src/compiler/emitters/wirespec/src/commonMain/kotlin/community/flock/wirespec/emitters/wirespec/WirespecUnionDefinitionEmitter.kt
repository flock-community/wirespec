package community.flock.wirespec.emitters.wirespec

import community.flock.wirespec.compiler.core.emit.UnionDefinitionEmitter
import community.flock.wirespec.compiler.core.parse.Union

interface WirespecUnionDefinitionEmitter: UnionDefinitionEmitter, WirespecTypeDefinitionEmitter {

    override fun emit(union: Union) =
        "type ${emit(union.identifier)} = ${union.entries.joinToString(" | ") { it.emit() }}\n"

}
