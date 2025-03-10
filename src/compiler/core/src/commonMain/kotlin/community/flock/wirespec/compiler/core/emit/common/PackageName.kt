package community.flock.wirespec.compiler.core.emit.common

import community.flock.wirespec.compiler.core.Value
import kotlin.jvm.JvmName
import kotlin.jvm.JvmStatic
import kotlin.jvm.JvmSynthetic

class PackageName(override val value: String) : Value<String> {
    override fun toString() = value

    companion object {
        @JvmStatic
        @JvmName("of")
        fun of(value: String) = invoke(value)

        @JvmSynthetic
        operator fun invoke(value: String? = null) = value
            ?.takeIf(String::isNotBlank)
            ?.let(::PackageName)
    }
}
