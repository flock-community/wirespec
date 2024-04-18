package community.flock.wirespec.compiler.core.emit.transformer

import community.flock.wirespec.compiler.core.parse.AST
import community.flock.wirespec.compiler.core.parse.Refined
import community.flock.wirespec.compiler.core.parse.Type
import community.flock.wirespec.compiler.core.parse.Union

interface TypeShapeTransformer<T : Any> {
    fun Type.transform(ast: AST): T
}

interface RefinedTransformer<T : Any> {
    fun Refined.transform(): T
}

interface UnionTransformer<T : Any> {
    fun Union.transform(): T
}
