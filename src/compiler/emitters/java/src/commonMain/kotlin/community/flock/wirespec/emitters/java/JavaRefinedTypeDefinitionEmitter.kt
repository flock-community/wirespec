package community.flock.wirespec.emitters.java

import community.flock.wirespec.compiler.core.emit.IdentifierEmitter
import community.flock.wirespec.compiler.core.emit.RefinedTypeDefinitionEmitter
import community.flock.wirespec.compiler.core.emit.Spacer
import community.flock.wirespec.compiler.core.emit.TypeDefinitionEmitter
import community.flock.wirespec.compiler.core.parse.Reference
import community.flock.wirespec.compiler.core.parse.Refined

interface JavaRefinedTypeDefinitionEmitter: RefinedTypeDefinitionEmitter, IdentifierEmitter, TypeDefinitionEmitter {

    override fun emit(refined: Refined) = """
        |public record ${emit(refined.identifier)} (String value) implements Wirespec.Refined {
        |${Spacer}@Override
        |${Spacer}public String toString() { return value; }
        |${Spacer}public static boolean validate(${emit(refined.identifier)} record) {
        |${Spacer}${Spacer}return ${refined.emitValidator()}
        |${Spacer}}
        |${Spacer}@Override
        |${Spacer}public String getValue() { return value; }
        |}
        |
    """.trimMargin()

}
