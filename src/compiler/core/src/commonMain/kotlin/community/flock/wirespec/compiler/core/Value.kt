package community.flock.wirespec.compiler.core

interface Value<T : Any> {
    val value: T
}

operator fun <T : Any> Value<T>.component1(): T = value
