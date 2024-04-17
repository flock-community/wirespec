package community.flock.wirespec.compiler.core.emit.common

import community.flock.wirespec.compiler.core.parse.Refined

interface RefinedTypeDefinitionEmitter {
    fun Refined.emit(): String
}
