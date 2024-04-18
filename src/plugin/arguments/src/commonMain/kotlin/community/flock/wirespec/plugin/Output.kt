package community.flock.wirespec.plugin

import community.flock.wirespec.compiler.core.Value
import kotlin.jvm.JvmInline

@JvmInline
value class Output private constructor(override val value: String) : Value<String> {

    override fun toString() = value

    companion object {
        operator fun invoke(s: String?) = s?.let(::Output)
    }
}
