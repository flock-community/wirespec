package community.flock.wirespec.openapi

import community.flock.wirespec.compiler.core.emit.common.Emitter.Companion.firstToUpper

object Common {
    fun className(vararg arg: String) = arg
        .flatMap { it.split("-", "/") }
        .joinToString("") { it.firstToUpper() }

    fun <K, V> Map<K, V?>.filterNotNullValues(): Map<K, V> =
        mapNotNull { (key, value) -> value?.let { key to it } }.toMap()
}
