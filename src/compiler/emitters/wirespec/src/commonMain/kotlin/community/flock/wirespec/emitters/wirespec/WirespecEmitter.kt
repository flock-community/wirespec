package community.flock.wirespec.emitters.wirespec

import arrow.core.NonEmptyList
import arrow.core.nel
import community.flock.wirespec.compiler.core.emit.Emitted
import community.flock.wirespec.compiler.core.emit.LanguageEmitter
import community.flock.wirespec.compiler.core.emit.FileExtension
import community.flock.wirespec.compiler.core.parse.AST
import community.flock.wirespec.compiler.core.parse.ast.Reference
import community.flock.wirespec.compiler.utils.Logger

interface WirespecEmitters:
    WirespecIdentifierEmitter,
    WirespecTypeDefinitionEmitter,
    WirespecEndpointDefinitionEmitter,
    WirespecChannelDefinitionEmitter,
    WirespecEnumDefinitionEmitter,
    WirespecUnionDefinitionEmitter,
    WirespecRefinedTypeDefinitionEmitter

open class WirespecEmitter : LanguageEmitter(), WirespecEmitters {

    override val extension = FileExtension.Wirespec

    override val shared = null

    override val singleLineComment = "\n"

    override fun notYetImplemented() = singleLineComment

    override fun emit(ast: AST, logger: Logger): NonEmptyList<Emitted> =
        super<LanguageEmitter>.emit(ast, logger)
            .let { e -> Emitted("wirespec.${extension.value}", e.map {it.result }.joinToString("\n")).nel() }

    override fun Reference.Primitive.Type.Constraint.emit() = when(this){
        is Reference.Primitive.Type.Constraint.RegExp -> "(${value})"
        is Reference.Primitive.Type.Constraint.Bound -> "(${min}, ${max})"
    }
}
