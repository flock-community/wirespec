package community.flock.wirespec.compiler.core.emit.transformer

import community.flock.wirespec.compiler.core.parse.Reference

interface FieldTransformer<T : Any> {
    fun Reference.transform(isNullable: Boolean, isOptional: Boolean): T
}
