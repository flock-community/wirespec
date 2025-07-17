package community.flock.wirespec.emitters.wirespec

import community.flock.wirespec.compiler.core.addBackticks
import community.flock.wirespec.compiler.core.emit.Emitter
import community.flock.wirespec.compiler.core.emit.FileExtension
import community.flock.wirespec.compiler.core.emit.Keywords
import community.flock.wirespec.compiler.core.emit.Spacer
import community.flock.wirespec.compiler.core.parse.Channel
import community.flock.wirespec.compiler.core.parse.DefinitionIdentifier
import community.flock.wirespec.compiler.core.parse.Endpoint
import community.flock.wirespec.compiler.core.parse.Enum
import community.flock.wirespec.compiler.core.parse.Field
import community.flock.wirespec.compiler.core.parse.FieldIdentifier
import community.flock.wirespec.compiler.core.parse.Identifier
import community.flock.wirespec.compiler.core.parse.Module
import community.flock.wirespec.compiler.core.parse.Reference
import community.flock.wirespec.compiler.core.parse.Refined
import community.flock.wirespec.compiler.core.parse.Type
import community.flock.wirespec.compiler.core.parse.Union

interface E:
    WirespecIdentifierEmitter,
    WirespecTypeDefinitionEmitter,
    WirespecEndpointDefinitionEmitter,
    WirespecChannelDefinitionEmitter,
    WirespecEnumDefinitionEmitter,
    WirespecUnionDefinitionEmitter,
    WirespecRefinedTypeDefinitionEmitter

open class WirespecEmitter : Emitter(), E {

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
