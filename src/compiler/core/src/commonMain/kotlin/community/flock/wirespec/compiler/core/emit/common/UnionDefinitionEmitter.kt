package community.flock.wirespec.compiler.core.emit.common

import community.flock.wirespec.compiler.core.parse.Union

interface UnionDefinitionEmitter {
    fun Union.emit(): String
}
