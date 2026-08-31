package community.flock.wirespec.compiler.core.emit

import community.flock.wirespec.compiler.core.Value
import kotlin.jvm.JvmSynthetic

public class EmitShared(override val value: Boolean) : Value<Boolean> {
    override fun toString(): String = value.toString()

    public companion object {

        @JvmSynthetic
        public operator fun invoke(value: Boolean? = null): EmitShared = EmitShared(value ?: false)
    }
}
