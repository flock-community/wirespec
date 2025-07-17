package community.flock.wirespec.emitters.kotlin

import community.flock.wirespec.compiler.core.emit.IdentifierEmitter
import community.flock.wirespec.compiler.core.emit.UnionDefinitionEmitter
import community.flock.wirespec.compiler.core.parse.Union

interface KotlinUnionDefinitionEmitter: UnionDefinitionEmitter, IdentifierEmitter {
    override fun emit(union: Union) = """
        |sealed interface ${emit(union.identifier)}
        |
    """.trimMargin()
}