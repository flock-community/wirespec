package community.flock.wirespec.compiler.core.parse.ast

import community.flock.wirespec.compiler.core.Value
import community.flock.wirespec.compiler.core.removeBackticks

public sealed class Identifier(name: String) : Value<String> {
    override val value: String = name.removeBackticks()
    override fun toString(): String = value

    // Final, so data subclasses compare by value: `type` and type name the same field.
    final override fun equals(other: Any?): Boolean = other != null && other::class == this::class && (other as Identifier).value == value
    final override fun hashCode(): Int = value.hashCode()
}

public data class DefinitionIdentifier(private val name: String) : Identifier(name)

public data class FieldIdentifier(private val name: String) : Identifier(name)
