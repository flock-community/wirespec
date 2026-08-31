package community.flock.wirespec.compiler.core.emit

import community.flock.wirespec.compiler.core.Value
import community.flock.wirespec.compiler.core.parse.ast.Definition
import kotlin.jvm.JvmName
import kotlin.jvm.JvmStatic
import kotlin.jvm.JvmSynthetic

public class PackageName(override val value: String, public val createDirectory: Boolean) : Value<String> {
    override fun toString(): String = value

    public companion object {
        @JvmStatic
        @JvmName("of")
        public fun of(value: String): PackageName = invoke(value)

        @JvmSynthetic
        public operator fun invoke(value: String? = null): PackageName = value
            .let { PackageName(it ?: DEFAULT_SHARED_PACKAGE_STRING, it != null) }
    }

    public fun toDir(): String = value.replace(".", "/") + "/"
}

public operator fun PackageName.plus(definition: Definition): PackageName = this + definition.namespace()

public operator fun PackageName.plus(subPackage: String): PackageName = PackageName("$value.$subPackage")
