package community.flock.wirespec.compiler.core.emit.transformer

import community.flock.wirespec.compiler.core.parse.Field

interface FieldTransformer<T : Any> {
    fun Field.Reference.transform(isNullable: Boolean, isOptional: Boolean): T
}
