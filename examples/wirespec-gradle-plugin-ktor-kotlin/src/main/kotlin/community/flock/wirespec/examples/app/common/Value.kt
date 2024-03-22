package community.flock.wirespec.examples.app.common

interface Value<T : Any> {
    val value: T
}

operator fun <T : Any> Value<T>.component1() = value

operator fun <T : Any> Value<T>.invoke() = value
