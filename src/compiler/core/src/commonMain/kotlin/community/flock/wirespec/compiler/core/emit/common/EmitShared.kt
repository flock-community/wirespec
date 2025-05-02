package community.flock.wirespec.compiler.core.emit.common

import community.flock.wirespec.compiler.core.Value
import kotlin.jvm.JvmSynthetic

class EmitShared(override val value: Boolean) : Value<Boolean> {
    override fun toString() = value.toString()

    companion object {

        @JvmSynthetic
        operator fun invoke(value: Boolean? = null) = EmitShared(value ?: false)
    }
}
