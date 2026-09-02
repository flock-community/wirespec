package community.flock.wirespec.emitters.wirespec

import arrow.core.NonEmptyList
import arrow.core.nel
import community.flock.wirespec.compiler.core.emit.Emitted
import community.flock.wirespec.compiler.core.emit.LanguageEmitter
import community.flock.wirespec.compiler.core.emit.FileExtension
import community.flock.wirespec.compiler.core.emit.Shared
import community.flock.wirespec.compiler.core.parse.ast.AST
import community.flock.wirespec.compiler.core.parse.ast.Reference
import community.flock.wirespec.compiler.utils.Logger

private interface WirespecEmitters:
    WirespecIdentifierEmitter,
    WirespecTypeDefinitionEmitter,
    WirespecEndpointDefinitionEmitter,
    WirespecChannelDefinitionEmitter,
    WirespecRpcDefinitionEmitter,
    WirespecEnumDefinitionEmitter,
    WirespecUnionDefinitionEmitter,
    WirespecRefinedTypeDefinitionEmitter

public open class WirespecEmitter : LanguageEmitter(), WirespecEmitters {

    override val extension: FileExtension = FileExtension.Wirespec

    override val shared: Shared? = null

    override val singleLineComment: String = "\n"

    override fun notYetImplemented(): String = singleLineComment

    override fun emit(ast: AST, logger: Logger): NonEmptyList<Emitted> =
        super<LanguageEmitter>.emit(ast, logger)
            .let { e -> Emitted("wirespec.${extension.value}", e.map {it.result }.joinToString("\n")).nel() }

    override fun Reference.Primitive.Type.Constraint.emit(): String = when(this){
        is Reference.Primitive.Type.Constraint.RegExp -> "(${value})"
        is Reference.Primitive.Type.Constraint.Bound -> "(${min ?: "_"}, ${max ?: "_"})"
    }
}
