package community.flock.wirespec.emitters.python

import community.flock.wirespec.compiler.core.emit.IdentifierEmitter
import community.flock.wirespec.compiler.core.emit.RefinedTypeDefinitionEmitter
import community.flock.wirespec.compiler.core.emit.Spacer
import community.flock.wirespec.compiler.core.emit.TypeDefinitionEmitter
import community.flock.wirespec.compiler.core.parse.Refined

interface PythonRefinedTypeDefinitionEmitter : RefinedTypeDefinitionEmitter, TypeDefinitionEmitter, IdentifierEmitter {

    override fun emit(refined: Refined) = """
        |@dataclass
        |class ${refined.identifier.sanitize()}(Wirespec.Refined):
        |${Spacer}value: str
        |
        |${Spacer}def validate(self) -> bool:
        |${Spacer(2)}return ${refined.emitValidator()}
        |
        |${Spacer}def __str__(self) -> str:
        |${Spacer(2)}return self.value
    """.trimMargin()
}
