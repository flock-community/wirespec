package community.flock.wirespec.compiler.core.parse

import arrow.core.Either
import arrow.core.raise.either
import community.flock.wirespec.compiler.core.exceptions.WirespecException
import community.flock.wirespec.compiler.core.exceptions.WrongTokenException
import community.flock.wirespec.compiler.core.tokenize.Arrow
import community.flock.wirespec.compiler.core.tokenize.Colon
import community.flock.wirespec.compiler.core.tokenize.LeftCurly
import community.flock.wirespec.compiler.core.tokenize.WirespecType

object ChannelParser {

    fun TokenProvider.parseChannel(comment: Comment?): Either<WirespecException, Channel> = either {
        eatToken().bind()
        token.log()
        when (token.type) {
            is WirespecType -> parseChannelDefinition(comment, DefinitionIdentifier(token.value)).bind()
            else -> raise(WrongTokenException<WirespecType>(token).also { eatToken().bind() })
        }
    }

    private fun TokenProvider.parseChannelDefinition(comment: Comment?, identifier: DefinitionIdentifier) = either {
        eatToken().bind()

        when (token.type) {
            is Arrow -> eatToken().bind()
            else -> raise(WrongTokenException<Colon>(token).also { eatToken().bind() })
        }

        val reference = with(TypeParser) {
            when (val type = token.type) {
                is LeftCurly -> parseDict().bind()
                is WirespecType -> parseWirespecType(type).bind()
                else -> raise(WrongTokenException<WirespecType>(token).also { eatToken().bind() })
            }
        }

        Channel(
            comment = comment,
            identifier = identifier,
            reference = reference,
        )
    }
}
