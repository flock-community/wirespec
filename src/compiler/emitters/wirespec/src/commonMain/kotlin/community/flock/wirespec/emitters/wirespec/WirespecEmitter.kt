package community.flock.wirespec.emitters.wirespec

import community.flock.wirespec.compiler.core.emit.LanguageEmitter
import community.flock.wirespec.compiler.core.emit.FileExtension
import community.flock.wirespec.compiler.core.emit.Keywords
import community.flock.wirespec.compiler.core.parse.Reference

interface E:
    WirespecIdentifierEmitter,
    WirespecTypeDefinitionEmitter,
    WirespecEndpointDefinitionEmitter,
    WirespecChannelDefinitionEmitter,
    WirespecEnumDefinitionEmitter,
    WirespecUnionDefinitionEmitter,
    WirespecRefinedTypeDefinitionEmitter

open class WirespecEmitter : LanguageEmitter(), E {

    override val extension = FileExtension.Wirespec

    override val shared = null

    override val singleLineComment = "\n"

    override fun notYetImplemented() = singleLineComment

    override fun Reference.Primitive.Type.Constraint.emit() = when(this){
        is Reference.Primitive.Type.Constraint.RegExp -> "(${value})"
        is Reference.Primitive.Type.Constraint.Bound -> "(${min}, ${max})"
    }

    companion object : Keywords {
        override val reservedKeywords = setOf(
            "type", "enum", "endpoint"
        )
    }
}
