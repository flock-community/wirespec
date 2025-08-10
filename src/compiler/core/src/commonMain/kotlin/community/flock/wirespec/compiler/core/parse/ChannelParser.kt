package community.flock.wirespec.compiler.core.parse

import arrow.core.Either
import community.flock.wirespec.compiler.core.exceptions.WirespecException
import community.flock.wirespec.compiler.core.tokenize.Arrow
import community.flock.wirespec.compiler.core.tokenize.Colon
import community.flock.wirespec.compiler.core.tokenize.LeftCurly
import community.flock.wirespec.compiler.core.tokenize.WirespecType

object ChannelParser {

    fun TokenProvider.parseChannel(comment: Comment?, annotations: List<Annotation>): Either<WirespecException, Channel> = parseToken {
        when (token.type) {
            is WirespecType -> parseChannelDefinition(comment, annotations, DefinitionIdentifier(token.value)).bind()
            else -> raiseWrongToken<WirespecType>().bind()
        }
    }

    private fun TokenProvider.parseChannelDefinition(comment: Comment?, annotations: List<Annotation>, identifier: DefinitionIdentifier) = parseToken {
        when (token.type) {
            is Arrow -> eatToken().bind()
            else -> raiseWrongToken<Colon>().bind()
        }

        val reference = with(TypeParser) {
            when (token.type) {
                is LeftCurly -> parseDict().bind()
                is WirespecType -> parseType().bind()
                else -> raiseWrongToken<WirespecType>().bind()
            }
        }

        Channel(
            comment = comment,
            annotations = annotations,
            identifier = identifier,
            reference = reference,
        )
    }
}
