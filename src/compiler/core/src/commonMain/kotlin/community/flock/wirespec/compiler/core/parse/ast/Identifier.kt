package community.flock.wirespec.compiler.core.parse.ast

import community.flock.wirespec.compiler.core.Value
import community.flock.wirespec.compiler.core.removeBackticks

public sealed class Identifier(name: String) : Value<String> {
    override val value: String = name.removeBackticks()
    override fun toString(): String = value
}

public data class DefinitionIdentifier(private val name: String) : Identifier(name)

public data class FieldIdentifier(private val name: String) : Identifier(name)
