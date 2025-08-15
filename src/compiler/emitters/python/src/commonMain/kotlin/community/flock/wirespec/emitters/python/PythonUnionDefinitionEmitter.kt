package community.flock.wirespec.emitters.python

import community.flock.wirespec.compiler.core.emit.IdentifierEmitter
import community.flock.wirespec.compiler.core.emit.Spacer
import community.flock.wirespec.compiler.core.emit.UnionDefinitionEmitter
import community.flock.wirespec.compiler.core.parse.Union

interface PythonUnionDefinitionEmitter: UnionDefinitionEmitter, IdentifierEmitter {
    override fun emit(union: Union) = """
        |class ${emit(union.identifier)}(ABC):
        |${Spacer}pass
    """.trimMargin()
}
