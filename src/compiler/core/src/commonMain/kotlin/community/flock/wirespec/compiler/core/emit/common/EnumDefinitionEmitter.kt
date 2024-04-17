package community.flock.wirespec.compiler.core.emit.common

import community.flock.wirespec.compiler.core.parse.Enum

interface EnumDefinitionEmitter {
    fun Enum.emit(): String
}
