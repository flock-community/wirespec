package community.flock.wirespec.emitters.kotlin

import community.flock.wirespec.compiler.core.emit.IdentifierEmitter
import community.flock.wirespec.compiler.core.emit.RefinedTypeDefinitionEmitter
import community.flock.wirespec.compiler.core.emit.Spacer
import community.flock.wirespec.compiler.core.emit.TypeDefinitionEmitter
import community.flock.wirespec.compiler.core.parse.Reference
import community.flock.wirespec.compiler.core.parse.Refined

interface KotlinRefinedTypeDefinitionEmitter: RefinedTypeDefinitionEmitter, IdentifierEmitter, TypeDefinitionEmitter {

    override fun emit(refined: Refined) = """
        |data class ${refined.identifier.sanitize()}(override val value: String): Wirespec.Refined {
        |${Spacer}override fun toString() = value
        |}
        |
        |fun ${refined.identifier.value}.validate() = ${refined.emitValidator()}
        |
    """.trimMargin()



}