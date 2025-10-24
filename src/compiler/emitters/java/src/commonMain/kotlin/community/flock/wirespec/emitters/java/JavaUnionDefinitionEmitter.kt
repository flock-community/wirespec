package community.flock.wirespec.emitters.java

import community.flock.wirespec.compiler.core.emit.UnionDefinitionEmitter
import community.flock.wirespec.compiler.core.parse.ast.Union

interface JavaUnionDefinitionEmitter: UnionDefinitionEmitter, JavaIdentifierEmitter {
    override fun emit(union: Union) = """
        |public sealed interface ${emit(union.identifier)} permits ${union.entries.joinToString { it.value }} {}
        |
    """.trimMargin()
}
