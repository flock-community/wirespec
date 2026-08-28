package community.flock.wirespec.compiler.core

public interface Value<T : Any> {
    public val value: T
}

private operator fun <T : Any> Value<T>.component1(): T = value
