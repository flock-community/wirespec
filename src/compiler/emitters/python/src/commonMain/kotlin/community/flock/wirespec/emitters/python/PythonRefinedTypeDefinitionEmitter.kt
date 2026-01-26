package community.flock.wirespec.emitters.python

import community.flock.wirespec.compiler.core.emit.RefinedTypeDefinitionEmitter
import community.flock.wirespec.compiler.core.emit.Spacer
import community.flock.wirespec.compiler.core.parse.ast.Reference
import community.flock.wirespec.compiler.core.parse.ast.Refined

interface PythonRefinedTypeDefinitionEmitter : RefinedTypeDefinitionEmitter, PythonTypeDefinitionEmitter, PythonIdentifierEmitter {

    override fun emit(refined: Refined) = """
        |@dataclass
        |class ${refined.identifier.sanitize()}(Wirespec.Refined):
        |${Spacer}value: ${refined.reference.emitType()}
        |
        |${Spacer}def validate(self) -> bool:
        |${Spacer(2)}return ${refined.emitValidator()}
        |
        |${Spacer}def __str__(self) -> str:
        |${Spacer(2)}return str(self.value)
        |
    """.trimMargin()

    override fun Refined.emitValidator():String {
        val defaultReturn = "True"
        return when (val type = reference.type) {
            is Reference.Primitive.Type.Integer -> type.constraint?.emit() ?: defaultReturn
            is Reference.Primitive.Type.Number -> type.constraint?.emit() ?: defaultReturn
            is Reference.Primitive.Type.String -> type.constraint?.emit() ?: defaultReturn
            Reference.Primitive.Type.Boolean -> defaultReturn
            Reference.Primitive.Type.Bytes -> defaultReturn
        }
    }
}
