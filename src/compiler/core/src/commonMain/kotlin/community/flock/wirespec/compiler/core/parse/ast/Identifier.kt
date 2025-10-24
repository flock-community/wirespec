package community.flock.wirespec.compiler.core.parse.ast

import community.flock.wirespec.compiler.core.Value
import community.flock.wirespec.compiler.core.removeBackticks

sealed class Identifier(name: String) : Value<String> {
    override val value = name.removeBackticks()
    override fun toString() = value

//    abstract fun map(fn: (String) -> String): Identifier
}

data class DefinitionIdentifier(private val name: String) : Identifier(name) {
//    override fun map(fn: (String) -> String) = DefinitionIdentifier(fn(value))
}

data class FieldIdentifier(private val name: String) : Identifier(name) {
//    override fun map(fn: (String) -> String) = FieldIdentifier(fn(value))
}
