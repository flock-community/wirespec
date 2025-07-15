package community.flock.wirespec.compiler.core.emit

import community.flock.wirespec.compiler.core.Value
import community.flock.wirespec.compiler.core.parse.Definition
import kotlin.jvm.JvmName
import kotlin.jvm.JvmStatic
import kotlin.jvm.JvmSynthetic

class PackageName(override val value: String, val createDirectory: Boolean) : Value<String> {
    override fun toString() = value

    companion object {
        @JvmStatic
        @JvmName("of")
        fun of(value: String) = invoke(value)

        @JvmSynthetic
        operator fun invoke(value: String? = null) = value
            .let { PackageName(it ?: DEFAULT_SHARED_PACKAGE_STRING, it != null) }
    }

    fun toDir(): String = value.replace(".", "/") + "/"
}

operator fun PackageName.plus(definition: Definition) = this + definition.namespace()

operator fun PackageName.plus(subPackage: String) = PackageName("$value.$subPackage")
