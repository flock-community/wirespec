package community.flock.wirespec.compiler.core.emit.transformer

import community.flock.wirespec.compiler.core.parse.Enum

interface EnumTransformer<T : Any> {
    fun Enum.transform(): T
}
