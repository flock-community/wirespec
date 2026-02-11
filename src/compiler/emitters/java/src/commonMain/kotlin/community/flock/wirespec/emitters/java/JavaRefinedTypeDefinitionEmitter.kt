package community.flock.wirespec.emitters.java

import community.flock.wirespec.compiler.core.emit.RefinedTypeDefinitionEmitter
import community.flock.wirespec.compiler.core.emit.Spacer
import community.flock.wirespec.compiler.core.parse.ast.Reference
import community.flock.wirespec.compiler.core.parse.ast.Refined

interface JavaRefinedTypeDefinitionEmitter: RefinedTypeDefinitionEmitter, JavaTypeDefinitionEmitter {

    override fun emit(refined: Refined) = """
        |public record ${emit(refined.identifier)} (${refined.reference.emit()} value) implements Wirespec.Refined<${refined.reference.emit()}> {
        |${Spacer}@Override
        |${Spacer}public String toString() { return value.toString(); }
        |${Spacer}public static boolean validate(${emit(refined.identifier)} record) {
        |${Spacer}${Spacer}${refined.emitValidator()}
        |${Spacer}}
        |${Spacer}@Override
        |${Spacer}public ${refined.reference.emit()} value() { return value; }
        |}
        |
    """.trimMargin()

    override fun Refined.emitValidator():String {
        val defaultReturn = "return true;"
        return when (val type = reference.type) {
            is Reference.Primitive.Type.Integer -> type.constraint?.emit() ?: defaultReturn
            is Reference.Primitive.Type.Number -> type.constraint?.emit() ?: defaultReturn
            is Reference.Primitive.Type.String -> type.constraint?.emit() ?: defaultReturn
            Reference.Primitive.Type.Boolean -> defaultReturn
            Reference.Primitive.Type.Bytes -> defaultReturn
        }
    }

}
