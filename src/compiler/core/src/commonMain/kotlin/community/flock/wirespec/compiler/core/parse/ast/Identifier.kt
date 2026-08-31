package community.flock.wirespec.compiler.core.parse.ast

import community.flock.wirespec.compiler.core.Value
import community.flock.wirespec.compiler.core.removeBackticks

public sealed class Identifier(name: String) : Value<String> {
    override val value: String = name.removeBackticks()
    override fun toString(): String = value

//    abstract fun map(fn: (String) -> String): Identifier
}

public data class DefinitionIdentifier(private val name: String) : Identifier(name) {
//    override fun map(fn: (String) -> String) = DefinitionIdentifier(fn(value))
}

public data class FieldIdentifier(private val name: String) : Identifier(name) {
//    override fun map(fn: (String) -> String) = FieldIdentifier(fn(value))
}
