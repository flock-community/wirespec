package community.flock.wirespec.compiler.core.parse

import arrow.core.Either
import community.flock.wirespec.compiler.core.exceptions.WirespecException
import community.flock.wirespec.compiler.core.tokenize.Arrow
import community.flock.wirespec.compiler.core.tokenize.Colon
import community.flock.wirespec.compiler.core.tokenize.LeftCurly
import community.flock.wirespec.compiler.core.tokenize.WirespecType

object ChannelParser {

    fun TokenProvider.parseChannel(comment: Comment?): Either<WirespecException, Channel> = parseToken {
        when (token.type) {
            is WirespecType -> parseChannelDefinition(comment, DefinitionIdentifier(token.value)).bind()
            else -> raiseWrongToken<WirespecType>().bind()
        }
    }

    private fun TokenProvider.parseChannelDefinition(comment: Comment?, identifier: DefinitionIdentifier) = parseToken {
        when (token.type) {
            is Arrow -> eatToken().bind()
            else -> raiseWrongToken<Colon>().bind()
        }

        val reference = with(TypeParser) {
            when (val type = token.type) {
                is LeftCurly -> parseDict().bind()
                is WirespecType -> parseWirespecType(type).bind()
                else -> raiseWrongToken<WirespecType>().bind()
            }
        }

        Channel(
            comment = comment,
            identifier = identifier,
            reference = reference,
        )
    }
}
