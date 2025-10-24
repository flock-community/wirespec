package community.flock.wirespec.emitters.kotlin

import community.flock.wirespec.compiler.core.emit.RefinedTypeDefinitionEmitter
import community.flock.wirespec.compiler.core.emit.Spacer
import community.flock.wirespec.compiler.core.parse.ast.Reference
import community.flock.wirespec.compiler.core.parse.ast.Refined

interface KotlinRefinedTypeDefinitionEmitter: RefinedTypeDefinitionEmitter, KotlinIdentifierEmitter, KotlinTypeDefinitionEmitter {

    override fun emit(refined: Refined) = """
        |data class ${refined.identifier.sanitize()}(override val value: String): Wirespec.Refined {
        |${Spacer}override fun toString() = value
        |}
        |
        |fun ${refined.identifier.value}.validate() = ${refined.emitValidator()}
        |
    """.trimMargin()

    override fun Refined.emitValidator():String {
        val defaultReturn = "true"
        return when (val type = reference.type) {
            is Reference.Primitive.Type.Integer -> type.constraint?.emit() ?: defaultReturn
            is Reference.Primitive.Type.Number -> type.constraint?.emit() ?: defaultReturn
            is Reference.Primitive.Type.String -> type.constraint?.emit() ?: defaultReturn
            Reference.Primitive.Type.Boolean -> defaultReturn
            Reference.Primitive.Type.Bytes -> defaultReturn
        }
    }

}
